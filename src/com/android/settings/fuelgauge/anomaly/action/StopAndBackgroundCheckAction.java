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
import android.support.annotation.VisibleForTesting;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.fuelgauge.anomaly.Anomaly;

/**
 * Force stop and background check action for anomaly app, this action will
 * 1. Force stop the app
 * 2. Turn on background check
 */
public class StopAndBackgroundCheckAction extends AnomalyAction {
    @VisibleForTesting
    ForceStopAction mForceStopAction;
    @VisibleForTesting
    BackgroundCheckAction mBackgroundCheckAction;

    public StopAndBackgroundCheckAction(Context context) {
        this(context, new ForceStopAction(context), new BackgroundCheckAction(context));
        mActionMetricKey = MetricsProto.MetricsEvent.ACTION_APP_STOP_AND_BACKGROUND_CHECK;
    }

    @VisibleForTesting
    StopAndBackgroundCheckAction(Context context, ForceStopAction forceStopAction,
            BackgroundCheckAction backgroundCheckAction) {
        super(context);
        mForceStopAction = forceStopAction;
        mBackgroundCheckAction = backgroundCheckAction;
    }

    @Override
    public void handlePositiveAction(Anomaly anomaly, int metricsKey) {
        super.handlePositiveAction(anomaly, metricsKey);
        mForceStopAction.handlePositiveAction(anomaly, metricsKey);
        mBackgroundCheckAction.handlePositiveAction(anomaly, metricsKey);
    }

    @Override
    public boolean isActionActive(Anomaly anomaly) {
        return mForceStopAction.isActionActive(anomaly)
                && mBackgroundCheckAction.isActionActive(anomaly);
    }

    @Override
    public int getActionType() {
        return Anomaly.AnomalyActionType.STOP_AND_BACKGROUND_CHECK;
    }
}
