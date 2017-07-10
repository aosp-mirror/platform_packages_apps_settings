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
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.widget.VideoPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public abstract class GesturePreferenceController extends PreferenceController
        implements Preference.OnPreferenceChangeListener, LifecycleObserver, OnStart, OnStop {

    private VideoPreference mVideoPreference;

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
    public void onStop() {
        if (mVideoPreference != null) {
            mVideoPreference.onViewInvisible();
        }
    }

    @Override
    public void onStart() {
        if (mVideoPreference != null) {
            mVideoPreference.onViewVisible();
        }
    }

    protected abstract String getVideoPrefKey();

    protected abstract boolean isSwitchPrefEnabled();

    protected boolean canHandleClicks() {
        return true;
    }
}
