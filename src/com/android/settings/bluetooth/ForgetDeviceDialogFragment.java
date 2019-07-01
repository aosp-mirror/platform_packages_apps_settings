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

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/** Implements an AlertDialog for confirming that a user wishes to unpair or "forget" a paired
 *  device*/
public class ForgetDeviceDialogFragment extends InstrumentedDialogFragment {
    public static final String TAG = "ForgetBluetoothDevice";
    private static final String KEY_DEVICE_ADDRESS = "device_address";

    private CachedBluetoothDevice mDevice;

    public static ForgetDeviceDialogFragment newInstance(String deviceAddress) {
        Bundle args = new Bundle(1);
        args.putString(KEY_DEVICE_ADDRESS, deviceAddress);
        ForgetDeviceDialogFragment dialog = new ForgetDeviceDialogFragment();
        dialog.setArguments(args);
        return dialog;
    }

    @VisibleForTesting
    CachedBluetoothDevice getDevice(Context context) {
        String deviceAddress = getArguments().getString(KEY_DEVICE_ADDRESS);
        LocalBluetoothManager manager = Utils.getLocalBtManager(context);
        BluetoothDevice device = manager.getBluetoothAdapter().getRemoteDevice(deviceAddress);
        return manager.getCachedDeviceManager().findDevice(device);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_BLUETOOTH_PAIRED_DEVICE_FORGET;
    }

    @Override
    public Dialog onCreateDialog(Bundle inState) {
        DialogInterface.OnClickListener onConfirm = (dialog, which) -> {
            mDevice.unpair();
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        };
        Context context = getContext();
        mDevice = getDevice(context);
        final boolean untetheredHeadset = BluetoothUtils.getBooleanMetaData(
                mDevice.getDevice(), BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setPositiveButton(R.string.bluetooth_unpair_dialog_forget_confirm_button,
                        onConfirm)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setTitle(R.string.bluetooth_unpair_dialog_title);
        dialog.setMessage(context.getString(untetheredHeadset
                        ? R.string.bluetooth_untethered_unpair_dialog_body
                        : R.string.bluetooth_unpair_dialog_body,
                mDevice.getName()));
        return dialog;
    }
}
