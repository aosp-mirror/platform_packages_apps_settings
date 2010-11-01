/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings.wifi;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.app.Activity;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

public class AdvancedSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_MAC_ADDRESS = "mac_address";
    private static final String KEY_CURRENT_IP_ADDRESS = "current_ip_address";
    private static final String KEY_SLEEP_POLICY = "sleep_policy";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_advanced_settings);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        initSleepPolicyPreference();
        refreshWifiInfo();
    }

    private void initSleepPolicyPreference() {
        ListPreference pref = (ListPreference) findPreference(KEY_SLEEP_POLICY);
        pref.setOnPreferenceChangeListener(this);
        int value = Settings.System.getInt(getContentResolver(),
                Settings.System.WIFI_SLEEP_POLICY,Settings. System.WIFI_SLEEP_POLICY_DEFAULT);
        pref.setValue(String.valueOf(value));
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (key == null) return true;

        if (key.equals(KEY_SLEEP_POLICY)) {
            try {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.WIFI_SLEEP_POLICY, Integer.parseInt(((String) newValue)));
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), R.string.wifi_setting_sleep_policy_error,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }

    private void refreshWifiInfo() {
        WifiManager wifiManager = (WifiManager) getSystemService(Activity.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        Preference wifiMacAddressPref = findPreference(KEY_MAC_ADDRESS);
        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        wifiMacAddressPref.setSummary(!TextUtils.isEmpty(macAddress) ? macAddress
                : getActivity().getString(R.string.status_unavailable));

        Preference wifiIpAddressPref = findPreference(KEY_CURRENT_IP_ADDRESS);
        String ipAddress = null;
        if (wifiInfo != null) {
            long addr = wifiInfo.getIpAddress();
            if (addr != 0) {
                // handle negative values whe first octet > 127
                if (addr < 0) addr += 0x100000000L;
                ipAddress = String.format("%d.%d.%d.%d",
                        addr & 0xFF, (addr >> 8) & 0xFF, (addr >> 16) & 0xFF, (addr >> 24) & 0xFF);
            }
        }
        wifiIpAddressPref.setSummary(ipAddress == null ?
                getActivity().getString(R.string.status_unavailable) : ipAddress);
    }

}
