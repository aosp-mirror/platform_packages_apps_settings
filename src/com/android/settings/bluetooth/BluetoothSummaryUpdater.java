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
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.widget.SummaryUpdater;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.Collection;
import java.util.Set;

/**
 * Helper class that listeners to bluetooth callback and notify client when there is update in
 * bluetooth summary info.
 */
public final class BluetoothSummaryUpdater extends SummaryUpdater implements BluetoothCallback {
    private static final String TAG = "BluetoothSummaryUpdater";

    private final LocalBluetoothManager mBluetoothManager;
    private final LocalBluetoothAdapter mBluetoothAdapter;

    private boolean mEnabled;
    private int mConnectionState;

    public BluetoothSummaryUpdater(Context context, OnSummaryChangeListener listener,
            LocalBluetoothManager bluetoothManager) {
        super(context, listener);
        mBluetoothManager = bluetoothManager;
        mBluetoothAdapter = mBluetoothManager != null
            ? mBluetoothManager.getBluetoothAdapter() : null;
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        mEnabled = bluetoothState == BluetoothAdapter.STATE_ON
            || bluetoothState == BluetoothAdapter.STATE_TURNING_ON;
        if (!mEnabled) {
            mConnectionState = BluetoothAdapter.STATE_DISCONNECTED;
        }
        notifyChangeIfNeeded();
    }

    @Override
    public void onConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
        mConnectionState = state;
        updateConnected();
        notifyChangeIfNeeded();
    }

    @Override
    public void onScanningStateChanged(boolean started) {
    }

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
    }

    @Override
    public void register(boolean listening) {
        if (mBluetoothAdapter == null) {
            return;
        }
        if (listening) {
            mEnabled = mBluetoothAdapter.isEnabled();
            mConnectionState = mBluetoothAdapter.getConnectionState();
            notifyChangeIfNeeded();
            mBluetoothManager.getEventManager().registerCallback(this);
        } else {
            mBluetoothManager.getEventManager().unregisterCallback(this);
        }
    }

    @Override
    public String getSummary() {
        if (!mEnabled) {
            return mContext.getString(R.string.bluetooth_disabled);
        }
        switch (mConnectionState) {
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

    private void updateConnected() {
        if (mBluetoothAdapter == null) {
            return;
        }
        // Make sure our connection state is up to date.
        int state = mBluetoothAdapter.getConnectionState();
        if (state != mConnectionState) {
            mConnectionState = state;
            return;
        }
        final Collection<CachedBluetoothDevice> devices = getDevices();
        if (devices == null) {
            mConnectionState = BluetoothAdapter.STATE_DISCONNECTED;
            return;
        }
        if (mConnectionState == BluetoothAdapter.STATE_CONNECTED) {
            CachedBluetoothDevice connectedDevice = null;
            for (CachedBluetoothDevice device : devices) {
                if (device.isConnected()) {
                    connectedDevice = device;
                    break;
                }
            }
            if (connectedDevice == null) {
                // If somehow we think we are connected, but have no connected devices, we
                // aren't connected.
                mConnectionState = BluetoothAdapter.STATE_DISCONNECTED;
            }
        }
    }

    private Collection<CachedBluetoothDevice> getDevices() {
        return mBluetoothManager != null
            ? mBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy()
            : null;
    }

    @VisibleForTesting
    String getConnectedDeviceSummary() {
        String deviceName = null;
        int count = 0;
        final Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        if (devices == null || devices.isEmpty()) {
            return null;
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
            Log.w(TAG, "getConnectedDeviceSummary, deviceName is null, numBondedDevices="
                    + devices.size());
            for (BluetoothDevice device : devices) {
                Log.w(TAG, "getConnectedDeviceSummary, device=" + device.getName() + "["
                        + device.getAddress() + "]" + ", isConnected=" + device.isConnected());
            }
        }
        return count > 1 ? mContext.getString(R.string.bluetooth_connected_multiple_devices_summary)
                : mContext.getString(R.string.bluetooth_connected_summary, deviceName);
    }

}
