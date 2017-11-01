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
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.notification.VolumeSeekBarPreference.Callback;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * Base class for preference controller that handles VolumeSeekBarPreference
 */
public abstract class VolumeSeekBarPreferenceController extends
        AdjustVolumeRestrictedPreferenceController implements LifecycleObserver, OnResume, OnPause {

    protected VolumeSeekBarPreference mPreference;
    protected VolumeSeekBarPreference.Callback mVolumePreferenceCallback;

    public VolumeSeekBarPreferenceController(Context context, Callback callback,
            Lifecycle lifecycle) {
        super(context);
        mVolumePreferenceCallback = callback;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mPreference = (VolumeSeekBarPreference) screen.findPreference(getPreferenceKey());
            mPreference.setCallback(mVolumePreferenceCallback);
            mPreference.setStream(getAudioStream());
            mPreference.setMuteIcon(getMuteIcon());
        }
    }

    @Override
    public void onResume() {
        if (mPreference != null) {
            mPreference.onActivityResume();
        }
    }

    @Override
    public void onPause() {
        if (mPreference != null) {
            mPreference.onActivityPause();
        }
    }

    protected abstract int getAudioStream();

    protected abstract int getMuteIcon();

}
