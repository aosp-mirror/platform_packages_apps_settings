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

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

public class AudioSharingDeviceVolumePreference extends SeekBarPreference {
    private static final String TAG = "AudioSharingVolPref";

    public static final int MIN_VOLUME = 0;
    public static final int MAX_VOLUME = 255;

    private final Context mContext;
    private final CachedBluetoothDevice mCachedDevice;
    @Nullable protected SeekBar mSeekBar;
    private Boolean mTrackingTouch = false;
    private MetricsFeatureProvider mMetricsFeatureProvider =
            FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();

    public AudioSharingDeviceVolumePreference(
            Context context, @NonNull CachedBluetoothDevice device) {
        super(context);
        setLayoutResource(R.layout.preference_volume_slider);
        mContext = context;
        mCachedDevice = device;
    }

    @NonNull
    public CachedBluetoothDevice getCachedDevice() {
        return mCachedDevice;
    }

    /**
     * Initialize {@link AudioSharingDeviceVolumePreference}.
     *
     * <p>Need to be called after creating the preference.
     */
    public void initialize() {
        setMax(MAX_VOLUME);
        setMin(MIN_VOLUME);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        super.onProgressChanged(seekBar, progress, fromUser);
        // When user use talk back swipe up/down or use Switch Access to change the volume bar
        // progress, there is no onStopTrackingTouch triggered. So we need to check this scenario
        // and update the device volume here.
        if (fromUser && !mTrackingTouch) {
            Log.d(TAG, "onProgressChanged from user and not in touch, handleProgressChange.");
            handleProgressChange(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = true;
        super.onStartTrackingTouch(seekBar);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = false;
        super.onStopTrackingTouch(seekBar);
        // When user touch the volume bar to change volume, we only update the device volume when
        // user stop touching the bar.
        Log.d(TAG, "onStopTrackingTouch, handleProgressChange.");
        handleProgressChange(seekBar.getProgress());
    }

    private void handleProgressChange(int progress) {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            int groupId = BluetoothUtils.getGroupId(mCachedDevice);
                            if (groupId != BluetoothCsipSetCoordinator.GROUP_ID_INVALID
                                    && groupId
                                            == BluetoothUtils.getPrimaryGroupIdForBroadcast(
                                                    mContext.getContentResolver())) {
                                // Set media stream volume for primary buds, audio manager will
                                // update all buds volume in the audio sharing.
                                setAudioManagerStreamVolume(progress);
                            } else {
                                // Set buds volume for other buds.
                                setDeviceVolume(mCachedDevice.getDevice(), progress);
                            }
                        });
    }

    private void setDeviceVolume(@Nullable BluetoothDevice device, int progress) {
        if (device == null) {
            Log.d(TAG, "Skip set device volume, device is null");
            return;
        }
        LocalBluetoothManager btManager = Utils.getLocalBtManager(mContext);
        VolumeControlProfile vc =
                btManager == null ? null : btManager.getProfileManager().getVolumeControlProfile();
        if (vc != null) {
            vc.setDeviceVolume(device, progress, /* isGroupOp= */ true);
            mMetricsFeatureProvider.action(
                    mContext,
                    SettingsEnums.ACTION_AUDIO_SHARING_CHANGE_MEDIA_DEVICE_VOLUME,
                    /* isPrimary= */ false);
            Log.d(
                    TAG,
                    "set device volume, device = "
                            + device.getAnonymizedAddress()
                            + " volume = "
                            + progress);
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
            mMetricsFeatureProvider.action(
                    mContext,
                    SettingsEnums.ACTION_AUDIO_SHARING_CHANGE_MEDIA_DEVICE_VOLUME,
                    /* isPrimary= */ true);
            Log.d(TAG, "set music stream volume, volume = " + progress);
        } catch (RuntimeException e) {
            Log.e(TAG, "Fail to setAudioManagerStreamVolumeForFallbackDevice, error = " + e);
        }
    }
}
