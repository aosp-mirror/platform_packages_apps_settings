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

import android.util.KeyValueListParser;
import android.util.Log;

/** Model class to parse and store anomaly info from statsd. */
public class AnomalyInfo {
    private static final String TAG = "AnomalyInfo";

    private static final String KEY_ANOMALY_TYPE = "anomaly_type";
    private static final String KEY_AUTO_RESTRICTION = "auto_restriction";
    public final Integer anomalyType;
    public final boolean autoRestriction;

    public AnomalyInfo(String info) {
        Log.i(TAG, "anomalyInfo: " + info);
        KeyValueListParser parser = new KeyValueListParser(',');
        parser.setString(info);
        anomalyType = parser.getInt(KEY_ANOMALY_TYPE, -1);
        autoRestriction = parser.getBoolean(KEY_AUTO_RESTRICTION, false);
    }
}
