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

import android.content.Context;

import com.android.settings.bluetooth.Utils;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

/** Robolectric shadow for the bluetooth utils. */
@Implements(Utils.class)
public class ShadowBluetoothUtils {

    public static LocalBluetoothManager sLocalBluetoothManager;

    @Implementation
    protected static LocalBluetoothManager getLocalBtManager(Context context) {
        return sLocalBluetoothManager;
    }

    /** Resets the local bluetooth manager to null. */
    @Resetter
    public static void reset() {
        sLocalBluetoothManager = null;
    }
}
