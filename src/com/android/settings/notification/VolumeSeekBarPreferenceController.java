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

import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.settings.notification.VolumeSeekBarPreference.Callback;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * Base class for preference controller that handles VolumeSeekBarPreference
 */
public abstract class VolumeSeekBarPreferenceController extends
        AdjustVolumeRestrictedPreferenceController implements LifecycleObserver {

    protected VolumeSeekBarPreference mPreference;
    protected VolumeSeekBarPreference.Callback mVolumePreferenceCallback;
    protected AudioHelper mHelper;

    public VolumeSeekBarPreferenceController(Context context, String key) {
        super(context, key);
        setAudioHelper(new AudioHelper(context));
    }

    @VisibleForTesting
    void setAudioHelper(AudioHelper helper) {
        mHelper = helper;
    }

    public void setCallback(Callback callback) {
        mVolumePreferenceCallback = callback;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mPreference = screen.findPreference(getPreferenceKey());
            mPreference.setCallback(mVolumePreferenceCallback);
            mPreference.setStream(getAudioStream());
            mPreference.setMuteIcon(getMuteIcon());
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        if (mPreference != null) {
            mPreference.onActivityResume();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        if (mPreference != null) {
            mPreference.onActivityPause();
        }
    }

    @Override
    public int getSliderPosition() {
        if (mPreference != null) {
            return mPreference.getProgress();
        }
        return mHelper.getStreamVolume(getAudioStream());
    }

    @Override
    public boolean setSliderPosition(int position) {
        if (mPreference != null) {
            mPreference.setProgress(position);
        }
        return mHelper.setStreamVolume(getAudioStream(), position);
    }

    @Override
    public int getMax() {
        if (mPreference != null) {
            return mPreference.getMax();
        }
        return mHelper.getMaxVolume(getAudioStream());
    }

    @Override
    public int getMin() {
        if (mPreference != null) {
            return mPreference.getMin();
        }
        return mHelper.getMinVolume(getAudioStream());
    }

    /**
     * @return the audio stream type
     */
    public abstract int getAudioStream();

    protected abstract int getMuteIcon();

}
