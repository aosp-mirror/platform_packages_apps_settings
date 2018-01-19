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

import android.app.Fragment;
import android.os.UserHandle;
import android.support.v14.preference.PreferenceFragment;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.fuelgauge.SmartBatterySettings;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class SmartBatteryAction extends BatteryTipAction {
    private SettingsActivity mSettingsActivity;
    private Fragment mFragment;

    public SmartBatteryAction(SettingsActivity settingsActivity, Fragment fragment) {
        super(settingsActivity.getApplicationContext());
        mSettingsActivity = settingsActivity;
        mFragment = fragment;
    }

    /**
     * Handle the action when user clicks positive button
     */
    @Override
    public void handlePositiveAction() {
        mSettingsActivity.startPreferencePanelAsUser(mFragment,
                SmartBatterySettings.class.getName(), null /* args */,
                R.string.smart_battery_manager_title, null /* titleText */,
                new UserHandle(UserHandle.myUserId()));
    }
}
