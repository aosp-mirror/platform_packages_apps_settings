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

package com.android.settings.wifi.tether;

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.BasePreferenceController;

public class WifiTetherAutoOffPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private final WifiManager mWifiManager;

    public WifiTetherAutoOffPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mWifiManager = context.getSystemService(WifiManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        SoftApConfiguration softApConfiguration = mWifiManager.getSoftApConfiguration();
        final boolean settingsOn = softApConfiguration.isAutoShutdownEnabled();

        ((SwitchPreference) preference).setChecked(settingsOn);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean settingsOn = (Boolean) newValue;
        SoftApConfiguration softApConfiguration = mWifiManager.getSoftApConfiguration();
        SoftApConfiguration newSoftApConfiguration =
                new SoftApConfiguration.Builder(softApConfiguration)
                        .setAutoShutdownEnabled(settingsOn)
                        .build();
        return mWifiManager.setSoftApConfiguration(newSoftApConfiguration);
    }
}
