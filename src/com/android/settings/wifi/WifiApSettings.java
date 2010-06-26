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
import android.app.Dialog;
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
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;
import android.os.Bundle;

/*
 * Displays preferences for Tethering.
 */
public class WifiApSettings extends PreferenceActivity
                            implements DialogInterface.OnClickListener {

    private static final String WIFI_AP_SSID_AND_SECURITY = "wifi_ap_ssid_and_security";
    private static final String ENABLE_WIFI_AP = "enable_wifi_ap";
    private static final int CONFIG_SUBTEXT = R.string.wifi_tether_configure_subtext;

    private static final int OPEN_INDEX = 0;
    private static final int WPA_INDEX = 1;

    private static final int DIALOG_AP_SETTINGS = 1;

    private String[] mSecurityType;
    private Preference mCreateNetwork;
    private CheckBoxPreference mEnableWifiAp;

    private WifiApDialog mDialog;
    private WifiManager mWifiManager;
    private WifiApEnabler mWifiApEnabler;
    private WifiConfiguration mWifiConfig = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiConfig = mWifiManager.getWifiApConfiguration();
        mSecurityType = getResources().getStringArray(R.array.wifi_ap_security);

        addPreferencesFromResource(R.xml.wifi_ap_settings);

        mCreateNetwork = findPreference(WIFI_AP_SSID_AND_SECURITY);
        mEnableWifiAp = (CheckBoxPreference) findPreference(ENABLE_WIFI_AP);

        mWifiApEnabler = new WifiApEnabler(this, mEnableWifiAp);

        if(mWifiConfig == null) {
            String s = getString(com.android.internal.R.string.wifi_tether_configure_ssid_default);
            mCreateNetwork.setSummary(String.format(getString(CONFIG_SUBTEXT),
                                                    s, mSecurityType[OPEN_INDEX]));
        } else {
            mCreateNetwork.setSummary(String.format(getString(CONFIG_SUBTEXT),
                                      mWifiConfig.SSID,
                                      mWifiConfig.allowedKeyManagement.get(KeyMgmt.WPA_PSK) ?
                                      mSecurityType[WPA_INDEX] : mSecurityType[OPEN_INDEX]));
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_AP_SETTINGS) {
            mDialog = new WifiApDialog(this, this, mWifiConfig);
            return mDialog;
        }
        return null;
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
            showDialog(DIALOG_AP_SETTINGS);
        }
        return true;
    }

    public void onClick(DialogInterface dialogInterface, int button) {

        if (button == DialogInterface.BUTTON_POSITIVE) {
            mWifiConfig = mDialog.getConfig();
            if (mWifiConfig != null) {
                /**
                 * if soft AP is running, bring up with new config
                 * else update the configuration alone
                 */
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
                    mWifiManager.setWifiApEnabled(mWifiConfig, true);
                    /**
                     * There is no tether notification on changing AP
                     * configuration. Update status with new config.
                     */
                    mWifiApEnabler.updateConfigSummary(mWifiConfig);
                } else {
                    mWifiManager.setWifiApConfiguration(mWifiConfig);
                }
                mCreateNetwork.setSummary(String.format(getString(CONFIG_SUBTEXT),
                            mWifiConfig.SSID,
                            mWifiConfig.allowedKeyManagement.get(KeyMgmt.WPA_PSK) ?
                            mSecurityType[WPA_INDEX] : mSecurityType[OPEN_INDEX]));
            }
        }
    }
}
