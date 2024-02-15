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

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.repository.WifiHotspotRepository;

public class WifiTetherAutoOffPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private final WifiManager mWifiManager;
    private boolean mSettingsOn;
    @VisibleForTesting
    boolean mNeedShutdownSecondarySap;

    public WifiTetherAutoOffPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        WifiHotspotRepository wifiHotspotRepository = FeatureFactory.getFeatureFactory()
                .getWifiFeatureProvider().getWifiHotspotRepository();
        if (wifiHotspotRepository.isSpeedFeatureAvailable() && wifiHotspotRepository.isDualBand()) {
            mNeedShutdownSecondarySap = true;
        }
        mWifiManager = context.getSystemService(WifiManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        SoftApConfiguration softApConfiguration = mWifiManager.getSoftApConfiguration();
        mSettingsOn = softApConfiguration.isAutoShutdownEnabled();

        ((TwoStatePreference) preference).setChecked(mSettingsOn);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean settingsOn = (Boolean) newValue;
        SoftApConfiguration.Builder configBuilder =
                new SoftApConfiguration.Builder(mWifiManager.getSoftApConfiguration());
        configBuilder.setAutoShutdownEnabled(settingsOn);
        if (mNeedShutdownSecondarySap) {
            configBuilder.setBridgedModeOpportunisticShutdownEnabled(settingsOn);
        }
        mSettingsOn = settingsOn;
        return mWifiManager.setSoftApConfiguration(configBuilder.build());
    }

    public boolean isEnabled() {
        return mSettingsOn;
    }
}
