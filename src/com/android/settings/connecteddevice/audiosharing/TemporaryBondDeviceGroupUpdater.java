/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.flags.Flags;

/** Maintain and update connected temporary bond bluetooth devices */
public class TemporaryBondDeviceGroupUpdater extends BluetoothDeviceUpdater {
    private static final String TAG = "TemporaryBondDeviceGroupUpdater";
    private static final String PREF_KEY_PREFIX = "temp_bond_bt_";

    public TemporaryBondDeviceGroupUpdater(
            @NonNull Context context,
            @NonNull DevicePreferenceCallback devicePreferenceCallback,
            int metricsCategory) {
        super(context, devicePreferenceCallback, metricsCategory);
    }

    @Override
    public boolean isFilterMatched(@NonNull CachedBluetoothDevice cachedDevice) {
        // Only connected temporary bond device should be shown in this section when Audio
        // sharing UI is available.
        boolean isFilterMatched = Flags.enableTemporaryBondDevicesUi()
                && BluetoothUtils.isTemporaryBondDevice(cachedDevice.getDevice())
                && isDeviceConnected(cachedDevice) && isDeviceInCachedDevicesList(cachedDevice)
                && BluetoothUtils.isAudioSharingUIAvailable(mContext);
        Log.d(
                TAG,
                "isFilterMatched() device : "
                        + cachedDevice.getName()
                        + ", isFilterMatched : "
                        + isFilterMatched);
        return isFilterMatched;
    }

    @Override
    protected String getPreferenceKeyPrefix() {
        return PREF_KEY_PREFIX;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected void update(CachedBluetoothDevice cachedBluetoothDevice) {
        super.update(cachedBluetoothDevice);
        Log.d(TAG, "Map : " + mPreferenceMap);
    }
}
