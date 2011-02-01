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

import com.android.settings.ProgressCategory;
import com.android.settings.R;

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
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;

import java.util.List;
import java.util.WeakHashMap;

/**
 * BluetoothSettings is the Settings screen for Bluetooth configuration and
 * connection management.
 */
public class BluetoothSettings extends PreferenceActivity
        implements LocalBluetoothManager.Callback {

    private static final String TAG = "BluetoothSettings";

    private static final String KEY_BT_CHECKBOX = "bt_checkbox";
    private static final String KEY_BT_DISCOVERABLE = "bt_discoverable";
    private static final String KEY_BT_DEVICE_LIST = "bt_device_list";
    private static final String KEY_BT_DISCOVERABLE_TIMEOUT = "bt_discoverable_timeout";
    private static final String KEY_BT_NAME = "bt_name";
    private static final String KEY_BT_SCAN = "bt_scan";

    private static final int SCREEN_TYPE_SETTINGS = 0;
    private static final int SCREEN_TYPE_DEVICEPICKER = 1;

    private int mScreenType;
    private int mFilterType;
    private boolean mNeedAuth;
    private String mLaunchPackage;
    private String mLaunchClass;

    private BluetoothDevice mSelectedDevice= null;

    private LocalBluetoothManager mLocalManager;

    private BluetoothEnabler mEnabler;
    private BluetoothDiscoverableEnabler mDiscoverableEnabler;

    private BluetoothNamePreference mNamePreference;

    private ProgressCategory mDeviceList;

    private WeakHashMap<CachedBluetoothDevice, BluetoothDevicePreference> mDevicePreferenceMap =
            new WeakHashMap<CachedBluetoothDevice, BluetoothDevicePreference>();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO: put this in callback instead of receiving

            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                onBluetoothStateChanged(mLocalManager.getBluetoothState());
            } else if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                    && mScreenType == SCREEN_TYPE_DEVICEPICKER) {
                int bondState = intent
                        .getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if (bondState == BluetoothDevice.BOND_BONDED) {
                    BluetoothDevice device = intent
                            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.equals(mSelectedDevice)) {
                        sendDevicePickedIntent(device);
                        finish();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocalManager = LocalBluetoothManager.getInstance(this);
        if (mLocalManager == null) finish();

        // Note:
        // If an application wish to show the BT device list, it can send an
        // intent to Settings application with below extra data:
        // -DEVICE_PICKER_FILTER_TYPE: the type of BT devices that want to show.
        // -DEVICE_PICKER_LAUNCH_PACKAGE: the package which the application belongs to.
        // -DEVICE_PICKER_LAUNCH_CLASS: the class which will receive user's selected
        // result from the BT list.
        // -DEVICE_PICKER_NEED_AUTH: to show if bonding procedure needed.

        mFilterType = BluetoothDevicePicker.FILTER_TYPE_ALL;
        Intent intent = getIntent();
        String action = intent.getAction();

        if (action.equals(BluetoothDevicePicker.ACTION_LAUNCH)) {
            mScreenType = SCREEN_TYPE_DEVICEPICKER;
            mNeedAuth = intent.getBooleanExtra(BluetoothDevicePicker.EXTRA_NEED_AUTH, false);
            mFilterType = intent.getIntExtra(BluetoothDevicePicker.EXTRA_FILTER_TYPE,
                    BluetoothDevicePicker.FILTER_TYPE_ALL);
            mLaunchPackage = intent.getStringExtra(BluetoothDevicePicker.EXTRA_LAUNCH_PACKAGE);
            mLaunchClass = intent.getStringExtra(BluetoothDevicePicker.EXTRA_LAUNCH_CLASS);

            setTitle(getString(R.string.device_picker));
            addPreferencesFromResource(R.xml.device_picker);
        } else {
            addPreferencesFromResource(R.xml.bluetooth_settings);

            mEnabler = new BluetoothEnabler(
                    this,
                    (CheckBoxPreference) findPreference(KEY_BT_CHECKBOX));

            mDiscoverableEnabler = new BluetoothDiscoverableEnabler(this,
                    (CheckBoxPreference) findPreference(KEY_BT_DISCOVERABLE),
                    (ListPreference) findPreference(KEY_BT_DISCOVERABLE_TIMEOUT));

            mNamePreference = (BluetoothNamePreference) findPreference(KEY_BT_NAME);

            mDeviceList = (ProgressCategory) findPreference(KEY_BT_DEVICE_LIST);
        }

        mDeviceList = (ProgressCategory) findPreference(KEY_BT_DEVICE_LIST);

        registerForContextMenu(getListView());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Repopulate (which isn't too bad since it's cached in the settings
        // bluetooth manager
        mDevicePreferenceMap.clear();
        mDeviceList.removeAll();
        addDevices();

        if (mScreenType == SCREEN_TYPE_SETTINGS) {
            mEnabler.resume();
            mDiscoverableEnabler.resume();
            mNamePreference.resume();
        }

        mLocalManager.registerCallback(this);

        mDeviceList.setProgress(mLocalManager.getBluetoothAdapter().isDiscovering());
        mLocalManager.startScanning(false);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
        mLocalManager.setForegroundActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mLocalManager.setForegroundActivity(null);

        unregisterReceiver(mReceiver);

        mLocalManager.unregisterCallback(this);
        if (mScreenType == SCREEN_TYPE_SETTINGS) {
            mNamePreference.pause();
            mDiscoverableEnabler.pause();
            mEnabler.pause();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        mLocalManager.stopScanning();
    }

    private void addDevices() {
        List<CachedBluetoothDevice> cachedDevices =
                mLocalManager.getCachedDeviceManager().getCachedDevicesCopy();
        for (CachedBluetoothDevice cachedDevice : cachedDevices) {
            onDeviceAdded(cachedDevice);
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
            if (mScreenType == SCREEN_TYPE_SETTINGS) {
                btPreference.getCachedDevice().onClicked();
            } else if (mScreenType == SCREEN_TYPE_DEVICEPICKER) {
                CachedBluetoothDevice device = btPreference.getCachedDevice();

                mSelectedDevice = device.getDevice();
                mLocalManager.stopScanning();
                mLocalManager.persistSelectedDeviceInPicker(mSelectedDevice.getAddress());
                if ((device.getBondState() == BluetoothDevice.BOND_BONDED) ||
                        (mNeedAuth == false)) {
                    sendDevicePickedIntent(mSelectedDevice);
                    finish();
                } else {
                    btPreference.getCachedDevice().onClicked();
                }
            }
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        //For device picker, disable Context Menu
        if (mScreenType != SCREEN_TYPE_SETTINGS) {
            return;
        }
        CachedBluetoothDevice cachedDevice = getDeviceFromMenuInfo(menuInfo);
        if (cachedDevice == null) return;

        cachedDevice.onCreateContextMenu(menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        CachedBluetoothDevice cachedDevice = getDeviceFromMenuInfo(item.getMenuInfo());
        if (cachedDevice == null) return false;

        cachedDevice.onContextItemSelected(item);
        return true;
    }

    private CachedBluetoothDevice getDeviceFromMenuInfo(ContextMenuInfo menuInfo) {
        if ((menuInfo == null) || !(menuInfo instanceof AdapterContextMenuInfo)) {
            return null;
        }

        AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) menuInfo;
        Preference pref = (Preference) getPreferenceScreen().getRootAdapter().getItem(
                adapterMenuInfo.position);
        if (pref == null || !(pref instanceof BluetoothDevicePreference)) {
            return null;
        }

        return ((BluetoothDevicePreference) pref).getCachedDevice();
    }

    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {

        if (mDevicePreferenceMap.get(cachedDevice) != null) {
            throw new IllegalStateException("Got onDeviceAdded, but cachedDevice already exists");
        }

        if (addDevicePreference(cachedDevice)) {
            createDevicePreference(cachedDevice);
        }
     }

    private boolean addDevicePreference(CachedBluetoothDevice cachedDevice) {
        ParcelUuid[] uuids = cachedDevice.getDevice().getUuids();
        BluetoothClass bluetoothClass = cachedDevice.getDevice().getBluetoothClass();

        switch(mFilterType) {
        case BluetoothDevicePicker.FILTER_TYPE_TRANSFER:
            if (uuids != null)
                if (BluetoothUuid.containsAnyUuid(uuids,
                        LocalBluetoothProfileManager.OPP_PROFILE_UUIDS))  return true;
                if (bluetoothClass != null
                        && bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_OPP)) {
                    return true;
                }
            break;
        case BluetoothDevicePicker.FILTER_TYPE_AUDIO:
            if (uuids != null) {
                if (BluetoothUuid.containsAnyUuid(uuids,
                        LocalBluetoothProfileManager.A2DP_PROFILE_UUIDS))  return true;

                if (BluetoothUuid.containsAnyUuid(uuids,
                        LocalBluetoothProfileManager.HEADSET_PROFILE_UUIDS))  return true;
            } else if (bluetoothClass != null) {
                if (bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_A2DP)) return true;

                if (bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_HEADSET)) return true;
            }
            break;
        default:
            return true;
        }
        return false;
    }

    private void createDevicePreference(CachedBluetoothDevice cachedDevice) {
        BluetoothDevicePreference preference = new BluetoothDevicePreference(this, cachedDevice);
        mDeviceList.addPreference(preference);
        mDevicePreferenceMap.put(cachedDevice, preference);
    }

    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
        BluetoothDevicePreference preference = mDevicePreferenceMap.remove(cachedDevice);
        if (preference != null) {
            mDeviceList.removePreference(preference);
        }
    }

    public void onScanningStateChanged(boolean started) {
        mDeviceList.setProgress(started);
    }

    private void onBluetoothStateChanged(int bluetoothState) {
        // When bluetooth is enabled (and we are in the activity, which we are),
        // we should start a scan
        if (bluetoothState == BluetoothAdapter.STATE_ON) {
            mLocalManager.startScanning(false);
        } else if (bluetoothState == BluetoothAdapter.STATE_OFF) {
            mDeviceList.setProgress(false);
        }
    }

    private void sendDevicePickedIntent(BluetoothDevice device) {
        Intent intent = new Intent(BluetoothDevicePicker.ACTION_DEVICE_SELECTED);
        if (mLaunchPackage != null && mLaunchClass != null) {
            intent.setClassName(mLaunchPackage, mLaunchClass);
        }
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        sendBroadcast(intent);
    }
}
