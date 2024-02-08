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

package com.android.settings.accessibility;

import static android.app.Activity.RESULT_OK;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.bluetooth.BluetoothDevicePreference;
import com.android.settings.bluetooth.BluetoothProgressCategory;
import com.android.settings.bluetooth.Utils;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.HearingAidInfo;
import com.android.settingslib.bluetooth.HearingAidStatsLogUtils;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This fragment shows all scanned hearing devices through BLE scanning. Users can
 * pair them in this page.
 */
public class HearingDevicePairingFragment extends RestrictedDashboardFragment implements
        BluetoothCallback {

    private static final boolean DEBUG = true;
    private static final String TAG = "HearingDevicePairingFragment";
    private static final String BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY =
            "persist.bluetooth.showdeviceswithoutnames";
    private static final String KEY_AVAILABLE_HEARING_DEVICES = "available_hearing_devices";

    LocalBluetoothManager mLocalManager;
    @Nullable
    BluetoothAdapter mBluetoothAdapter;
    @Nullable
    CachedBluetoothDeviceManager mCachedDeviceManager;

    private boolean mShowDevicesWithoutNames;
    @Nullable
    private BluetoothProgressCategory mAvailableHearingDeviceGroup;

    @Nullable
    BluetoothDevice mSelectedDevice;
    final List<BluetoothDevice> mSelectedDeviceList = new ArrayList<>();
    final List<BluetoothGatt> mConnectingGattList = new ArrayList<>();
    final Map<CachedBluetoothDevice, BluetoothDevicePreference> mDevicePreferenceMap =
            new HashMap<>();

    private List<ScanFilter> mLeScanFilters;

    public HearingDevicePairingFragment() {
        super(DISALLOW_CONFIG_BLUETOOTH);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocalManager = Utils.getLocalBtManager(getActivity());
        if (mLocalManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
        mBluetoothAdapter = getSystemService(BluetoothManager.class).getAdapter();
        mCachedDeviceManager = mLocalManager.getCachedDeviceManager();
        mShowDevicesWithoutNames = SystemProperties.getBoolean(
                BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY, false);

        initPreferencesFromPreferenceScreen();
        initHearingDeviceLeScanFilters();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(ViewAllBluetoothDevicesPreferenceController.class).init(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mLocalManager == null || mBluetoothAdapter == null || isUiRestricted()) {
            return;
        }
        mLocalManager.setForegroundActivity(getActivity());
        mLocalManager.getEventManager().registerCallback(this);
        if (mBluetoothAdapter.isEnabled()) {
            startScanning();
        } else {
            // Turn on bluetooth if it is disabled
            mBluetoothAdapter.enable();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mLocalManager == null || isUiRestricted()) {
            return;
        }
        stopScanning();
        removeAllDevices();
        for (BluetoothGatt gatt: mConnectingGattList) {
            gatt.disconnect();
        }
        mConnectingGattList.clear();
        mLocalManager.setForegroundActivity(null);
        mLocalManager.getEventManager().unregisterCallback(this);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof BluetoothDevicePreference) {
            stopScanning();
            BluetoothDevicePreference devicePreference = (BluetoothDevicePreference) preference;
            mSelectedDevice = devicePreference.getCachedDevice().getDevice();
            if (mSelectedDevice != null) {
                mSelectedDeviceList.add(mSelectedDevice);
            }
            devicePreference.onClicked();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onDeviceDeleted(@NonNull CachedBluetoothDevice cachedDevice) {
        removeDevice(cachedDevice);
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        switch (bluetoothState) {
            case BluetoothAdapter.STATE_ON:
                startScanning();
                showBluetoothTurnedOnToast();
                break;
            case BluetoothAdapter.STATE_OFF:
                finish();
                break;
        }
    }

    @Override
    public void onDeviceBondStateChanged(@NonNull CachedBluetoothDevice cachedDevice,
            int bondState) {
        if (DEBUG) {
            Log.d(TAG, "onDeviceBondStateChanged: " + cachedDevice.getDevice() + ", state = "
                    + bondState);
        }
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
        if (mSelectedDevice != null) {
            BluetoothDevice device = cachedDevice.getDevice();
            if (mSelectedDevice.equals(device) && bondState == BluetoothDevice.BOND_NONE) {
                // If current selected device failed to bond, restart scanning
                startScanning();
            }
        }
    }

    @Override
    public void onProfileConnectionStateChanged(@NonNull CachedBluetoothDevice cachedDevice,
            int state, int bluetoothProfile) {
        // This callback is used to handle the case that bonded device is connected in pairing list.
        // 1. If user selected multiple bonded devices in pairing list, after connected
        // finish this page.
        // 2. If the bonded devices auto connected in paring list, after connected it will be
        // removed from paring list.
        if (cachedDevice.isConnected()) {
            final BluetoothDevice device = cachedDevice.getDevice();
            if (device != null && mSelectedDeviceList.contains(device)) {
                setResult(RESULT_OK);
                finish();
            } else {
                removeDevice(cachedDevice);
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.HEARING_AID_PAIRING;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.hearing_device_pairing_fragment;
    }


    @Override
    protected String getLogTag() {
        return TAG;
    }

    void addDevice(CachedBluetoothDevice cachedDevice) {
        if (mBluetoothAdapter == null) {
            return;
        }
        // Do not create new preference while the list shows one of the state messages
        if (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_ON) {
            return;
        }
        if (mDevicePreferenceMap.get(cachedDevice) != null) {
            return;
        }
        String key = cachedDevice.getDevice().getAddress();
        BluetoothDevicePreference preference = (BluetoothDevicePreference) getCachedPreference(key);
        if (preference == null) {
            preference = new BluetoothDevicePreference(getPrefContext(), cachedDevice,
                    mShowDevicesWithoutNames, BluetoothDevicePreference.SortType.TYPE_FIFO);
            preference.setKey(key);
            preference.hideSecondTarget(true);
        }
        if (mAvailableHearingDeviceGroup != null) {
            mAvailableHearingDeviceGroup.addPreference(preference);
        }
        mDevicePreferenceMap.put(cachedDevice, preference);
        if (DEBUG) {
            Log.d(TAG, "Add device. device: " + cachedDevice.getDevice());
        }
    }

    void removeDevice(CachedBluetoothDevice cachedDevice) {
        if (DEBUG) {
            Log.d(TAG, "removeDevice: " + cachedDevice.getDevice());
        }
        BluetoothDevicePreference preference = mDevicePreferenceMap.remove(cachedDevice);
        if (mAvailableHearingDeviceGroup != null && preference != null) {
            mAvailableHearingDeviceGroup.removePreference(preference);
        }
    }

    void startScanning() {
        if (mCachedDeviceManager != null) {
            mCachedDeviceManager.clearNonBondedDevices();
        }
        removeAllDevices();
        startLeScanning();
    }

    void stopScanning() {
        stopLeScanning();
    }

    private final ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            handleLeScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result: results) {
                handleLeScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.w(TAG, "BLE Scan failed with error code " + errorCode);
        }
    };

    void handleLeScanResult(ScanResult result) {
        if (mCachedDeviceManager == null) {
            return;
        }
        final BluetoothDevice device = result.getDevice();
        CachedBluetoothDevice cachedDevice = mCachedDeviceManager.findDevice(device);
        if (cachedDevice == null) {
            cachedDevice = mCachedDeviceManager.addDevice(device);
        } else if (cachedDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            if (DEBUG) {
                Log.d(TAG, "Skip this device, already bonded: " + cachedDevice.getDevice());
            }
            return;
        }
        if (cachedDevice.getHearingAidInfo() == null) {
            if (DEBUG) {
                Log.d(TAG, "Set hearing aid info on device: " + cachedDevice.getDevice());
            }
            cachedDevice.setHearingAidInfo(new HearingAidInfo.Builder().build());
        }
        // No need to handle the device if the device is already in the list or discovering services
        if (mDevicePreferenceMap.get(cachedDevice) == null
                && mConnectingGattList.stream().noneMatch(
                        gatt -> gatt.getDevice().equals(device))) {
            if (isAndroidCompatibleHearingAid(result)) {
                addDevice(cachedDevice);
            } else {
                discoverServices(cachedDevice);
            }
        }
    }

    void startLeScanning() {
        if (mBluetoothAdapter == null) {
            return;
        }
        if (DEBUG) {
            Log.v(TAG, "startLeScanning");
        }
        final BluetoothLeScanner leScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (leScanner == null) {
            Log.w(TAG, "LE scanner not found, cannot start LE scanning");
        } else {
            final ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setLegacy(false)
                    .build();
            leScanner.startScan(mLeScanFilters, settings, mLeScanCallback);
            if (mAvailableHearingDeviceGroup != null) {
                mAvailableHearingDeviceGroup.setProgress(true);
            }
        }
    }

    void stopLeScanning() {
        if (mBluetoothAdapter == null) {
            return;
        }
        if (DEBUG) {
            Log.v(TAG, "stopLeScanning");
        }
        final BluetoothLeScanner leScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (leScanner != null) {
            leScanner.stopScan(mLeScanCallback);
            if (mAvailableHearingDeviceGroup != null) {
                mAvailableHearingDeviceGroup.setProgress(false);
            }
        }
    }

    private void removeAllDevices() {
        mDevicePreferenceMap.clear();
        if (mAvailableHearingDeviceGroup != null) {
            mAvailableHearingDeviceGroup.removeAll();
        }
    }

    void initPreferencesFromPreferenceScreen() {
        mAvailableHearingDeviceGroup = findPreference(KEY_AVAILABLE_HEARING_DEVICES);
    }

    private void initHearingDeviceLeScanFilters() {
        mLeScanFilters = new ArrayList<>();
        // Filters for ASHA hearing aids
        mLeScanFilters.add(
                new ScanFilter.Builder().setServiceUuid(BluetoothUuid.HEARING_AID).build());
        mLeScanFilters.add(new ScanFilter.Builder()
                .setServiceData(BluetoothUuid.HEARING_AID, new byte[0]).build());
        // Filters for LE audio hearing aids
        mLeScanFilters.add(new ScanFilter.Builder().setServiceUuid(BluetoothUuid.HAS).build());
        mLeScanFilters.add(new ScanFilter.Builder()
                .setServiceData(BluetoothUuid.HAS, new byte[0]).build());
        // Filters for MFi hearing aids
        mLeScanFilters.add(new ScanFilter.Builder().setServiceUuid(BluetoothUuid.MFI_HAS).build());
        mLeScanFilters.add(new ScanFilter.Builder()
                .setServiceData(BluetoothUuid.MFI_HAS, new byte[0]).build());
    }

    boolean isAndroidCompatibleHearingAid(ScanResult scanResult) {
        ScanRecord scanRecord = scanResult.getScanRecord();
        if (scanRecord == null) {
            if (DEBUG) {
                Log.d(TAG, "Scan record is null, not compatible with Android. device: "
                        + scanResult.getDevice());
            }
            return false;
        }
        List<ParcelUuid> uuids = scanRecord.getServiceUuids();
        if (uuids != null) {
            if (uuids.contains(BluetoothUuid.HEARING_AID) || uuids.contains(BluetoothUuid.HAS)) {
                if (DEBUG) {
                    Log.d(TAG, "Scan record uuid matched, compatible with Android. device: "
                            + scanResult.getDevice());
                }
                return true;
            }
        }
        if (scanRecord.getServiceData(BluetoothUuid.HEARING_AID) != null
                || scanRecord.getServiceData(BluetoothUuid.HAS) != null) {
            if (DEBUG) {
                Log.d(TAG, "Scan record service data matched, compatible with Android. device: "
                        + scanResult.getDevice());
            }
            return true;
        }
        if (DEBUG) {
            Log.d(TAG, "Scan record mismatched, not compatible with Android. device: "
                    + scanResult.getDevice());
        }
        return false;
    }

    void discoverServices(CachedBluetoothDevice cachedDevice) {
        if (DEBUG) {
            Log.d(TAG, "connectGattToCheckCompatibility, device: " + cachedDevice.getDevice());
        }
        BluetoothGatt gatt = cachedDevice.getDevice().connectGatt(getContext(), false,
                new BluetoothGattCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status,
                            int newState) {
                        super.onConnectionStateChange(gatt, status, newState);
                        if (DEBUG) {
                            Log.d(TAG, "onConnectionStateChange, status: " + status + ", newState: "
                                    + newState + ", device: " + cachedDevice.getDevice());
                        }
                        if (status == GATT_SUCCESS
                                && newState == BluetoothProfile.STATE_CONNECTED) {
                            gatt.discoverServices();
                        } else {
                            gatt.disconnect();
                            mConnectingGattList.remove(gatt);
                        }
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        super.onServicesDiscovered(gatt, status);
                        if (DEBUG) {
                            Log.d(TAG, "onServicesDiscovered, status: " + status + ", device: "
                                    + cachedDevice.getDevice());
                        }
                        if (status == GATT_SUCCESS) {
                            if (gatt.getService(BluetoothUuid.HEARING_AID.getUuid()) != null
                                    || gatt.getService(BluetoothUuid.HAS.getUuid()) != null) {
                                if (DEBUG) {
                                    Log.d(TAG, "compatible with Android, device: "
                                            + cachedDevice.getDevice());
                                }
                                addDevice(cachedDevice);
                            }
                        } else {
                            gatt.disconnect();
                            mConnectingGattList.remove(gatt);
                        }
                    }
                });
        mConnectingGattList.add(gatt);
    }

    void showBluetoothTurnedOnToast() {
        Toast.makeText(getContext(), R.string.connected_device_bluetooth_turned_on_toast,
                Toast.LENGTH_SHORT).show();
    }
}
