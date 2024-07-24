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

import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.tips.UnrestrictAppTip;

/** Action to clear the restriction to the app */
public class UnrestrictAppAction extends BatteryTipAction {
    private UnrestrictAppTip mUnRestrictAppTip;
    @VisibleForTesting BatteryUtils mBatteryUtils;

    public UnrestrictAppAction(Context context, UnrestrictAppTip tip) {
        super(context);
        mUnRestrictAppTip = tip;
        mBatteryUtils = BatteryUtils.getInstance(context);
    }

    /** Handle the action when user clicks positive button */
    @Override
    public void handlePositiveAction(int metricsKey) {
        final AppInfo appInfo = mUnRestrictAppTip.getUnrestrictAppInfo();
        // Clear force app standby, then app can run in the background
        mBatteryUtils.setForceAppStandby(
                appInfo.uid, appInfo.packageName, AppOpsManager.MODE_ALLOWED);
        mMetricsFeatureProvider.action(
                SettingsEnums.PAGE_UNKNOWN,
                SettingsEnums.ACTION_TIP_UNRESTRICT_APP,
                metricsKey,
                appInfo.packageName,
                0);
    }
}
