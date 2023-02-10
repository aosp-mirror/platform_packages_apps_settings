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
import android.bluetooth.BluetoothUuid;
import android.bluetooth.le.ScanFilter;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.bluetooth.BluetoothDevicePairingDetailBase;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import java.util.Collections;

/**
 * HearingDevicePairingDetail is a page to scan hearing devices. This page shows scanning icons and
 * pairing them.
 */
public class HearingDevicePairingDetail extends BluetoothDevicePairingDetailBase {

    private static final String TAG = "HearingDevicePairingDetail";
    @VisibleForTesting
    static final String KEY_AVAILABLE_HEARING_DEVICES = "available_hearing_devices";

    public HearingDevicePairingDetail() {
        super();
        final ScanFilter filter = new ScanFilter.Builder()
                .setServiceData(BluetoothUuid.HEARING_AID, new byte[]{0}, new byte[]{0})
                .build();
        setFilter(Collections.singletonList(filter));
    }

    @Override
    public void onStart() {
        super.onStart();
        mAvailableDevicesCategory.setProgress(mBluetoothAdapter.isEnabled());
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        super.onDeviceBondStateChanged(cachedDevice, bondState);

        mAvailableDevicesCategory.setProgress(bondState == BluetoothDevice.BOND_NONE);
    }

    @Override
    public int getMetricsCategory() {
        // TODO(b/262839191): To be updated settings_enums.proto
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.hearing_device_pairing_detail;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public String getDeviceListKey() {
        return KEY_AVAILABLE_HEARING_DEVICES;
    }
}
