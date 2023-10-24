/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.media.Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE;

import android.content.Context;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.Spatializer;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * The controller of the Spatial audio setting in the bluetooth detail settings.
 */
public class BluetoothDetailsSpatialAudioController extends BluetoothDetailsController
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "BluetoothSpatialAudioController";
    private static final String KEY_SPATIAL_AUDIO_GROUP = "spatial_audio_group";
    private static final String KEY_SPATIAL_AUDIO = "spatial_audio";
    private static final String KEY_HEAD_TRACKING = "head_tracking";

    private final Spatializer mSpatializer;

    @VisibleForTesting
    PreferenceCategory mProfilesContainer;
    @VisibleForTesting
    AudioDeviceAttributes mAudioDevice = null;

    public BluetoothDetailsSpatialAudioController(
            Context context,
            PreferenceFragmentCompat fragment,
            CachedBluetoothDevice device,
            Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        AudioManager audioManager = context.getSystemService(AudioManager.class);
        mSpatializer = audioManager.getSpatializer();
    }

    @Override
    public boolean isAvailable() {
        return mSpatializer.getImmersiveAudioLevel() != SPATIALIZER_IMMERSIVE_LEVEL_NONE;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        TwoStatePreference switchPreference = (TwoStatePreference) preference;
        String key = switchPreference.getKey();
        if (TextUtils.equals(key, KEY_SPATIAL_AUDIO)) {
            updateSpatializerEnabled(switchPreference.isChecked());
            refreshSpatialAudioEnabled(switchPreference);
            return true;
        } else if (TextUtils.equals(key, KEY_HEAD_TRACKING)) {
            updateSpatializerHeadTracking(switchPreference.isChecked());
            return true;
        } else {
            Log.w(TAG, "invalid key name.");
            return false;
        }
    }

    private void updateSpatializerEnabled(boolean enabled)  {
        if (mAudioDevice == null) {
            Log.w(TAG, "cannot update spatializer enabled for null audio device.");
            return;
        }
        if (enabled) {
            mSpatializer.addCompatibleAudioDevice(mAudioDevice);
        } else {
            mSpatializer.removeCompatibleAudioDevice(mAudioDevice);
        }
    }

    private void updateSpatializerHeadTracking(boolean enabled)  {
        if (mAudioDevice == null) {
            Log.w(TAG, "cannot update spatializer head tracking for null audio device.");
            return;
        }
        mSpatializer.setHeadTrackerEnabled(enabled, mAudioDevice);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SPATIAL_AUDIO_GROUP;
    }

    @Override
    protected void init(PreferenceScreen screen) {
        mProfilesContainer = screen.findPreference(getPreferenceKey());
        refresh();
    }

    @Override
    protected void refresh() {
        if (mAudioDevice == null) {
            getAvailableDevice();
        }

        TwoStatePreference spatialAudioPref = mProfilesContainer.findPreference(KEY_SPATIAL_AUDIO);
        if (spatialAudioPref == null && mAudioDevice != null) {
            spatialAudioPref = createSpatialAudioPreference(mProfilesContainer.getContext());
            mProfilesContainer.addPreference(spatialAudioPref);
        } else if (mAudioDevice == null || !mSpatializer.isAvailableForDevice(mAudioDevice)) {
            if (spatialAudioPref != null) {
                mProfilesContainer.removePreference(spatialAudioPref);
            }
            final TwoStatePreference headTrackingPref =
                    mProfilesContainer.findPreference(KEY_HEAD_TRACKING);
            if (headTrackingPref != null) {
                mProfilesContainer.removePreference(headTrackingPref);
            }
            mAudioDevice = null;
            return;
        }

        refreshSpatialAudioEnabled(spatialAudioPref);
    }

    private void refreshSpatialAudioEnabled(TwoStatePreference spatialAudioPref) {
        boolean isSpatialAudioOn = mSpatializer.getCompatibleAudioDevices().contains(mAudioDevice);
        Log.d(TAG, "refresh() isSpatialAudioOn : " + isSpatialAudioOn);
        spatialAudioPref.setChecked(isSpatialAudioOn);

        TwoStatePreference headTrackingPref = mProfilesContainer.findPreference(KEY_HEAD_TRACKING);
        if (headTrackingPref == null) {
            headTrackingPref = createHeadTrackingPreference(mProfilesContainer.getContext());
            mProfilesContainer.addPreference(headTrackingPref);
        }
        refreshHeadTracking(spatialAudioPref, headTrackingPref);
    }

    private void refreshHeadTracking(TwoStatePreference spatialAudioPref,
            TwoStatePreference headTrackingPref) {
        boolean isHeadTrackingAvailable =
                spatialAudioPref.isChecked() && mSpatializer.hasHeadTracker(mAudioDevice);
        Log.d(TAG, "refresh() has head tracker : " + mSpatializer.hasHeadTracker(mAudioDevice));
        headTrackingPref.setVisible(isHeadTrackingAvailable);
        if (isHeadTrackingAvailable) {
            headTrackingPref.setChecked(mSpatializer.isHeadTrackerEnabled(mAudioDevice));
        }
    }

    @VisibleForTesting
    TwoStatePreference createSpatialAudioPreference(Context context) {
        TwoStatePreference pref = new SwitchPreferenceCompat(context);
        pref.setKey(KEY_SPATIAL_AUDIO);
        pref.setTitle(context.getString(R.string.bluetooth_details_spatial_audio_title));
        pref.setSummary(context.getString(R.string.bluetooth_details_spatial_audio_summary));
        pref.setOnPreferenceClickListener(this);
        return pref;
    }

    @VisibleForTesting
    TwoStatePreference createHeadTrackingPreference(Context context) {
        TwoStatePreference pref = new SwitchPreferenceCompat(context);
        pref.setKey(KEY_HEAD_TRACKING);
        pref.setTitle(context.getString(R.string.bluetooth_details_head_tracking_title));
        pref.setSummary(context.getString(R.string.bluetooth_details_head_tracking_summary));
        pref.setOnPreferenceClickListener(this);
        return pref;
    }

    private void getAvailableDevice() {
        AudioDeviceAttributes a2dpDevice = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                mCachedDevice.getAddress());
        AudioDeviceAttributes bleHeadsetDevice = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                mCachedDevice.getAddress());
        AudioDeviceAttributes bleSpeakerDevice = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
                mCachedDevice.getAddress());
        AudioDeviceAttributes bleBroadcastDevice = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_BLE_BROADCAST,
                mCachedDevice.getAddress());
        AudioDeviceAttributes hearingAidDevice = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_HEARING_AID,
                mCachedDevice.getAddress());

        if (mSpatializer.isAvailableForDevice(bleHeadsetDevice)) {
            mAudioDevice = bleHeadsetDevice;
        } else if (mSpatializer.isAvailableForDevice(bleSpeakerDevice)) {
            mAudioDevice = bleSpeakerDevice;
        } else if (mSpatializer.isAvailableForDevice(bleBroadcastDevice)) {
            mAudioDevice = bleBroadcastDevice;
        } else if (mSpatializer.isAvailableForDevice(a2dpDevice)) {
            mAudioDevice = a2dpDevice;
        } else if (mSpatializer.isAvailableForDevice(hearingAidDevice)) {
            mAudioDevice = hearingAidDevice;
        } else {
            mAudioDevice = null;
        }

        Log.d(TAG, "getAvailableDevice() device : "
                + mCachedDevice.getDevice().getAnonymizedAddress()
                + ", is available : " + (mAudioDevice != null)
                + ", type : " + (mAudioDevice == null ? "no type" : mAudioDevice.getType()));
    }

    @VisibleForTesting
    void setAvailableDevice(AudioDeviceAttributes audioDevice) {
        mAudioDevice = audioDevice;
    }
}
