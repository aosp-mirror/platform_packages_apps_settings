/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Developer option controller for enhanced MAC randomization.
 */
public class WifiEnhancedMacRandomizationPreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private static final String WIFI_ENHANCED_MAC_RANDOMIZATION_KEY =
            "wifi_enhanced_mac_randomization";
    private static final String ENHANCED_MAC_RANDOMIZATION_FEATURE_FLAG =
            "enhanced_mac_randomization_force_enabled";

    public WifiEnhancedMacRandomizationPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return WIFI_ENHANCED_MAC_RANDOMIZATION_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int isEnabledInt = ((Boolean) newValue) ? 1 : 0;
        Settings.Global.putInt(mContext.getContentResolver(),
                ENHANCED_MAC_RANDOMIZATION_FEATURE_FLAG, isEnabledInt);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        boolean enabled = false;
        if (Settings.Global.getInt(mContext.getContentResolver(),
                ENHANCED_MAC_RANDOMIZATION_FEATURE_FLAG, 0) == 1) {
            enabled = true;
        }
        ((SwitchPreference) mPreference).setChecked(enabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(mContext.getContentResolver(),
                ENHANCED_MAC_RANDOMIZATION_FEATURE_FLAG, 0);
        ((SwitchPreference) mPreference).setChecked(false);
    }
}
