/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows;

import android.bluetooth.BluetoothLeBroadcastReceiveState;

import com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsHelper;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.List;
import java.util.Optional;

@Implements(value = AudioStreamsHelper.class, callThroughByDefault = false)
public class ShadowAudioStreamsHelper {
    private static AudioStreamsHelper sMockHelper;
    private static Optional<CachedBluetoothDevice> sCachedBluetoothDevice;

    public static void setUseMock(AudioStreamsHelper mockAudioStreamsHelper) {
        sMockHelper = mockAudioStreamsHelper;
    }

    /** Resets {@link CachedBluetoothDevice} */
    public static void resetCachedBluetoothDevice() {
        sCachedBluetoothDevice = Optional.empty();
    }

    public static void setCachedBluetoothDeviceInSharingOrLeConnected(
            CachedBluetoothDevice cachedBluetoothDevice) {
        sCachedBluetoothDevice = Optional.of(cachedBluetoothDevice);
    }

    @Implementation
    public List<BluetoothLeBroadcastReceiveState> getAllConnectedSources() {
        return sMockHelper.getAllConnectedSources();
    }

    /** Gets {@link CachedBluetoothDevice} in sharing or le connected */
    @Implementation
    public static Optional<CachedBluetoothDevice> getCachedBluetoothDeviceInSharingOrLeConnected(
            LocalBluetoothManager manager) {
        return sCachedBluetoothDevice;
    }
}
