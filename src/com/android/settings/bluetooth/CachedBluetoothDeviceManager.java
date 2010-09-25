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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.bluetooth.LocalBluetoothManager.Callback;
import com.android.settings.bluetooth.LocalBluetoothProfileManager.Profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * CachedBluetoothDeviceManager manages the set of remote Bluetooth devices.
 */
public class CachedBluetoothDeviceManager {
    private static final String TAG = "CachedBluetoothDeviceManager";

    final LocalBluetoothManager mLocalManager;
    final List<Callback> mCallbacks;

    final List<CachedBluetoothDevice> mCachedDevices = new ArrayList<CachedBluetoothDevice>();

    public CachedBluetoothDeviceManager(LocalBluetoothManager localManager) {
        mLocalManager = localManager;
        mCallbacks = localManager.getCallbacks();
    }

    private synchronized boolean readPairedDevices() {
        BluetoothAdapter adapter = mLocalManager.getBluetoothAdapter();
        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
        if (bondedDevices == null) return false;

        boolean deviceAdded = false;
        for (BluetoothDevice device : bondedDevices) {
            CachedBluetoothDevice cachedDevice = findDevice(device);
            if (cachedDevice == null) {
                cachedDevice = new CachedBluetoothDevice(mLocalManager.getContext(), device);
                mCachedDevices.add(cachedDevice);
                dispatchDeviceAdded(cachedDevice);
                deviceAdded = true;
            }
        }

        return deviceAdded;
    }

    public synchronized List<CachedBluetoothDevice> getCachedDevicesCopy() {
        return new ArrayList<CachedBluetoothDevice>(mCachedDevices);
    }

    void onBluetoothStateChanged(boolean enabled) {
        if (enabled) {
            readPairedDevices();
        }
    }

    public synchronized void onDeviceAppeared(BluetoothDevice device, short rssi,
            BluetoothClass btClass, String name) {
        boolean deviceAdded = false;

        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice == null) {
            cachedDevice = new CachedBluetoothDevice(mLocalManager.getContext(), device);
            mCachedDevices.add(cachedDevice);
            deviceAdded = true;
        }
        cachedDevice.setRssi(rssi);
        cachedDevice.setBtClass(btClass);
        cachedDevice.setName(name);
        cachedDevice.setVisible(true);

        if (deviceAdded) {
            dispatchDeviceAdded(cachedDevice);
        }
    }

    public synchronized void onDeviceDisappeared(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice == null) return;

        cachedDevice.setVisible(false);
        checkForDeviceRemoval(cachedDevice);
    }

    private void checkForDeviceRemoval(CachedBluetoothDevice cachedDevice) {
        if (cachedDevice.getBondState() == BluetoothDevice.BOND_NONE &&
                !cachedDevice.isVisible()) {
            // If device isn't paired, remove it altogether
            mCachedDevices.remove(cachedDevice);
            dispatchDeviceDeleted(cachedDevice);
        }
    }

    public synchronized void onDeviceNameUpdated(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice != null) {
            cachedDevice.refreshName();
        }
    }

    public synchronized CachedBluetoothDevice findDevice(BluetoothDevice device) {

        for (int i = mCachedDevices.size() - 1; i >= 0; i--) {
            CachedBluetoothDevice cachedDevice = mCachedDevices.get(i);

            if (cachedDevice.getDevice().equals(device)) {
                return cachedDevice;
            }
        }

        return null;
    }

    /**
     * Attempts to get the name of a remote device, otherwise returns the address.
     *
     * @param device The remote device.
     * @return The name, or if unavailable, the address.
     */
    public String getName(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice != null) return cachedDevice.getName();

        String name = device.getName();
        if (name != null) return name;

        return device.getAddress();
    }

    private void dispatchDeviceAdded(CachedBluetoothDevice cachedDevice) {
        synchronized (mCallbacks) {
            for (Callback callback : mCallbacks) {
                callback.onDeviceAdded(cachedDevice);
            }
        }

        // TODO: divider between prev paired/connected and scanned
    }

    private void dispatchDeviceDeleted(CachedBluetoothDevice cachedDevice) {
        synchronized (mCallbacks) {
            for (Callback callback : mCallbacks) {
                callback.onDeviceDeleted(cachedDevice);
            }
        }
    }

    public synchronized void onBondingStateChanged(BluetoothDevice device, int bondState) {
        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice == null) {
            if (!readPairedDevices()) {
                Log.e(TAG, "Got bonding state changed for " + device +
                        ", but we have no record of that device.");
                return;
            }
            cachedDevice = findDevice(device);
            if (cachedDevice == null) {
                Log.e(TAG, "Got bonding state changed for " + device +
                        "but device not added in cache");
                return;
            }
        }

        cachedDevice.onBondingStateChanged(bondState);
    }

    /**
     * Called when we have reached the un-bond state.
     *
     * @param device The remote device.
     * @param reason The reason, one of the error reasons from
     *            BluetoothDevice.UNBOND_REASON_*
     */
    public synchronized void showUnbondMessage(BluetoothDevice device, int reason) {
        int errorMsg;

        switch(reason) {
        case BluetoothDevice.UNBOND_REASON_AUTH_FAILED:
            errorMsg = R.string.bluetooth_pairing_pin_error_message;
            mLocalManager.showError(device, R.string.bluetooth_error_title, errorMsg);
            break;
        case BluetoothDevice.UNBOND_REASON_AUTH_REJECTED:
            errorMsg = R.string.bluetooth_pairing_rejected_error_message;
            mLocalManager.showError(device, R.string.bluetooth_error_title, errorMsg);
            break;
        case BluetoothDevice.UNBOND_REASON_REMOTE_DEVICE_DOWN:
            errorMsg = R.string.bluetooth_pairing_device_down_error_message;
            mLocalManager.showError(device, R.string.bluetooth_error_title, errorMsg);
            break;
        case BluetoothDevice.UNBOND_REASON_DISCOVERY_IN_PROGRESS:
        case BluetoothDevice.UNBOND_REASON_AUTH_TIMEOUT:
        case BluetoothDevice.UNBOND_REASON_REPEATED_ATTEMPTS:
        case BluetoothDevice.UNBOND_REASON_REMOTE_AUTH_CANCELED:
            errorMsg = R.string.bluetooth_pairing_error_message;
            mLocalManager.showError(device, R.string.bluetooth_error_title, errorMsg);
            break;
        default:
            Log.w(TAG, "showUnbondMessage: Not displaying any message for reason:" + reason);
            break;
        }
    }

    public synchronized void onProfileStateChanged(BluetoothDevice device, Profile profile,
            int newProfileState) {
        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice == null) return;

        cachedDevice.onProfileStateChanged(profile, newProfileState);
        cachedDevice.refresh();
    }

    public synchronized void onConnectingError(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice == null) return;

        /*
         * Go through the device's delegate so we don't spam the user with
         * errors connecting to different profiles, and instead make sure the
         * user sees a single error for his single 'connect' action.
         */
        cachedDevice.showConnectingError();
    }

    public synchronized void onScanningStateChanged(boolean started) {
        if (!started) return;

        // If starting a new scan, clear old visibility
        for (int i = mCachedDevices.size() - 1; i >= 0; i--) {
            CachedBluetoothDevice cachedDevice = mCachedDevices.get(i);
            cachedDevice.setVisible(false);
            checkForDeviceRemoval(cachedDevice);
        }
    }

    public synchronized void onBtClassChanged(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice != null) {
            cachedDevice.refreshBtClass();
        }
    }

    public synchronized void onUuidChanged(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice != null) {
            cachedDevice.onUuidChanged();
        }
    }
}
