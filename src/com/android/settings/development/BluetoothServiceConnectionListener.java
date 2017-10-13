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

package com.android.settings.development;

import android.bluetooth.BluetoothA2dp;

/**
 * Interface for callbacks about bluetooth connectivity.
 */
public interface BluetoothServiceConnectionListener {

    /**
     * Called when the bluetooth service is connected.
     * @param bluetoothA2dp controller for Bluetooth A2DP profile.
     */
    void onBluetoothServiceConnected(BluetoothA2dp bluetoothA2dp);

    /**
     * Called when the bluetooth codec configuration is changed.
     */
    void onBluetoothCodecUpdated();

    /**
     * Called with the bluetooth service is disconnected.
     */
    void onBluetoothServiceDisconnected();
}
