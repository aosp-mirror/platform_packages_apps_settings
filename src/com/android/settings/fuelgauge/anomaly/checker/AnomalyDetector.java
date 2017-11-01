/*
 * Copyright (C) 2017 The Android Open Source Project
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
 *
 *
 */

package com.android.settings.fuelgauge.anomaly.checker;

import android.annotation.Nullable;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.fuelgauge.anomaly.Anomaly;

import java.util.List;

public interface AnomalyDetector {
    /**
     * Detect whether there is anomaly among all the applications in the device
     *
     * @param batteryStatsHelper used to detect the anomaly
     * @return anomaly list
     */
    List<Anomaly> detectAnomalies(BatteryStatsHelper batteryStatsHelper);

    /**
     * Detect whether application with {@code targetPackageName} has anomaly. When
     * {@code targetPackageName} is null, start detection among all the applications.
     *
     * @param batteryStatsHelper used to detect the anomaly
     * @param targetPackageName  represents the app need to be detected
     * @return anomaly list
     */
    List<Anomaly> detectAnomalies(BatteryStatsHelper batteryStatsHelper,
            @Nullable String targetPackageName);
}
