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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import com.android.settings.R;

import java.util.HashMap;
import java.util.List;

/**
 * PanProfile handles Bluetooth PAN profile (NAP and PANU).
 */
final class PanProfile implements LocalBluetoothProfile {
    private BluetoothPan mService;
    // Tethering direction for each device
    private final HashMap<BluetoothDevice, Integer> mDeviceRoleMap =
            new HashMap<BluetoothDevice, Integer>();

    static final String NAME = "PAN";

    // Order of this profile in device profiles list
    private static final int ORDINAL = 4;

    // These callbacks run on the main thread.
    private final class PanServiceListener
            implements BluetoothProfile.ServiceListener {

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mService = (BluetoothPan) proxy;
        }

        public void onServiceDisconnected(int profile) {

        }
    }

    PanProfile(Context context) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.getProfileProxy(context, new PanServiceListener(),
                BluetoothProfile.PAN);
    }

    public boolean isConnectable() {
        return true;
    }

    public boolean isAutoConnectable() {
        return false;
    }

    public boolean connect(BluetoothDevice device) {
        List<BluetoothDevice> sinks = mService.getConnectedDevices();
        if (sinks != null) {
            for (BluetoothDevice sink : sinks) {
                mService.disconnect(sink);
            }
        }
        return mService.connect(device);
    }

    public boolean disconnect(BluetoothDevice device) {
        return mService.disconnect(device);
    }

    public int getConnectionStatus(BluetoothDevice device) {
        return mService.getConnectionState(device);
    }

    public boolean isPreferred(BluetoothDevice device) {
        return true;
    }

    public int getPreferred(BluetoothDevice device) {
        return -1;
    }

    public void setPreferred(BluetoothDevice device, boolean preferred) {
        // ignore: isPreferred is always true for PAN
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

    public int getNameResource(BluetoothDevice device) {
        if (isLocalRoleNap(device)) {
            return R.string.bluetooth_profile_pan_nap;
        } else {
            return R.string.bluetooth_profile_pan;
        }
    }

    public int getSummaryResourceForDevice(BluetoothDevice device) {
        int state = mService.getConnectionState(device);
        switch (state) {
            case BluetoothProfile.STATE_DISCONNECTED:
                return R.string.bluetooth_pan_profile_summary_use_for;

            case BluetoothProfile.STATE_CONNECTED:
                if (isLocalRoleNap(device)) {
                    return R.string.bluetooth_pan_nap_profile_summary_connected;
                } else {
                    return R.string.bluetooth_pan_user_profile_summary_connected;
                }

            default:
                return Utils.getConnectionStateSummary(state);
        }
    }

    public int getDrawableResource(BluetoothClass btClass) {
        return R.drawable.ic_bt_network_pan;
    }

    // Tethering direction determines UI strings.
    void setLocalRole(BluetoothDevice device, int role) {
        mDeviceRoleMap.put(device, role);
    }

    boolean isLocalRoleNap(BluetoothDevice device) {
        if (mDeviceRoleMap.containsKey(device)) {
            return mDeviceRoleMap.get(device) == BluetoothPan.LOCAL_NAP_ROLE;
        } else {
            return false;
        }
    }
}
