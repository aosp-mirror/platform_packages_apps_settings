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

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.widget.EditText;


/**
 * Provide an interface for testing out the Wifi API
 */
public class WifiAPITest extends PreferenceActivity implements
Preference.OnPreferenceClickListener {

    private static final String TAG = "WifiAPITest";
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onCreatePreferences();
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    }


    private void onCreatePreferences() {
        addPreferencesFromResource(R.layout.wifi_api_test);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();

        mWifiDisconnect = (Preference) preferenceScreen.findPreference(KEY_DISCONNECT);
        mWifiDisconnect.setOnPreferenceClickListener(this);

        mWifiDisableNetwork = (Preference) preferenceScreen.findPreference(KEY_DISABLE_NETWORK);
        mWifiDisableNetwork.setOnPreferenceClickListener(this);

        mWifiEnableNetwork = (Preference) preferenceScreen.findPreference(KEY_ENABLE_NETWORK);
        mWifiEnableNetwork.setOnPreferenceClickListener(this);

    }

    //============================
    // Preference callbacks
    //============================

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);
        return false;
    }

    /**
     *  Implements OnPreferenceClickListener interface
     */
    public boolean onPreferenceClick(Preference pref) {
        if (pref == mWifiDisconnect) {
            mWifiManager.disconnect();
        } else if (pref == mWifiDisableNetwork) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Input");
            alert.setMessage("Enter Network ID");
            // Set an EditText view to get user input
            final EditText input = new EditText(this);
            alert.setView(input);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    Editable value = input.getText();
                    netid = Integer.parseInt(value.toString());
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
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Input");
            alert.setMessage("Enter Network ID");
            // Set an EditText view to get user input
            final EditText input = new EditText(this);
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
