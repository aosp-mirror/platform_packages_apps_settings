/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.os.Bundle;
import android.os.SystemProperties;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.text.BidiFormatter;
import android.util.Log;

import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.R;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.Collection;
import java.util.WeakHashMap;

/**
 * Parent class for settings fragments that contain a list of Bluetooth
 * devices.
 *
 * @see BluetoothSettings
 * @see DevicePickerFragment
 */
// TODO: Refactor this fragment
public abstract class DeviceListPreferenceFragment extends
        RestrictedDashboardFragment implements BluetoothCallback {

    private static final String TAG = "DeviceListPreferenceFragment";

    private static final String KEY_BT_SCAN = "bt_scan";

    // Copied from DevelopmentSettings.java
    private static final String BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY =
            "persist.bluetooth.showdeviceswithoutnames";

    private BluetoothDeviceFilter.Filter mFilter;

    @VisibleForTesting
    boolean mScanEnabled;

    BluetoothDevice mSelectedDevice;

    LocalBluetoothAdapter mLocalAdapter;
    LocalBluetoothManager mLocalManager;

    @VisibleForTesting
    PreferenceGroup mDeviceListGroup;

    final WeakHashMap<CachedBluetoothDevice, BluetoothDevicePreference> mDevicePreferenceMap =
            new WeakHashMap<CachedBluetoothDevice, BluetoothDevicePreference>();

    boolean mShowDevicesWithoutNames;

    DeviceListPreferenceFragment(String restrictedKey) {
        super(restrictedKey);
        mFilter = BluetoothDeviceFilter.ALL_FILTER;
    }

    final void setFilter(BluetoothDeviceFilter.Filter filter) {
        mFilter = filter;
    }

    final void setFilter(int filterType) {
        mFilter = BluetoothDeviceFilter.getFilter(filterType);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocalManager = Utils.getLocalBtManager(getActivity());
        if (mLocalManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
        mLocalAdapter = mLocalManager.getBluetoothAdapter();

        initPreferencesFromPreferenceScreen();

        mDeviceListGroup = (PreferenceCategory) findPreference(getDeviceListKey());
    }

    /** find and update preference that already existed in preference screen */
    abstract void initPreferencesFromPreferenceScreen();

    @Override
    public void onStart() {
        super.onStart();
        mShowDevicesWithoutNames = SystemProperties.getBoolean(
                BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY, false);
        if (mLocalManager == null || isUiRestricted()) return;

        mLocalManager.setForegroundActivity(getActivity());
        mLocalManager.getEventManager().registerCallback(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mLocalManager == null || isUiRestricted()) {
            return;
        }

        removeAllDevices();
        mLocalManager.setForegroundActivity(null);
        mLocalManager.getEventManager().unregisterCallback(this);
    }

    void removeAllDevices() {
        mDevicePreferenceMap.clear();
        mDeviceListGroup.removeAll();
    }

    void addCachedDevices() {
        Collection<CachedBluetoothDevice> cachedDevices =
                mLocalManager.getCachedDeviceManager().getCachedDevicesCopy();
        for (CachedBluetoothDevice cachedDevice : cachedDevices) {
            onDeviceAdded(cachedDevice);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (KEY_BT_SCAN.equals(preference.getKey())) {
            mLocalAdapter.startScanning(true);
            return true;
        }

        if (preference instanceof BluetoothDevicePreference) {
            BluetoothDevicePreference btPreference = (BluetoothDevicePreference) preference;
            CachedBluetoothDevice device = btPreference.getCachedDevice();
            mSelectedDevice = device.getDevice();
            onDevicePreferenceClick(btPreference);
            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    void onDevicePreferenceClick(BluetoothDevicePreference btPreference) {
        btPreference.onClicked();
    }

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
        if (mDevicePreferenceMap.get(cachedDevice) != null) {
            return;
        }

        // Prevent updates while the list shows one of the state messages
        if (mLocalAdapter.getBluetoothState() != BluetoothAdapter.STATE_ON) return;

        if (mFilter.matches(cachedDevice.getDevice())) {
            createDevicePreference(cachedDevice);
        }
    }

    void createDevicePreference(CachedBluetoothDevice cachedDevice) {
        if (mDeviceListGroup == null) {
            Log.w(TAG, "Trying to create a device preference before the list group/category "
                    + "exists!");
            return;
        }

        String key = cachedDevice.getDevice().getAddress();
        BluetoothDevicePreference preference = (BluetoothDevicePreference) getCachedPreference(key);

        if (preference == null) {
            preference = new BluetoothDevicePreference(getPrefContext(), cachedDevice, this);
            preference.setKey(key);
            mDeviceListGroup.addPreference(preference);
        } else {
            // Tell the preference it is being re-used in case there is new info in the
            // cached device.
            preference.rebind();
        }

        initDevicePreference(preference);
        mDevicePreferenceMap.put(cachedDevice, preference);
    }

    /**
     * Overridden in {@link BluetoothSettings} to add a listener.
     * @param preference the newly added preference
     */
    void initDevicePreference(BluetoothDevicePreference preference) {
        // Does nothing by default
    }

    @VisibleForTesting
    void updateFooterPreference(Preference myDevicePreference) {
        final BidiFormatter bidiFormatter = BidiFormatter.getInstance();

        myDevicePreference.setTitle(getString(
                R.string.bluetooth_footer_mac_message,
                bidiFormatter.unicodeWrap(mLocalAdapter.getAddress())));
    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
        BluetoothDevicePreference preference = mDevicePreferenceMap.remove(cachedDevice);
        if (preference != null) {
            mDeviceListGroup.removePreference(preference);
        }
    }

    @VisibleForTesting
    void enableScanning() {
        // LocalBluetoothAdapter already handles repeated scan requests
        mLocalAdapter.startScanning(true);
        mScanEnabled = true;
    }

    @VisibleForTesting
    void disableScanning() {
        mLocalAdapter.stopScanning();
        mScanEnabled = false;
    }

    @Override
    public void onScanningStateChanged(boolean started) {
        if (!started && mScanEnabled) {
            mLocalAdapter.startScanning(true);
        }
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {}

    /**
     * Add bluetooth device preferences to {@code preferenceGroup} which satisfy the {@code filter}
     *
     * This method will also (1) set the title for {@code preferenceGroup} and (2) change the
     * default preferenceGroup and filter
     * @param preferenceGroup
     * @param titleId
     * @param filter
     * @param addCachedDevices
     */
    public void addDeviceCategory(PreferenceGroup preferenceGroup, int titleId,
            BluetoothDeviceFilter.Filter filter, boolean addCachedDevices) {
        cacheRemoveAllPrefs(preferenceGroup);
        preferenceGroup.setTitle(titleId);
        mDeviceListGroup = preferenceGroup;
        setFilter(filter);
        if (addCachedDevices) {
            addCachedDevices();
        }
        preferenceGroup.setEnabled(true);
        removeCachedPrefs(preferenceGroup);
    }

    public void onConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) { }

    /**
     * Return the key of the {@link PreferenceGroup} that contains the bluetooth devices
     */
    public abstract String getDeviceListKey();

    public boolean shouldShowDevicesWithoutNames() {
        return mShowDevicesWithoutNames;
    }
}
