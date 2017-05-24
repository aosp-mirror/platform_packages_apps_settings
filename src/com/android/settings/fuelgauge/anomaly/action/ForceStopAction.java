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

import android.app.ActivityManager;
import android.content.Context;
import android.util.Pair;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.overlay.FeatureFactory;

/**
 * Force stop action for anomaly app, which means to stop the app which causes anomaly
 */
public class ForceStopAction implements AnomalyAction {

    private Context mContext;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private ActivityManager mActivityManager;

    public ForceStopAction(Context context) {
        mContext = context;
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        mActivityManager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
    }

    @Override
    public void handlePositiveAction(Anomaly anomaly, int metricsKey) {
        final String packageName = anomaly.packageName;
        // force stop the package
        mMetricsFeatureProvider.action(mContext,
                MetricsProto.MetricsEvent.ACTION_APP_FORCE_STOP, packageName,
                Pair.create(MetricsProto.MetricsEvent.FIELD_CONTEXT, metricsKey));

        mActivityManager.forceStopPackage(packageName);
    }

    @Override
    public int getActionType() {
        return Anomaly.AnomalyActionType.FORCE_STOP;
    }
}
