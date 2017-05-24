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

package com.android.settings.fuelgauge.anomaly.action;

import com.android.settings.fuelgauge.anomaly.Anomaly;

/**
 * Interface for anomaly action, which is triggered if we need to handle the anomaly
 */
public interface AnomalyAction {
    /**
     * handle the action when user clicks positive button
     * @param Anomaly about the app that we need to handle
     * @param metricsKey key for the page that invokes the action
     *
     * @see com.android.internal.logging.nano.MetricsProto
     */
    void handlePositiveAction(Anomaly Anomaly, int metricsKey);
    int getActionType();
}
