/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.app.Activity.RESULT_OK;
import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityStatsLogUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HearingAidStatsLogUtils;

/**
 * Abstract class for providing basic interaction for a list of Bluetooth devices in bluetooth
 * device pairing detail page.
 */
public abstract class BluetoothDevicePairingDetailBase extends DeviceListPreferenceFragment {

    protected boolean mInitialScanStarted;
    @VisibleForTesting
    protected BluetoothProgressCategory mAvailableDevicesCategory;

    public BluetoothDevicePairingDetailBase() {
        super(DISALLOW_CONFIG_BLUETOOTH);
    }

    @Override
    public void initPreferencesFromPreferenceScreen() {
        mAvailableDevicesCategory = findPreference(getDeviceListKey());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mInitialScanStarted = false;
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mLocalManager == null) {
            Log.e(getLogTag(), "Bluetooth is not supported on this device");
            return;
        }
        updateBluetooth();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mLocalManager == null) {
            Log.e(getLogTag(), "Bluetooth is not supported on this device");
            return;
        }
        disableScanning();
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        super.onBluetoothStateChanged(bluetoothState);
        updateContent(bluetoothState);
        if (bluetoothState == BluetoothAdapter.STATE_ON) {
            showBluetoothTurnedOnToast();
        }
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        if (bondState == BluetoothDevice.BOND_BONDED) {
            // If one device is connected(bonded), then close this fragment.
            setResult(RESULT_OK);
            finish();
            return;
        } else if (bondState == BluetoothDevice.BOND_BONDING) {
            // Set the bond entry where binding process starts for logging hearing aid device info
            final int pageId = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider()
                    .getAttribution(getActivity());
            final int bondEntry = AccessibilityStatsLogUtils.convertToHearingAidInfoBondEntry(
                    pageId);
            HearingAidStatsLogUtils.setBondEntryForDevice(bondEntry, cachedDevice);
        }
        if (mSelectedDevice != null && cachedDevice != null) {
            BluetoothDevice device = cachedDevice.getDevice();
            if (device != null && mSelectedDevice.equals(device)
                    && bondState == BluetoothDevice.BOND_NONE) {
                // If currently selected device failed to bond, restart scanning
                enableScanning();
            }
        }
    }

    @Override
    public void onProfileConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state,
            int bluetoothProfile) {
        // This callback is used to handle the case that bonded device is connected in pairing list.
        // 1. If user selected multiple bonded devices in pairing list, after connected
        // finish this page.
        // 2. If the bonded devices auto connected in paring list, after connected it will be
        // removed from paring list.
        if (cachedDevice != null && cachedDevice.isConnected()) {
            final BluetoothDevice device = cachedDevice.getDevice();
            if (device != null && mSelectedList.contains(device)) {
                setResult(RESULT_OK);
                finish();
            } else {
                onDeviceDeleted(cachedDevice);
            }
        }
    }

    @Override
    public void enableScanning() {
        // Clear all device states before first scan
        if (!mInitialScanStarted) {
            if (mAvailableDevicesCategory != null) {
                removeAllDevices();
            }
            mLocalManager.getCachedDeviceManager().clearNonBondedDevices();
            mInitialScanStarted = true;
        }
        super.enableScanning();
    }

    @Override
    public void onDevicePreferenceClick(BluetoothDevicePreference btPreference) {
        disableScanning();
        super.onDevicePreferenceClick(btPreference);
    }

    @VisibleForTesting
    void updateBluetooth() {
        if (mBluetoothAdapter.isEnabled()) {
            updateContent(mBluetoothAdapter.getState());
        } else {
            // Turn on bluetooth if it is disabled
            mBluetoothAdapter.enable();
        }
    }

    /**
     * Enables the scanning when {@code bluetoothState} is on, or finish the page when
     * {@code bluetoothState} is off.
     *
     * @param bluetoothState the current Bluetooth state, the possible values that will handle here:
     * {@link android.bluetooth.BluetoothAdapter#STATE_OFF},
     * {@link android.bluetooth.BluetoothAdapter#STATE_ON},
     */
    @VisibleForTesting
    public void updateContent(int bluetoothState) {
        switch (bluetoothState) {
            case BluetoothAdapter.STATE_ON:
                mBluetoothAdapter.enable();
                enableScanning();
                break;

            case BluetoothAdapter.STATE_OFF:
                finish();
                break;
        }
    }

    @VisibleForTesting
    void showBluetoothTurnedOnToast() {
        Toast.makeText(getContext(), R.string.connected_device_bluetooth_turned_on_toast,
                Toast.LENGTH_SHORT).show();
    }
}
