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
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.text.format.DateUtils;
import android.util.KeyValueListParser;
import android.util.Log;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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
    static final String KEY_BLUETOOTH_SCAN_DETECTION_ENABLED = "bluetooth_scan_enabled";
    @VisibleForTesting
    static final String KEY_WAKELOCK_THRESHOLD = "wakelock_threshold";
    @VisibleForTesting
    static final String KEY_WAKEUP_ALARM_THRESHOLD = "wakeup_alarm_threshold";
    @VisibleForTesting
    static final String KEY_WAKEUP_BLACKLISTED_TAGS = "wakeup_blacklisted_tags";
    @VisibleForTesting
    static final String KEY_BLUETOOTH_SCAN_THRESHOLD = "bluetooth_scan_threshold";

    /**
     * {@code true} if general anomaly detection is enabled
     *
     * @see Settings.Global#ANOMALY_DETECTION_CONSTANTS
     * @see #KEY_ANOMALY_DETECTION_ENABLED
     */
    final boolean anomalyDetectionEnabled;

    /**
     * {@code true} if wakelock anomaly detection is enabled
     *
     * @see Settings.Global#ANOMALY_DETECTION_CONSTANTS
     * @see #KEY_WAKELOCK_DETECTION_ENABLED
     */
    final boolean wakeLockDetectionEnabled;

    /**
     * {@code true} if wakeup alarm detection is enabled
     *
     * @see Settings.Global#ANOMALY_DETECTION_CONSTANTS
     * @see #KEY_WAKEUP_ALARM_DETECTION_ENABLED
     */
    final boolean wakeupAlarmDetectionEnabled;

    /**
     * {@code true} if bluetooth scanning detection is enabled
     *
     * @see Settings.Global#ANOMALY_DETECTION_CONSTANTS
     * @see #KEY_BLUETOOTH_SCAN_THRESHOLD
     */
    final boolean bluetoothScanDetectionEnabled;

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

    /**
     * Array of blacklisted wakeups, by tag.
     *
     * @see Settings.Global#ANOMALY_DETECTION_CONSTANTS
     * @see #KEY_WAKEUP_BLACKLISTED_TAGS
     */
    public final Set<String> wakeupBlacklistedTags;

    /**
     * Threshold for bluetooth unoptimized scanning time in milli seconds
     *
     * @see Settings.Global#ANOMALY_DETECTION_CONSTANTS
     * @see #KEY_BLUETOOTH_SCAN_THRESHOLD
     */
    public final long bluetoothScanThreshold;

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

        anomalyDetectionEnabled =
                mParserWrapper.getBoolean(KEY_ANOMALY_DETECTION_ENABLED, false);
        wakeLockDetectionEnabled =
                mParserWrapper.getBoolean(KEY_WAKELOCK_DETECTION_ENABLED,false);
        wakeupAlarmDetectionEnabled =
                mParserWrapper.getBoolean(KEY_WAKEUP_ALARM_DETECTION_ENABLED,false);
        bluetoothScanDetectionEnabled = mParserWrapper.getBoolean(
                KEY_BLUETOOTH_SCAN_DETECTION_ENABLED, false);
        wakeLockThreshold = mParserWrapper.getLong(KEY_WAKELOCK_THRESHOLD,
                DateUtils.HOUR_IN_MILLIS);
        wakeupAlarmThreshold = mParserWrapper.getLong(KEY_WAKEUP_ALARM_THRESHOLD, 10);
        wakeupBlacklistedTags = parseStringSet(KEY_WAKEUP_BLACKLISTED_TAGS, null);
        bluetoothScanThreshold = mParserWrapper.getLong(KEY_BLUETOOTH_SCAN_THRESHOLD,
                30 * DateUtils.MINUTE_IN_MILLIS);
    }

    public boolean isAnomalyDetectionEnabled() {
        return anomalyDetectionEnabled;
    }

    public boolean isAnomalyDetectorEnabled(@Anomaly.AnomalyType int type) {
        switch (type) {
            case Anomaly.AnomalyType.WAKE_LOCK:
                return wakeLockDetectionEnabled;
            case Anomaly.AnomalyType.WAKEUP_ALARM:
                return wakeupAlarmDetectionEnabled;
            case Anomaly.AnomalyType.BLUETOOTH_SCAN:
                return bluetoothScanDetectionEnabled;
            default:
                return false; // Disabled when no this type
        }
    }

    private Set<String> parseStringSet(final String key, final Set<String> defaultSet) {
        final String value = mParserWrapper.getString(key, null);
        if (value != null) {
            return Arrays.stream(value.split(":"))
                    .map(String::trim).map(Uri::decode).collect(Collectors.toSet());
        } else {
            return defaultSet;
        }
    }
}
