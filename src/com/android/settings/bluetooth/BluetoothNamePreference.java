/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothError;
import android.bluetooth.BluetoothIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

/**
 * BluetoothNamePreference is the preference type for editing the device's
 * Bluetooth name. It asks the user for a name, and persists it via the
 * Bluetooth API.
 */
public class BluetoothNamePreference extends EditTextPreference {
    private static final String TAG = "BluetoothNamePreference";

    private LocalBluetoothManager mLocalManager;
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothIntent.NAME_CHANGED_ACTION)) {
                setSummaryToName();
            } else if (action.equals(BluetoothIntent.BLUETOOTH_STATE_CHANGED_ACTION) &&
                    (intent.getIntExtra(BluetoothIntent.BLUETOOTH_STATE,
                    BluetoothError.ERROR) == BluetoothDevice.BLUETOOTH_STATE_ON)) {
                setSummaryToName();
            }
        }
    };
    
    public BluetoothNamePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        mLocalManager = LocalBluetoothManager.getInstance(context);
        
        setSummaryToName();        
    }

    public void resume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothIntent.BLUETOOTH_STATE_CHANGED_ACTION);
        filter.addAction(BluetoothIntent.NAME_CHANGED_ACTION);
        getContext().registerReceiver(mReceiver, filter);
    }
    
    public void pause() {
        getContext().unregisterReceiver(mReceiver);
    }
    
    private void setSummaryToName() {
        BluetoothDevice manager = mLocalManager.getBluetoothManager();
        if (manager.isEnabled()) {
            setSummary(manager.getName());
        }
    }

    @Override
    protected boolean persistString(String value) {
        BluetoothDevice manager = mLocalManager.getBluetoothManager();
        manager.setName(value);
        return true;        
    }
    
}
