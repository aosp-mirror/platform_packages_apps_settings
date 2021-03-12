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

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import android.Manifest;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothDevicePicker;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.password.PasswordUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;

/**
 * BluetoothSettings is the Settings screen for Bluetooth configuration and
 * connection management.
 */
public final class DevicePickerFragment extends DeviceListPreferenceFragment {
    private static final String KEY_BT_DEVICE_LIST = "bt_device_list";
    private static final String TAG = "DevicePickerFragment";

    @VisibleForTesting
    BluetoothProgressCategory mAvailableDevicesCategory;
    @VisibleForTesting
    Context mContext;
    @VisibleForTesting
    String mLaunchPackage;
    @VisibleForTesting
    String mLaunchClass;
    @VisibleForTesting
    String mCallingAppPackageName;

    private boolean mNeedAuth;
    private boolean mScanAllowed;

    public DevicePickerFragment() {
        super(null /* Not tied to any user restrictions. */);
    }

    @Override
    void initPreferencesFromPreferenceScreen() {
        Intent intent = getActivity().getIntent();
        mNeedAuth = intent.getBooleanExtra(BluetoothDevicePicker.EXTRA_NEED_AUTH, false);
        setFilter(intent.getIntExtra(BluetoothDevicePicker.EXTRA_FILTER_TYPE,
                BluetoothDevicePicker.FILTER_TYPE_ALL));
        mLaunchPackage = intent.getStringExtra(BluetoothDevicePicker.EXTRA_LAUNCH_PACKAGE);
        mLaunchClass = intent.getStringExtra(BluetoothDevicePicker.EXTRA_LAUNCH_CLASS);
        mAvailableDevicesCategory = (BluetoothProgressCategory) findPreference(KEY_BT_DEVICE_LIST);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BLUETOOTH_DEVICE_PICKER;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(getString(R.string.device_picker));
        UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        mScanAllowed = !um.hasUserRestriction(DISALLOW_CONFIG_BLUETOOTH);
        mCallingAppPackageName = PasswordUtils.getCallingAppPackageName(
                getActivity().getActivityToken());
        if (!TextUtils.equals(mCallingAppPackageName, mLaunchPackage)) {
            Log.w(TAG, "sendDevicePickedIntent() launch package name is not equivalent to"
                    + " calling package name!");
        }
        mContext = getContext();
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        addCachedDevices();
        mSelectedDevice = null;
        if (mScanAllowed) {
            enableScanning();
            mAvailableDevicesCategory.setProgress(mBluetoothAdapter.isDiscovering());
        }
    }

    @Override
    public void onStop() {
        // Try disable scanning no matter what, no effect if enableScanning has not been called
        disableScanning();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /* Check if any device was selected, if no device selected
         * send  ACTION_DEVICE_SELECTED with a null device, otherwise
         * don't do anything */
        if (mSelectedDevice == null) {
            sendDevicePickedIntent(null);
        }
    }

    @Override
    void onDevicePreferenceClick(BluetoothDevicePreference btPreference) {
        disableScanning();
        LocalBluetoothPreferences.persistSelectedDeviceInPicker(
                getActivity(), mSelectedDevice.getAddress());
        if ((btPreference.getCachedDevice().getBondState() ==
                BluetoothDevice.BOND_BONDED) || !mNeedAuth) {
            sendDevicePickedIntent(mSelectedDevice);
            finish();
        } else {
            super.onDevicePreferenceClick(btPreference);
        }
    }

    @Override
    public void onScanningStateChanged(boolean started) {
        super.onScanningStateChanged(started);
        started |= mScanEnabled;
        mAvailableDevicesCategory.setProgress(started);
    }

    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice,
            int bondState) {
        BluetoothDevice device = cachedDevice.getDevice();
        if (!device.equals(mSelectedDevice)) {
            return;
        }
        if (bondState == BluetoothDevice.BOND_BONDED) {
            sendDevicePickedIntent(device);
            finish();
        } else if (bondState == BluetoothDevice.BOND_NONE) {
            enableScanning();
        }
    }

    @Override
    protected void initDevicePreference(BluetoothDevicePreference preference) {
        super.initDevicePreference(preference);
        preference.setNeedNotifyHierarchyChanged(true);
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        super.onBluetoothStateChanged(bluetoothState);

        if (bluetoothState == BluetoothAdapter.STATE_ON) {
            enableScanning();
        }
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.device_picker;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return null;
    }

    @Override
    public String getDeviceListKey() {
        return KEY_BT_DEVICE_LIST;
    }

    private void sendDevicePickedIntent(BluetoothDevice device) {
        Intent intent = new Intent(BluetoothDevicePicker.ACTION_DEVICE_SELECTED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        if (mLaunchPackage != null && mLaunchClass != null) {
            if (TextUtils.equals(mCallingAppPackageName, mLaunchPackage)) {
                intent.setClassName(mLaunchPackage, mLaunchClass);
            }
        }

        mContext.sendBroadcast(intent, Manifest.permission.BLUETOOTH_ADMIN);
    }
}
