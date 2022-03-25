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

package com.android.settings.notification;

import android.content.Context;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.Spatializer;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * The controller of the Spatial audio setting for wired headphones in the SoundSettings.
 */
public class SpatialAudioWiredHeadphonesController extends TogglePreferenceController {

    private final Spatializer mSpatializer;
    @VisibleForTesting
    final AudioDeviceAttributes mWiredHeadphones = new AudioDeviceAttributes(
            AudioDeviceAttributes.ROLE_OUTPUT, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, ""
    );

    public SpatialAudioWiredHeadphonesController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        AudioManager audioManager = context.getSystemService(AudioManager.class);
        mSpatializer = audioManager.getSpatializer();
    }

    @Override
    public int getAvailabilityStatus() {
        return mSpatializer.isAvailableForDevice(mWiredHeadphones) ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return mSpatializer.getCompatibleAudioDevices().contains(mWiredHeadphones);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            mSpatializer.addCompatibleAudioDevice(mWiredHeadphones);
        } else {
            mSpatializer.removeCompatibleAudioDevice(mWiredHeadphones);
        }
        return isChecked == isChecked();
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_sound;
    }
}
