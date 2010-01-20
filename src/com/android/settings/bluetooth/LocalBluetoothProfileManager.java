/*
 * Copyright (C) 2008 The Android Open Source Project
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

import com.android.settings.R;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothUuid;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LocalBluetoothProfileManager is an abstract class defining the basic
 * functionality related to a profile.
 */
public abstract class LocalBluetoothProfileManager {
    private static final String TAG = "LocalBluetoothProfileManager";

    /* package */ static final ParcelUuid[] HEADSET_PROFILE_UUIDS = new ParcelUuid[] {
        BluetoothUuid.HSP,
        BluetoothUuid.Handsfree,
    };

    /* package */ static final ParcelUuid[] A2DP_PROFILE_UUIDS = new ParcelUuid[] {
        BluetoothUuid.AudioSink,
        BluetoothUuid.AdvAudioDist,
    };

    /* package */ static final ParcelUuid[] OPP_PROFILE_UUIDS = new ParcelUuid[] {
        BluetoothUuid.ObexObjectPush
    };

    // TODO: close profiles when we're shutting down
    private static Map<Profile, LocalBluetoothProfileManager> sProfileMap =
            new HashMap<Profile, LocalBluetoothProfileManager>();

    protected LocalBluetoothManager mLocalManager;

    public static void init(LocalBluetoothManager localManager) {
        synchronized (sProfileMap) {
            if (sProfileMap.size() == 0) {
                LocalBluetoothProfileManager profileManager;

                profileManager = new A2dpProfileManager(localManager);
                sProfileMap.put(Profile.A2DP, profileManager);

                profileManager = new HeadsetProfileManager(localManager);
                sProfileMap.put(Profile.HEADSET, profileManager);

                profileManager = new OppProfileManager(localManager);
                sProfileMap.put(Profile.OPP, profileManager);
            }
        }
    }

    public static LocalBluetoothProfileManager getProfileManager(LocalBluetoothManager localManager,
            Profile profile) {
        // Note: This code assumes that "localManager" is same as the
        // LocalBluetoothManager that was used to initialize the sProfileMap.
        // If that every changes, we can't just keep one copy of sProfileMap.
        synchronized (sProfileMap) {
            LocalBluetoothProfileManager profileManager = sProfileMap.get(profile);
            if (profileManager == null) {
                Log.e(TAG, "profileManager can't be found for " + profile.toString());
            }
            return profileManager;
        }
    }

    /**
     * Temporary method to fill profiles based on a device's class.
     *
     * NOTE: This list happens to define the connection order. We should put this logic in a more
     * well known place when this method is no longer temporary.
     * @param uuids of the remote device
     * @param profiles The list of profiles to fill
     */
    public static void updateProfiles(ParcelUuid[] uuids, List<Profile> profiles) {
        profiles.clear();

        if (uuids == null) {
            return;
        }

        if (BluetoothUuid.containsAnyUuid(uuids, HEADSET_PROFILE_UUIDS)) {
            profiles.add(Profile.HEADSET);
        }

        if (BluetoothUuid.containsAnyUuid(uuids, A2DP_PROFILE_UUIDS)) {
            profiles.add(Profile.A2DP);
        }

        if (BluetoothUuid.containsAnyUuid(uuids, OPP_PROFILE_UUIDS)) {
            profiles.add(Profile.OPP);
        }
    }

    protected LocalBluetoothProfileManager(LocalBluetoothManager localManager) {
        mLocalManager = localManager;
    }

    public abstract Set<BluetoothDevice> getConnectedDevices();

    public abstract boolean connect(BluetoothDevice device);

    public abstract boolean disconnect(BluetoothDevice device);

    public abstract int getConnectionStatus(BluetoothDevice device);

    public abstract int getSummary(BluetoothDevice device);

    public abstract int convertState(int a2dpState);

    public abstract boolean isPreferred(BluetoothDevice device);

    public abstract int getPreferred(BluetoothDevice device);

    public abstract void setPreferred(BluetoothDevice device, boolean preferred);

    public boolean isConnected(BluetoothDevice device) {
        return SettingsBtStatus.isConnectionStatusConnected(getConnectionStatus(device));
    }

    // TODO: int instead of enum
    public enum Profile {
        HEADSET(R.string.bluetooth_profile_headset),
        A2DP(R.string.bluetooth_profile_a2dp),
        OPP(R.string.bluetooth_profile_opp);

        public final int localizedString;

        private Profile(int localizedString) {
            this.localizedString = localizedString;
        }
    }

    /**
     * A2dpProfileManager is an abstraction for the {@link BluetoothA2dp} service.
     */
    private static class A2dpProfileManager extends LocalBluetoothProfileManager {
        private BluetoothA2dp mService;

        public A2dpProfileManager(LocalBluetoothManager localManager) {
            super(localManager);
            mService = new BluetoothA2dp(localManager.getContext());
        }

        @Override
        public Set<BluetoothDevice> getConnectedDevices() {
            return mService.getNonDisconnectedSinks();
        }

        @Override
        public boolean connect(BluetoothDevice device) {
            Set<BluetoothDevice> sinks = mService.getNonDisconnectedSinks();
            if (sinks != null) {
                for (BluetoothDevice sink : sinks) {
                    mService.disconnectSink(sink);
                }
            }
            return mService.connectSink(device);
        }

        @Override
        public boolean disconnect(BluetoothDevice device) {
            // Downgrade priority as user is disconnecting the sink.
            if (mService.getSinkPriority(device) > BluetoothA2dp.PRIORITY_ON) {
                mService.setSinkPriority(device, BluetoothA2dp.PRIORITY_ON);
            }
            return mService.disconnectSink(device);
        }

        @Override
        public int getConnectionStatus(BluetoothDevice device) {
            return convertState(mService.getSinkState(device));
        }

        @Override
        public int getSummary(BluetoothDevice device) {
            int connectionStatus = getConnectionStatus(device);

            if (SettingsBtStatus.isConnectionStatusConnected(connectionStatus)) {
                return R.string.bluetooth_a2dp_profile_summary_connected;
            } else {
                return SettingsBtStatus.getConnectionStatusSummary(connectionStatus);
            }
        }

        @Override
        public boolean isPreferred(BluetoothDevice device) {
            return mService.getSinkPriority(device) > BluetoothA2dp.PRIORITY_OFF;
        }

        @Override
        public int getPreferred(BluetoothDevice device) {
            return mService.getSinkPriority(device);
        }

        @Override
        public void setPreferred(BluetoothDevice device, boolean preferred) {
            if (preferred) {
                if (mService.getSinkPriority(device) < BluetoothA2dp.PRIORITY_ON) {
                    mService.setSinkPriority(device, BluetoothA2dp.PRIORITY_ON);
                }
            } else {
                mService.setSinkPriority(device, BluetoothA2dp.PRIORITY_OFF);
            }
        }

        @Override
        public int convertState(int a2dpState) {
            switch (a2dpState) {
            case BluetoothA2dp.STATE_CONNECTED:
                return SettingsBtStatus.CONNECTION_STATUS_CONNECTED;
            case BluetoothA2dp.STATE_CONNECTING:
                return SettingsBtStatus.CONNECTION_STATUS_CONNECTING;
            case BluetoothA2dp.STATE_DISCONNECTED:
                return SettingsBtStatus.CONNECTION_STATUS_DISCONNECTED;
            case BluetoothA2dp.STATE_DISCONNECTING:
                return SettingsBtStatus.CONNECTION_STATUS_DISCONNECTING;
            case BluetoothA2dp.STATE_PLAYING:
                return SettingsBtStatus.CONNECTION_STATUS_ACTIVE;
            default:
                return SettingsBtStatus.CONNECTION_STATUS_UNKNOWN;
            }
        }
    }

    /**
     * HeadsetProfileManager is an abstraction for the {@link BluetoothHeadset} service.
     */
    private static class HeadsetProfileManager extends LocalBluetoothProfileManager
            implements BluetoothHeadset.ServiceListener {
        private BluetoothHeadset mService;
        private Handler mUiHandler = new Handler();

        public HeadsetProfileManager(LocalBluetoothManager localManager) {
            super(localManager);
            mService = new BluetoothHeadset(localManager.getContext(), this);
        }

        public void onServiceConnected() {
            // This could be called on a non-UI thread, funnel to UI thread.
            mUiHandler.post(new Runnable() {
                public void run() {
                    /*
                     * We just bound to the service, so refresh the UI of the
                     * headset device.
                     */
                    BluetoothDevice device = mService.getCurrentHeadset();
                    if (device == null) return;
                    mLocalManager.getCachedDeviceManager()
                            .onProfileStateChanged(device, Profile.HEADSET,
                                                   BluetoothHeadset.STATE_CONNECTED);
                }
            });
        }

        public void onServiceDisconnected() {
        }

        @Override
        public Set<BluetoothDevice> getConnectedDevices() {
            Set<BluetoothDevice> devices = null;
            BluetoothDevice device = mService.getCurrentHeadset();
            if (device != null) {
                devices = new HashSet<BluetoothDevice>();
                devices.add(device);
            }
            return devices;
        }

        @Override
        public boolean connect(BluetoothDevice device) {
            // Since connectHeadset fails if already connected to a headset, we
            // disconnect from any headset first
            mService.disconnectHeadset();
            return mService.connectHeadset(device);
        }

        @Override
        public boolean disconnect(BluetoothDevice device) {
            if (mService.getCurrentHeadset().equals(device)) {
                // Downgrade prority as user is disconnecting the headset.
                if (mService.getPriority(device) > BluetoothHeadset.PRIORITY_ON) {
                    mService.setPriority(device, BluetoothHeadset.PRIORITY_ON);
                }
                return mService.disconnectHeadset();
            } else {
                return false;
            }
        }

        @Override
        public int getConnectionStatus(BluetoothDevice device) {
            BluetoothDevice currentDevice = mService.getCurrentHeadset();
            return currentDevice != null && currentDevice.equals(device)
                    ? convertState(mService.getState())
                    : SettingsBtStatus.CONNECTION_STATUS_DISCONNECTED;
        }

        @Override
        public int getSummary(BluetoothDevice device) {
            int connectionStatus = getConnectionStatus(device);

            if (SettingsBtStatus.isConnectionStatusConnected(connectionStatus)) {
                return R.string.bluetooth_headset_profile_summary_connected;
            } else {
                return SettingsBtStatus.getConnectionStatusSummary(connectionStatus);
            }
        }

        @Override
        public boolean isPreferred(BluetoothDevice device) {
            return mService.getPriority(device) > BluetoothHeadset.PRIORITY_OFF;
        }

        @Override
        public int getPreferred(BluetoothDevice device) {
            return mService.getPriority(device);
        }

        @Override
        public void setPreferred(BluetoothDevice device, boolean preferred) {
            if (preferred) {
                if (mService.getPriority(device) < BluetoothHeadset.PRIORITY_ON) {
                    mService.setPriority(device, BluetoothHeadset.PRIORITY_ON);
                }
            } else {
                mService.setPriority(device, BluetoothHeadset.PRIORITY_OFF);
            }
        }

        @Override
        public int convertState(int headsetState) {
            switch (headsetState) {
            case BluetoothHeadset.STATE_CONNECTED:
                return SettingsBtStatus.CONNECTION_STATUS_CONNECTED;
            case BluetoothHeadset.STATE_CONNECTING:
                return SettingsBtStatus.CONNECTION_STATUS_CONNECTING;
            case BluetoothHeadset.STATE_DISCONNECTED:
                return SettingsBtStatus.CONNECTION_STATUS_DISCONNECTED;
            default:
                return SettingsBtStatus.CONNECTION_STATUS_UNKNOWN;
            }
        }
    }

    /**
     * OppProfileManager
     */
    private static class OppProfileManager extends LocalBluetoothProfileManager {

        public OppProfileManager(LocalBluetoothManager localManager) {
            super(localManager);
        }

        @Override
        public Set<BluetoothDevice> getConnectedDevices() {
            return null;
        }

        @Override
        public boolean connect(BluetoothDevice device) {
            return false;
        }

        @Override
        public boolean disconnect(BluetoothDevice device) {
            return false;
        }

        @Override
        public int getConnectionStatus(BluetoothDevice device) {
            return -1;
        }

        @Override
        public int getSummary(BluetoothDevice device) {
            int connectionStatus = getConnectionStatus(device);

            if (SettingsBtStatus.isConnectionStatusConnected(connectionStatus)) {
                return R.string.bluetooth_opp_profile_summary_connected;
            } else {
                return R.string.bluetooth_opp_profile_summary_not_connected;
            }
        }

        @Override
        public boolean isPreferred(BluetoothDevice device) {
            return false;
        }

        @Override
        public int getPreferred(BluetoothDevice device) {
            return -1;
        }

        @Override
        public void setPreferred(BluetoothDevice device, boolean preferred) {
        }

        @Override
        public int convertState(int oppState) {
            switch (oppState) {
            case 0:
                return SettingsBtStatus.CONNECTION_STATUS_CONNECTED;
            case 1:
                return SettingsBtStatus.CONNECTION_STATUS_CONNECTING;
            case 2:
                return SettingsBtStatus.CONNECTION_STATUS_DISCONNECTED;
            default:
                return SettingsBtStatus.CONNECTION_STATUS_UNKNOWN;
            }
        }
    }
}
