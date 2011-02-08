/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;

import com.android.settings.R;

/**
 * OppProfile handles Bluetooth OPP.
 */
final class OppProfile implements LocalBluetoothProfile {

    static final String NAME = "OPP";

    // Order of this profile in device profiles list
    private static final int ORDINAL = 2;

    public boolean isConnectable() {
        return false;
    }

    public boolean isAutoConnectable() {
        return false;
    }

    public boolean connect(BluetoothDevice device) {
        return false;
    }

    public boolean disconnect(BluetoothDevice device) {
        return false;
    }

    public int getConnectionStatus(BluetoothDevice device) {
        return -1;  // FIXME: change to DISCONNECTED?
    }

    public boolean isPreferred(BluetoothDevice device) {
        return false;
    }

    public int getPreferred(BluetoothDevice device) {
        return -1;  // FIXME: is this correct?
    }

    public void setPreferred(BluetoothDevice device, boolean preferred) {
    }

    public boolean isProfileReady() {
        return true;
    }

    public String toString() {
        return NAME;
    }

    public int getOrdinal() {
        return ORDINAL;
    }

    public int getNameResource() {
        return R.string.bluetooth_profile_opp;
    }

    public int getDisconnectResource() {
        return 0; // user must use notification to disconnect OPP transfer.
    }

    public int getSummaryResourceForDevice(BluetoothDevice device) {
        return 0;   // OPP profile not displayed in UI
    }

    public int getDrawableResource(BluetoothClass btClass) {
        return 0;   // no icon for OPP
    }
}
