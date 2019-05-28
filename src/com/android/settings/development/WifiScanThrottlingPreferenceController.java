/*
 * Copyright (C) 2019 The Android Open Source Project
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

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class WifiScanThrottlingPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private static final String WIFI_SCAN_THROTTLING_KEY = "wifi_scan_throttling";
    @VisibleForTesting
    static final int SETTING_THROTTLING_ENABLE_VALUE_ON = 1;  // default is throttling enabled.
    @VisibleForTesting
    static final int SETTING_THROTTLING_ENABLE_VALUE_OFF = 0;

    public WifiScanThrottlingPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return WIFI_SCAN_THROTTLING_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.WIFI_SCAN_THROTTLE_ENABLED,
                isEnabled
                        ? SETTING_THROTTLING_ENABLE_VALUE_ON
                        : SETTING_THROTTLING_ENABLE_VALUE_OFF);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int scanThrottleEnabled = Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.WIFI_SCAN_THROTTLE_ENABLED,
                SETTING_THROTTLING_ENABLE_VALUE_ON);
        ((SwitchPreference) mPreference).setChecked(
                scanThrottleEnabled == SETTING_THROTTLING_ENABLE_VALUE_ON);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.WIFI_SCAN_THROTTLE_ENABLED, SETTING_THROTTLING_ENABLE_VALUE_ON);
        ((SwitchPreference) mPreference).setChecked(true);
    }
}
