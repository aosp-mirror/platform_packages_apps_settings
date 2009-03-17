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

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.widget.Toast;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.bluetooth.LocalBluetoothManager.Callback;

import java.util.ArrayList;
import java.util.List;

/**
 * LocalBluetoothDeviceManager manages the set of remote Bluetooth devices.
 */
public class LocalBluetoothDeviceManager {
    private static final String TAG = "LocalBluetoothDeviceManager";

    final LocalBluetoothManager mLocalManager;
    final List<Callback> mCallbacks;
    
    final List<LocalBluetoothDevice> mDevices = new ArrayList<LocalBluetoothDevice>();

    public LocalBluetoothDeviceManager(LocalBluetoothManager localManager) {
        mLocalManager = localManager;
        mCallbacks = localManager.getCallbacks();
        readPairedDevices();
    }

    private synchronized boolean readPairedDevices() {
        BluetoothDevice manager = mLocalManager.getBluetoothManager();
        String[] bondedAddresses = manager.listBonds();
        if (bondedAddresses == null) return false;
        
        boolean deviceAdded = false;
        for (String address : bondedAddresses) {
            LocalBluetoothDevice device = findDevice(address);
            if (device == null) {
                device = new LocalBluetoothDevice(mLocalManager.getContext(), address);
                mDevices.add(device);
                dispatchDeviceAdded(device);
                deviceAdded = true;
            }
        }
        
        return deviceAdded;
    }
    
    public synchronized List<LocalBluetoothDevice> getDevicesCopy() {
        return new ArrayList<LocalBluetoothDevice>(mDevices);
    }
    
    void onBluetoothStateChanged(boolean enabled) {
        if (enabled) {
            readPairedDevices();
        }
    }

    public synchronized void onDeviceAppeared(String address, short rssi) {
        boolean deviceAdded = false;
        
        LocalBluetoothDevice device = findDevice(address);
        if (device == null) {
            device = new LocalBluetoothDevice(mLocalManager.getContext(), address);
            mDevices.add(device);
            deviceAdded = true;
        }
        
        device.setRssi(rssi);
        device.setVisible(true);
        
        if (deviceAdded) {
            dispatchDeviceAdded(device);
        }
    }
    
    public synchronized void onDeviceDisappeared(String address) {
        LocalBluetoothDevice device = findDevice(address);
        if (device == null) return;
        
        device.setVisible(false);
        checkForDeviceRemoval(device);
    }
    
    private void checkForDeviceRemoval(LocalBluetoothDevice device) {
        if (device.getBondState() == BluetoothDevice.BOND_NOT_BONDED &&
                !device.isVisible()) {
            // If device isn't paired, remove it altogether
            mDevices.remove(device);
            dispatchDeviceDeleted(device);
        }            
    }
    
    public synchronized void onDeviceNameUpdated(String address) {
        LocalBluetoothDevice device = findDevice(address);
        if (device != null) {
            device.refreshName();
        }
    }

    public synchronized LocalBluetoothDevice findDevice(String address) {
        
        for (int i = mDevices.size() - 1; i >= 0; i--) {
            LocalBluetoothDevice device = mDevices.get(i);
            
            if (device.getAddress().equals(address)) {
                return device;
            }
        }
        
        return null;
    }
    
    /**
     * Attempts to get the name of a remote device, otherwise returns the address.
     * 
     * @param address The address.
     * @return The name, or if unavailable, the address.
     */
    public String getName(String address) {
        LocalBluetoothDevice device = findDevice(address);
        return device != null ? device.getName() : address;
    }
    
    private void dispatchDeviceAdded(LocalBluetoothDevice device) {
        synchronized (mCallbacks) {
            for (Callback callback : mCallbacks) {
                callback.onDeviceAdded(device);
            }
        }
        
        // TODO: divider between prev paired/connected and scanned
    }
    
    private void dispatchDeviceDeleted(LocalBluetoothDevice device) {
        synchronized (mCallbacks) {
            for (Callback callback : mCallbacks) {
                callback.onDeviceDeleted(device);
            }
        }
    }

    public synchronized void onBondingStateChanged(String address, int bondState) {
        LocalBluetoothDevice device = findDevice(address);
        if (device == null) {
            if (!readPairedDevices()) {
                Log.e(TAG, "Got bonding state changed for " + address +
                        ", but we have no record of that device.");
            }
            return;
        }

        device.refresh();

        if (bondState == BluetoothDevice.BOND_BONDED) {
            // Auto-connect after pairing
            device.connect();
        }
    }

    /**
     * Called when there is a bonding error.
     * 
     * @param address The address of the remote device.
     * @param reason The reason, one of the error reasons from
     *            BluetoothDevice.UNBOND_REASON_*
     */
    public synchronized void onBondingError(String address, int reason) {
        mLocalManager.showError(address, R.string.bluetooth_error_title,
                (reason == BluetoothDevice.UNBOND_REASON_AUTH_FAILED) ?
                        R.string.bluetooth_pairing_pin_error_message :
                        R.string.bluetooth_pairing_error_message);
    }
    
    public synchronized void onProfileStateChanged(String address) {
        LocalBluetoothDevice device = findDevice(address);
        if (device == null) return;
        
        device.refresh();
    }
    
    public synchronized void onConnectingError(String address) {
        LocalBluetoothDevice device = findDevice(address);
        if (device == null) return;
        
        /*
         * Go through the device's delegate so we don't spam the user with
         * errors connecting to different profiles, and instead make sure the
         * user sees a single error for his single 'connect' action.
         */
        device.showConnectingError();
    }
    
    public synchronized void onScanningStateChanged(boolean started) {
        if (!started) return;
        
        // If starting a new scan, clear old visibility
        for (int i = mDevices.size() - 1; i >= 0; i--) {
            LocalBluetoothDevice device = mDevices.get(i);
            device.setVisible(false);
            checkForDeviceRemoval(device);
        }
    }
    
    public synchronized void onBtClassChanged(String address) {
        LocalBluetoothDevice device = findDevice(address);
        if (device != null) {
            device.refreshBtClass();
        }
    }
}
