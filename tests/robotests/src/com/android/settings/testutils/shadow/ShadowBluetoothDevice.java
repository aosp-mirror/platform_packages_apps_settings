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

import android.bluetooth.BluetoothDevice;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(value = BluetoothDevice.class)
public class ShadowBluetoothDevice extends org.robolectric.shadows.ShadowBluetoothDevice {

    private int mMessageAccessPermission = BluetoothDevice.ACCESS_UNKNOWN;
    private int mPhonebookAccessPermission = BluetoothDevice.ACCESS_UNKNOWN;
    private int mSimAccessPermission = BluetoothDevice.ACCESS_UNKNOWN;

    @Implementation
    protected void setMessageAccessPermission(int value) {
        mMessageAccessPermission = value;
    }

    @Implementation
    protected int getMessageAccessPermission() {
        return mMessageAccessPermission;
    }

    @Implementation
    protected void setPhonebookAccessPermission(int value) {
        mPhonebookAccessPermission = value;
    }

    @Implementation
    protected int getPhonebookAccessPermission() {
        return mPhonebookAccessPermission;
    }

    @Implementation
    protected void setSimAccessPermission(int value) {
        mSimAccessPermission = value;
    }

    @Implementation
    protected int getSimAccessPermission() {
        return mSimAccessPermission;
    }
}
