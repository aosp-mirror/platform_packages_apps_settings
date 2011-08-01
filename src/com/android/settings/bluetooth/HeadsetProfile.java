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
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import com.android.settings.R;

import java.util.List;

/**
 * HeadsetProfile handles Bluetooth HFP and Headset profiles.
 */
final class HeadsetProfile implements LocalBluetoothProfile {
    private static final String TAG = "HeadsetProfile";

    private BluetoothHeadset mService;
    private boolean mProfileReady;

    private final LocalBluetoothAdapter mLocalAdapter;
    private final CachedBluetoothDeviceManager mDeviceManager;
    private final LocalBluetoothProfileManager mProfileManager;

    static final ParcelUuid[] UUIDS = {
        BluetoothUuid.HSP,
        BluetoothUuid.Handsfree,
    };

    static final String NAME = "HEADSET";

    // Order of this profile in device profiles list
    private static final int ORDINAL = 0;

    // These callbacks run on the main thread.
    private final class HeadsetServiceListener
            implements BluetoothProfile.ServiceListener {

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mService = (BluetoothHeadset) proxy;
            mProfileReady = true;
            // We just bound to the service, so refresh the UI of the
            // headset device.
            List<BluetoothDevice> deviceList = mService.getConnectedDevices();
            if (deviceList.isEmpty()) {
                return;
            }
            BluetoothDevice firstDevice = deviceList.get(0);
            CachedBluetoothDevice device = mDeviceManager.findDevice(firstDevice);
            // we may add a new device here, but generally this should not happen
            if (device == null) {
                Log.w(TAG, "HeadsetProfile found new device: " + firstDevice);
                device = mDeviceManager.addDevice(mLocalAdapter, mProfileManager, firstDevice);
            }
            device.onProfileStateChanged(HeadsetProfile.this,
                    BluetoothProfile.STATE_CONNECTED);

            mProfileManager.callServiceConnectedListeners();
        }

        public void onServiceDisconnected(int profile) {
            mProfileReady = false;
            mService = null;
            mProfileManager.callServiceDisconnectedListeners();
        }
    }

    // TODO(): The calls must get queued if mService becomes null.
    // It can happen when the phone app crashes for some reason.
    // All callers should have service listeners. Dock Service is the only
    // one right now.
    HeadsetProfile(Context context, LocalBluetoothAdapter adapter,
            CachedBluetoothDeviceManager deviceManager,
            LocalBluetoothProfileManager profileManager) {
        mLocalAdapter = adapter;
        mDeviceManager = deviceManager;
        mProfileManager = profileManager;
        mLocalAdapter.getProfileProxy(context, new HeadsetServiceListener(),
                BluetoothProfile.HEADSET);
    }

    public boolean isConnectable() {
        return true;
    }

    public boolean isAutoConnectable() {
        return true;
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
        List<BluetoothDevice> deviceList = mService.getConnectedDevices();
        if (!deviceList.isEmpty() && deviceList.get(0).equals(device)) {
            // Downgrade priority as user is disconnecting the headset.
            if (mService.getPriority(device) > BluetoothProfile.PRIORITY_ON) {
                mService.setPriority(device, BluetoothProfile.PRIORITY_ON);
            }
            return mService.disconnect(device);
        } else {
            return false;
        }
    }

    public int getConnectionStatus(BluetoothDevice device) {
        if (mService == null) return BluetoothProfile.STATE_DISCONNECTED;

        List<BluetoothDevice> deviceList = mService.getConnectedDevices();

        return !deviceList.isEmpty() && deviceList.get(0).equals(device)
                ? mService.getConnectionState(device)
                : BluetoothProfile.STATE_DISCONNECTED;
    }

    public boolean isPreferred(BluetoothDevice device) {
        return mService.getPriority(device) > BluetoothProfile.PRIORITY_OFF;
    }

    public int getPreferred(BluetoothDevice device) {
        return mService.getPriority(device);
    }

    public void setPreferred(BluetoothDevice device, boolean preferred) {
        if (preferred) {
            if (mService.getPriority(device) < BluetoothProfile.PRIORITY_ON) {
                mService.setPriority(device, BluetoothProfile.PRIORITY_ON);
            }
        } else {
            mService.setPriority(device, BluetoothProfile.PRIORITY_OFF);
        }
    }

    public synchronized boolean isProfileReady() {
        return mProfileReady;
    }

    public String toString() {
        return NAME;
    }

    public int getOrdinal() {
        return ORDINAL;
    }

    public int getNameResource(BluetoothDevice device) {
        return R.string.bluetooth_profile_headset;
    }

    public int getSummaryResourceForDevice(BluetoothDevice device) {
        int state = mService.getConnectionState(device);
        switch (state) {
            case BluetoothProfile.STATE_DISCONNECTED:
                return R.string.bluetooth_headset_profile_summary_use_for;

            case BluetoothProfile.STATE_CONNECTED:
                return R.string.bluetooth_headset_profile_summary_connected;

            default:
                return Utils.getConnectionStateSummary(state);
        }
    }

    public int getDrawableResource(BluetoothClass btClass) {
        return R.drawable.ic_bt_headset_hfp;
    }
}
