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

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.Indexable;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.FooterPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * BluetoothPairingDetail is a page to scan bluetooth devices and pair them.
 */
public class BluetoothPairingDetail extends DeviceListPreferenceFragment implements
        Indexable {
    private static final String TAG = "BluetoothPairingDetail";

    @VisibleForTesting
    static final String KEY_AVAIL_DEVICES = "available_devices";
    @VisibleForTesting
    static final String KEY_FOOTER_PREF = "footer_preference";

    @VisibleForTesting
    BluetoothDeviceNamePreferenceController mDeviceNamePrefController;
    @VisibleForTesting
    BluetoothProgressCategory mAvailableDevicesCategory;
    @VisibleForTesting
    FooterPreference mFooterPreference;
    @VisibleForTesting
    AlwaysDiscoverable mAlwaysDiscoverable;

    private boolean mInitialScanStarted;

    public BluetoothPairingDetail() {
        super(DISALLOW_CONFIG_BLUETOOTH);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mInitialScanStarted = false;
        mAlwaysDiscoverable = new AlwaysDiscoverable(getContext(), mLocalAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        updateContent(mLocalAdapter.getBluetoothState());
        mAvailableDevicesCategory.setProgress(mLocalAdapter.isDiscovering());
    }

    @Override
    public void onStop() {
        super.onStop();

        // Make the device only visible to connected devices.
        mAlwaysDiscoverable.stop();
        disableScanning();
    }

    @Override
    void initPreferencesFromPreferenceScreen() {
        mAvailableDevicesCategory = (BluetoothProgressCategory) findPreference(KEY_AVAIL_DEVICES);
        mFooterPreference = (FooterPreference) findPreference(KEY_FOOTER_PREF);
        mFooterPreference.setSelectable(false);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.BLUETOOTH_PAIRING;
    }

    @Override
    void enableScanning() {
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
    void onDevicePreferenceClick(BluetoothDevicePreference btPreference) {
        disableScanning();
        super.onDevicePreferenceClick(btPreference);
    }

    @Override
    public void onScanningStateChanged(boolean started) {
        super.onScanningStateChanged(started);
        started |= mScanEnabled;
        mAvailableDevicesCategory.setProgress(started);
    }

    @VisibleForTesting
    void updateContent(int bluetoothState) {
        switch (bluetoothState) {
            case BluetoothAdapter.STATE_ON:
                mDevicePreferenceMap.clear();
                mLocalAdapter.setBluetoothEnabled(true);

                addDeviceCategory(mAvailableDevicesCategory,
                        R.string.bluetooth_preference_found_devices,
                        BluetoothDeviceFilter.UNBONDED_DEVICE_FILTER, mInitialScanStarted);
                updateFooterPreference(mFooterPreference);
                mAlwaysDiscoverable.start();
                enableScanning();
                break;

            case BluetoothAdapter.STATE_OFF:
                finish();
                break;
        }
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        super.onBluetoothStateChanged(bluetoothState);
        updateContent(bluetoothState);
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        if (bondState == BluetoothDevice.BOND_BONDED) {
            // If one device is connected(bonded), then close this fragment.
            finish();
            return;
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
    protected int getHelpResource() {
        return R.string.help_url_bluetooth;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_pairing_detail;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        mDeviceNamePrefController = new BluetoothDeviceNamePreferenceController(context,
                getLifecycle());
        controllers.add(mDeviceNamePrefController);

        return controllers;
    }

    @Override
    public String getDeviceListKey() {
        return KEY_AVAIL_DEVICES;
    }

}
