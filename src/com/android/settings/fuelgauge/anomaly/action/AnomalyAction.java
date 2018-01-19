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

import android.content.Context;
import android.util.Pair;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.overlay.FeatureFactory;

/**
 * Abstract class for anomaly action, which is triggered if we need to handle the anomaly
 */
public abstract class AnomalyAction {
    protected Context mContext;
    protected int mActionMetricKey;

    private MetricsFeatureProvider mMetricsFeatureProvider;

    public AnomalyAction(Context context) {
        mContext = context;
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    /**
     * handle the action when user clicks positive button
     *
     * @param anomaly    about the app that we need to handle
     * @param contextMetricsKey key for the page that invokes the action
     * @see com.android.internal.logging.nano.MetricsProto
     */
    public void handlePositiveAction(Anomaly anomaly, int contextMetricsKey) {
        mMetricsFeatureProvider.action(mContext, mActionMetricKey, anomaly.packageName,
                Pair.create(MetricsProto.MetricsEvent.FIELD_CONTEXT, contextMetricsKey));
    }

    /**
     * Check whether the action is active for {@code anomaly}
     *
     * @param anomaly about the app that we need to handle
     * @return {@code true} if action is active, otherwise return {@code false}
     */
    public abstract boolean isActionActive(Anomaly anomaly);

    @Anomaly.AnomalyActionType
    public abstract int getActionType();
}
