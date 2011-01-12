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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothDevicePicker;
import android.bluetooth.BluetoothUuid;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;

import com.android.settings.ProgressCategory;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;
import java.util.WeakHashMap;

/**
 * Parent class for settings fragments that contain a list of Bluetooth
 * devices.
 *
 * @see BluetoothSettings
 * @see DevicePickerFragment
 * @see BluetoothFindNearby
 */
public abstract class DeviceListPreferenceFragment extends SettingsPreferenceFragment
        implements LocalBluetoothManager.Callback, View.OnClickListener {

    private static final String TAG = "DeviceListPreferenceFragment";

    static final String KEY_BT_DEVICE_LIST = "bt_device_list";
    static final String KEY_BT_SCAN = "bt_scan";

    int mFilterType;

    BluetoothDevice mSelectedDevice = null;

    LocalBluetoothManager mLocalManager;

    private PreferenceCategory mDeviceList;

    WeakHashMap<CachedBluetoothDevice, BluetoothDevicePreference> mDevicePreferenceMap =
            new WeakHashMap<CachedBluetoothDevice, BluetoothDevicePreference>();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                onBluetoothStateChanged(mLocalManager.getBluetoothState());
            }
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // We delay calling super.onActivityCreated(). See WifiSettings.java for more info.

        final Activity activity = getActivity();
        mLocalManager = LocalBluetoothManager.getInstance(activity);
        if (mLocalManager == null) {
            finish();
        }

        mFilterType = BluetoothDevicePicker.FILTER_TYPE_ALL;

        if (getPreferenceScreen() != null) getPreferenceScreen().removeAll();

        addPreferencesForActivity(activity);

        mDeviceList = (PreferenceCategory) findPreference(KEY_BT_DEVICE_LIST);

        super.onActivityCreated(savedInstanceState);
    }

    /** Add preferences from the subclass. */
    abstract void addPreferencesForActivity(Activity activity);

    @Override
    public void onResume() {
        super.onResume();

        mLocalManager.registerCallback(this);

        updateProgressUi(mLocalManager.getBluetoothAdapter().isDiscovering());

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(mReceiver, intentFilter);
        mLocalManager.setForegroundActivity(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        mLocalManager.stopScanning();
        mLocalManager.setForegroundActivity(null);
        mDevicePreferenceMap.clear();
        mDeviceList.removeAll();
        getActivity().unregisterReceiver(mReceiver);

        mLocalManager.unregisterCallback(this);
    }

    void addDevices() {
        List<CachedBluetoothDevice> cachedDevices =
                mLocalManager.getCachedDeviceManager().getCachedDevicesCopy();
        for (CachedBluetoothDevice cachedDevice : cachedDevices) {
            onDeviceAdded(cachedDevice);
        }
    }

    public void onClick(View v) {
        // User clicked on advanced options icon for a device in the list
        if (v.getTag() instanceof CachedBluetoothDevice) {
            CachedBluetoothDevice device = (CachedBluetoothDevice) v.getTag();
            device.onClickedAdvancedOptions(this);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {

        if (KEY_BT_SCAN.equals(preference.getKey())) {
            mLocalManager.startScanning(true);
            return true;
        }

        if (preference instanceof BluetoothDevicePreference) {
            BluetoothDevicePreference btPreference = (BluetoothDevicePreference)preference;
            CachedBluetoothDevice device = btPreference.getCachedDevice();
            mSelectedDevice = device.getDevice();
            onDevicePreferenceClick(btPreference);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    void onDevicePreferenceClick(BluetoothDevicePreference btPreference) {
        btPreference.getCachedDevice().onClicked();
    }

    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
        if (mDevicePreferenceMap.get(cachedDevice) != null) {
            Log.e(TAG, "Got onDeviceAdded, but cachedDevice already exists");
            return;
        }

        if (addDevicePreference(cachedDevice)) {
            createDevicePreference(cachedDevice);
        }
     }

    /**
     * Determine whether to add the new device to the list.
     * @param cachedDevice the newly discovered device
     * @return true if the device should be added; false otherwise
     */
    boolean addDevicePreference(CachedBluetoothDevice cachedDevice) {
        ParcelUuid[] uuids = cachedDevice.getDevice().getUuids();
        BluetoothClass bluetoothClass = cachedDevice.getDevice().getBluetoothClass();

        switch(mFilterType) {
        case BluetoothDevicePicker.FILTER_TYPE_TRANSFER:
            if (uuids != null) {
                if (BluetoothUuid.containsAnyUuid(uuids,
                        LocalBluetoothProfileManager.OPP_PROFILE_UUIDS))  return true;
            }
            if (bluetoothClass != null
                   && bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_OPP)) {
                return true;
            }
            break;
        case BluetoothDevicePicker.FILTER_TYPE_AUDIO:
            if (uuids != null) {
                if (BluetoothUuid.containsAnyUuid(uuids,
                        LocalBluetoothProfileManager.A2DP_SINK_PROFILE_UUIDS))  return true;

                if (BluetoothUuid.containsAnyUuid(uuids,
                        LocalBluetoothProfileManager.HEADSET_PROFILE_UUIDS))  return true;
            } else if (bluetoothClass != null) {
                if (bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_A2DP)) return true;

                if (bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_HEADSET)) return true;
            }
            break;
        case BluetoothDevicePicker.FILTER_TYPE_PANU:
            if (uuids != null) {
                if (BluetoothUuid.containsAnyUuid(uuids,
                        LocalBluetoothProfileManager.PANU_PROFILE_UUIDS))  return true;

            }
            if (bluetoothClass != null
                   && bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_PANU)) {
                return true;
            }
            break;
        case BluetoothDevicePicker.FILTER_TYPE_NAP:
            if (uuids != null) {
                if (BluetoothUuid.containsAnyUuid(uuids,
                        LocalBluetoothProfileManager.NAP_PROFILE_UUIDS))  return true;
            }
            if (bluetoothClass != null
                   && bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_NAP)) {
                return true;
            }
            break;
        default:
            return true;
        }
        return false;
    }

    void createDevicePreference(CachedBluetoothDevice cachedDevice) {
        BluetoothDevicePreference preference = new BluetoothDevicePreference(
                getActivity(), cachedDevice);

        initDevicePreference(preference);
        mDeviceList.addPreference(preference);
        mDevicePreferenceMap.put(cachedDevice, preference);
    }

    /**
     * Overridden in {@link BluetoothSettings} to add a listener.
     * @param preference the newly added preference
     */
    void initDevicePreference(BluetoothDevicePreference preference) { }

    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
        BluetoothDevicePreference preference = mDevicePreferenceMap.remove(cachedDevice);
        if (preference != null) {
            mDeviceList.removePreference(preference);
        }
    }

    public void onScanningStateChanged(boolean started) {
        updateProgressUi(started);
    }

    private void updateProgressUi(boolean start) {
        if (mDeviceList instanceof ProgressCategory) {
            ((ProgressCategory) mDeviceList).setProgress(start);
        }
    }

    void onBluetoothStateChanged(int bluetoothState) {
        if (bluetoothState == BluetoothAdapter.STATE_OFF) {
            updateProgressUi(false);
        }
    }

    void sendDevicePickedIntent(BluetoothDevice device) {
        Intent intent = new Intent(BluetoothDevicePicker.ACTION_DEVICE_SELECTED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        getActivity().sendBroadcast(intent);
    }
}
