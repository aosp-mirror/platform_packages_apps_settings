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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.support.annotation.VisibleForTesting;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import android.support.v7.preference.Preference;
import android.util.Log;

/**
 * Maintain and update saved bluetooth devices(bonded but not connected)
 */
public class SavedBluetoothDeviceUpdater extends BluetoothDeviceUpdater
        implements Preference.OnPreferenceClickListener {
    private static final String TAG = "SavedBluetoothDeviceUpdater";

    public SavedBluetoothDeviceUpdater(Context context, DashboardFragment fragment,
            DevicePreferenceCallback devicePreferenceCallback) {
        super(context, fragment, devicePreferenceCallback);
    }

    @VisibleForTesting
    SavedBluetoothDeviceUpdater(DashboardFragment fragment,
            DevicePreferenceCallback devicePreferenceCallback,
            LocalBluetoothManager localBluetoothManager) {
        super(fragment, devicePreferenceCallback, localBluetoothManager);
    }

    @Override
    public void onProfileConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state,
            int bluetoothProfile) {
        if (state == BluetoothProfile.STATE_CONNECTED) {
            removePreference(cachedDevice);
        } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
            addPreference(cachedDevice);
        }
    }

    @Override
    public boolean isFilterMatched(CachedBluetoothDevice cachedDevice) {
        final BluetoothDevice device = cachedDevice.getDevice();
        return device.getBondState() == BluetoothDevice.BOND_BONDED && !device.isConnected();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        final CachedBluetoothDevice device = ((BluetoothDevicePreference) preference)
                .getBluetoothDevice();
        device.connect(true);
        return true;
    }
}
