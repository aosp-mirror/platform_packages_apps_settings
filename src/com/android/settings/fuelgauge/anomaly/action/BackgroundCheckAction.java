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

import android.app.AppOpsManager;
import android.content.Context;

import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.overlay.FeatureFactory;

/**
 * Background check action for anomaly app, which means to stop app running in the background
 */
public class BackgroundCheckAction implements AnomalyAction {

    private Context mContext;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private AppOpsManager mAppOpsManager;

    public BackgroundCheckAction(Context context) {
        mContext = context;
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
    }

    @Override
    public void handlePositiveAction(Anomaly anomaly, int metricsKey) {
        // TODO(b/37681923): add metric log here if possible
        mAppOpsManager.setMode(AppOpsManager.OP_RUN_IN_BACKGROUND, anomaly.uid, anomaly.packageName,
                AppOpsManager.MODE_IGNORED);
    }

    @Override
    public int getActionType() {
        return Anomaly.AnomalyActionType.BACKGROUND_CHECK;
    }
}
