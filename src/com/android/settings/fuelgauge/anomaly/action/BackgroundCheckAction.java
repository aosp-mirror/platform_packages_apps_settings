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
import android.os.Build;
import android.support.annotation.VisibleForTesting;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.anomaly.Anomaly;

/**
 * Background check action for anomaly app, which means to stop app running in the background
 */
public class BackgroundCheckAction extends AnomalyAction {

    private AppOpsManager mAppOpsManager;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;

    public BackgroundCheckAction(Context context) {
        super(context);
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        mActionMetricKey = MetricsProto.MetricsEvent.ACTION_APP_BACKGROUND_CHECK;
        mBatteryUtils = BatteryUtils.getInstance(context);
    }

    @Override
    public void handlePositiveAction(Anomaly anomaly, int contextMetricsKey) {
        super.handlePositiveAction(anomaly, contextMetricsKey);
        if (anomaly.targetSdkVersion < Build.VERSION_CODES.O) {
            mAppOpsManager.setMode(AppOpsManager.OP_RUN_IN_BACKGROUND, anomaly.uid,
                    anomaly.packageName,
                    AppOpsManager.MODE_IGNORED);
        }
    }

    @Override
    public boolean isActionActive(Anomaly anomaly) {
        return !mBatteryUtils.isBackgroundRestrictionEnabled(anomaly.targetSdkVersion, anomaly.uid,
                anomaly.packageName);
    }

    @Override
    public int getActionType() {
        return Anomaly.AnomalyActionType.BACKGROUND_CHECK;
    }
}
