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

import com.android.settings.fuelgauge.anomaly.action.AnomalyAction;
import com.android.settings.fuelgauge.anomaly.action.ForceStopAction;

/**
 * Utitily class for anomaly detection
 */
public class AnomalyUtils {

    /**
     * Return the corresponding {@link AnomalyAction} according to {@link AnomalyType}
     *
     * @return corresponding {@link AnomalyAction}, or null if cannot find it.
     */
    public static final AnomalyAction getAnomalyAction(int anomalyType) {
        switch (anomalyType) {
            case Anomaly.AnomalyType.WAKE_LOCK:
                return new ForceStopAction();
            default:
                return null;
        }
    }
}
