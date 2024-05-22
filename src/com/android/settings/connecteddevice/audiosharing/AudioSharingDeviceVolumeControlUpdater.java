/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;
import android.widget.SeekBar;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.bluetooth.BluetoothDevicePreference;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;

public class AudioSharingDeviceVolumeControlUpdater extends BluetoothDeviceUpdater
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "AudioSharingDeviceVolumeControlUpdater";

    private static final String PREF_KEY = "audio_sharing_volume_control";

    @Nullable private final LocalBluetoothManager mBtManager;
    @Nullable private final VolumeControlProfile mVolumeControl;

    public AudioSharingDeviceVolumeControlUpdater(
            Context context,
            DevicePreferenceCallback devicePreferenceCallback,
            int metricsCategory) {
        super(context, devicePreferenceCallback, metricsCategory);
        mBtManager = Utils.getLocalBluetoothManager(context);
        mVolumeControl =
                mBtManager == null
                        ? null
                        : mBtManager.getProfileManager().getVolumeControlProfile();
    }

    @Override
    public boolean isFilterMatched(CachedBluetoothDevice cachedDevice) {
        boolean isFilterMatched = false;
        if (isDeviceConnected(cachedDevice) && isDeviceInCachedDevicesList(cachedDevice)) {
            // If device is LE audio device and in a sharing session on current sharing device,
            // it would show in volume control group.
            if (cachedDevice.isConnectedLeAudioDevice()
                    && AudioSharingUtils.isBroadcasting(mBtManager)
                    && BluetoothUtils.hasConnectedBroadcastSource(cachedDevice, mBtManager)) {
                isFilterMatched = true;
            }
        }
        Log.d(
                TAG,
                "isFilterMatched() device : "
                        + cachedDevice.getName()
                        + ", isFilterMatched : "
                        + isFilterMatched);
        return isFilterMatched;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return true;
    }

    @Override
    protected void addPreference(CachedBluetoothDevice cachedDevice) {
        if (cachedDevice == null) return;
        final BluetoothDevice device = cachedDevice.getDevice();
        if (!mPreferenceMap.containsKey(device)) {
            SeekBar.OnSeekBarChangeListener listener =
                    new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(
                                SeekBar seekBar, int progress, boolean fromUser) {}

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {}

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                            int progress = seekBar.getProgress();
                            int groupId = AudioSharingUtils.getGroupId(cachedDevice);
                            if (groupId != BluetoothCsipSetCoordinator.GROUP_ID_INVALID
                                    && groupId
                                            == AudioSharingUtils.getFallbackActiveGroupId(
                                                    mContext)) {
                                // Set media stream volume for primary buds, audio manager will
                                // update all buds volume in the audio sharing.
                                setAudioManagerStreamVolume(progress);
                            } else {
                                // Set buds volume for other buds.
                                setDeviceVolume(cachedDevice, progress);
                            }
                        }
                    };
            AudioSharingDeviceVolumePreference vPreference =
                    new AudioSharingDeviceVolumePreference(mPrefContext, cachedDevice);
            vPreference.initialize();
            vPreference.setOnSeekBarChangeListener(listener);
            vPreference.setKey(getPreferenceKey());
            vPreference.setIcon(com.android.settingslib.R.drawable.ic_bt_untethered_earbuds);
            vPreference.setTitle(cachedDevice.getName());
            mPreferenceMap.put(device, vPreference);
            mDevicePreferenceCallback.onDeviceAdded(vPreference);
        }
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

    @Override
    protected void addPreference(
            CachedBluetoothDevice cachedDevice, @BluetoothDevicePreference.SortType int type) {}

    @Override
    protected void launchDeviceDetails(Preference preference) {}

    @Override
    public void refreshPreference() {}

    private void setDeviceVolume(CachedBluetoothDevice cachedDevice, int progress) {
        if (mVolumeControl != null) {
            mVolumeControl.setDeviceVolume(
                    cachedDevice.getDevice(), progress, /* isGroupOp= */ true);
        }
    }

    private void setAudioManagerStreamVolume(int progress) {
        int seekbarRange =
                AudioSharingDeviceVolumePreference.MAX_VOLUME
                        - AudioSharingDeviceVolumePreference.MIN_VOLUME;
        try {
            AudioManager audioManager = mContext.getSystemService(AudioManager.class);
            int streamVolumeRange =
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            - audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
            int volume = Math.round((float) progress * streamVolumeRange / seekbarRange);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        } catch (RuntimeException e) {
            Log.e(TAG, "Fail to setAudioManagerStreamVolumeForFallbackDevice, error = " + e);
        }
    }
}
