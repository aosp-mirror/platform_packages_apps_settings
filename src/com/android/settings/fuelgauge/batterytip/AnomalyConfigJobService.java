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

import android.app.StatsManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.TimeUnit;

/** A JobService check whether to update the anomaly config periodically */
public class AnomalyConfigJobService extends JobService {
    private static final String TAG = "AnomalyConfigJobService";

    public static final String PREF_DB = "anomaly_pref";
    public static final String KEY_ANOMALY_CONFIG_VERSION = "anomaly_config_version";
    private static final int DEFAULT_VERSION = 0;

    @VisibleForTesting static final long CONFIG_UPDATE_FREQUENCY_MS = TimeUnit.DAYS.toMillis(1);

    public static void scheduleConfigUpdate(Context context) {
        final JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);

        final ComponentName component = new ComponentName(context, AnomalyConfigJobService.class);
        final JobInfo.Builder jobBuilder =
                new JobInfo.Builder(R.integer.job_anomaly_config_update, component)
                        .setPeriodic(CONFIG_UPDATE_FREQUENCY_MS)
                        .setRequiresDeviceIdle(true)
                        .setRequiresCharging(true)
                        .setPersisted(true);
        final JobInfo pending = jobScheduler.getPendingJob(R.integer.job_anomaly_config_update);

        // Don't schedule it if it already exists, to make sure it runs periodically even after
        // reboot
        if (pending == null
                && jobScheduler.schedule(jobBuilder.build()) != JobScheduler.RESULT_SUCCESS) {
            Log.i(TAG, "Anomaly config update job service schedule failed.");
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        ThreadUtils.postOnBackgroundThread(
                () -> {
                    final StatsManager statsManager = getSystemService(StatsManager.class);
                    checkAnomalyConfig(statsManager);
                    try {
                        BatteryTipUtils.uploadAnomalyPendingIntent(this, statsManager);
                    } catch (StatsManager.StatsUnavailableException e) {
                        Log.w(TAG, "Failed to uploadAnomalyPendingIntent.", e);
                    }
                    jobFinished(params, false /* wantsReschedule */);
                });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    @VisibleForTesting
    synchronized void checkAnomalyConfig(StatsManager statsManager) {
        final SharedPreferences sharedPreferences =
                getSharedPreferences(PREF_DB, Context.MODE_PRIVATE);
        final int currentVersion =
                sharedPreferences.getInt(KEY_ANOMALY_CONFIG_VERSION, DEFAULT_VERSION);
        final int newVersion =
                Settings.Global.getInt(
                        getContentResolver(),
                        Settings.Global.ANOMALY_CONFIG_VERSION,
                        DEFAULT_VERSION);
        final String rawConfig =
                Settings.Global.getString(getContentResolver(), Settings.Global.ANOMALY_CONFIG);
        Log.i(TAG, "CurrentVersion: " + currentVersion + " new version: " + newVersion);

        if (newVersion > currentVersion) {
            try {
                statsManager.removeConfig(StatsManagerConfig.ANOMALY_CONFIG_KEY);
            } catch (StatsManager.StatsUnavailableException e) {
                Log.i(
                        TAG,
                        "When updating anomaly config, failed to first remove the old config "
                                + StatsManagerConfig.ANOMALY_CONFIG_KEY,
                        e);
            }
            if (!TextUtils.isEmpty(rawConfig)) {
                try {
                    final byte[] config = Base64.decode(rawConfig, Base64.DEFAULT);
                    statsManager.addConfig(StatsManagerConfig.ANOMALY_CONFIG_KEY, config);
                    Log.i(
                            TAG,
                            "Upload the anomaly config. configKey: "
                                    + StatsManagerConfig.ANOMALY_CONFIG_KEY);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt(KEY_ANOMALY_CONFIG_VERSION, newVersion);
                    editor.commit();
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Anomaly raw config is in wrong format", e);
                } catch (StatsManager.StatsUnavailableException e) {
                    Log.i(
                            TAG,
                            "Upload of anomaly config failed for configKey "
                                    + StatsManagerConfig.ANOMALY_CONFIG_KEY,
                            e);
                }
            }
        }
    }
}
