/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.util.Log;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;

/*
 * Displays preferences for Tethering.
 */
public class WifiApSettings extends PreferenceActivity
        implements DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener {

    private static final String WIFI_AP_SSID_AND_SECURITY = "wifi_ap_ssid_and_security";
    private static final String WIFI_AP_CHANNEL = "wifi_ap_channel";
    private static final String ENABLE_WIFI_AP = "enable_wifi_ap";

    private Preference mCreateNetwork;
    private ListPreference mChannel;
    private CheckBoxPreference mEnableWifiAp;

    private WifiApDialog mDialog;
    private WifiManager mWifiManager;
    private WifiApEnabler mWifiApEnabler;
    private WifiConfiguration mWifiConfig = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        addPreferencesFromResource(R.xml.wifi_ap_settings);

        mCreateNetwork = findPreference(WIFI_AP_SSID_AND_SECURITY);
        mChannel = (ListPreference) findPreference(WIFI_AP_CHANNEL);
        mEnableWifiAp = (CheckBoxPreference) findPreference(ENABLE_WIFI_AP);

        mWifiApEnabler = new WifiApEnabler(this, mEnableWifiAp);

        initChannels();
    }

    public void initChannels() {
        mChannel.setOnPreferenceChangeListener(this);

        int numChannels = mWifiManager.getNumAllowedChannels();

        String[] entries = new String[numChannels];
        String[] entryValues = new String[numChannels];

        for (int i = 1; i <= numChannels; i++) {
            entries[i-1] = "Channel "+i;
            entryValues[i-1] = i+"";
        }

        mChannel.setEntries(entries);
        mChannel.setEntryValues(entryValues);
        mChannel.setEnabled(true);
        /**
         * TODO: randomize initial channel chosen
         */
        mChannel.setValue("2");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWifiApEnabler.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWifiApEnabler.pause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference == mCreateNetwork) {
            showDialog();
        }
        return true;
    }

    private void showDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = new WifiApDialog(this, this, mWifiConfig);
        mDialog.show();
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        /**
         * TODO: Needs work
         */
        mWifiConfig = mDialog.getConfig();

        if(mWifiConfig.SSID != null)
            mCreateNetwork.setSummary(mWifiConfig.SSID);

        /**
         * TODO: set SSID and security
         */
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        /**
         * TODO: Needs work
         */

        String key = preference.getKey();
        if (key == null) return true;

        if (key.equals(WIFI_AP_CHANNEL)) {
            int chosenChannel = Integer.parseInt((String) newValue);
            if(newValue != null)
                mChannel.setSummary(newValue.toString());
        }
        return true;
    }
}
