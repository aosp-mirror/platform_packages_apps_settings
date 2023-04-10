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

import com.android.settings.bluetooth.SavedBluetoothDeviceUpdater;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

/**
 * Maintains and updates saved (bonded but not connected) hearing devices, including ASHA and HAP
 * profile.
 */
public class SavedHearingDeviceUpdater extends SavedBluetoothDeviceUpdater {

    private static final String PREF_KEY = "saved_hearing_device";

    public SavedHearingDeviceUpdater(Context context,
            DevicePreferenceCallback devicePreferenceCallback, int metricsCategory) {
        super(context, devicePreferenceCallback, /* showConnectedDevice= */ false, metricsCategory);
    }

    @Override
    public boolean isFilterMatched(CachedBluetoothDevice cachedDevice) {
        final BluetoothDevice device = cachedDevice.getDevice();
        final boolean isSavedHearingAidDevice = cachedDevice.isHearingAidDevice()
                && device.getBondState() == BluetoothDevice.BOND_BONDED
                && !device.isConnected();

        return isSavedHearingAidDevice && isDeviceInCachedDevicesList(cachedDevice);
    }

    @Override
    protected String getPreferenceKey() {
        return PREF_KEY;
    }
}
