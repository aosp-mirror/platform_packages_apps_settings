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

import com.android.settings.bluetooth.LocalBluetoothManager.ExtendedBluetoothState;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothIntent;
import android.bluetooth.IBluetoothDeviceCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

/**
 * BluetoothEventRedirector receives broadcasts and callbacks from the Bluetooth
 * API and dispatches the event on the UI thread to the right class in the
 * Settings.
 */
public class BluetoothEventRedirector {
    private static final String TAG = "BluetoothEventRedirector";
    private static final boolean V = LocalBluetoothManager.V;
    
    private LocalBluetoothManager mManager;
    private Handler mUiHandler = new Handler();
    
    private IBluetoothDeviceCallback mBtDevCallback = new IBluetoothDeviceCallback.Stub() {
        public void onCreateBondingResult(final String address, final int result) {
            if (V) {
                Log.v(TAG, "onCreateBondingResult(" + address + ", " + result + ")");
            }
            
            mUiHandler.post(new Runnable() {
                public void run() {
                    boolean wasSuccess = result == BluetoothDevice.RESULT_SUCCESS; 
                    LocalBluetoothDeviceManager deviceManager = mManager.getLocalDeviceManager();
                    deviceManager.onBondingStateChanged(address, wasSuccess);
                    if (!wasSuccess) {
                        deviceManager.onBondingError(address);
                    }
                }
            });
        }

        public void onEnableResult(int result) { }
        public void onGetRemoteServiceChannelResult(String address, int channel) { }
    };
    
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (V) {
                Log.v(TAG, "Received " + intent.getAction());
            }
            
            String action = intent.getAction();
            String address = intent.getStringExtra(BluetoothIntent.ADDRESS);
                
            if (action.equals(BluetoothIntent.ENABLED_ACTION)) {
                mManager.setBluetoothStateInt(ExtendedBluetoothState.ENABLED);
            } else if (action.equals(BluetoothIntent.DISABLED_ACTION)) {
                mManager.setBluetoothStateInt(ExtendedBluetoothState.DISABLED);
                    
            } else if (action.equals(BluetoothIntent.DISCOVERY_STARTED_ACTION)) {
                mManager.onScanningStateChanged(true);
            } else if (action.equals(BluetoothIntent.DISCOVERY_COMPLETED_ACTION)) {
                mManager.onScanningStateChanged(false);
                    
            } else if (action.equals(BluetoothIntent.REMOTE_DEVICE_FOUND_ACTION)) {
                short rssi = intent.getShortExtra(BluetoothIntent.RSSI, Short.MIN_VALUE);
                mManager.getLocalDeviceManager().onDeviceAppeared(address, rssi);                    
            } else if (action.equals(BluetoothIntent.REMOTE_DEVICE_DISAPPEARED_ACTION)) {
                mManager.getLocalDeviceManager().onDeviceDisappeared(address);
            } else if (action.equals(BluetoothIntent.REMOTE_NAME_UPDATED_ACTION)) {
                mManager.getLocalDeviceManager().onDeviceNameUpdated(address);
                
            } else if (action.equals(BluetoothIntent.BONDING_CREATED_ACTION)) {
                mManager.getLocalDeviceManager().onBondingStateChanged(address, true);
            } else if (action.equals(BluetoothIntent.BONDING_REMOVED_ACTION)) {
                mManager.getLocalDeviceManager().onBondingStateChanged(address, false);
                
            } else if (action.equals(BluetoothIntent.HEADSET_STATE_CHANGED_ACTION)) {
                mManager.getLocalDeviceManager().onProfileStateChanged(address);

                int newState = intent.getIntExtra(BluetoothIntent.HEADSET_STATE, 0);
                int oldState = intent.getIntExtra(BluetoothIntent.HEADSET_PREVIOUS_STATE, 0);
                if (newState == BluetoothHeadset.STATE_DISCONNECTED &&
                        oldState == BluetoothHeadset.STATE_CONNECTING) {
                    mManager.getLocalDeviceManager().onConnectingError(address);
                }
                
            } else if (action.equals(BluetoothA2dp.SINK_STATE_CHANGED_ACTION)) {
                mManager.getLocalDeviceManager().onProfileStateChanged(address);

                int newState = intent.getIntExtra(BluetoothA2dp.SINK_STATE, 0);
                int oldState = intent.getIntExtra(BluetoothA2dp.SINK_PREVIOUS_STATE, 0);
                if (newState == BluetoothA2dp.STATE_DISCONNECTED &&
                        oldState == BluetoothA2dp.STATE_CONNECTING) {
                    mManager.getLocalDeviceManager().onConnectingError(address);
                }
            }
        }
    };

    public BluetoothEventRedirector(LocalBluetoothManager localBluetoothManager) {
        mManager = localBluetoothManager;
    }

    public void start() {
        IntentFilter filter = new IntentFilter();
        
        // Bluetooth on/off broadcasts
        filter.addAction(BluetoothIntent.ENABLED_ACTION);
        filter.addAction(BluetoothIntent.DISABLED_ACTION);
        
        // Discovery broadcasts
        filter.addAction(BluetoothIntent.DISCOVERY_STARTED_ACTION);
        filter.addAction(BluetoothIntent.DISCOVERY_COMPLETED_ACTION);
        filter.addAction(BluetoothIntent.REMOTE_DEVICE_DISAPPEARED_ACTION);
        filter.addAction(BluetoothIntent.REMOTE_DEVICE_FOUND_ACTION);
        filter.addAction(BluetoothIntent.REMOTE_NAME_UPDATED_ACTION);
        
        // Pairing broadcasts
        filter.addAction(BluetoothIntent.BONDING_CREATED_ACTION);
        filter.addAction(BluetoothIntent.BONDING_REMOVED_ACTION);
        
        // Fine-grained state broadcasts
        filter.addAction(BluetoothA2dp.SINK_STATE_CHANGED_ACTION);
        filter.addAction(BluetoothIntent.HEADSET_STATE_CHANGED_ACTION);
        
        mManager.getContext().registerReceiver(mBroadcastReceiver, filter);
    }
    
    public void stop() {
        mManager.getContext().unregisterReceiver(mBroadcastReceiver);   
    }
    
    public IBluetoothDeviceCallback getBluetoothDeviceCallback() { 
        return mBtDevCallback;
    }
}
