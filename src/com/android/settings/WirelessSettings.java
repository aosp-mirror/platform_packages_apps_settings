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

package com.android.settings;

import com.android.settings.bluetooth.BluetoothEnabler;
import com.android.settings.wifi.WifiEnabler;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.CheckBoxPreference;

public class WirelessSettings extends PreferenceActivity {

    private static final String KEY_TOGGLE_AIRPLANE = "toggle_airplane";
    private static final String KEY_TOGGLE_BLUETOOTH = "toggle_bluetooth";
    private static final String KEY_TOGGLE_WIFI = "toggle_wifi";

    private WifiEnabler mWifiEnabler;
    private AirplaneModeEnabler mAirplaneModeEnabler;
    private BluetoothEnabler mBtEnabler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.wireless_settings);

        initToggles();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        mWifiEnabler.resume();
        mAirplaneModeEnabler.resume();
        mBtEnabler.resume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        mWifiEnabler.pause();
        mAirplaneModeEnabler.pause();
        mBtEnabler.pause();
    }
    
    private void initToggles() {
        
        mWifiEnabler = new WifiEnabler(
                this,
                (WifiManager) getSystemService(WIFI_SERVICE),
                (CheckBoxPreference) findPreference(KEY_TOGGLE_WIFI));
        
        mAirplaneModeEnabler = new AirplaneModeEnabler(
                this,
                (CheckBoxPreference) findPreference(KEY_TOGGLE_AIRPLANE));
        
        mBtEnabler = new BluetoothEnabler(
                this,
                (CheckBoxPreference) findPreference(KEY_TOGGLE_BLUETOOTH));
    }
    
}
