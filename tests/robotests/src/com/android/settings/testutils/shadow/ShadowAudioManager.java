/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.testutils.shadow;

import static org.robolectric.RuntimeEnvironment.application;

import android.media.AudioDeviceCallback;
import android.media.AudioManager;
import android.os.Handler;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;

@Implements(value = AudioManager.class, inheritImplementationMethods = true)
public class ShadowAudioManager extends org.robolectric.shadows.ShadowAudioManager {
    private int mRingerMode;
    private boolean mMusicActiveRemotely = false;
    private ArrayList<AudioDeviceCallback> mDeviceCallbacks = new ArrayList();

    @Implementation
    private int getRingerModeInternal() {
        return mRingerMode;
    }

    public static ShadowAudioManager getShadow() {
        return Shadow.extract(application.getSystemService(AudioManager.class));
    }

    public void setRingerModeInternal(int mode) {
        mRingerMode = mode;
    }

    public void registerAudioDeviceCallback(AudioDeviceCallback callback, Handler handler) {
        mDeviceCallbacks.add(callback);
    }

    public void unregisterAudioDeviceCallback(AudioDeviceCallback callback) {
        if (mDeviceCallbacks.contains(callback)) {
            mDeviceCallbacks.remove(callback);
        }
    }

    public void setMusicActiveRemotely(boolean flag) {
        mMusicActiveRemotely = flag;
    }

    public boolean isMusicActiveRemotely() {
        return mMusicActiveRemotely;
    }

    @Resetter
    public void reset() {
        mDeviceCallbacks.clear();
    }
}
