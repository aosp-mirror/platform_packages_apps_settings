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

package com.android.settings.wifi.details;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.FeatureFlagUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.FeatureFlags;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * {@link AbstractPreferenceController} that controls whether the wifi network is mac randomized
 * or not
 */
public class WifiPrivacyPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_WIFI_PRIVACY = "privacy";
    private WifiConfiguration mWifiConfiguration;
    private WifiManager mWifiManager;

    public WifiPrivacyPreferenceController(Context context) {
        super(context, KEY_WIFI_PRIVACY);
        mWifiConfiguration = null;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public void setWifiConfiguration(WifiConfiguration wifiConfiguration) {
        mWifiConfiguration = wifiConfiguration;
    }

    @Override
    public int getAvailabilityStatus() {
        return FeatureFlagUtils.isEnabled(mContext, FeatureFlags.WIFI_MAC_RANDOMIZATION)
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        final DropDownPreference dropDownPreference = (DropDownPreference) preference;
        final int randomizationLevel = getRandomizationValue();
        dropDownPreference.setValue(Integer.toString(randomizationLevel));
        updateSummary((DropDownPreference) preference, randomizationLevel);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mWifiConfiguration != null) {
            mWifiConfiguration.macRandomizationSetting = Integer.parseInt((String) newValue);
            mWifiManager.updateNetwork(mWifiConfiguration);
        }
        updateSummary((DropDownPreference) preference, Integer.parseInt((String) newValue));
        return true;
    }

    @VisibleForTesting
    int getRandomizationValue() {
        if (mWifiConfiguration != null) {
            return mWifiConfiguration.macRandomizationSetting;
        }
        return WifiConfiguration.RANDOMIZATION_PERSISTENT;
    }

    private void updateSummary(DropDownPreference preference, int macRandomized) {
        preference.setSummary(preference.getEntries()[macRandomized]);
    }
}
