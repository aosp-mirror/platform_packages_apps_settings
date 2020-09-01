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
package com.android.settings.location;

import android.content.Context;
import android.net.wifi.WifiManager;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class WifiScanningPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_WIFI_SCAN_ALWAYS_AVAILABLE = "wifi_always_scanning";
    private final WifiManager mWifiManager;

    public WifiScanningPreferenceController(Context context) {
        super(context);
        mWifiManager = context.getSystemService(WifiManager.class);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_WIFI_SCAN_ALWAYS_AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) preference).setChecked(mWifiManager.isScanAlwaysAvailable());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_WIFI_SCAN_ALWAYS_AVAILABLE.equals(preference.getKey())) {
            mWifiManager.setScanAlwaysAvailable(((SwitchPreference) preference).isChecked());
            return true;
        }
        return false;
    }
}
