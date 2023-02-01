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

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.connecteddevice.PreviouslyConnectedDeviceDashboardFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintain and update saved bluetooth devices(bonded but not connected)
 */
public class SavedBluetoothDeviceUpdater extends BluetoothDeviceUpdater
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "SavedBluetoothDeviceUpdater";
    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String PREF_KEY = "saved_bt";

    private final boolean mDisplayConnected;

    @VisibleForTesting
    BluetoothAdapter mBluetoothAdapter;

    public SavedBluetoothDeviceUpdater(Context context, DashboardFragment fragment,
            DevicePreferenceCallback devicePreferenceCallback) {
        super(context, fragment, devicePreferenceCallback);
        mDisplayConnected = (fragment instanceof PreviouslyConnectedDeviceDashboardFragment);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void forceUpdate() {
        if (mBluetoothAdapter.isEnabled()) {
            final CachedBluetoothDeviceManager cachedManager =
                    mLocalManager.getCachedDeviceManager();
            final List<BluetoothDevice> bluetoothDevices =
                    mBluetoothAdapter.getMostRecentlyConnectedDevices();
            removePreferenceIfNecessary(bluetoothDevices, cachedManager);
            for (BluetoothDevice device : bluetoothDevices) {
                final CachedBluetoothDevice cachedDevice = cachedManager.findDevice(device);
                if (cachedDevice != null && !cachedManager.isSubDevice(device)) {
                    update(cachedDevice);
                }
            }
        } else {
            removeAllDevicesFromPreference();
        }
    }

    private void removePreferenceIfNecessary(List<BluetoothDevice> bluetoothDevices,
            CachedBluetoothDeviceManager cachedManager) {
        for (BluetoothDevice device : new ArrayList<>(mPreferenceMap.keySet())) {
            if (!bluetoothDevices.contains(device)) {
                final CachedBluetoothDevice cachedDevice = cachedManager.findDevice(device);
                if (cachedDevice != null) {
                    removePreference(cachedDevice);
                }
            }
        }
    }

    @Override
    public void update(CachedBluetoothDevice cachedDevice) {
        if (isFilterMatched(cachedDevice)) {
            // Add the preference if it is new one
            addPreference(cachedDevice, BluetoothDevicePreference.SortType.TYPE_NO_SORT);
        } else {
            removePreference(cachedDevice);
        }
    }

    @Override
    public boolean isFilterMatched(CachedBluetoothDevice cachedDevice) {
        final BluetoothDevice device = cachedDevice.getDevice();
        if (DBG) {
            Log.d(TAG, "isFilterMatched() device name : " + cachedDevice.getName() +
                    ", is connected : " + device.isConnected() + ", is profile connected : "
                    + cachedDevice.isConnected());
        }
        return device.getBondState() == BluetoothDevice.BOND_BONDED
                && (mDisplayConnected || (!device.isConnected() && isDeviceInCachedDevicesList(
                cachedDevice)));
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        mMetricsFeatureProvider.logClickedPreference(preference, mFragment.getMetricsCategory());
        final CachedBluetoothDevice device = ((BluetoothDevicePreference) preference)
                .getBluetoothDevice();
        if (device.isConnected()) {
            return device.setActive();
        }
        mMetricsFeatureProvider.action(mPrefContext,
                SettingsEnums.ACTION_SETTINGS_BLUETOOTH_CONNECT);
        device.connect();
        return true;
    }

    @Override
    protected String getPreferenceKey() {
        return PREF_KEY;
    }
}
