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

import static android.net.wifi.WifiConfiguration.AP_BAND_2GHZ;
import static android.net.wifi.WifiConfiguration.AP_BAND_5GHZ;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;

public class WifiTetherApBandPreferenceController extends WifiTetherBasePreferenceController {

    private static final String PREF_KEY = "wifi_tether_network_ap_band";
    private static final String[] BAND_VALUES =
            {String.valueOf(AP_BAND_2GHZ), String.valueOf(AP_BAND_5GHZ)};

    private final String[] mBandEntries;
    private int mBandIndex;

    public WifiTetherApBandPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        mBandEntries = mContext.getResources().getStringArray(R.array.wifi_ap_band_config_full);
        final WifiConfiguration config = mWifiManager.getWifiApConfiguration();
        if (config == null) {
            mBandIndex = 0;
        } else if (is5GhzBandSupported()) {
            mBandIndex = config.apBand;
        } else {
            config.apBand = 0;
            mWifiManager.setWifiApConfiguration(config);
            mBandIndex = config.apBand;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        ListPreference preference = (ListPreference) mPreference;
        if (!is5GhzBandSupported()) {
            preference.setEnabled(false);
            preference.setSummary(R.string.wifi_ap_choose_2G);
        } else {
            preference.setEntries(mBandEntries);
            preference.setEntryValues(BAND_VALUES);
            preference.setSummary(mBandEntries[mBandIndex]);
            preference.setValue(String.valueOf(mBandIndex));
        }
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mBandIndex = Integer.parseInt((String) newValue);
        preference.setSummary(mBandEntries[mBandIndex]);
        mListener.onTetherConfigUpdated();
        return true;
    }

    private boolean is5GhzBandSupported() {
        final String countryCode = mWifiManager.getCountryCode();
        if (!mWifiManager.isDualBandSupported() || countryCode == null) {
            return false;
        }
        return true;
    }

    public int getBandIndex() {
        return mBandIndex;
    }
}
