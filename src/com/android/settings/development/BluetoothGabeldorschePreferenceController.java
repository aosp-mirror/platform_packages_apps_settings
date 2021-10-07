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

package com.android.settings.development;

import android.content.Context;
import android.provider.DeviceConfig;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Preference controller for Bluetooth Gabeldorche feature
 */
public class BluetoothGabeldorschePreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String BLUETOOTH_GABELDORSCHE_KEY =
            "bluetooth_gabeldorsche_enable";

    @VisibleForTesting
    static final String CURRENT_GD_FLAG = "INIT_gd_scanning";

    public BluetoothGabeldorschePreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return BLUETOOTH_GABELDORSCHE_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_BLUETOOTH,
                CURRENT_GD_FLAG, isEnabled ? "true" : "false", false /* makeDefault */);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean isEnabled = DeviceConfig.getBoolean(
                DeviceConfig.NAMESPACE_BLUETOOTH, CURRENT_GD_FLAG, false /* default */);
        ((SwitchPreference) mPreference).setChecked(isEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_BLUETOOTH,
                CURRENT_GD_FLAG, null, false /* makeDefault */);
        ((SwitchPreference) mPreference).setChecked(false);
    }
}
