/*
 * Copyright (C) 2018 The Android Open Source Project
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
 *
 *
 */

package com.android.settings.fuelgauge;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Switch;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

/**
 * Controller to change and update the smart battery toggle
 */
public class SmartBatteryPreferenceController extends BasePreferenceController implements
        OnMainSwitchChangeListener {

    private static final String KEY_SMART_BATTERY = "smart_battery";
    private static final int ON = 1;
    private static final int OFF = 0;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    private MainSwitchPreference mPreference;

    public SmartBatteryPreferenceController(Context context) {
        super(context, KEY_SMART_BATTERY);
        mPowerUsageFeatureProvider = FeatureFactory.getFactory(context)
                .getPowerUsageFeatureProvider(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return mPowerUsageFeatureProvider.isSmartBatterySupported()
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "smart_battery");
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final boolean smartBatteryOn = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ADAPTIVE_BATTERY_MANAGEMENT_ENABLED, ON) == ON;
        ((MainSwitchPreference) preference).updateStatus(smartBatteryOn);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (MainSwitchPreference) screen.findPreference(getPreferenceKey());
        mPreference.addOnSwitchChangeListener(this);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ADAPTIVE_BATTERY_MANAGEMENT_ENABLED, isChecked ? ON : OFF);
    }
}
