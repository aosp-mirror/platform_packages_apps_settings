/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * This fragment allowed users to find the nearby broadcast sources.
 */
public class BluetoothFindBroadcastsFragment extends RestrictedDashboardFragment {

    private static final String TAG = "BTFindBroadcastsFrg";

    public static final String KEY_DEVICE_ADDRESS = "device_address";

    public static final String PREF_KEY_BROADCAST_SOURCE = "broadcast_source";

    @VisibleForTesting
    String mDeviceAddress;
    @VisibleForTesting
    LocalBluetoothManager mManager;
    @VisibleForTesting
    CachedBluetoothDevice mCachedDevice;

    public BluetoothFindBroadcastsFragment() {
        super(DISALLOW_CONFIG_BLUETOOTH);
    }

    @VisibleForTesting
    LocalBluetoothManager getLocalBluetoothManager(Context context) {
        return Utils.getLocalBtManager(context);
    }

    @VisibleForTesting
    CachedBluetoothDevice getCachedDevice(String deviceAddress) {
        BluetoothDevice remoteDevice =
                mManager.getBluetoothAdapter().getRemoteDevice(deviceAddress);
        return mManager.getCachedDeviceManager().findDevice(remoteDevice);
    }

    @Override
    public void onAttach(Context context) {
        mDeviceAddress = getArguments().getString(KEY_DEVICE_ADDRESS);
        mManager = getLocalBluetoothManager(context);
        mCachedDevice = getCachedDevice(mDeviceAddress);
        super.onAttach(context);
        if (mCachedDevice == null) {
            //Close this page if device is null with invalid device mac address
            Log.w(TAG, "onAttach() CachedDevice is null!");
            finish();
            return;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        finishFragmentIfNecessary();
    }

    @VisibleForTesting
    void finishFragmentIfNecessary() {
        if (mCachedDevice.getBondState() == BOND_NONE) {
            finish();
            return;
        }
    }

    @Override
    public int getMetricsCategory() {
        //TODO(b/228255796) : add new enum for find broadcast fragment
        return SettingsEnums.PAGE_UNKNOWN;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_find_broadcasts_fragment;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList<AbstractPreferenceController> controllers = new ArrayList<>();

        if (mCachedDevice != null) {
            Lifecycle lifecycle = getSettingsLifecycle();
            controllers.add(new BluetoothFindBroadcastsHeaderController(context, this,
                    mCachedDevice, lifecycle, mManager));
        }
        return controllers;
    }
}
