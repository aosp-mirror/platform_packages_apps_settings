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
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.widget.SummaryUpdater;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.Set;

/**
 * Helper class that listeners to bluetooth callback and notify client when there is update in
 * bluetooth summary info.
 */
public final class BluetoothSummaryUpdater extends SummaryUpdater implements BluetoothCallback {
    private static final String TAG = "BluetoothSummaryUpdater";

    private final BluetoothAdapter mBluetoothAdapter;
    private final LocalBluetoothManager mBluetoothManager;

    public BluetoothSummaryUpdater(Context context, OnSummaryChangeListener listener,
            LocalBluetoothManager bluetoothManager) {
        super(context, listener);
        mBluetoothManager = bluetoothManager;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        notifyChangeIfNeeded();
    }

    @Override
    public void onConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
        notifyChangeIfNeeded();
    }

    @Override
    public void register(boolean listening) {
        if (mBluetoothAdapter == null) {
            return;
        }
        if (listening) {
            notifyChangeIfNeeded();
            mBluetoothManager.getEventManager().registerCallback(this);
        } else {
            mBluetoothManager.getEventManager().unregisterCallback(this);
        }
    }

    @Override
    public String getSummary() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            return mContext.getString(R.string.bluetooth_disabled);
        }
        switch (mBluetoothAdapter.getConnectionState()) {
            case BluetoothAdapter.STATE_CONNECTED:
                return getConnectedDeviceSummary();
            case BluetoothAdapter.STATE_CONNECTING:
                return mContext.getString(R.string.bluetooth_connecting);
            case BluetoothAdapter.STATE_DISCONNECTING:
                return mContext.getString(R.string.bluetooth_disconnecting);
            default:
                return mContext.getString(R.string.disconnected);
        }
    }

    @VisibleForTesting
    String getConnectedDeviceSummary() {
        String deviceName = null;
        int count = 0;
        final Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        if (devices == null) {
            Log.e(TAG, "getConnectedDeviceSummary, bonded devices are null");
            return mContext.getString(R.string.bluetooth_disabled);
        } else if (devices.isEmpty()) {
            Log.e(TAG, "getConnectedDeviceSummary, no bonded devices");
            return mContext.getString(R.string.disconnected);
        }
        for (BluetoothDevice device : devices) {
            if (device.isConnected()) {
                deviceName = device.getName();
                count++;
                if (count > 1) {
                    break;
                }
            }
        }
        if (deviceName == null) {
            Log.e(TAG, "getConnectedDeviceSummary, deviceName is null, numBondedDevices="
                    + devices.size());
            for (BluetoothDevice device : devices) {
                Log.e(TAG, "getConnectedDeviceSummary, device=" + device.getName() + "["
                        + device.getAddress() + "]" + ", isConnected=" + device.isConnected());
            }
            return mContext.getString(R.string.disconnected);
        }
        return count > 1 ? mContext.getString(R.string.bluetooth_connected_multiple_devices_summary)
                : mContext.getString(R.string.bluetooth_connected_summary, deviceName);
    }

}
