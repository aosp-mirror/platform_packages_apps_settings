/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.Uri;

/**
 * Impl of {@link BluetoothFeatureProvider}
 */
public class BluetoothFeatureProviderImpl implements BluetoothFeatureProvider {

    private Context mContext;

    public BluetoothFeatureProviderImpl(Context context) {
        mContext = context;
    }

    @Override
    public Uri getBluetoothDeviceSettingsUri(BluetoothDevice bluetoothDevice) {
        final byte[] uriByte = bluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_ENHANCED_SETTINGS_UI_URI);
        return uriByte == null ? null : Uri.parse(new String(uriByte));
    }
}
