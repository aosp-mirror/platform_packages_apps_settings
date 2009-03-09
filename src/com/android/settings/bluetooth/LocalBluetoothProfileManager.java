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
import android.bluetooth.BluetoothError;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LocalBluetoothProfileManager is an abstract class defining the basic
 * functionality related to a profile.
 */
public abstract class LocalBluetoothProfileManager {

    // TODO: close profiles when we're shutting down
    private static Map<Profile, LocalBluetoothProfileManager> sProfileMap =
            new HashMap<Profile, LocalBluetoothProfileManager>(); 
    
    protected LocalBluetoothManager mLocalManager;
    
    public static LocalBluetoothProfileManager getProfileManager(LocalBluetoothManager localManager,
            Profile profile) {
        
        LocalBluetoothProfileManager profileManager;
        
        synchronized (sProfileMap) {
            profileManager = sProfileMap.get(profile);
            
            if (profileManager == null) {
                switch (profile) {
                case A2DP:
                    profileManager = new A2dpProfileManager(localManager);
                    break;
                    
                case HEADSET:
                    profileManager = new HeadsetProfileManager(localManager);
                    break;
                }
                
                sProfileMap.put(profile, profileManager);    
            }
        }
        
        return profileManager;
    }

    /**
     * Temporary method to fill profiles based on a device's class.
     * 
     * @param btClass The class
     * @param profiles The list of profiles to fill
     */
    public static void fill(int btClass, List<Profile> profiles) {
        profiles.clear();

        if (BluetoothA2dp.doesClassMatchSink(btClass)) {
            profiles.add(Profile.A2DP);
        }
        
        if (BluetoothHeadset.doesClassMatch(btClass)) {
            profiles.add(Profile.HEADSET);
        }
    }

    protected LocalBluetoothProfileManager(LocalBluetoothManager localManager) {
        mLocalManager = localManager;
    }
    
    public abstract int connect(String address);
    
    public abstract int disconnect(String address);
    
    public abstract int getConnectionStatus(String address);

    public abstract int getSummary(String address);

    public abstract boolean isPreferred(String address);

    public abstract void setPreferred(String address, boolean preferred);

    public boolean isConnected(String address) {
        return SettingsBtStatus.isConnectionStatusConnected(getConnectionStatus(address));
    }
    
    // TODO: int instead of enum
    public enum Profile {
        HEADSET(R.string.bluetooth_profile_headset),
        A2DP(R.string.bluetooth_profile_a2dp);
        
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
        public int connect(String address) {
            List<String> sinks = mService.listConnectedSinks();
            if (sinks != null) {
                for (String sinkAddress : sinks) {
                    mService.disconnectSink(sinkAddress);
                }
            }
            return mService.connectSink(address);
        }

        @Override
        public int disconnect(String address) {
            return mService.disconnectSink(address);
        }
        
        @Override
        public int getConnectionStatus(String address) {
            return convertState(mService.getSinkState(address));
        }
        
        @Override
        public int getSummary(String address) {
            int connectionStatus = getConnectionStatus(address);

            if (SettingsBtStatus.isConnectionStatusConnected(connectionStatus)) {
                return R.string.bluetooth_a2dp_profile_summary_connected;
            } else {
                return SettingsBtStatus.getConnectionStatusSummary(connectionStatus);
            }
        }

        @Override
        public boolean isPreferred(String address) {
            return mService.getSinkPriority(address) > BluetoothA2dp.PRIORITY_OFF;
        }

        @Override
        public void setPreferred(String address, boolean preferred) {
            mService.setSinkPriority(address,
                    preferred ? BluetoothA2dp.PRIORITY_AUTO : BluetoothA2dp.PRIORITY_OFF);
        }

        private static int convertState(int a2dpState) {
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
                    String address = mService.getHeadsetAddress();
                    if (TextUtils.isEmpty(address)) return;
                    mLocalManager.getLocalDeviceManager().onProfileStateChanged(address);
                }
            });
        }

        public void onServiceDisconnected() {
        }

        @Override
        public int connect(String address) {
            // Since connectHeadset fails if already connected to a headset, we
            // disconnect from any headset first
            mService.disconnectHeadset();
            return mService.connectHeadset(address)
                    ? BluetoothError.SUCCESS : BluetoothError.ERROR;
        }

        @Override
        public int disconnect(String address) {
            if (mService.getHeadsetAddress().equals(address)) {
                return mService.disconnectHeadset() ? BluetoothError.SUCCESS : BluetoothError.ERROR;
            } else {
                return BluetoothError.SUCCESS;
            }
        }
        
        @Override
        public int getConnectionStatus(String address) {
            String headsetAddress = mService.getHeadsetAddress();
            return headsetAddress != null && headsetAddress.equals(address)
                    ? convertState(mService.getState())
                    : SettingsBtStatus.CONNECTION_STATUS_DISCONNECTED;
        }
        
        @Override
        public int getSummary(String address) {
            int connectionStatus = getConnectionStatus(address);
            
            if (SettingsBtStatus.isConnectionStatusConnected(connectionStatus)) {
                return R.string.bluetooth_headset_profile_summary_connected;
            } else {
                return SettingsBtStatus.getConnectionStatusSummary(connectionStatus);
            }
        }

        @Override
        public boolean isPreferred(String address) {
            return mService.getPriority(address) > BluetoothHeadset.PRIORITY_OFF;
        }

        @Override
        public void setPreferred(String address, boolean preferred) {
            mService.setPriority(address,
                    preferred ? BluetoothHeadset.PRIORITY_AUTO : BluetoothHeadset.PRIORITY_OFF);
        }

        private static int convertState(int headsetState) {
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
}
