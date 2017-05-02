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

package com.android.settings.fuelgauge.anomaly.checker;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.fuelgauge.anomaly.Anomaly;

import java.util.ArrayList;
import java.util.List;

/**
 * Check whether apps holding wakelock too long
 */
public class WakeLockAnomalyDetector implements AnomalyDetector {

    @Override
    public List<Anomaly> detectAnomalies(BatteryStatsHelper batteryStatsHelper) {
        //TODO(b/36921529): check anomaly using the batteryStatsHelper
        final List<Anomaly> anomalies = new ArrayList<>();
        return anomalies;
    }
}
