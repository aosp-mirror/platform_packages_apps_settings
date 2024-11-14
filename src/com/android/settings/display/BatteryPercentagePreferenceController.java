/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.display;

import static android.provider.Settings.System.SHOW_BATTERY_PERCENT;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;

/**
 * A controller to manage the switch for showing battery percentage in the status bar.
 */

public class BatteryPercentagePreferenceController extends BasePreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private Preference mPreference;

    public BatteryPercentagePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (!Utils.isBatteryPresent(mContext)) {
            // Disable battery percentage
            onPreferenceChange(mPreference, false /* newValue */);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (!Utils.isBatteryPresent(mContext)) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        if (isBatteryDead()) {
            return DISABLED_DEPENDENT_SETTING;
        }

        return mContext.getResources().getBoolean(
                R.bool.config_battery_percentage_setting_available) ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    private boolean isBatteryDead() {
        Intent batteryBroadcast = mContext.registerReceiver(null /* receiver */,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return batteryBroadcast != null && batteryBroadcast.getIntExtra(
                BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                == BatteryManager.BATTERY_HEALTH_DEAD;
    }

    @Override
    public void updateState(Preference preference) {
        int setting = Settings.System.getInt(mContext.getContentResolver(),
                SHOW_BATTERY_PERCENT, 0);

        ((SwitchPreference) preference).setChecked(setting == 1);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean showPercentage = (Boolean) newValue;
        Settings.System.putInt(mContext.getContentResolver(), SHOW_BATTERY_PERCENT,
                showPercentage ? 1 : 0);
        FeatureFactory.getFactory(mContext).getMetricsFeatureProvider()
                .action(mContext, SettingsEnums.OPEN_BATTERY_PERCENTAGE, showPercentage);
        return true;
    }
}
