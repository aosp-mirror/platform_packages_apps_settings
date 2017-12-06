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

package com.android.settings.anomaly.tester.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder to build the anomaly policy string, used in {@link android.provider.Settings.Global}
 *
 * @see android.provider.Settings.Global#ANOMALY_DETECTION_CONSTANTS
 */
public class AnomalyPolicyBuilder {
    public static final String KEY_ANOMALY_DETECTION_ENABLED = "anomaly_detection_enabled";
    public static final String KEY_WAKELOCK_DETECTION_ENABLED = "wakelock_enabled";
    public static final String KEY_WAKEUP_ALARM_DETECTION_ENABLED = "wakeup_alarm_enabled";
    public static final String KEY_BLUETOOTH_SCAN_DETECTION_ENABLED = "bluetooth_scan_enabled";
    public static final String KEY_WAKELOCK_THRESHOLD = "wakelock_threshold";
    public static final String KEY_WAKEUP_ALARM_THRESHOLD = "wakeup_alarm_threshold";
    public static final String KEY_BLUETOOTH_SCAN_THRESHOLD = "bluetooth_scan_threshold";

    public static final String DELIM = ",";

    private Map<String, String> mValues;

    public AnomalyPolicyBuilder() {
        mValues = new HashMap<>();
    }

    public AnomalyPolicyBuilder addPolicy(String key, String value) {
        mValues.put(key, value);
        return this;
    }

    public AnomalyPolicyBuilder addPolicy(String key, long value) {
        mValues.put(key, Long.toString(value));
        return this;
    }


    public AnomalyPolicyBuilder addPolicy(String key, boolean value) {
        mValues.put(key, value ? "true" : "false");
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : mValues.entrySet()) {
            sb.append(entry.getKey() + "=" + entry.getValue() + DELIM);
        }

        if (sb.length() != 0) {
            return sb.substring(0, sb.length() - 1);
        } else {
            return "";
        }
    }
}
