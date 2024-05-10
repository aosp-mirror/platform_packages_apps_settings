/*
 * Copyright 2018 The Android Open Source Project
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

import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.connecteddevice.audiosharing.AudioSharingUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/** Controller to maintain available media Bluetooth devices */
public class AvailableMediaBluetoothDeviceUpdater extends BluetoothDeviceUpdater
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "AvailableMediaBluetoothDeviceUpdater";
    private static final boolean DBG = Log.isLoggable(BluetoothDeviceUpdater.TAG, Log.DEBUG);

    private static final String PREF_KEY = "available_media_bt";

    private final AudioManager mAudioManager;
    private final LocalBluetoothManager mLocalBtManager;

    public AvailableMediaBluetoothDeviceUpdater(
            Context context,
            DevicePreferenceCallback devicePreferenceCallback,
            int metricsCategory) {
        super(context, devicePreferenceCallback, metricsCategory);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mLocalBtManager = Utils.getLocalBtManager(context);
    }

    @Override
    public void onAudioModeChanged() {
        forceUpdate();
    }

    @Override
    public boolean isFilterMatched(CachedBluetoothDevice cachedDevice) {
        final int audioMode = mAudioManager.getMode();
        final int currentAudioProfile;

        if (audioMode == AudioManager.MODE_RINGTONE
                || audioMode == AudioManager.MODE_IN_CALL
                || audioMode == AudioManager.MODE_IN_COMMUNICATION) {
            // in phone call
            currentAudioProfile = BluetoothProfile.HEADSET;
        } else {
            // without phone call
            currentAudioProfile = BluetoothProfile.A2DP;
        }

        boolean isFilterMatched = false;
        if (isDeviceConnected(cachedDevice) && isDeviceInCachedDevicesList(cachedDevice)) {
            if (DBG) {
                Log.d(TAG, "isFilterMatched() current audio profile : " + currentAudioProfile);
            }
            // If device is Hearing Aid, it is compatible with HFP and A2DP.
            // It would show in Available Devices group.
            if (cachedDevice.isConnectedAshaHearingAidDevice()) {
                Log.d(
                        TAG,
                        "isFilterMatched() device : "
                                + cachedDevice.getName()
                                + ", the Hearing Aid profile is connected.");
                return true;
            }
            // If device is LE Audio, it is compatible with HFP and A2DP.
            // It would show in Available Devices group if the audio sharing flag is disabled or
            // the device is not in the audio sharing session.
            if (cachedDevice.isConnectedLeAudioDevice()) {
                if (!AudioSharingUtils.isFeatureEnabled()
                        || !AudioSharingUtils.hasBroadcastSource(cachedDevice, mLocalManager)) {
                    Log.d(
                            TAG,
                            "isFilterMatched() device : "
                                    + cachedDevice.getName()
                                    + ", the LE Audio profile is connected and not in sharing.");
                    return true;
                }
            }
            // According to the current audio profile type,
            // this page will show the bluetooth device that have corresponding profile.
            // For example:
            // If current audio profile is a2dp, show the bluetooth device that have a2dp profile.
            // If current audio profile is headset,
            // show the bluetooth device that have headset profile.
            switch (currentAudioProfile) {
                case BluetoothProfile.A2DP:
                    isFilterMatched = cachedDevice.isConnectedA2dpDevice();
                    break;
                case BluetoothProfile.HEADSET:
                    isFilterMatched = cachedDevice.isConnectedHfpDevice();
                    break;
            }
            if (DBG) {
                Log.d(
                        TAG,
                        "isFilterMatched() device : "
                                + cachedDevice.getName()
                                + ", isFilterMatched : "
                                + isFilterMatched);
            }
        }
        return isFilterMatched;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        mMetricsFeatureProvider.logClickedPreference(preference, mMetricsCategory);
        final CachedBluetoothDevice device =
                ((BluetoothDevicePreference) preference).getBluetoothDevice();
        if (AudioSharingUtils.isFeatureEnabled()
                && AudioSharingUtils.isBroadcasting(mLocalBtManager)) {
            if (DBG) {
                Log.d(TAG, "onPreferenceClick stop broadcasting.");
            }
            AudioSharingUtils.stopBroadcasting(mLocalBtManager);
        }
        return device.setActive();
    }

    @Override
    protected String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected void update(CachedBluetoothDevice cachedBluetoothDevice) {
        super.update(cachedBluetoothDevice);
        Log.d(TAG, "Map : " + mPreferenceMap);
    }
}
