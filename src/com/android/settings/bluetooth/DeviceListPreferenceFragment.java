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
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.BidiFormatter;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Parent class for settings fragments that contain a list of Bluetooth
 * devices.
 *
 * @see DevicePickerFragment
 */
// TODO: Refactor this fragment
public abstract class DeviceListPreferenceFragment extends
        RestrictedDashboardFragment implements BluetoothCallback {

    private static final String TAG = "DeviceListPreferenceFragment";

    private static final String KEY_BT_SCAN = "bt_scan";

    // Copied from BluetoothDeviceNoNamePreferenceController.java
    private static final String BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY =
            "persist.bluetooth.showdeviceswithoutnames";

    private BluetoothDeviceFilter.Filter mFilter;
    private List<ScanFilter> mLeScanFilters;
    private ScanCallback mScanCallback;

    @VisibleForTesting
    protected boolean mScanEnabled;

    protected BluetoothDevice mSelectedDevice;

    protected BluetoothAdapter mBluetoothAdapter;
    protected LocalBluetoothManager mLocalManager;
    protected CachedBluetoothDeviceManager mCachedDeviceManager;

    @VisibleForTesting
    protected PreferenceGroup mDeviceListGroup;

    protected final HashMap<CachedBluetoothDevice, BluetoothDevicePreference> mDevicePreferenceMap =
            new HashMap<>();
    protected final List<BluetoothDevice> mSelectedList = new ArrayList<>();

    protected boolean mShowDevicesWithoutNames;

    public DeviceListPreferenceFragment(String restrictedKey) {
        super(restrictedKey);
        mFilter = BluetoothDeviceFilter.ALL_FILTER;
    }

    protected final void setFilter(BluetoothDeviceFilter.Filter filter) {
        mFilter = filter;
    }

    protected final void setFilter(int filterType) {
        mFilter = BluetoothDeviceFilter.getFilter(filterType);
    }

    /**
     * Sets the bluetooth device scanning filter with {@link ScanFilter}s. It will change to start
     * {@link BluetoothLeScanner} which will scan BLE device only.
     *
     * @param leScanFilters list of settings to filter scan result
     */
    protected void setFilter(List<ScanFilter> leScanFilters) {
        mFilter = null;
        mLeScanFilters = leScanFilters;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocalManager = Utils.getLocalBtManager(getActivity());
        if (mLocalManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mCachedDeviceManager = mLocalManager.getCachedDeviceManager();
        mShowDevicesWithoutNames = SystemProperties.getBoolean(
                BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY, false);

        initPreferencesFromPreferenceScreen();

        mDeviceListGroup = (PreferenceCategory) findPreference(getDeviceListKey());
    }

    /** find and update preference that already existed in preference screen */
    protected abstract void initPreferencesFromPreferenceScreen();

    @Override
    public void onStart() {
        super.onStart();
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
                mCachedDeviceManager.getCachedDevicesCopy();
        for (CachedBluetoothDevice cachedDevice : cachedDevices) {
            onDeviceAdded(cachedDevice);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (KEY_BT_SCAN.equals(preference.getKey())) {
            startScanning();
            return true;
        }

        if (preference instanceof BluetoothDevicePreference) {
            BluetoothDevicePreference btPreference = (BluetoothDevicePreference) preference;
            CachedBluetoothDevice device = btPreference.getCachedDevice();
            mSelectedDevice = device.getDevice();
            mSelectedList.add(mSelectedDevice);
            onDevicePreferenceClick(btPreference);
            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    protected void onDevicePreferenceClick(BluetoothDevicePreference btPreference) {
        btPreference.onClicked();
    }

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
        if (mDevicePreferenceMap.get(cachedDevice) != null) {
            return;
        }

        // Prevent updates while the list shows one of the state messages
        if (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_ON) return;

        if (mLeScanFilters != null
                || (mFilter != null && mFilter.matches(cachedDevice.getDevice()))) {
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
            preference = new BluetoothDevicePreference(getPrefContext(), cachedDevice,
                    mShowDevicesWithoutNames, BluetoothDevicePreference.SortType.TYPE_FIFO);
            preference.setKey(key);
            //Set hideSecondTarget is true if it's bonded device.
            preference.hideSecondTarget(true);
            mDeviceListGroup.addPreference(preference);
        }

        initDevicePreference(preference);
        mDevicePreferenceMap.put(cachedDevice, preference);
    }

    protected void initDevicePreference(BluetoothDevicePreference preference) {
        // Does nothing by default
    }

    @VisibleForTesting
    void updateFooterPreference(Preference myDevicePreference) {
        final BidiFormatter bidiFormatter = BidiFormatter.getInstance();

        myDevicePreference.setTitle(getString(
                R.string.bluetooth_footer_mac_message,
                bidiFormatter.unicodeWrap(mBluetoothAdapter.getAddress())));
    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
        BluetoothDevicePreference preference = mDevicePreferenceMap.remove(cachedDevice);
        if (preference != null) {
            mDeviceListGroup.removePreference(preference);
        }
    }

    @VisibleForTesting
    protected void enableScanning() {
        // BluetoothAdapter already handles repeated scan requests
        if (!mScanEnabled) {
            startScanning();
            mScanEnabled = true;
        }
    }

    @VisibleForTesting
    protected void disableScanning() {
        if (mScanEnabled) {
            stopScanning();
            mScanEnabled = false;
        }
    }

    @Override
    public void onScanningStateChanged(boolean started) {
        if (!started && mScanEnabled) {
            startScanning();
        }
    }

    /**
     * Return the key of the {@link PreferenceGroup} that contains the bluetooth devices
     */
    public abstract String getDeviceListKey();

    public boolean shouldShowDevicesWithoutNames() {
        return mShowDevicesWithoutNames;
    }

    @VisibleForTesting
    void startScanning() {
        if (mFilter != null) {
            startClassicScanning();
        } else if (mLeScanFilters != null) {
            startLeScanning();
        }

    }

    @VisibleForTesting
    void stopScanning() {
        if (mFilter != null) {
            stopClassicScanning();
        } else if (mLeScanFilters != null) {
            stopLeScanning();
        }
    }

    private void startClassicScanning() {
        if (!mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.startDiscovery();
        }
    }

    private void stopClassicScanning() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    private void startLeScanning() {
        final BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                final BluetoothDevice device = result.getDevice();
                CachedBluetoothDevice cachedDevice = mCachedDeviceManager.findDevice(device);
                if (cachedDevice == null) {
                    cachedDevice = mCachedDeviceManager.addDevice(device);
                }
                onDeviceAdded(cachedDevice);
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.w(TAG, "BLE Scan failed with error code " + errorCode);
            }
        };
        scanner.startScan(mLeScanFilters, settings, mScanCallback);
    }

    private void stopLeScanning() {
        final BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (scanner != null) {
            scanner.stopScan(mScanCallback);
        }
    }
}
