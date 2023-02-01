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
import android.media.AudioManager;
import android.media.Spatializer;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/**
 * Parent menu summary of the Spatial audio settings
 */
public class SpatialAudioParentPreferenceController extends BasePreferenceController {
    private static final String TAG = "SpatialAudioSetting";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Spatializer mSpatializer;
    private SpatialAudioPreferenceController mSpatialAudioPreferenceController;
    private SpatialAudioWiredHeadphonesController mSpatialAudioWiredHeadphonesController;

    public SpatialAudioParentPreferenceController(Context context, String key) {
        super(context, key);
        AudioManager audioManager = context.getSystemService(AudioManager.class);
        mSpatializer = audioManager.getSpatializer();
        mSpatialAudioPreferenceController = new SpatialAudioPreferenceController(context, "unused");
        mSpatialAudioWiredHeadphonesController = new SpatialAudioWiredHeadphonesController(context,
                "unused");
    }

    @Override
    public int getAvailabilityStatus() {
        int level = mSpatializer.getImmersiveAudioLevel();
        if (DEBUG) {
            Log.d(TAG, "spatialization level: " + level);
        }
        return level == Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE
                ? UNSUPPORTED_ON_DEVICE : AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        boolean speakerOn = mSpatialAudioPreferenceController.isAvailable()
                && mSpatialAudioPreferenceController.isChecked();
        boolean wiredHeadphonesOn = mSpatialAudioWiredHeadphonesController.isAvailable()
                && mSpatialAudioWiredHeadphonesController.isChecked();
        if (speakerOn && wiredHeadphonesOn) {
            return mContext.getString(R.string.spatial_summary_on_two,
                    mContext.getString(R.string.spatial_audio_speaker),
                    mContext.getString(R.string.spatial_audio_wired_headphones));
        } else if (speakerOn) {
            return mContext.getString(R.string.spatial_summary_on_one,
                    mContext.getString(R.string.spatial_audio_speaker));
        } else if (wiredHeadphonesOn) {
            return mContext.getString(R.string.spatial_summary_on_one,
                    mContext.getString(R.string.spatial_audio_wired_headphones));
        } else {
            return mContext.getString(R.string.spatial_summary_off);
        }
    }
}
