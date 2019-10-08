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

import android.net.wifi.p2p.WifiP2pManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

/**
 * Shadow class for WifiP2pManager.
 */
@Implements(value = WifiP2pManager.class)
public class ShadowWifiP2pManager extends org.robolectric.shadows.ShadowWifiP2pManager {

    private static int sFactoryResetCount;

    @Implementation
    protected void factoryReset(WifiP2pManager.Channel c, WifiP2pManager.ActionListener listener) {
        if (c != null) {
            sFactoryResetCount++;
        } else {
            throw new IllegalArgumentException("channel must be non-null.");
        }
    }

    @Resetter
    public static void reset() {
        sFactoryResetCount = 0;
    }

    /**
     * Return the count of factoryReset called.
     *
     * @return the count of factoryReset called.
     */
    public static int getFactoryResetCount() {
        return sFactoryResetCount;
    }
}
