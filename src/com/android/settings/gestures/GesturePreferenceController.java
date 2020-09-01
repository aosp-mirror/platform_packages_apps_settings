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

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.widget.VideoPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public abstract class GesturePreferenceController extends TogglePreferenceController
        implements Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnStart, OnStop {

    private VideoPreference mVideoPreference;

    public GesturePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mVideoPreference = screen.findPreference(getVideoPrefKey());
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            // Different meanings of "Enabled" for the Preference and Controller.
            preference.setEnabled(canHandleClicks());
        }
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getText(
                isChecked() ? R.string.gesture_setting_on : R.string.gesture_setting_off);
    }

    @Override
    public void onStart() {
        if (mVideoPreference != null) {
            mVideoPreference.onViewVisible();
        }
    }

    @Override
    public void onStop() {
        if (mVideoPreference != null) {
            mVideoPreference.onViewInvisible();
        }
    }

    protected abstract String getVideoPrefKey();

    protected boolean canHandleClicks() {
        return true;
    }
}
