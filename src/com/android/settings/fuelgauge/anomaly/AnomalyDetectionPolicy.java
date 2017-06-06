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
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.text.format.DateUtils;
import android.util.KeyValueListParser;
import android.util.Log;

/**
 * Class to store the policy for anomaly detection, which comes from
 * {@link android.provider.Settings.Global}
 */
public class AnomalyDetectionPolicy {
    public static final String TAG = "AnomalyDetectionPolicy";

    @VisibleForTesting
    static final String KEY_ANOMALY_DETECTION_ENABLED = "anomaly_detection_enabled";
    @VisibleForTesting
    static final String KEY_WAKELOCK_DETECTION_ENABLED = "wakelock_enabled";
    @VisibleForTesting
    static final String KEY_WAKEUP_ALARM_DETECTION_ENABLED = "wakeup_alarm_enabled";
    @VisibleForTesting
    static final String KEY_WAKELOCK_THRESHOLD = "wakelock_threshold";
    @VisibleForTesting
    static final String KEY_WAKEUP_ALARM_THRESHOLD = "wakeup_alarm_threshold";

    /**
     * {@code true} if general anomaly detection is enabled
     *
     * @see Settings.Global#ANOMALY_DETECTION_CONSTANTS
     * @see #KEY_ANOMALY_DETECTION_ENABLED
     */
    public final boolean anomalyDetectionEnabled;

    /**
     * {@code true} if wakelock anomaly detection is enabled
     *
     * @see Settings.Global#ANOMALY_DETECTION_CONSTANTS
     * @see #KEY_WAKELOCK_DETECTION_ENABLED
     */
    public final boolean wakeLockDetectionEnabled;

    /**
     * {@code true} if wakeup alarm detection is enabled
     *
     * @see Settings.Global#ANOMALY_DETECTION_CONSTANTS
     * @see #KEY_WAKEUP_ALARM_DETECTION_ENABLED
     */
    public final boolean wakeupAlarmDetectionEnabled;

    /**
     * Threshold for wakelock time in milli seconds
     *
     * @see Settings.Global#ANOMALY_DETECTION_CONSTANTS
     * @see #KEY_WAKELOCK_THRESHOLD
     */
    public final long wakeLockThreshold;

    /**
     * Threshold for wakeup alarm count per hour
     *
     * @see Settings.Global#ANOMALY_DETECTION_CONSTANTS
     * @see #KEY_WAKEUP_ALARM_THRESHOLD
     */
    public final long wakeupAlarmThreshold;

    private final KeyValueListParserWrapper mParserWrapper;

    public AnomalyDetectionPolicy(Context context) {
        this(context, new KeyValueListParserWrapperImpl(new KeyValueListParser(',')));
    }

    @VisibleForTesting
    AnomalyDetectionPolicy(Context context, KeyValueListParserWrapper parserWrapper) {
        mParserWrapper = parserWrapper;
        final String value = Settings.Global.getString(context.getContentResolver(),
                Settings.Global.ANOMALY_DETECTION_CONSTANTS);

        try {
            mParserWrapper.setString(value);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Bad anomaly detection constants");
        }

        anomalyDetectionEnabled = mParserWrapper.getBoolean(KEY_ANOMALY_DETECTION_ENABLED, true);
        wakeLockDetectionEnabled = mParserWrapper.getBoolean(KEY_WAKELOCK_DETECTION_ENABLED, true);
        wakeupAlarmDetectionEnabled = mParserWrapper.getBoolean(KEY_WAKEUP_ALARM_DETECTION_ENABLED,
                true);
        wakeLockThreshold = mParserWrapper.getLong(KEY_WAKELOCK_THRESHOLD,
                DateUtils.HOUR_IN_MILLIS);
        wakeupAlarmThreshold = mParserWrapper.getLong(KEY_WAKEUP_ALARM_THRESHOLD, 60);
    }

    public boolean isAnomalyDetectorEnabled(@Anomaly.AnomalyType int type) {
        switch (type) {
            case Anomaly.AnomalyType.WAKE_LOCK:
                return wakeLockDetectionEnabled;
            case Anomaly.AnomalyType.WAKEUP_ALARM:
                return wakeupAlarmDetectionEnabled;
            default:
                return false; // Disabled when no this type
        }
    }
}
