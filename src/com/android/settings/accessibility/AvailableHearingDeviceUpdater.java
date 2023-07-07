/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.android.settings.bluetooth.AvailableMediaBluetoothDeviceUpdater;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

/**
 * Maintains and updates connected hearing devices, including ASHA and HAP profile.
 */
public class AvailableHearingDeviceUpdater extends AvailableMediaBluetoothDeviceUpdater {

    private static final String PREF_KEY = "connected_hearing_device";

    public AvailableHearingDeviceUpdater(Context context,
            DevicePreferenceCallback devicePreferenceCallback, int metricsCategory) {
        super(context, devicePreferenceCallback, metricsCategory);
    }

    @Override
    public boolean isFilterMatched(CachedBluetoothDevice cachedDevice) {
        final BluetoothDevice device = cachedDevice.getDevice();
        final boolean isConnectedHearingAidDevice = (cachedDevice.isConnectedHearingAidDevice()
                && (device.getBondState() == BluetoothDevice.BOND_BONDED));

        return isConnectedHearingAidDevice && isDeviceInCachedDevicesList(cachedDevice);
    }

    @Override
    protected String getPreferenceKey() {
        return PREF_KEY;
    }
}
