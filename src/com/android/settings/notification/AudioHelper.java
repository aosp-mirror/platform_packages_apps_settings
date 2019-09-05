/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.annotation.UserIdInt;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import com.android.settings.Utils;

/**
 * Helper class to wrap API for testing
 */
public class AudioHelper {

    private static final String TAG = "AudioHelper";
    private Context mContext;
    private AudioManager mAudioManager;

    public AudioHelper(Context context) {
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    public boolean isSingleVolume() {
        return AudioSystem.isSingleVolume(mContext);
    }

    public int getManagedProfileId(UserManager um) {
        return Utils.getManagedProfileId(um, UserHandle.myUserId());
    }

    public boolean isUserUnlocked(UserManager um, @UserIdInt int userId) {
        return um.isUserUnlocked(userId);
    }

    public Context createPackageContextAsUser(@UserIdInt int profileId) {
        return Utils.createPackageContextAsUser(mContext, profileId);
    }

    public int getRingerModeInternal() {
        return mAudioManager.getRingerModeInternal();
    }

    public int getLastAudibleStreamVolume(int stream) {
        return mAudioManager.getLastAudibleStreamVolume(stream);
    }

    public int getStreamVolume(int stream) {
        return mAudioManager.getStreamVolume(stream);
    }

    public boolean setStreamVolume(int stream, int volume) {
        mAudioManager.setStreamVolume(stream, volume, 0);
        return true;
    }

    public int getMaxVolume(int stream) {
        return mAudioManager.getStreamMaxVolume(stream);
    }

    public int getMinVolume(int stream) {
        int minVolume;
        try {
            minVolume = mAudioManager.getStreamMinVolume(stream);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid stream type " + stream);
            // Fallback to STREAM_VOICE_CALL because CallVolumePreferenceController.java default
            // return STREAM_VOICE_CALL in getAudioStream
            minVolume = mAudioManager.getStreamMinVolume(AudioManager.STREAM_VOICE_CALL);
        }
        return minVolume;
    }
}
