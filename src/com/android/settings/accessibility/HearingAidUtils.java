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

package com.android.settings.accessibility;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.android.settings.bluetooth.HearingAidPairingDialogFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HearingAidProfile;

/** Provides utility methods related hearing aids. */
public final class HearingAidUtils {
    private static final String TAG = "HearingAidUtils";

    private HearingAidUtils(){}

    /**
     * Launches pairing dialog when hearing aid device needs other side of hearing aid device to
     * work.
     *
     * @param fragmentManager The {@link FragmentManager} used to show dialog fragment
     * @param device The {@link CachedBluetoothDevice} need to be hearing aid device
     */
    public static void launchHearingAidPairingDialog(FragmentManager fragmentManager,
            @NonNull CachedBluetoothDevice device) {
        if (device.isConnectedHearingAidDevice()
                && device.getDeviceMode() == HearingAidProfile.DeviceMode.MODE_BINAURAL
                && device.getSubDevice() == null) {
            launchHearingAidPairingDialogInternal(fragmentManager, device);
        }
    }

    private static void launchHearingAidPairingDialogInternal(FragmentManager fragmentManager,
            @NonNull CachedBluetoothDevice device) {
        if (device.getDeviceSide() == HearingAidProfile.DeviceSide.SIDE_INVALID) {
            Log.w(TAG, "Can not launch hearing aid pairing dialog for invalid side");
            return;
        }
        HearingAidPairingDialogFragment.newInstance(device).show(fragmentManager,
                HearingAidPairingDialogFragment.TAG);
    }
}
