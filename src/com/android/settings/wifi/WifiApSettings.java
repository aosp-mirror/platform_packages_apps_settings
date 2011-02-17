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
import com.android.settings.SettingsPreferenceFragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

/*
 * Displays preferences for Tethering.
 */
public class WifiApSettings extends SettingsPreferenceFragment
                            implements DialogInterface.OnClickListener {

    private static final String WIFI_AP_SSID_AND_SECURITY = "wifi_ap_ssid_and_security";
    private static final String ENABLE_WIFI_AP = "enable_wifi_ap";
    private static final int CONFIG_SUBTEXT = R.string.wifi_tether_configure_subtext;

    private static final int DIALOG_AP_SETTINGS = 1;

    private String[] mSecurityType;
    private Preference mCreateNetwork;
    private CheckBoxPreference mEnableWifiAp;

    private WifiApDialog mDialog;
    private WifiManager mWifiManager;
    private WifiApEnabler mWifiApEnabler;
    private WifiConfiguration mWifiConfig = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_ap_settings);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Activity activity = getActivity();

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiConfig = mWifiManager.getWifiApConfiguration();
        mSecurityType = getResources().getStringArray(R.array.wifi_ap_security);



        mCreateNetwork = findPreference(WIFI_AP_SSID_AND_SECURITY);
        mEnableWifiAp = (CheckBoxPreference) findPreference(ENABLE_WIFI_AP);

        mWifiApEnabler = new WifiApEnabler(activity, mEnableWifiAp);

        if(mWifiConfig == null) {
            final String s = activity.getString(
                    com.android.internal.R.string.wifi_tether_configure_ssid_default);
            mCreateNetwork.setSummary(String.format(activity.getString(CONFIG_SUBTEXT),
                                                    s, mSecurityType[WifiApDialog.OPEN_INDEX]));
        } else {
            int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
            mCreateNetwork.setSummary(String.format(activity.getString(CONFIG_SUBTEXT),
                                      mWifiConfig.SSID,
                                      mSecurityType[index]));
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_AP_SETTINGS) {
            final Activity activity = getActivity();
            mDialog = new WifiApDialog(activity, this, mWifiConfig);
            return mDialog;
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        mWifiApEnabler.resume();
    }

    @Override
    public void onPause() {
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
                int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
                mCreateNetwork.setSummary(String.format(getActivity().getString(CONFIG_SUBTEXT),
                            mWifiConfig.SSID,
                            mSecurityType[index]));
            }
        }
    }
}
