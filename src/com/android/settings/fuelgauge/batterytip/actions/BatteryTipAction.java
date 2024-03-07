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

package com.android.settings.fuelgauge.batterytip.actions;

import android.content.Context;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Abstract class for battery tip action, which is triggered if we need to handle the battery tip
 */
public abstract class BatteryTipAction {
    protected Context mContext;
    protected MetricsFeatureProvider mMetricsFeatureProvider;

    public BatteryTipAction(Context context) {
        mContext = context;
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    /** Handle the action when user clicks positive button */
    public abstract void handlePositiveAction(int metricsKey);
}
