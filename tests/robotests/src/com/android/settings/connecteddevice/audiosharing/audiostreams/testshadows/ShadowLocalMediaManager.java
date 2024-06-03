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

import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.Collections;

@Implements(value = LocalMediaManager.class, callThroughByDefault = false)
public class ShadowLocalMediaManager {

    private static LocalMediaManager sMockManager;
    private static LocalMediaManager.DeviceCallback sDeviceCallback;

    public static void setUseMock(LocalMediaManager mockLocalMediaManager) {
        sMockManager = mockLocalMediaManager;
    }

    /** Reset static fields */
    @Resetter
    public static void reset() {
        sMockManager = null;
        sDeviceCallback = null;
    }

    /** Triggers onDeviceListUpdate of {@link LocalMediaManager.DeviceCallback} */
    public static void onDeviceListUpdate() {
        sDeviceCallback.onDeviceListUpdate(Collections.emptyList());
    }

    /** Starts scan */
    @Implementation
    public void startScan() {
        sMockManager.startScan();
    }

    /** Registers {@link LocalMediaManager.DeviceCallback} */
    @Implementation
    public void registerCallback(LocalMediaManager.DeviceCallback deviceCallback) {
        sMockManager.registerCallback(deviceCallback);
        sDeviceCallback = deviceCallback;
    }

    @Implementation
    public MediaDevice getCurrentConnectedDevice() {
        return sMockManager.getCurrentConnectedDevice();
    }
}
