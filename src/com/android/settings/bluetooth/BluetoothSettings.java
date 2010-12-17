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
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.UserLeaveHintListener;

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
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.util.List;
import java.util.WeakHashMap;

/**
 * BluetoothSettings is the Settings screen for Bluetooth configuration and
 * connection management.
 */
public class BluetoothSettings extends SettingsPreferenceFragment
        implements LocalBluetoothManager.Callback, UserLeaveHintListener, View.OnClickListener {

    private static final String TAG = "BluetoothSettings";

    private static final String KEY_BT_CHECKBOX = "bt_checkbox";
    private static final String KEY_BT_DISCOVERABLE = "bt_discoverable";
    private static final String KEY_BT_DEVICE_LIST = "bt_device_list";
    private static final String KEY_BT_NAME = "bt_name";
    private static final String KEY_BT_SCAN = "bt_scan";

    private static final int SCREEN_TYPE_SETTINGS = 0;
    private static final int SCREEN_TYPE_DEVICEPICKER = 1;
    private static final int SCREEN_TYPE_SCAN = 2;

    public static final String ACTION = "bluetooth_action";
    public static final String ACTION_LAUNCH_SCAN_MODE =
            "com.android.settings.bluetooth.action.LAUNCH_SCAN_MODE";

    /*package*/ int mScreenType;
    private int mFilterType;
    private boolean mNeedAuth;
    private String mLaunchPackage;
    private String mLaunchClass;

    /*package*/ BluetoothDevice mSelectedDevice= null;

    /*package*/ LocalBluetoothManager mLocalManager;

    private BluetoothEnabler mEnabler;
    private BluetoothDiscoverableEnabler mDiscoverableEnabler;

    private BluetoothNamePreference mNamePreference;

    private PreferenceCategory mDeviceList;

    private WeakHashMap<CachedBluetoothDevice, BluetoothDevicePreference> mDevicePreferenceMap =
            new WeakHashMap<CachedBluetoothDevice, BluetoothDevicePreference>();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                onBluetoothStateChanged(mLocalManager.getBluetoothState());
            } else if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                // TODO: If this is a scanning screen, maybe return on successful pairing

                int bondState = intent
                        .getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if (bondState == BluetoothDevice.BOND_BONDED) {
                    BluetoothDevice device = intent
                            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.equals(mSelectedDevice)) {
                        if (mScreenType == SCREEN_TYPE_DEVICEPICKER) {
                            sendDevicePickedIntent(device);
                            finish();
                        } else if (mScreenType == SCREEN_TYPE_SCAN) {
                            finish();
                        }
                    }
                }
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

        // Note:
        // If an application wishes to show the BT device list, it can send an
        // intent to Settings application with the following extra data:
        // -DEVICE_PICKER_FILTER_TYPE: the type of BT devices to show.
        // -DEVICE_PICKER_LAUNCH_PACKAGE: the package which the application belongs to.
        // -DEVICE_PICKER_LAUNCH_CLASS: the class which will receive user's selected
        // result from the BT list.
        // -DEVICE_PICKER_NEED_AUTH: to show if bonding procedure needed.

        mFilterType = BluetoothDevicePicker.FILTER_TYPE_ALL;
        final Intent intent = activity.getIntent();

        // This additional argument comes from PreferenceScreen (See TetherSettings.java).
        Bundle args = getArguments();
        String action = args != null ? args.getString(ACTION) : null;
        if (TextUtils.isEmpty(action)) {
            action = intent.getAction();
        }

        if (getPreferenceScreen() != null) getPreferenceScreen().removeAll();

        if (action.equals(BluetoothDevicePicker.ACTION_LAUNCH)) {
            mScreenType = SCREEN_TYPE_DEVICEPICKER;
            mNeedAuth = intent.getBooleanExtra(BluetoothDevicePicker.EXTRA_NEED_AUTH, false);
            mFilterType = intent.getIntExtra(BluetoothDevicePicker.EXTRA_FILTER_TYPE,
                    BluetoothDevicePicker.FILTER_TYPE_ALL);
            mLaunchPackage = intent.getStringExtra(BluetoothDevicePicker.EXTRA_LAUNCH_PACKAGE);
            mLaunchClass = intent.getStringExtra(BluetoothDevicePicker.EXTRA_LAUNCH_CLASS);

            activity.setTitle(activity.getString(R.string.device_picker));
            addPreferencesFromResource(R.xml.device_picker);
        } else if (action.equals(ACTION_LAUNCH_SCAN_MODE)) {
            mScreenType = SCREEN_TYPE_SCAN;

            addPreferencesFromResource(R.xml.device_picker);
        } else {
            addPreferencesFromResource(R.xml.bluetooth_settings);

            mEnabler = new BluetoothEnabler(
                    activity,
                    (CheckBoxPreference) findPreference(KEY_BT_CHECKBOX));

            mDiscoverableEnabler = new BluetoothDiscoverableEnabler(
                    activity,
                    (CheckBoxPreference) findPreference(KEY_BT_DISCOVERABLE));

            mNamePreference = (BluetoothNamePreference) findPreference(KEY_BT_NAME);

        }

        mDeviceList = (PreferenceCategory) findPreference(KEY_BT_DEVICE_LIST);

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Repopulate (which isn't too bad since it's cached in the settings
        // bluetooth manager)
        mDevicePreferenceMap.clear();
        mDeviceList.removeAll();
        if (mScreenType != SCREEN_TYPE_SCAN) {
            addDevices();
        }

        if (mScreenType == SCREEN_TYPE_SETTINGS) {
            mEnabler.resume();
            mDiscoverableEnabler.resume();
            mNamePreference.resume();
        }

        mLocalManager.registerCallback(this);

        updateProgressUi(mLocalManager.getBluetoothAdapter().isDiscovering());

        if (mScreenType != SCREEN_TYPE_SETTINGS) {
            mLocalManager.startScanning(true);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getActivity().registerReceiver(mReceiver, intentFilter);
        mLocalManager.setForegroundActivity(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        mLocalManager.setForegroundActivity(null);
        mDevicePreferenceMap.clear();
        mDeviceList.removeAll();
        getActivity().unregisterReceiver(mReceiver);

        mLocalManager.unregisterCallback(this);
        if (mScreenType == SCREEN_TYPE_SETTINGS) {
            mNamePreference.pause();
            mDiscoverableEnabler.pause();
            mEnabler.pause();
        }
    }

    public void onUserLeaveHint() {
        mLocalManager.stopScanning();
    }

    private void addDevices() {
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

            if (mScreenType == SCREEN_TYPE_SETTINGS || mScreenType == SCREEN_TYPE_SCAN) {
                btPreference.getCachedDevice().onClicked();
            } else if (mScreenType == SCREEN_TYPE_DEVICEPICKER) {
                mLocalManager.stopScanning();
                mLocalManager.persistSelectedDeviceInPicker(mSelectedDevice.getAddress());
                if ((device.getBondState() == BluetoothDevice.BOND_BONDED) ||
                        (mNeedAuth == false)) {
                    sendDevicePickedIntent(mSelectedDevice);
                    finish();
                } else {
                    btPreference.getCachedDevice().onClicked();
                }
            } else {
                Log.e(TAG, "onPreferenceTreeClick has invalid mScreenType: "
                        + mScreenType);
            }
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {

        if (mDevicePreferenceMap.get(cachedDevice) != null) {
            throw new IllegalStateException("Got onDeviceAdded, but cachedDevice already exists");
        }

        if (mScreenType != SCREEN_TYPE_SETTINGS
                || cachedDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            if (addDevicePreference(cachedDevice)) {
                createDevicePreference(cachedDevice);
            }
        }
     }

    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice,
            int bondState) {
        // add to "Paired devices" list after remote-initiated pairing
        if (mDevicePreferenceMap.get(cachedDevice) == null &&
                mScreenType == SCREEN_TYPE_SETTINGS &&
                bondState == BluetoothDevice.BOND_BONDED) {
            if (addDevicePreference(cachedDevice)) {
                createDevicePreference(cachedDevice);
            }
        }
    }

    private boolean addDevicePreference(CachedBluetoothDevice cachedDevice) {
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

    private void createDevicePreference(CachedBluetoothDevice cachedDevice) {
        BluetoothDevicePreference preference = new BluetoothDevicePreference(
                getActivity(), cachedDevice);

        if (mScreenType == SCREEN_TYPE_SETTINGS) {
            preference.setOnSettingsClickListener(this);
        }
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
        updateProgressUi(started);
    }

    private void updateProgressUi(boolean start) {
        if (mDeviceList instanceof ProgressCategory) {
            ((ProgressCategory) mDeviceList).setProgress(start);
        }
    }

    /*package*/ void onBluetoothStateChanged(int bluetoothState) {
        // When bluetooth is enabled (and we are in the activity, which we are),
        // we should start a scan
        if (bluetoothState == BluetoothAdapter.STATE_ON) {
            if (mScreenType != SCREEN_TYPE_SETTINGS) {
                mLocalManager.startScanning(false);
            }
        } else if (bluetoothState == BluetoothAdapter.STATE_OFF) {
            updateProgressUi(false);
        }
    }

    /*package*/ void sendDevicePickedIntent(BluetoothDevice device) {
        Intent intent = new Intent(BluetoothDevicePicker.ACTION_DEVICE_SELECTED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        if (mScreenType == SCREEN_TYPE_DEVICEPICKER &&
                mLaunchPackage != null && mLaunchClass != null) {
            intent.setClassName(mLaunchPackage, mLaunchClass);
        }
        getActivity().sendBroadcast(intent);
    }

    public static class FindNearby extends BluetoothSettings {

        public FindNearby() {
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            Bundle args = super.getArguments();
            if (args == null) {
                args = new Bundle();
                setArguments(args);
            }
            args.putString(ACTION, ACTION_LAUNCH_SCAN_MODE);
            super.onActivityCreated(savedInstanceState);
        }
    }
}
