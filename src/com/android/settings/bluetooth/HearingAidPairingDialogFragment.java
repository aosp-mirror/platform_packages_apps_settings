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
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HearingAidProfile;

/**
 * Provides a dialog to pair another side of hearing aid device.
 */
public class HearingAidPairingDialogFragment extends InstrumentedDialogFragment {
    public static final String TAG = "HearingAidPairingDialogFragment";
    private static final String KEY_CACHED_DEVICE_SIDE = "cached_device_side";

    /**
     * Creates a new {@link HearingAidPairingDialogFragment} and shows pair another side of hearing
     * aid device according to {@code CachedBluetoothDevice} side.
     *
     * @param device The remote Bluetooth device, that needs to be hearing aid device.
     * @return a DialogFragment
     */
    public static HearingAidPairingDialogFragment newInstance(CachedBluetoothDevice device) {
        Bundle args = new Bundle(1);
        args.putInt(KEY_CACHED_DEVICE_SIDE, device.getDeviceSide());
        final HearingAidPairingDialogFragment fragment = new HearingAidPairingDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_ACCESSIBILITY_HEARING_AID_PAIR_ANOTHER;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final int deviceSide = getArguments().getInt(KEY_CACHED_DEVICE_SIDE);
        final int titleId = R.string.bluetooth_pair_other_ear_dialog_title;
        final int messageId = (deviceSide == HearingAidProfile.DeviceSide.SIDE_LEFT)
                        ? R.string.bluetooth_pair_other_ear_dialog_left_ear_message
                        : R.string.bluetooth_pair_other_ear_dialog_right_ear_message;
        final int pairBtnId = (deviceSide == HearingAidProfile.DeviceSide.SIDE_LEFT)
                        ? R.string.bluetooth_pair_other_ear_dialog_right_ear_positive_button
                        : R.string.bluetooth_pair_other_ear_dialog_left_ear_positive_button;

        return new AlertDialog.Builder(getActivity())
                .setTitle(titleId)
                .setMessage(messageId)
                .setNegativeButton(
                        android.R.string.cancel, /* listener= */ null)
                .setPositiveButton(pairBtnId, (dialog, which) -> positiveButtonListener())
                .create();
    }

    private void positiveButtonListener() {
        new SubSettingLauncher(getActivity())
                .setDestination(BluetoothPairingDetail.class.getName())
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
    }
}
