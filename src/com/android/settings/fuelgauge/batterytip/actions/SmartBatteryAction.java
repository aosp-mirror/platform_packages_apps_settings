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

import android.app.settings.SettingsEnums;

import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.fuelgauge.SmartBatterySettings;
import com.android.settingslib.core.instrumentation.Instrumentable;

public class SmartBatteryAction extends BatteryTipAction {
    private SettingsActivity mSettingsActivity;
    private Fragment mFragment;

    public SmartBatteryAction(SettingsActivity settingsActivity, Fragment fragment) {
        super(settingsActivity.getApplicationContext());
        mSettingsActivity = settingsActivity;
        mFragment = fragment;
    }

    /** Handle the action when user clicks positive button */
    @Override
    public void handlePositiveAction(int metricsKey) {
        mMetricsFeatureProvider.action(
                mContext, SettingsEnums.ACTION_TIP_OPEN_SMART_BATTERY, metricsKey);
        new SubSettingLauncher(mSettingsActivity)
                .setSourceMetricsCategory(
                        mFragment instanceof Instrumentable
                                ? ((Instrumentable) mFragment).getMetricsCategory()
                                : Instrumentable.METRICS_CATEGORY_UNKNOWN)
                .setDestination(SmartBatterySettings.class.getName())
                .setTitleRes(R.string.smart_battery_manager_title)
                .launch();
    }
}
