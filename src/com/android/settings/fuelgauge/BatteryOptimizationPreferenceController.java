/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.fuelgauge;

import android.os.Bundle;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.fuelgauge.PowerWhitelistBackend;

/**
 * Controller that jumps to high power optimization fragment
 */
public class BatteryOptimizationPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_BACKGROUND_ACTIVITY = "battery_optimization";


    private PowerWhitelistBackend mBackend;
    private DashboardFragment mFragment;
    private SettingsActivity mSettingsActivity;
    private String mPackageName;

    public BatteryOptimizationPreferenceController(SettingsActivity settingsActivity,
            DashboardFragment fragment, String packageName) {
        super(settingsActivity);
        mFragment = fragment;
        mSettingsActivity = settingsActivity;
        mPackageName = packageName;
        mBackend = PowerWhitelistBackend.getInstance(mSettingsActivity);
    }

    @VisibleForTesting
    BatteryOptimizationPreferenceController(SettingsActivity settingsActivity,
            DashboardFragment fragment, String packageName, PowerWhitelistBackend backend) {
        super(settingsActivity);
        mFragment = fragment;
        mSettingsActivity = settingsActivity;
        mPackageName = packageName;
        mBackend = backend;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean isWhitelisted = mBackend.isWhitelisted(mPackageName);
        preference.setSummary(isWhitelisted ? R.string.high_power_on : R.string.high_power_off);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BACKGROUND_ACTIVITY;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!KEY_BACKGROUND_ACTIVITY.equals(preference.getKey())) {
            return false;
        }

        final Bundle args = new Bundle();
        args.putString(ManageApplications.EXTRA_CLASSNAME,
                Settings.HighPowerApplicationsActivity.class.getName());
        new SubSettingLauncher(mSettingsActivity)
                .setDestination(ManageApplications.class.getName())
                .setArguments(args)
                .setTitleRes(R.string.high_power_apps)
                .setSourceMetricsCategory(mFragment.getMetricsCategory())
                .launch();

        return true;
    }

}
