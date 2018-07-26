/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.VisibleForTesting;

/** Helper class, intended to be used by an Activity, to keep the local Bluetooth adapter in
 *  discoverable mode indefinitely. By default setting the scan mode to
 *  BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE will time out after some time, but some
 *  Bluetooth settings pages would like to keep the device discoverable as long as the page is
 *  visible. */
public class AlwaysDiscoverable extends BroadcastReceiver {
    private static final String TAG = "AlwaysDiscoverable";

    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private IntentFilter mIntentFilter;

    @VisibleForTesting
    boolean mStarted;

    public AlwaysDiscoverable(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
    }

    /** After calling start(), consumers should make a matching call to stop() when they no longer
     * wish to enforce discoverable mode. */
    public void start() {
        if (mStarted) {
            return;
        }
        mContext.registerReceiver(this, mIntentFilter);
        mStarted = true;
        if (mBluetoothAdapter.getScanMode()
                != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            mBluetoothAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
        }
    }

    public void stop() {
        if (!mStarted) {
            return;
        }
        mContext.unregisterReceiver(this);
        mStarted = false;
        mBluetoothAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != BluetoothAdapter.ACTION_SCAN_MODE_CHANGED) {
            return;
        }
        if (mBluetoothAdapter.getScanMode()
                != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            mBluetoothAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
        }
    }
}
