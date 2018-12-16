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

package com.android.settings.testutils.shadow;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Collection;

/**
 * Shadow class for {@link CachedBluetoothDeviceManager} to allow tests to manages the set of
 * remote Bluetooth devices.
 */
@Implements(CachedBluetoothDeviceManager.class)
public class ShadowCachedBluetoothDeviceManager {

    private Collection<CachedBluetoothDevice> mCachedDevices;

    public void setCachedDevicesCopy(Collection<CachedBluetoothDevice> cachedDevices) {
        mCachedDevices = cachedDevices;
    }

    @Implementation
    protected synchronized Collection<CachedBluetoothDevice> getCachedDevicesCopy() {
        return mCachedDevices;
    }
}