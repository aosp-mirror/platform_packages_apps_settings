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

import com.android.settings.wifi.WifiEnabler;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothIntent;
import android.bluetooth.IBluetoothDeviceCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.widget.Toast;

public class WirelessSettings extends PreferenceActivity {

    private static final String KEY_TOGGLE_AIRPLANE = "toggle_airplane";
    private static final String KEY_TOGGLE_BLUETOOTH = "toggle_bluetooth";
    private static final String KEY_TOGGLE_WIFI = "toggle_wifi";

    private WifiEnabler mWifiEnabler;
    private AirplaneModeEnabler mAirplaneModeEnabler;
    
    private CheckBoxPreference mToggleBluetooth;
    
    private IntentFilter mIntentFilter;
    
    private static final int EVENT_FAILED_BT_ENABLE = 1;
    private static final int EVENT_PASSED_BT_ENABLE = 2;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.wireless_settings);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(BluetoothIntent.ENABLED_ACTION);
        mIntentFilter.addAction(BluetoothIntent.DISABLED_ACTION);

        initToggles();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        refreshToggles();
        registerReceiver(mReceiver, mIntentFilter);       
        
        mWifiEnabler.resume();
        mAirplaneModeEnabler.resume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);

        mWifiEnabler.pause();
        mAirplaneModeEnabler.pause();
    }
    
    private void initToggles() {
        
        mWifiEnabler = new WifiEnabler(
                this,
                (WifiManager) getSystemService(WIFI_SERVICE),
                (CheckBoxPreference) findPreference(KEY_TOGGLE_WIFI));
        
        mAirplaneModeEnabler = new AirplaneModeEnabler(
                this,
                (CheckBoxPreference) findPreference(KEY_TOGGLE_AIRPLANE));
        
        mToggleBluetooth = (CheckBoxPreference) findPreference(KEY_TOGGLE_BLUETOOTH);
        mToggleBluetooth.setPersistent(false);
    }
    
    private void refreshToggles() {
        mToggleBluetooth.setChecked(isBluetoothEnabled());
        mToggleBluetooth.setEnabled(true);
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mToggleBluetooth) {
            setBluetoothEnabled(mToggleBluetooth.isChecked());
            return true;
        }
        
        return false;
    }

    private boolean isBluetoothEnabled() {
        BluetoothDevice device = (BluetoothDevice)getSystemService(BLUETOOTH_SERVICE);
        if (device != null) {
            return device.isEnabled();
        } else {
            return false;
        }
    }
    
    private void setBluetoothEnabled(boolean enabled) {
        try {
            BluetoothDevice device = (BluetoothDevice)getSystemService(BLUETOOTH_SERVICE);
            if (enabled) {
                // Turn it off until intent or callback is delivered
                mToggleBluetooth.setChecked(false);
                if (device.enable(mBtCallback)) {
                    mToggleBluetooth.setSummary(R.string.bluetooth_enabling);
                    mToggleBluetooth.setEnabled(false);
                }
            } else {
                if (device.disable()) {
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.BLUETOOTH_ON, 0);
                } else {
                    // Unusual situation, that you can't turn off bluetooth
                    mToggleBluetooth.setChecked(true);
                }
            }
        } catch (NullPointerException e) {
            // TODO: 1071858
            mToggleBluetooth.setChecked(false);
            mToggleBluetooth.setEnabled(false);
        }
    }

    private IBluetoothDeviceCallback mBtCallback = new IBluetoothDeviceCallback.Stub() {
        
        public void onEnableResult(int res) {
            switch (res) {
            case BluetoothDevice.RESULT_FAILURE:
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FAILED_BT_ENABLE, 0));
                break;
            case BluetoothDevice.RESULT_SUCCESS:
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_PASSED_BT_ENABLE, 0));
                break;
            }
        }
        
        public void onCreateBondingResult(String device, int res) {
            // Don't care
        }
        public void onGetRemoteServiceChannelResult(String address, int channel) { }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothIntent.ENABLED_ACTION)) {
                updateBtStatus(true);
            } else if (action.equals(BluetoothIntent.DISABLED_ACTION)) {
                mToggleBluetooth.setChecked(false);
            }
        }
    };

    private void updateBtStatus(boolean enabled) {
        mToggleBluetooth.setChecked(enabled);
        mToggleBluetooth.setEnabled(true);
        mToggleBluetooth.setSummary(R.string.bluetooth_quick_toggle_summary);
        if (enabled) {
            Settings.System.putInt(getContentResolver(),
                Settings.System.BLUETOOTH_ON, 1);
        }
    }
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_PASSED_BT_ENABLE:
                    updateBtStatus(true);
                    break;
                case EVENT_FAILED_BT_ENABLE:
                    updateBtStatus(false);
                    Toast.makeText(WirelessSettings.this, 
                            getResources().getString(R.string.bluetooth_failed_to_enable),
                            Toast.LENGTH_SHORT).show();

                    break;
            }
        }
    };
}
