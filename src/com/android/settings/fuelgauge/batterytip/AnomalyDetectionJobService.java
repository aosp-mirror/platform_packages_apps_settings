/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.fuelgauge.batterytip;

import static android.os.StatsDimensionsValue.INT_VALUE_TYPE;
import static android.os.StatsDimensionsValue.TUPLE_VALUE_TYPE;

import android.app.AppOpsManager;
import android.app.StatsManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.app.job.JobWorkItem;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.StatsDimensionsValue;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.VisibleForTesting;

import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.fuelgauge.PowerWhitelistBackend;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** A JobService to store anomaly data to anomaly database */
public class AnomalyDetectionJobService extends JobService {
    private static final String TAG = "AnomalyDetectionService";
    private static final int ON = 1;
    @VisibleForTesting
    static final int UID_NULL = -1;
    @VisibleForTesting
    static final int STATSD_UID_FILED = 1;
    @VisibleForTesting
    static final long MAX_DELAY_MS = TimeUnit.MINUTES.toMillis(30);

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    @VisibleForTesting
    boolean mIsJobCanceled = false;

    public static void scheduleAnomalyDetection(Context context, Intent intent) {
        final JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        final ComponentName component = new ComponentName(context,
                AnomalyDetectionJobService.class);
        final JobInfo.Builder jobBuilder =
                new JobInfo.Builder(R.integer.job_anomaly_detection, component)
                        .setOverrideDeadline(MAX_DELAY_MS);

        if (jobScheduler.enqueue(jobBuilder.build(), new JobWorkItem(intent))
                != JobScheduler.RESULT_SUCCESS) {
            Log.i(TAG, "Anomaly detection job service enqueue failed.");
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        synchronized (mLock) {
            mIsJobCanceled = false;
        }
        ThreadUtils.postOnBackgroundThread(() -> {
            final Context context = AnomalyDetectionJobService.this;
            final BatteryDatabaseManager batteryDatabaseManager =
                    BatteryDatabaseManager.getInstance(this);
            final BatteryTipPolicy policy = new BatteryTipPolicy(this);
            final BatteryUtils batteryUtils = BatteryUtils.getInstance(this);
            final ContentResolver contentResolver = getContentResolver();
            final UserManager userManager = getSystemService(UserManager.class);
            final PowerWhitelistBackend powerWhitelistBackend =
                    PowerWhitelistBackend.getInstance(context);
            final PowerUsageFeatureProvider powerUsageFeatureProvider = FeatureFactory
                    .getFactory(this).getPowerUsageFeatureProvider(this);
            final MetricsFeatureProvider metricsFeatureProvider = FeatureFactory
                    .getFactory(this).getMetricsFeatureProvider();

            for (JobWorkItem item = dequeueWork(params); item != null; item = dequeueWork(params)) {
                saveAnomalyToDatabase(context, userManager,
                        batteryDatabaseManager, batteryUtils, policy, powerWhitelistBackend,
                        contentResolver, powerUsageFeatureProvider, metricsFeatureProvider,
                        item.getIntent().getExtras());

                completeWork(params, item);
            }
        });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        synchronized (mLock) {
            mIsJobCanceled = true;
        }
        return true; // Need to reschedule
    }

    @VisibleForTesting
    void saveAnomalyToDatabase(Context context, UserManager userManager,
            BatteryDatabaseManager databaseManager, BatteryUtils batteryUtils,
            BatteryTipPolicy policy, PowerWhitelistBackend powerWhitelistBackend,
            ContentResolver contentResolver, PowerUsageFeatureProvider powerUsageFeatureProvider,
            MetricsFeatureProvider metricsFeatureProvider, Bundle bundle) {
        // The Example of intentDimsValue is: 35:{1:{1:{1:10013|}|}|}
        final StatsDimensionsValue intentDimsValue =
                bundle.getParcelable(StatsManager.EXTRA_STATS_DIMENSIONS_VALUE);
        final long timeMs = bundle.getLong(AnomalyDetectionReceiver.KEY_ANOMALY_TIMESTAMP,
                System.currentTimeMillis());
        final ArrayList<String> cookies = bundle.getStringArrayList(
                StatsManager.EXTRA_STATS_BROADCAST_SUBSCRIBER_COOKIES);
        final AnomalyInfo anomalyInfo = new AnomalyInfo(
                !ArrayUtils.isEmpty(cookies) ? cookies.get(0) : "");
        Log.i(TAG, "Extra stats value: " + intentDimsValue.toString());

        try {
            final int uid = extractUidFromStatsDimensionsValue(intentDimsValue);
            final boolean autoFeatureOn = powerUsageFeatureProvider.isSmartBatterySupported()
                    ? Settings.Global.getInt(contentResolver,
                    Settings.Global.ADAPTIVE_BATTERY_MANAGEMENT_ENABLED, ON) == ON
                    : Settings.Global.getInt(contentResolver,
                            Settings.Global.APP_AUTO_RESTRICTION_ENABLED, ON) == ON;
            final String packageName = batteryUtils.getPackageName(uid);
            final long versionCode = batteryUtils.getAppLongVersionCode(packageName);
            final String versionedPackage = packageName + "/" + versionCode;
            if (batteryUtils.shouldHideAnomaly(powerWhitelistBackend, uid, anomalyInfo)) {
                metricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                        SettingsEnums.ACTION_ANOMALY_IGNORED,
                        SettingsEnums.PAGE_UNKNOWN,
                        versionedPackage,
                        anomalyInfo.anomalyType);
            } else {
                if (autoFeatureOn && anomalyInfo.autoRestriction) {
                    // Auto restrict this app
                    batteryUtils.setForceAppStandby(uid, packageName,
                            AppOpsManager.MODE_IGNORED);
                    databaseManager.insertAnomaly(uid, packageName, anomalyInfo.anomalyType,
                            AnomalyDatabaseHelper.State.AUTO_HANDLED,
                            timeMs);
                } else {
                    databaseManager.insertAnomaly(uid, packageName, anomalyInfo.anomalyType,
                            AnomalyDatabaseHelper.State.NEW,
                            timeMs);
                }
                metricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                        SettingsEnums.ACTION_ANOMALY_TRIGGERED,
                        SettingsEnums.PAGE_UNKNOWN,
                        versionedPackage,
                        anomalyInfo.anomalyType);
            }

        } catch (NullPointerException | IndexOutOfBoundsException e) {
            Log.e(TAG, "Parse stats dimensions value error.", e);
        }
    }

    /**
     * Extract the uid from {@link StatsDimensionsValue}
     *
     * The uid dimension has the format: 1:<int> inside the tuple list. Here are some examples:
     * 1. Excessive bg anomaly: 27:{1:10089|}
     * 2. Wakeup alarm anomaly: 35:{1:{1:{1:10013|}|}|}
     * 3. Bluetooth anomaly:    3:{1:{1:{1:10140|}|}|}
     */
    @VisibleForTesting
    int extractUidFromStatsDimensionsValue(StatsDimensionsValue statsDimensionsValue) {
        if (statsDimensionsValue == null) {
            return UID_NULL;
        }
        if (statsDimensionsValue.isValueType(INT_VALUE_TYPE)
                && statsDimensionsValue.getField() == STATSD_UID_FILED) {
            // Find out the real uid
            return statsDimensionsValue.getIntValue();
        }
        if (statsDimensionsValue.isValueType(TUPLE_VALUE_TYPE)) {
            final List<StatsDimensionsValue> values = statsDimensionsValue.getTupleValueList();
            for (int i = 0, size = values.size(); i < size; i++) {
                int uid = extractUidFromStatsDimensionsValue(values.get(i));
                if (uid != UID_NULL) {
                    return uid;
                }
            }
        }

        return UID_NULL;
    }

    @VisibleForTesting
    JobWorkItem dequeueWork(JobParameters parameters) {
        synchronized (mLock) {
            if (mIsJobCanceled) {
                return null;
            }

            return parameters.dequeueWork();
        }
    }

    @VisibleForTesting
    void completeWork(JobParameters parameters, JobWorkItem item) {
        synchronized (mLock) {
            if (mIsJobCanceled) {
                return;
            }

            parameters.completeWork(item);
        }
    }
}
