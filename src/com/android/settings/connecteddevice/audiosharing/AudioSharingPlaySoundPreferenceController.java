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
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class AudioSharingPlaySoundPreferenceController
        extends AudioSharingBasePreferenceController {

    private static final String TAG = "AudioSharingPlaySoundPreferenceController";

    private static final String PREF_KEY = "audio_sharing_play_sound";

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private Ringtone mRingtone;

    public AudioSharingPlaySoundPreferenceController(Context context) {
        super(context, PREF_KEY);
        mRingtone = RingtoneManager.getRingtone(context, getMediaVolumeUri());
        if (mRingtone != null) {
            mRingtone.setStreamType(AudioManager.STREAM_MUSIC);
        }
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return (mRingtone != null && BluetoothUtils.isAudioSharingEnabled())
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (mPreference != null) {
            mPreference.setOnPreferenceClickListener(
                    (v) -> {
                        if (mRingtone == null) {
                            Log.d(TAG, "Skip onClick due to ringtone is null");
                            return true;
                        }
                        try {
                            mRingtone.setAudioAttributes(
                                    new AudioAttributes.Builder(mRingtone.getAudioAttributes())
                                            .setFlags(AudioAttributes.FLAG_BYPASS_MUTE)
                                            .addTag("VX_AOSP_SAMPLESOUND")
                                            .build());
                            if (!mRingtone.isPlaying()) {
                                mRingtone.play();
                                mMetricsFeatureProvider.action(
                                        mContext,
                                        SettingsEnums.ACTION_AUDIO_SHARING_PLAY_TEST_SOUND);
                            }
                        } catch (Throwable e) {
                            Log.w(TAG, "Fail to play sample, error = " + e);
                        }
                        return true;
                    });
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        super.onStop(owner);
        if (mRingtone != null && mRingtone.isPlaying()) {
            mRingtone.stop();
        }
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @VisibleForTesting
    void setRingtone(Ringtone ringtone) {
        mRingtone = ringtone;
    }

    private Uri getMediaVolumeUri() {
        return Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE
                        + "://"
                        + mContext.getPackageName()
                        + "/"
                        + R.raw.media_volume);
    }
}
