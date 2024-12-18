/*
 * Copyright (C) 2009 The Android Open Source Project
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

import static android.content.Context.WIFI_SERVICE;

import static com.android.settingslib.wifi.WifiEnterpriseRestrictionUtils.isChangeWifiStateAllowed;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * Provide an interface for testing out the Wifi API
 */
public class WifiAPITest extends SettingsPreferenceFragment implements
        Preference.OnPreferenceClickListener {

    private static final String TAG = "WifiAPITest+++";
    private int netid;

    //============================
    // Preference/activity member variables
    //============================

    private static final String KEY_DISCONNECT = "disconnect";
    private static final String KEY_DISABLE_NETWORK = "disable_network";
    private static final String KEY_ENABLE_NETWORK = "enable_network";

    private Preference mWifiDisconnect;
    private Preference mWifiDisableNetwork;
    private Preference mWifiEnableNetwork;

    private WifiManager mWifiManager;


    //============================
    // Activity lifecycle
    //============================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        addPreferencesFromResource(R.xml.wifi_api_test);

        boolean isChangeWifiStateAllowed = isChangeWifiStateAllowed(context);
        final PreferenceScreen preferenceScreen = getPreferenceScreen();

        mWifiDisconnect = (Preference) preferenceScreen.findPreference(KEY_DISCONNECT);
        if (mWifiDisconnect != null) {
            mWifiDisconnect.setEnabled(isChangeWifiStateAllowed);
            if (isChangeWifiStateAllowed) {
                mWifiDisconnect.setOnPreferenceClickListener(this);
            }
        }

        mWifiDisableNetwork = (Preference) preferenceScreen.findPreference(KEY_DISABLE_NETWORK);
        if (mWifiDisableNetwork != null) {
            mWifiDisableNetwork.setEnabled(isChangeWifiStateAllowed);
            if (isChangeWifiStateAllowed) {
                mWifiDisableNetwork.setOnPreferenceClickListener(this);
            }
        }

        mWifiEnableNetwork = (Preference) preferenceScreen.findPreference(KEY_ENABLE_NETWORK);
        if (mWifiEnableNetwork != null) {
            mWifiEnableNetwork.setEnabled(isChangeWifiStateAllowed);
            if (isChangeWifiStateAllowed) {
                mWifiEnableNetwork.setOnPreferenceClickListener(this);
            }
        }

    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TESTING;
    }

    //============================
    // Preference callbacks
    //============================

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        super.onPreferenceTreeClick(preference);
        return false;
    }

    /**
     *  Implements OnPreferenceClickListener interface
     */
    public boolean onPreferenceClick(Preference pref) {
        if (pref == mWifiDisconnect) {
            mWifiManager.disconnect();
        } else if (pref == mWifiDisableNetwork) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
            alert.setTitle("Input");
            alert.setMessage("Enter Network ID");
            // Set an EditText view to get user input
            final EditText input = new EditText(getPrefContext());
            alert.setView(input);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    Editable value = input.getText();
                    try {
                        netid = Integer.parseInt(value.toString());
                    } catch (NumberFormatException e) {
                        // Invalid netid
                        e.printStackTrace();
                        return;
                    }
                    mWifiManager.disableNetwork(netid);
                    }
                    });
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                    }
                    });
            alert.show();
        } else if (pref == mWifiEnableNetwork) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
            alert.setTitle("Input");
            alert.setMessage("Enter Network ID");
            // Set an EditText view to get user input
            final EditText input = new EditText(getPrefContext());
            alert.setView(input);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    Editable value = input.getText();
                    netid =  Integer.parseInt(value.toString());
                    mWifiManager.enableNetwork(netid, false);
                    }
                    });
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                    }
                    });
            alert.show();
        }
        return true;
    }
}
