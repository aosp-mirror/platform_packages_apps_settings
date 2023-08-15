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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.accessibility.HearingDevicePairingDetail;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HearingAidInfo;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/**
 * Provides a dialog to pair another side of hearing aid device.
 */
public class HearingAidPairingDialogFragment extends InstrumentedDialogFragment implements
        CachedBluetoothDevice.Callback {
    public static final String TAG = "HearingAidPairingDialogFragment";
    private static final String KEY_DEVICE_ADDRESS = "device_address";
    private static final String KEY_LAUNCH_PAGE = "launch_page";

    private LocalBluetoothManager mLocalBluetoothManager;
    private CachedBluetoothDevice mDevice;

    /**
     * Creates a new {@link HearingAidPairingDialogFragment} and shows pair another side of hearing
     * aid device according to {@code deviceAddress}.
     *
     * @param deviceAddress The remote Bluetooth device address, that needs to be a hearing aid
     *                      device.
     * @param launchPage The id of the page where this dialog launch from. Should be one of
     *                   {@link SettingsEnums#ACCESSIBILITY},
     *                   {@link SettingsEnums#ACCESSIBILITY_HEARING_AID_SETTINGS}, or
     *                   {@link SettingsEnums#SETTINGS_CONNECTED_DEVICE_CATEGORY}
     * @return a DialogFragment
     */
    public static HearingAidPairingDialogFragment newInstance(String deviceAddress,
            int launchPage) {
        Bundle args = new Bundle(1);
        args.putString(KEY_DEVICE_ADDRESS, deviceAddress);
        args.putInt(KEY_LAUNCH_PAGE, launchPage);
        final HearingAidPairingDialogFragment fragment = new HearingAidPairingDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mLocalBluetoothManager = Utils.getLocalBtManager(context);
        mDevice = getDevice();
        if (mDevice != null) {
            mDevice.registerCallback(this);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mDevice != null) {
            mDevice.unregisterCallback(this);
        }
    }

    private CachedBluetoothDevice getDevice() {
        final String deviceAddress = getArguments().getString(KEY_DEVICE_ADDRESS);
        final BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(
                deviceAddress);
        return mLocalBluetoothManager.getCachedDeviceManager().findDevice(device);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_ACCESSIBILITY_HEARING_AID_PAIR_ANOTHER;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final int deviceSide = mDevice.getDeviceSide();
        final int titleId = R.string.bluetooth_pair_other_ear_dialog_title;
        final int messageId = (deviceSide == HearingAidInfo.DeviceSide.SIDE_LEFT)
                        ? R.string.bluetooth_pair_other_ear_dialog_left_ear_message
                        : R.string.bluetooth_pair_other_ear_dialog_right_ear_message;
        final int pairBtnId = (deviceSide == HearingAidInfo.DeviceSide.SIDE_LEFT)
                        ? R.string.bluetooth_pair_other_ear_dialog_right_ear_positive_button
                        : R.string.bluetooth_pair_other_ear_dialog_left_ear_positive_button;

        return new AlertDialog.Builder(getActivity())
                .setTitle(titleId)
                .setMessage(messageId)
                .setNegativeButton(android.R.string.cancel, /* listener= */ null)
                .setPositiveButton(pairBtnId, (dialog, which) -> positiveButtonListener())
                .create();
    }

    private void positiveButtonListener() {
        final int launchPage = getArguments().getInt(KEY_LAUNCH_PAGE);
        final boolean launchFromA11y = (launchPage == SettingsEnums.ACCESSIBILITY)
                || (launchPage == SettingsEnums.ACCESSIBILITY_HEARING_AID_SETTINGS);
        final String destination = launchFromA11y
                ? HearingDevicePairingDetail.class.getName()
                : BluetoothPairingDetail.class.getName();
        new SubSettingLauncher(getActivity())
                .setDestination(destination)
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
    }

    @Override
    public void onDeviceAttributesChanged() {
        final CachedBluetoothDevice subDevice = mDevice.getSubDevice();
        if (subDevice != null && subDevice.isConnectedAshaHearingAidDevice()) {
            this.dismiss();
        }
    }
}
