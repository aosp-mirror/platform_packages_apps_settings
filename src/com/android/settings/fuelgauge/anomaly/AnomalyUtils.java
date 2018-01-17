/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.fuelgauge.anomaly;

import android.content.Context;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.util.Pair;
import android.util.SparseIntArray;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.fuelgauge.anomaly.action.AnomalyAction;
import com.android.settings.fuelgauge.anomaly.action.ForceStopAction;
import com.android.settings.fuelgauge.anomaly.action.LocationCheckAction;
import com.android.settings.fuelgauge.anomaly.action.StopAndBackgroundCheckAction;
import com.android.settings.fuelgauge.anomaly.checker.AnomalyDetector;
import com.android.settings.fuelgauge.anomaly.checker.BluetoothScanAnomalyDetector;
import com.android.settings.fuelgauge.anomaly.checker.WakeLockAnomalyDetector;
import com.android.settings.fuelgauge.anomaly.checker.WakeupAlarmAnomalyDetector;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for anomaly detection
 */
public class AnomalyUtils {
    private Context mContext;
    private static AnomalyUtils sInstance;

    private static final SparseIntArray mMetricArray;
    static {
        mMetricArray = new SparseIntArray();
        mMetricArray.append(Anomaly.AnomalyType.WAKE_LOCK,
                MetricsProto.MetricsEvent.ANOMALY_TYPE_WAKELOCK);
        mMetricArray.append(Anomaly.AnomalyType.WAKEUP_ALARM,
                MetricsProto.MetricsEvent.ANOMALY_TYPE_WAKEUP_ALARM);
        mMetricArray.append(Anomaly.AnomalyType.BLUETOOTH_SCAN,
                MetricsProto.MetricsEvent.ANOMALY_TYPE_UNOPTIMIZED_BT);
    }

    @VisibleForTesting
    AnomalyUtils(Context context) {
        mContext = context.getApplicationContext();
    }

    public static AnomalyUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AnomalyUtils(context);
        }
        return sInstance;
    }

    /**
     * Return the corresponding {@link AnomalyAction} according to
     * {@link com.android.settings.fuelgauge.anomaly.Anomaly}
     *
     * @return corresponding {@link AnomalyAction}, or null if cannot find it.
     */
    public AnomalyAction getAnomalyAction(Anomaly anomaly) {
        switch (anomaly.type) {
            case Anomaly.AnomalyType.WAKE_LOCK:
                return new ForceStopAction(mContext);
            case Anomaly.AnomalyType.WAKEUP_ALARM:
                if (anomaly.targetSdkVersion >= Build.VERSION_CODES.O
                        || (anomaly.targetSdkVersion < Build.VERSION_CODES.O
                                && anomaly.backgroundRestrictionEnabled)) {
                    return new ForceStopAction(mContext);
                } else {
                    return new StopAndBackgroundCheckAction(mContext);
                }
            case Anomaly.AnomalyType.BLUETOOTH_SCAN:
                return new LocationCheckAction(mContext);
            default:
                return null;
        }
    }

    /**
     * Return the corresponding {@link AnomalyDetector} according to
     * {@link com.android.settings.fuelgauge.anomaly.Anomaly.AnomalyType}
     *
     * @return corresponding {@link AnomalyDetector}, or null if cannot find it.
     */
    public AnomalyDetector getAnomalyDetector(@Anomaly.AnomalyType int anomalyType) {
        switch (anomalyType) {
            case Anomaly.AnomalyType.WAKE_LOCK:
                return new WakeLockAnomalyDetector(mContext);
            case Anomaly.AnomalyType.WAKEUP_ALARM:
                return new WakeupAlarmAnomalyDetector(mContext);
            case Anomaly.AnomalyType.BLUETOOTH_SCAN:
                return new BluetoothScanAnomalyDetector(mContext);
            default:
                return null;
        }
    }

    /**
     * Detect whether application with {@code targetPackageName} has anomaly. When
     * {@code targetPackageName} is null, start detection among all the applications.
     *
     * @param batteryStatsHelper contains battery stats, used to detect anomaly
     * @param policy             contains configuration about anomaly check
     * @param targetPackageName  represents the app need to be detected
     * @return the list of anomalies
     */
    public List<Anomaly> detectAnomalies(BatteryStatsHelper batteryStatsHelper,
            AnomalyDetectionPolicy policy, String targetPackageName) {
        final List<Anomaly> anomalies = new ArrayList<>();
        for (@Anomaly.AnomalyType int type : Anomaly.ANOMALY_TYPE_LIST) {
            if (policy.isAnomalyDetectorEnabled(type)) {
                anomalies.addAll(getAnomalyDetector(type).detectAnomalies(
                        batteryStatsHelper, targetPackageName));
            }
        }

        return anomalies;
    }

    /**
     * Log the list of {@link Anomaly} using {@link MetricsFeatureProvider}, which contains
     * anomaly type, package name, field_context, field_action_type
     *
     * @param provider  provider to do the logging
     * @param anomalies contains the data to log
     * @param contextId which page invoke this logging
     * @see #logAnomaly(MetricsFeatureProvider, Anomaly, int)
     */
    public void logAnomalies(MetricsFeatureProvider provider, List<Anomaly> anomalies,
            int contextId) {
        for (int i = 0, size = anomalies.size(); i < size; i++) {
            logAnomaly(provider, anomalies.get(i), contextId);
        }
    }

    /**
     * Log the {@link Anomaly} using {@link MetricsFeatureProvider}, which contains
     * anomaly type, package name, field_context, field_action_type
     *
     * @param provider  provider to do the logging
     * @param anomaly   contains the data to log
     * @param contextId which page invoke this logging
     * @see #logAnomalies(MetricsFeatureProvider, List, int)
     */
    public void logAnomaly(MetricsFeatureProvider provider, Anomaly anomaly, int contextId) {
        provider.action(
                mContext,
                mMetricArray.get(anomaly.type, MetricsProto.MetricsEvent.VIEW_UNKNOWN),
                anomaly.packageName,
                Pair.create(MetricsProto.MetricsEvent.FIELD_CONTEXT, contextId),
                Pair.create(MetricsProto.MetricsEvent.FIELD_ANOMALY_ACTION_TYPE,
                        getAnomalyAction(anomaly).getActionType()));
    }

}
