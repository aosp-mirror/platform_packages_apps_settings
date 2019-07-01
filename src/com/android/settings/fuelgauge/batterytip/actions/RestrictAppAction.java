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

import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.android.internal.util.CollectionUtils;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryDatabaseManager;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;

import java.util.List;

/**
 * Action to restrict the apps, then app is not allowed to run in the background.
 */
public class RestrictAppAction extends BatteryTipAction {
    private RestrictAppTip mRestrictAppTip;
    @VisibleForTesting
    BatteryDatabaseManager mBatteryDatabaseManager;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;

    public RestrictAppAction(Context context, RestrictAppTip tip) {
        super(context);
        mRestrictAppTip = tip;
        mBatteryUtils = BatteryUtils.getInstance(context);
        mBatteryDatabaseManager = BatteryDatabaseManager.getInstance(context);
    }

    /**
     * Handle the action when user clicks positive button
     */
    @Override
    public void handlePositiveAction(int metricsKey) {
        final List<AppInfo> appInfos = mRestrictAppTip.getRestrictAppList();

        for (int i = 0, size = appInfos.size(); i < size; i++) {
            final AppInfo appInfo = appInfos.get(i);
            final String packageName = appInfo.packageName;
            // Force app standby, then app can't run in the background
            mBatteryUtils.setForceAppStandby(appInfo.uid, packageName,
                    AppOpsManager.MODE_IGNORED);
            if (CollectionUtils.isEmpty(appInfo.anomalyTypes)) {
                // Only log context if there is no anomaly type
                mMetricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                        SettingsEnums.ACTION_TIP_RESTRICT_APP,
                        metricsKey,
                        packageName,
                        0);
            } else {
                for (int type : appInfo.anomalyTypes) {
                    mMetricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                            SettingsEnums.ACTION_TIP_RESTRICT_APP,
                            metricsKey,
                            packageName,
                            type);
                }
            }
        }

        mBatteryDatabaseManager.updateAnomalies(appInfos, AnomalyDatabaseHelper.State.HANDLED);
    }
}
