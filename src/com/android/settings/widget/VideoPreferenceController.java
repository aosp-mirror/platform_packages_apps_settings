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

package com.android.settings.widget;

import android.content.Context;

import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class VideoPreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnResume, OnPause {

    private VideoPreference mVideoPreference;
    private boolean mVideoPaused;

    public VideoPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return mVideoPreference.isAnimationAvailable() ?
                AVAILABLE_UNSEARCHABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mVideoPreference = screen.findPreference(getPreferenceKey());
        super.displayPreference(screen);
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

}
