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

package com.android.settings.wifi.details2;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.wifi.WifiDialog2;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.wifitrackerlib.WifiEntry;

/**
 * {@link AbstractPreferenceController} that controls whether the wifi network is mac randomized
 * or not
 */
public class WifiPrivacyPreferenceController2 extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener, WifiDialog2.WifiDialog2Listener {

    private static final String KEY_WIFI_PRIVACY = "privacy";
    private WifiManager mWifiManager;
    private WifiEntry mWifiEntry;
    private Preference mPreference;

    public WifiPrivacyPreferenceController2(Context context) {
        super(context, KEY_WIFI_PRIVACY);

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public void setWifiEntry(WifiEntry wifiEntry) {
        mWifiEntry = wifiEntry;
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
        if (!mWifiEntry.canSetPrivacy()) {
            preference.setSelectable(false);
            dropDownPreference.setSummary(R.string.wifi_privacy_settings_ephemeral_summary);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final int privacy = Integer.parseInt((String) newValue);
        mWifiEntry.setPrivacy(privacy);

        // To activate changing, we need to reconnect network. WiFi will auto connect to
        // current network after disconnect(). Only needed when this is connected network.
        if (mWifiEntry.getConnectedState() == WifiEntry.CONNECTED_STATE_CONNECTED) {
            mWifiEntry.disconnect(null /* callback */);
            mWifiEntry.connect(null /* callback */);
        }
        updateSummary((DropDownPreference) preference, privacy);
        return true;
    }

    @VisibleForTesting
    int getRandomizationValue() {
        return mWifiEntry.getPrivacy();
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
        return (macRandomized == WifiEntry.PRIVACY_RANDOMIZED_MAC)
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
            ? WifiEntry.PRIVACY_RANDOMIZED_MAC : WifiEntry.PRIVACY_DEVICE_MAC;
    }

    private void updateSummary(DropDownPreference preference, int macRandomized) {
        // Translates value here to set RANDOMIZATION_PERSISTENT as first item in UI for better UX.
        final int prefMacRandomized = translateMacRandomizedValueToPrefValue(macRandomized);
        preference.setSummary(preference.getEntries()[prefMacRandomized]);
    }

    @Override
    public void onSubmit(WifiDialog2 dialog) {
        if (dialog.getController() != null) {
            final WifiConfiguration newConfig = dialog.getController().getConfig();
            if (newConfig == null) {
                return;
            }

            if (getWifiEntryPrivacy(newConfig) != mWifiEntry.getPrivacy()) {
                mWifiEntry.setPrivacy(getWifiEntryPrivacy(newConfig));
                onPreferenceChange(mPreference, String.valueOf(newConfig.macRandomizationSetting));
            }
        }
    }

    private int getWifiEntryPrivacy(WifiConfiguration wifiConfiguration) {
        switch (wifiConfiguration.macRandomizationSetting) {
            case WifiConfiguration.RANDOMIZATION_NONE:
                return WifiEntry.PRIVACY_DEVICE_MAC;
            case WifiConfiguration.RANDOMIZATION_PERSISTENT:
                return WifiEntry.PRIVACY_RANDOMIZED_MAC;
            default:
                return WifiEntry.PRIVACY_UNKNOWN;
        }
    }
}
