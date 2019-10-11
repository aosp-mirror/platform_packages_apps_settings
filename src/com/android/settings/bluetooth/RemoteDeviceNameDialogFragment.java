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
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/** Provides a dialog for changing the display name of a remote bluetooth device. */
public class RemoteDeviceNameDialogFragment extends BluetoothNameDialogFragment {
    public static final String TAG = "RemoteDeviceName";
    private static final String KEY_CACHED_DEVICE_ADDRESS = "cached_device";

    private CachedBluetoothDevice mDevice;

    public static RemoteDeviceNameDialogFragment newInstance(CachedBluetoothDevice device) {
        Bundle args = new Bundle(1);
        args.putString(KEY_CACHED_DEVICE_ADDRESS, device.getDevice().getAddress());
        RemoteDeviceNameDialogFragment fragment = new RemoteDeviceNameDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @VisibleForTesting
    CachedBluetoothDevice getDevice(Context context) {
        String deviceAddress = getArguments().getString(KEY_CACHED_DEVICE_ADDRESS);
        LocalBluetoothManager manager = Utils.getLocalBtManager(context);
        BluetoothDevice device = manager.getBluetoothAdapter().getRemoteDevice(deviceAddress);
        return manager.getCachedDeviceManager().findDevice(device);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mDevice = getDevice(context);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_BLUETOOTH_PAIRED_DEVICE_RENAME;
    }

    @Override
    protected int getDialogTitle() {
        return R.string.bluetooth_device_name;
    }

    @Override
    protected String getDeviceName() {
        if (mDevice != null) {
            return mDevice.getName();
        }
        return null;
    }

    @Override
    protected void setDeviceName(String deviceName) {
        if (mDevice != null) {
            mDevice.setName(deviceName);
        }
    }
}
