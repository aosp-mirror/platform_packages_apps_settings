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

package com.android.settings.gestures;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.VideoPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

public abstract class GesturePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnResume, OnPause, OnCreate, OnSaveInstanceState  {

    @VisibleForTesting
    static final String KEY_VIDEO_PAUSED = "key_video_paused";

    private VideoPreference mVideoPreference;
    @VisibleForTesting
    boolean mVideoPaused;

    public GesturePreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mVideoPreference = (VideoPreference) screen.findPreference(getVideoPrefKey());
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final boolean isEnabled = isSwitchPrefEnabled();
        if (preference != null) {
            if (preference instanceof TwoStatePreference) {
                ((TwoStatePreference) preference).setChecked(isEnabled);
            } else {
                preference.setSummary(isEnabled
                        ? R.string.gesture_setting_on
                        : R.string.gesture_setting_off);
            }
            // Different meanings of "Enabled" for the Preference and Controller.
            preference.setEnabled(canHandleClicks());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mVideoPaused = savedInstanceState.getBoolean(KEY_VIDEO_PAUSED, false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_VIDEO_PAUSED, mVideoPaused);
    }

    @Override
    public void onPause() {
        if (mVideoPreference != null) {
            mVideoPaused = mVideoPreference.isVideoPaused();
            mVideoPreference.onViewInvisible();
        }
    }

    @Override
    public void onResume() {
        if (mVideoPreference != null) {
            mVideoPreference.onViewVisible(mVideoPaused);
        }
    }

    protected abstract String getVideoPrefKey();

    protected abstract boolean isSwitchPrefEnabled();

    protected boolean canHandleClicks() {
        return true;
    }
}
