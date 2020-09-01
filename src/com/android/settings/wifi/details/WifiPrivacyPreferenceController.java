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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.wifi.WifiDialog;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * {@link AbstractPreferenceController} that controls whether the wifi network is mac randomized
 * or not.
 *
 * Migrating from Wi-Fi SettingsLib to to WifiTrackerLib, this object will be removed in the near
 * future, please develop in
 * {@link com.android.settings.wifi.details2.WifiPrivacyPreferenceController2}.
 */
public class WifiPrivacyPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener, WifiDialog.WifiDialogListener {

    private static final String KEY_WIFI_PRIVACY = "privacy";
    private WifiConfiguration mWifiConfiguration;
    private WifiManager mWifiManager;
    private boolean mIsEphemeral = false;
    private boolean mIsPasspoint = false;
    private Preference mPreference;

    public WifiPrivacyPreferenceController(Context context) {
        super(context, KEY_WIFI_PRIVACY);
        mWifiConfiguration = null;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public void setWifiConfiguration(WifiConfiguration wifiConfiguration) {
        mWifiConfiguration = wifiConfiguration;
    }

    public void setIsEphemeral(boolean isEphemeral) {
        mIsEphemeral = isEphemeral;
    }

    public void setIsPasspoint(boolean isPasspoint) {
        mIsPasspoint = isPasspoint;
    }

    @Override
    public int getAvailabilityStatus() {
        return mWifiManager.isConnectedMacRandomizationSupported()
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        final DropDownPreference dropDownPreference = (DropDownPreference) preference;
        final int randomizationLevel = getRandomizationValue();
        dropDownPreference.setValue(Integer.toString(randomizationLevel));
        updateSummary(dropDownPreference, randomizationLevel);

        // Makes preference not selectable, when this is a ephemeral network.
        if (mIsEphemeral || mIsPasspoint) {
            preference.setSelectable(false);
            dropDownPreference.setSummary(R.string.wifi_privacy_settings_ephemeral_summary);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mWifiConfiguration != null) {
            mWifiConfiguration.macRandomizationSetting = Integer.parseInt((String) newValue);
            mWifiManager.updateNetwork(mWifiConfiguration);

            // To activate changing, we need to reconnect network. WiFi will auto connect to
            // current network after disconnect(). Only needed when this is connected network.
            final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getNetworkId() == mWifiConfiguration.networkId) {
                mWifiManager.disconnect();
            }
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

    private static final int PREF_RANDOMIZATION_PERSISTENT = 0;
    private static final int PREF_RANDOMIZATION_NONE = 1;

    /**
     * Returns preference index value.
     *
     * @param macRandomized is mac randomized value
     * @return index value of preference
     */
    public static int translateMacRandomizedValueToPrefValue(int macRandomized) {
        return (macRandomized == WifiConfiguration.RANDOMIZATION_PERSISTENT)
            ? PREF_RANDOMIZATION_PERSISTENT : PREF_RANDOMIZATION_NONE;
    }

    /**
     * Returns mac randomized value.
     *
     * @param prefMacRandomized is preference index value
     * @return mac randomized value
     */
    public static int translatePrefValueToMacRandomizedValue(int prefMacRandomized) {
        return (prefMacRandomized == PREF_RANDOMIZATION_PERSISTENT)
            ? WifiConfiguration.RANDOMIZATION_PERSISTENT : WifiConfiguration.RANDOMIZATION_NONE;
    }

    private void updateSummary(DropDownPreference preference, int macRandomized) {
        // Translates value here to set RANDOMIZATION_PERSISTENT as first item in UI for better UX.
        final int prefMacRandomized = translateMacRandomizedValueToPrefValue(macRandomized);
        preference.setSummary(preference.getEntries()[prefMacRandomized]);
    }

    @Override
    public void onSubmit(WifiDialog dialog) {
        if (dialog.getController() != null) {
            final WifiConfiguration newConfig = dialog.getController().getConfig();
            if (newConfig == null || mWifiConfiguration == null) {
                return;
            }

            if (newConfig.macRandomizationSetting != mWifiConfiguration.macRandomizationSetting) {
                mWifiConfiguration = newConfig;
                onPreferenceChange(mPreference, String.valueOf(newConfig.macRandomizationSetting));
            }
        }
    }
}
