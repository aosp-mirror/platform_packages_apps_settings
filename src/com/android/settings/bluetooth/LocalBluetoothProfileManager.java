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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothInputDevice;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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

    /* package */ static final ParcelUuid[] HID_PROFILE_UUIDS = new ParcelUuid[] {
        BluetoothUuid.Hid
    };

    /* package */ static final ParcelUuid[] PANU_PROFILE_UUIDS = new ParcelUuid[] {
        BluetoothUuid.PANU
    };

    /* package */ static final ParcelUuid[] NAP_PROFILE_UUIDS = new ParcelUuid[] {
        BluetoothUuid.NAP
    };

    /**
     * An interface for notifying BluetoothHeadset IPC clients when they have
     * been connected to the BluetoothHeadset service.
     */
    public interface ServiceListener {
        /**
         * Called to notify the client when this proxy object has been
         * connected to the BluetoothHeadset service. Clients must wait for
         * this callback before making IPC calls on the BluetoothHeadset
         * service.
         */
        public void onServiceConnected();

        /**
         * Called to notify the client that this proxy object has been
         * disconnected from the BluetoothHeadset service. Clients must not
         * make IPC calls on the BluetoothHeadset service after this callback.
         * This callback will currently only occur if the application hosting
         * the BluetoothHeadset service, but may be called more often in future.
         */
        public void onServiceDisconnected();
    }

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

                profileManager = new HidProfileManager(localManager);
                sProfileMap.put(Profile.HID, profileManager);

                profileManager = new PanProfileManager(localManager);
                sProfileMap.put(Profile.PAN, profileManager);
            }
        }
    }

    private static LinkedList<ServiceListener> mServiceListeners = new LinkedList<ServiceListener>();

    public static void addServiceListener(ServiceListener l) {
        mServiceListeners.add(l);
    }

    public static void removeServiceListener(ServiceListener l) {
        mServiceListeners.remove(l);
    }

    public static boolean isManagerReady() {
        // Getting just the headset profile is fine for now. Will need to deal with A2DP
        // and others if they aren't always in a ready state.
        LocalBluetoothProfileManager profileManager = sProfileMap.get(Profile.HEADSET);
        if (profileManager == null) {
            return sProfileMap.size() > 0;
        }
        return profileManager.isProfileReady();
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

        if (BluetoothUuid.containsAnyUuid(uuids, HID_PROFILE_UUIDS)) {
            profiles.add(Profile.HID);
        }

        if (BluetoothUuid.containsAnyUuid(uuids, PANU_PROFILE_UUIDS)) {
            profiles.add(Profile.PAN);
        }
    }

    protected LocalBluetoothProfileManager(LocalBluetoothManager localManager) {
        mLocalManager = localManager;
    }

    public abstract List<BluetoothDevice> getConnectedDevices();

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

    public abstract boolean isProfileReady();

    // TODO: int instead of enum
    public enum Profile {
        HEADSET(R.string.bluetooth_profile_headset),
        A2DP(R.string.bluetooth_profile_a2dp),
        OPP(R.string.bluetooth_profile_opp),
        HID(R.string.bluetooth_profile_hid),
        PAN(R.string.bluetooth_profile_pan);

        public final int localizedString;

        private Profile(int localizedString) {
            this.localizedString = localizedString;
        }
    }

    /**
     * A2dpProfileManager is an abstraction for the {@link BluetoothA2dp} service.
     */
    private static class A2dpProfileManager extends LocalBluetoothProfileManager
          implements BluetoothProfile.ServiceListener {
        private BluetoothA2dp mService;

        // TODO(): The calls must wait for mService. Its not null just
        // because it runs in the system server.
        public A2dpProfileManager(LocalBluetoothManager localManager) {
            super(localManager);
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            adapter.getProfileProxy(localManager.getContext(), this, BluetoothProfile.A2DP);

        }

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mService = (BluetoothA2dp) proxy;
        }

        public void onServiceDisconnected(int profile) {
            mService = null;
        }

        @Override
        public List<BluetoothDevice> getConnectedDevices() {
            return mService.getDevicesMatchingConnectionStates(
                  new int[] {BluetoothProfile.STATE_CONNECTED,
                             BluetoothProfile.STATE_CONNECTING,
                             BluetoothProfile.STATE_DISCONNECTING});
        }

        @Override
        public boolean connect(BluetoothDevice device) {
            List<BluetoothDevice> sinks = getConnectedDevices();
            if (sinks != null) {
                for (BluetoothDevice sink : sinks) {
                    mService.disconnect(sink);
                }
            }
            return mService.connect(device);
        }

        @Override
        public boolean disconnect(BluetoothDevice device) {
            // Downgrade priority as user is disconnecting the sink.
            if (mService.getPriority(device) > BluetoothProfile.PRIORITY_ON) {
                mService.setPriority(device, BluetoothProfile.PRIORITY_ON);
            }
            return mService.disconnect(device);
        }

        @Override
        public int getConnectionStatus(BluetoothDevice device) {
            return convertState(mService.getConnectionState(device));
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
            return mService.getPriority(device) > BluetoothProfile.PRIORITY_OFF;
        }

        @Override
        public int getPreferred(BluetoothDevice device) {
            return mService.getPriority(device);
        }

        @Override
        public void setPreferred(BluetoothDevice device, boolean preferred) {
            if (preferred) {
                if (mService.getPriority(device) < BluetoothProfile.PRIORITY_ON) {
                    mService.setPriority(device, BluetoothProfile.PRIORITY_ON);
                }
            } else {
                mService.setPriority(device, BluetoothProfile.PRIORITY_OFF);
            }
        }

        @Override
        public int convertState(int a2dpState) {
            switch (a2dpState) {
            case BluetoothProfile.STATE_CONNECTED:
                return SettingsBtStatus.CONNECTION_STATUS_CONNECTED;
            case BluetoothProfile.STATE_CONNECTING:
                return SettingsBtStatus.CONNECTION_STATUS_CONNECTING;
            case BluetoothProfile.STATE_DISCONNECTED:
                return SettingsBtStatus.CONNECTION_STATUS_DISCONNECTED;
            case BluetoothProfile.STATE_DISCONNECTING:
                return SettingsBtStatus.CONNECTION_STATUS_DISCONNECTING;
            case BluetoothA2dp.STATE_PLAYING:
                return SettingsBtStatus.CONNECTION_STATUS_ACTIVE;
            default:
                return SettingsBtStatus.CONNECTION_STATUS_UNKNOWN;
            }
        }

        @Override
        public boolean isProfileReady() {
            return true;
        }
    }

    /**
     * HeadsetProfileManager is an abstraction for the {@link BluetoothHeadset} service.
     */
    private static class HeadsetProfileManager extends LocalBluetoothProfileManager
            implements BluetoothProfile.ServiceListener {
        private BluetoothHeadset mService;
        private Handler mUiHandler = new Handler();
        private boolean profileReady = false;

        // TODO(): The calls must get queued if mService becomes null.
        // It can happen  when phone app crashes for some reason.
        // All callers should have service listeners. Dock Service is the only
        // one right now.
        public HeadsetProfileManager(LocalBluetoothManager localManager) {
            super(localManager);
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            adapter.getProfileProxy(localManager.getContext(), this, BluetoothProfile.HEADSET);
        }

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mService = (BluetoothHeadset) proxy;
            profileReady = true;
            // This could be called on a non-UI thread, funnel to UI thread.
            mUiHandler.post(new Runnable() {
                public void run() {
                    /*
                     * We just bound to the service, so refresh the UI of the
                     * headset device.
                     */
                    List<BluetoothDevice> deviceList = mService.getConnectedDevices();
                    if (deviceList.size() == 0) return;

                    mLocalManager.getCachedDeviceManager()
                            .onProfileStateChanged(deviceList.get(0), Profile.HEADSET,
                                                   BluetoothProfile.STATE_CONNECTED);
                }
            });

            if (mServiceListeners.size() > 0) {
                Iterator<ServiceListener> it = mServiceListeners.iterator();
                while(it.hasNext()) {
                    it.next().onServiceConnected();
                }
            }
        }

        public void onServiceDisconnected(int profile) {
            mService = null;
            profileReady = false;
            if (mServiceListeners.size() > 0) {
                Iterator<ServiceListener> it = mServiceListeners.iterator();
                while(it.hasNext()) {
                    it.next().onServiceDisconnected();
                }
            }
        }

        @Override
        public boolean isProfileReady() {
            return profileReady;
        }

        @Override
        public List<BluetoothDevice> getConnectedDevices() {
            return mService.getConnectedDevices();
        }

        @Override
        public boolean connect(BluetoothDevice device) {
            return mService.connect(device);
        }

        @Override
        public boolean disconnect(BluetoothDevice device) {
            List<BluetoothDevice> deviceList = getConnectedDevices();
            if (deviceList.size() != 0 && deviceList.get(0).equals(device)) {
                // Downgrade prority as user is disconnecting the headset.
                if (mService.getPriority(device) > BluetoothProfile.PRIORITY_ON) {
                    mService.setPriority(device, BluetoothProfile.PRIORITY_ON);
                }
                return mService.disconnect(device);
            } else {
                return false;
            }
        }

        @Override
        public int getConnectionStatus(BluetoothDevice device) {
            List<BluetoothDevice> deviceList = getConnectedDevices();

            return deviceList.size() > 0 && deviceList.get(0).equals(device)
                    ? convertState(mService.getConnectionState(device))
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
            return mService.getPriority(device) > BluetoothProfile.PRIORITY_OFF;
        }

        @Override
        public int getPreferred(BluetoothDevice device) {
            return mService.getPriority(device);
        }

        @Override
        public void setPreferred(BluetoothDevice device, boolean preferred) {
            if (preferred) {
                if (mService.getPriority(device) < BluetoothProfile.PRIORITY_ON) {
                    mService.setPriority(device, BluetoothProfile.PRIORITY_ON);
                }
            } else {
                mService.setPriority(device, BluetoothProfile.PRIORITY_OFF);
            }
        }

        @Override
        public int convertState(int headsetState) {
            switch (headsetState) {
            case BluetoothProfile.STATE_CONNECTED:
                return SettingsBtStatus.CONNECTION_STATUS_CONNECTED;
            case BluetoothProfile.STATE_CONNECTING:
                return SettingsBtStatus.CONNECTION_STATUS_CONNECTING;
            case BluetoothProfile.STATE_DISCONNECTED:
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
        public List<BluetoothDevice> getConnectedDevices() {
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
        public boolean isProfileReady() {
            return true;
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

    private static class HidProfileManager extends LocalBluetoothProfileManager {
        private BluetoothInputDevice mService;

        public HidProfileManager(LocalBluetoothManager localManager) {
            super(localManager);
            mService = new BluetoothInputDevice(localManager.getContext());
        }

        @Override
        public boolean connect(BluetoothDevice device) {
            return mService.connectInputDevice(device);
        }

        @Override
        public int convertState(int hidState) {
            switch (hidState) {
            case BluetoothInputDevice.STATE_CONNECTED:
                return SettingsBtStatus.CONNECTION_STATUS_CONNECTED;
            case BluetoothInputDevice.STATE_CONNECTING:
                return SettingsBtStatus.CONNECTION_STATUS_CONNECTING;
            case BluetoothInputDevice.STATE_DISCONNECTED:
                return SettingsBtStatus.CONNECTION_STATUS_DISCONNECTED;
            case BluetoothInputDevice.STATE_DISCONNECTING:
                return SettingsBtStatus.CONNECTION_STATUS_DISCONNECTING;
            default:
                return SettingsBtStatus.CONNECTION_STATUS_UNKNOWN;
            }
        }

        @Override
        public boolean disconnect(BluetoothDevice device) {
            return mService.disconnectInputDevice(device);
        }

        @Override
        public List<BluetoothDevice> getConnectedDevices() {
            return mService.getConnectedInputDevices();
        }

        @Override
        public int getConnectionStatus(BluetoothDevice device) {
            return convertState(mService.getInputDeviceState(device));
        }

        @Override
        public int getPreferred(BluetoothDevice device) {
            return mService.getInputDevicePriority(device);
        }

        @Override
        public int getSummary(BluetoothDevice device) {
            final int connectionStatus = getConnectionStatus(device);

            if (SettingsBtStatus.isConnectionStatusConnected(connectionStatus)) {
                return R.string.bluetooth_hid_profile_summary_connected;
            } else {
                return SettingsBtStatus.getConnectionStatusSummary(connectionStatus);
            }
        }

        @Override
        public boolean isPreferred(BluetoothDevice device) {
            return mService.getInputDevicePriority(device) > BluetoothInputDevice.PRIORITY_OFF;
        }

        @Override
        public boolean isProfileReady() {
            return true;
        }

        @Override
        public void setPreferred(BluetoothDevice device, boolean preferred) {
            if (preferred) {
                if (mService.getInputDevicePriority(device) < BluetoothInputDevice.PRIORITY_ON) {
                    mService.setInputDevicePriority(device, BluetoothInputDevice.PRIORITY_ON);
                }
            } else {
                mService.setInputDevicePriority(device, BluetoothInputDevice.PRIORITY_OFF);
            }
        }
    }

    private static class PanProfileManager extends LocalBluetoothProfileManager {
        private BluetoothPan mService;

        public PanProfileManager(LocalBluetoothManager localManager) {
            super(localManager);
            mService = new BluetoothPan(localManager.getContext());
        }

        @Override
        public boolean connect(BluetoothDevice device) {
            return mService.connect(device);
        }

        @Override
        public int convertState(int panState) {
            switch (panState) {
            case BluetoothPan.STATE_CONNECTED:
                return SettingsBtStatus.CONNECTION_STATUS_CONNECTED;
            case BluetoothPan.STATE_CONNECTING:
                return SettingsBtStatus.CONNECTION_STATUS_CONNECTING;
            case BluetoothPan.STATE_DISCONNECTED:
                return SettingsBtStatus.CONNECTION_STATUS_DISCONNECTED;
            case BluetoothPan.STATE_DISCONNECTING:
                return SettingsBtStatus.CONNECTION_STATUS_DISCONNECTING;
            default:
                return SettingsBtStatus.CONNECTION_STATUS_UNKNOWN;
            }
        }

        @Override
        public boolean disconnect(BluetoothDevice device) {
            return mService.disconnect(device);
        }

        @Override
        public int getSummary(BluetoothDevice device) {
            final int connectionStatus = getConnectionStatus(device);

            if (SettingsBtStatus.isConnectionStatusConnected(connectionStatus)) {
                return R.string.bluetooth_pan_profile_summary_connected;
            } else {
                return SettingsBtStatus.getConnectionStatusSummary(connectionStatus);
            }
        }

        @Override
        public boolean isProfileReady() {
            return true;
        }

        @Override
        public List<BluetoothDevice> getConnectedDevices() {
            return mService.getConnectedDevices();
        }

        @Override
        public int getConnectionStatus(BluetoothDevice device) {
            return convertState(mService.getPanDeviceState(device));
        }

        @Override
        public int getPreferred(BluetoothDevice device) {
            return -1;
        }

        @Override
        public boolean isPreferred(BluetoothDevice device) {
            return false;
        }

        @Override
        public void setPreferred(BluetoothDevice device, boolean preferred) {
            return;
        }
    }
}
