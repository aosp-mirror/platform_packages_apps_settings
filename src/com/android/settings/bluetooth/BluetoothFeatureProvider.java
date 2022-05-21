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
import android.content.ComponentName;
import android.net.Uri;

import java.util.List;

/**
 * Provider for bluetooth related features.
 */
public interface BluetoothFeatureProvider {

    /**
     * Gets the {@link Uri} that represents extra settings for a specific bluetooth device
     *
     * @param bluetoothDevice bluetooth device
     * @return {@link Uri} for extra settings
     */
    Uri getBluetoothDeviceSettingsUri(BluetoothDevice bluetoothDevice);

    /**
     * Gets the {@link Uri} that represents extra control for a specific bluetooth device
     *
     * @param bluetoothDevice bluetooth device
     * @return {@link String} uri string for extra control
     */
    String getBluetoothDeviceControlUri(BluetoothDevice bluetoothDevice);

    /**
     * Gets the {@link ComponentName} of services or activities that need to be shown in related
     * tools.
     *
     * @return list of {@link ComponentName}
     */
    List<ComponentName> getRelatedTools();
}
