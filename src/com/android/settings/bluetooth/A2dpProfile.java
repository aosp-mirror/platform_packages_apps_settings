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

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

final class A2dpProfile implements LocalBluetoothProfile {
    private static final String TAG = "A2dpProfile";
    private static boolean V = true;

    private BluetoothA2dp mService;
    private boolean mIsProfileReady;

    static final ParcelUuid[] SINK_UUIDS = {
        BluetoothUuid.AudioSink,
        BluetoothUuid.AdvAudioDist,
    };

    static final String NAME = "A2DP";
    private final LocalBluetoothProfileManager mProfileManager;

    // Order of this profile in device profiles list
    private static final int ORDINAL = 1;

    // These callbacks run on the main thread.
    private final class A2dpServiceListener
            implements BluetoothProfile.ServiceListener {

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (V) Log.d(TAG,"Bluetooth service connected");
            mService = (BluetoothA2dp) proxy;
            mProfileManager.setA2dpServiceUp(true);
            mIsProfileReady=true;
        }

        public void onServiceDisconnected(int profile) {
            if (V) Log.d(TAG,"Bluetooth service disconnected");
            mProfileManager.setA2dpServiceUp(false);
            mIsProfileReady=false;
        }
    }

    public boolean isProfileReady() {
        return mIsProfileReady;
    }
    A2dpProfile(Context context, LocalBluetoothProfileManager profileManager) {
        mProfileManager = profileManager;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.getProfileProxy(context, new A2dpServiceListener(),
                BluetoothProfile.A2DP);
    }

    public boolean isConnectable() {
        return true;
    }

    public boolean isAutoConnectable() {
        return true;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        if (mService == null) return new ArrayList<BluetoothDevice>(0);
        return mService.getDevicesMatchingConnectionStates(
              new int[] {BluetoothProfile.STATE_CONNECTED,
                         BluetoothProfile.STATE_CONNECTING,
                         BluetoothProfile.STATE_DISCONNECTING});
    }

    public boolean connect(BluetoothDevice device) {
        if (mService == null) return false;
        List<BluetoothDevice> sinks = getConnectedDevices();
        if (sinks != null) {
            for (BluetoothDevice sink : sinks) {
                mService.disconnect(sink);
            }
        }
        return mService.connect(device);
    }

    public boolean disconnect(BluetoothDevice device) {
        if (mService == null) return false;
        return mService.disconnect(device);
    }

    // This function is added as the AUTO CONNECT priority could not be set by using setPreferred(),
    // as setPreferred() takes only boolean input but getPreferred() supports interger output.
    // Also this need not implemented by all profiles so this has been added here.
    public void enableAutoConnect(BluetoothDevice device, boolean enable) {
        if (mService == null) return;
        if (enable) {
             mService.setPriority(device, BluetoothProfile.PRIORITY_AUTO_CONNECT);
        } else {
             if (mService.getPriority(device) > BluetoothProfile.PRIORITY_ON) {
                 mService.setPriority(device, BluetoothProfile.PRIORITY_ON);
             }
        }
    }

    public int getConnectionStatus(BluetoothDevice device) {
        if (mService == null) {
            return BluetoothProfile.STATE_DISCONNECTED;
        }
        return mService.getConnectionState(device);
    }

    public boolean isPreferred(BluetoothDevice device) {
        if (mService == null) return false;
        return mService.getPriority(device) > BluetoothProfile.PRIORITY_OFF;
    }

    public int getPreferred(BluetoothDevice device) {
        if (mService == null) return BluetoothProfile.PRIORITY_OFF;
        return mService.getPriority(device);
    }

    public void setPreferred(BluetoothDevice device, boolean preferred) {
        if (mService == null) return;
        if (preferred) {
            if (mService.getPriority(device) < BluetoothProfile.PRIORITY_ON) {
                mService.setPriority(device, BluetoothProfile.PRIORITY_ON);
            }
        } else {
            mService.setPriority(device, BluetoothProfile.PRIORITY_OFF);
        }
    }

    boolean isA2dpPlaying() {
        if (mService == null) return false;
        List<BluetoothDevice> sinks = mService.getConnectedDevices();
        if (!sinks.isEmpty()) {
            if (mService.isA2dpPlaying(sinks.get(0))) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return NAME;
    }

    public int getOrdinal() {
        return ORDINAL;
    }

    public int getNameResource(BluetoothDevice device) {
        return R.string.bluetooth_profile_a2dp;
    }

    public int getSummaryResourceForDevice(BluetoothDevice device) {
        int state = getConnectionStatus(device);
        switch (state) {
            case BluetoothProfile.STATE_DISCONNECTED:
            {
                setPreferred(device, false);
                return R.string.bluetooth_a2dp_profile_summary_use_for;
            }
            case BluetoothProfile.STATE_CONNECTED:
            {
                setPreferred(device, true);
                return R.string.bluetooth_a2dp_profile_summary_connected;
            }
            default:
                return Utils.getConnectionStateSummary(state);
        }
    }

    public int getDrawableResource(BluetoothClass btClass) {
        return R.drawable.ic_bt_headphones_a2dp;
    }

    protected void finalize() {
        if (V) Log.d(TAG, "finalize()");
        if (mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(BluetoothProfile.A2DP, mService);
                mService = null;
            }catch (Throwable t) {
                Log.w(TAG, "Error cleaning up A2DP proxy", t);
            }
        }
    }
}
