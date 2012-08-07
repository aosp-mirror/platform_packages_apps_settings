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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothInputDevice;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;

/**
 * LocalBluetoothProfileManager provides access to the LocalBluetoothProfile
 * objects for the available Bluetooth profiles.
 */
final class LocalBluetoothProfileManager {
    private static final String TAG = "LocalBluetoothProfileManager";
    private static final int CONNECT_HF_OR_A2DP = 1;
    private static final int CONNECT_OTHER_PROFILES = 2;
    // If either a2dp or hf is connected and if the other profile conneciton is not
    // happening with the timeout , the other profile(a2dp or hf) will be inititate connection.
    // Give reasonable timeout for the device to initiate the other profile connection.
    private static final int CONNECT_HF_OR_A2DP_TIMEOUT = 6000;


    /** Singleton instance. */
    private static LocalBluetoothProfileManager sInstance;

    /**
     * An interface for notifying BluetoothHeadset IPC clients when they have
     * been connected to the BluetoothHeadset service.
     * Only used by {@link DockService}.
     */
    public interface ServiceListener {
        /**
         * Called to notify the client when this proxy object has been
         * connected to the BluetoothHeadset service. Clients must wait for
         * this callback before making IPC calls on the BluetoothHeadset
         * service.
         */
        void onServiceConnected();

        /**
         * Called to notify the client that this proxy object has been
         * disconnected from the BluetoothHeadset service. Clients must not
         * make IPC calls on the BluetoothHeadset service after this callback.
         * This callback will currently only occur if the application hosting
         * the BluetoothHeadset service, but may be called more often in future.
         */
        void onServiceDisconnected();
    }

    private final Context mContext;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final CachedBluetoothDeviceManager mDeviceManager;
    private final BluetoothEventManager mEventManager;

    private A2dpProfile mA2dpProfile;
    private HeadsetProfile mHeadsetProfile;
    private final HidProfile mHidProfile;
    private OppProfile mOppProfile;
    private final PanProfile mPanProfile;
    private boolean isHfServiceUp;
    private boolean isA2dpServiceUp;
    private boolean isHfA2dpConnectMessagePosted;
    private final Handler hfA2dpConnectHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

        synchronized (this) {
            if (isA2dpConnectRequired((BluetoothDevice)msg.obj)) {
                mA2dpProfile.connect((BluetoothDevice)msg.obj);
            } else if (isHfConnectRequired((BluetoothDevice)msg.obj)) {
                mHeadsetProfile.connect((BluetoothDevice)msg.obj);
            }
            isHfA2dpConnectMessagePosted =false;
        }
    }
};

        private final Handler connectOtherProfilesHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
            synchronized (this) {
                // Connect all the profiles which are enabled
                // Right now hf/a2dp profiles connect is handled here
                List<BluetoothDevice> hfConnDevList= mHeadsetProfile.getConnectedDevices();

                if (hfConnDevList.isEmpty() && mHeadsetProfile.isPreferred((BluetoothDevice)msg.obj))
                    mHeadsetProfile.connect((BluetoothDevice)msg.obj);
                else
                    Log.d(TAG,"Hf device is not preferred or already Hf connected device exist");

                List<BluetoothDevice> a2dpConnDevList= mA2dpProfile.getConnectedDevices();

                if (a2dpConnDevList.isEmpty() && mA2dpProfile.isPreferred((BluetoothDevice)msg.obj))
                    mA2dpProfile.connect((BluetoothDevice)msg.obj);
                else
                    Log.d(TAG,"A2dp device is not preferred or already a2dp connected device exist");

            }
        }
    };

    /**
     * Mapping from profile name, e.g. "HEADSET" to profile object.
     */
    private final Map<String, LocalBluetoothProfile>
            mProfileNameMap = new HashMap<String, LocalBluetoothProfile>();

    LocalBluetoothProfileManager(Context context,
            LocalBluetoothAdapter adapter,
            CachedBluetoothDeviceManager deviceManager,
            BluetoothEventManager eventManager) {
        mContext = context;

        mLocalAdapter = adapter;
        mDeviceManager = deviceManager;
        mEventManager = eventManager;
        // pass this reference to adapter and event manager (circular dependency)
        mLocalAdapter.setProfileManager(this);
        mEventManager.setProfileManager(this);

        ParcelUuid[] uuids = adapter.getUuids();

        // uuids may be null if Bluetooth is turned off
        if (uuids != null) {
            updateLocalProfiles(uuids);
        }

        // Always add HID and PAN profiles
        mHidProfile = new HidProfile(context, mLocalAdapter);
        addProfile(mHidProfile, HidProfile.NAME,
                BluetoothInputDevice.ACTION_CONNECTION_STATE_CHANGED);

        mPanProfile = new PanProfile(context);
        addPanProfile(mPanProfile, PanProfile.NAME,
                BluetoothPan.ACTION_CONNECTION_STATE_CHANGED);

        Log.d(TAG, "LocalBluetoothProfileManager construction complete");
    }

    /**
     * Initialize or update the local profile objects. If a UUID was previously
     * present but has been removed, we print a warning but don't remove the
     * profile object as it might be referenced elsewhere, or the UUID might
     * come back and we don't want multiple copies of the profile objects.
     * @param uuids
     */
    void updateLocalProfiles(ParcelUuid[] uuids) {
        // A2DP
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.AudioSource)) {
            if (mA2dpProfile == null) {
                Log.d(TAG, "Adding local A2DP profile");
                mA2dpProfile = new A2dpProfile(mContext, this);
                addProfile(mA2dpProfile, A2dpProfile.NAME,
                        BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
            }
        } else if (mA2dpProfile != null) {
            Log.w(TAG, "Warning: A2DP profile was previously added but the UUID is now missing.");
        }

        // Headset / Handsfree
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Handsfree_AG) ||
            BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HSP_AG)) {
            if (mHeadsetProfile == null) {
                Log.d(TAG, "Adding local HEADSET profile");
                mHeadsetProfile = new HeadsetProfile(mContext, mLocalAdapter,
                        mDeviceManager, this);
                addProfile(mHeadsetProfile, HeadsetProfile.NAME,
                        BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
            }
        } else if (mHeadsetProfile != null) {
            Log.w(TAG, "Warning: HEADSET profile was previously added but the UUID is now missing.");
        }

        // OPP
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.ObexObjectPush)) {
            if (mOppProfile == null) {
                Log.d(TAG, "Adding local OPP profile");
                mOppProfile = new OppProfile();
                // Note: no event handler for OPP, only name map.
                mProfileNameMap.put(OppProfile.NAME, mOppProfile);
            }
        } else if (mOppProfile != null) {
            Log.w(TAG, "Warning: OPP profile was previously added but the UUID is now missing.");
        }
        mEventManager.registerProfileIntentReceiver();

        // There is no local SDP record for HID and Settings app doesn't control PBAP
    }

    private final Collection<ServiceListener> mServiceListeners =
            new ArrayList<ServiceListener>();

    private void addProfile(LocalBluetoothProfile profile,
            String profileName, String stateChangedAction) {
        mEventManager.addProfileHandler(stateChangedAction, new StateChangedHandler(profile));
        mProfileNameMap.put(profileName, profile);
    }

    private void addPanProfile(LocalBluetoothProfile profile,
            String profileName, String stateChangedAction) {
        mEventManager.addProfileHandler(stateChangedAction,
                new PanStateChangedHandler(profile));
        mProfileNameMap.put(profileName, profile);
    }

    LocalBluetoothProfile getProfileByName(String name) {
        return mProfileNameMap.get(name);
    }

    // Called from LocalBluetoothAdapter when state changes to ON
    void setBluetoothStateOn() {
        ParcelUuid[] uuids = mLocalAdapter.getUuids();
        if (uuids != null) {
            updateLocalProfiles(uuids);
        }
        mEventManager.readPairedDevices();
    }

    /**
     * Generic handler for connection state change events for the specified profile.
     */
    private class StateChangedHandler implements BluetoothEventManager.Handler {
        final LocalBluetoothProfile mProfile;

        StateChangedHandler(LocalBluetoothProfile profile) {
            mProfile = profile;
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            CachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.w(TAG, "StateChangedHandler found new device: " + device);
                cachedDevice = mDeviceManager.addDevice(mLocalAdapter,
                        LocalBluetoothProfileManager.this, device);
            }
            int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
            int oldState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, 0);
            if (newState == BluetoothProfile.STATE_DISCONNECTED &&
                    oldState == BluetoothProfile.STATE_CONNECTING) {
                Log.i(TAG, "Failed to connect " + mProfile + " device");
            }

            cachedDevice.onProfileStateChanged(mProfile, newState);
            cachedDevice.refresh();

            if ((mProfile instanceof HeadsetProfile)||(mProfile instanceof A2dpProfile)) {
                if ((BluetoothProfile.STATE_CONNECTED == newState)&&
                    (!isHfA2dpConnectMessagePosted)) {
                    Message mes = hfA2dpConnectHandler.obtainMessage(CONNECT_HF_OR_A2DP);
                    mes.obj = device;
                    hfA2dpConnectHandler.sendMessageDelayed(mes,CONNECT_HF_OR_A2DP_TIMEOUT);
                    Log.i(TAG,"Message posted for hf/a2dp connection");
                    isHfA2dpConnectMessagePosted = true;
                } else if (isHfA2dpConnectMessagePosted) {
                    hfA2dpConnectHandler.removeMessages(CONNECT_HF_OR_A2DP);
                    Log.i(TAG,"Message removed for hf/a2dp connection");
                    isHfA2dpConnectMessagePosted =false;
                }
            }
        }
    }

    /** State change handler for NAP and PANU profiles. */
    private class PanStateChangedHandler extends StateChangedHandler {

        PanStateChangedHandler(LocalBluetoothProfile profile) {
            super(profile);
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            PanProfile panProfile = (PanProfile) mProfile;
            int role = intent.getIntExtra(BluetoothPan.EXTRA_LOCAL_ROLE, 0);
            panProfile.setLocalRole(device, role);
            super.onReceive(context, intent, device);
        }
    }

    // called from DockService
    void addServiceListener(ServiceListener l) {
        mServiceListeners.add(l);
    }

    // called from DockService
    void removeServiceListener(ServiceListener l) {
        mServiceListeners.remove(l);
    }

    // not synchronized: use only from UI thread! (TODO: verify)
    void callServiceConnectedListeners() {
        for (ServiceListener l : mServiceListeners) {
            l.onServiceConnected();
        }
    }

    // not synchronized: use only from UI thread! (TODO: verify)
    void callServiceDisconnectedListeners() {
        for (ServiceListener listener : mServiceListeners) {
            listener.onServiceDisconnected();
        }
    }

    synchronized void setHfServiceUp(boolean isUp) {
        isHfServiceUp = isUp;
        if (isHfServiceUp && isA2dpServiceUp) {
            // connect hf and then a2dp
            // this order is maintained as per the white paper
                handleAutoConnect(mHeadsetProfile);
                handleAutoConnect(mA2dpProfile);
        }
    }

    synchronized void setA2dpServiceUp(boolean isUp) {
        isA2dpServiceUp= isUp;
        if (isHfServiceUp && isA2dpServiceUp) {
            // connect hf and then a2dp
            // this order is maintained as per the white paper
                handleAutoConnect(mHeadsetProfile);
                handleAutoConnect(mA2dpProfile);
        }
    }

    private void handleAutoConnect(LocalBluetoothProfile profile) {
        Set<BluetoothDevice> bondedDevices = mLocalAdapter.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            if (profile.getPreferred(device) ==
                      BluetoothProfile.PRIORITY_AUTO_CONNECT) {
                  Log.d(TAG,"handleAutoConnect for device");
                  CachedBluetoothDevice cacheDevice = mDeviceManager.findDevice(device);
                  if (null == cacheDevice)
                  {
                      Log.w(TAG,"Dev not found in cached dev list. Adding the dev to cached list");
                      cacheDevice = mDeviceManager.addDevice(mLocalAdapter,
                                       LocalBluetoothProfileManager.this, device);
                  }
                  cacheDevice.connectInt(profile);
                  break;
            }
        }
    }

    public void enableAutoConnectForHf(BluetoothDevice device,boolean enable) {
        mHeadsetProfile.enableAutoConnect(device,enable);
    }

    public void enableAutoConnectForA2dp(BluetoothDevice device,boolean enable) {
        mA2dpProfile.enableAutoConnect(device,enable);
    }

    public void handleConnectOtherProfiles(BluetoothDevice device) {
        if (device != null){
            // Remove previous messages if any
            connectOtherProfilesHandler.removeMessages(CONNECT_OTHER_PROFILES);
            Message mes = connectOtherProfilesHandler.obtainMessage(CONNECT_OTHER_PROFILES);
            mes.obj = device;
            connectOtherProfilesHandler.sendMessageDelayed(mes,CONNECT_HF_OR_A2DP_TIMEOUT);
            Log.i(TAG,"Message posted for connection other Profiles ");
        } else {
            Log.e(TAG,"Device = Null received in handleConnectOtherProfiles ");
        }
    }

    // This is called by DockService, so check Headset and A2DP.
    public synchronized boolean isManagerReady() {
        // Getting just the headset profile is fine for now. Will need to deal with A2DP
        // and others if they aren't always in a ready state.
        LocalBluetoothProfile profile = mHeadsetProfile;
        if (profile != null) {
            return profile.isProfileReady();
        }
        profile = mA2dpProfile;
        if (profile != null) {
            return profile.isProfileReady();
        }
        return false;
    }

    A2dpProfile getA2dpProfile() {
        return mA2dpProfile;
    }

    HeadsetProfile getHeadsetProfile() {
        return mHeadsetProfile;
    }

    /**
     * Fill in a list of LocalBluetoothProfile objects that are supported by
     * the local device and the remote device.
     *
     * @param uuids of the remote device
     * @param localUuids UUIDs of the local device
     * @param profiles The list of profiles to fill
     * @param removedProfiles list of profiles that were removed
     */
    synchronized void updateProfiles(ParcelUuid[] uuids, ParcelUuid[] localUuids,
            Collection<LocalBluetoothProfile> profiles,
            Collection<LocalBluetoothProfile> removedProfiles,
            boolean isPanNapConnected) {
        // Copy previous profile list into removedProfiles
        removedProfiles.clear();
        removedProfiles.addAll(profiles);
        profiles.clear();

        if (uuids == null) {
            return;
        }

        if (mHeadsetProfile != null) {
            if ((BluetoothUuid.isUuidPresent(localUuids, BluetoothUuid.HSP_AG) &&
                    BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HSP)) ||
                    (BluetoothUuid.isUuidPresent(localUuids, BluetoothUuid.Handsfree_AG) &&
                            BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Handsfree))) {
                profiles.add(mHeadsetProfile);
                removedProfiles.remove(mHeadsetProfile);
            }
        }

        if (BluetoothUuid.containsAnyUuid(uuids, A2dpProfile.SINK_UUIDS) &&
            mA2dpProfile != null) {
            profiles.add(mA2dpProfile);
            removedProfiles.remove(mA2dpProfile);
        }

        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.ObexObjectPush) &&
            mOppProfile != null) {
            profiles.add(mOppProfile);
            removedProfiles.remove(mOppProfile);
        }

        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Hid) &&
            mHidProfile != null) {
            profiles.add(mHidProfile);
            removedProfiles.remove(mHidProfile);
        }

        if(isPanNapConnected)
            Log.d(TAG, "Valid PAN-NAP connection exists.");
        if ((BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.NAP) &&
            mPanProfile != null) || isPanNapConnected) {
            profiles.add(mPanProfile);
            removedProfiles.remove(mPanProfile);
        }
    }

    private boolean isHfConnectRequired(BluetoothDevice device) {
        List<BluetoothDevice> a2dpConnDevList= mA2dpProfile.getConnectedDevices();
        List<BluetoothDevice> hfConnDevList= mHeadsetProfile.getConnectedDevices();

        // If both hf and a2dp is connected hf connection is not required
        // Hf connection is required only when a2dp is connected but
        // hf connect did no happen untill CONNECT_HF_OR_A2DP_TIMEOUT
        if (!a2dpConnDevList.isEmpty() && !hfConnDevList.isEmpty())
            return false;
        if (hfConnDevList.isEmpty() && mHeadsetProfile.isPreferred(device))
            return true;

        return false;
    }

    private boolean isA2dpConnectRequired(BluetoothDevice device) {
        List<BluetoothDevice> a2dpConnDevList= mA2dpProfile.getConnectedDevices();
        List<BluetoothDevice> hfConnDevList= mHeadsetProfile.getConnectedDevices();

        // If both hf and a2dp is connected a2dp connection is not required
        // A2dp connection is required only when hf is connected but
        // a2dp connect did no happen until CONNECT_HF_OR_A2DP_TIMEOUT
        if (!a2dpConnDevList.isEmpty() && !hfConnDevList.isEmpty())
            return false;
        if (a2dpConnDevList.isEmpty() && mA2dpProfile.isPreferred(device))
            return true;

        return false;
    }

}
