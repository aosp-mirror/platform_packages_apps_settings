/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class PictureColorModePreferenceController extends DeveloperOptionsPreferenceController
        implements LifecycleObserver, OnResume, OnPause, PreferenceControllerMixin {

    private static final String KEY_COLOR_MODE = "picture_color_mode";

    private ColorModePreference mPreference;

    public PictureColorModePreferenceController(Context context, Lifecycle lifecycle) {
        super(context);

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        return getColorModeDescriptionsSize() > 1 && !isWideColorGamut();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_COLOR_MODE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.updateCurrentAndSupported();
        }
    }

    @Override
    public void onResume() {
        if (mPreference == null) {
            return;
        }
        mPreference.startListening();
        mPreference.updateCurrentAndSupported();
    }

    @Override
    public void onPause() {
        if (mPreference == null) {
            return;
        }
        mPreference.stopListening();
    }

    @VisibleForTesting
    boolean isWideColorGamut() {
        return mContext.getResources().getConfiguration().isScreenWideColorGamut();
    }

    @VisibleForTesting
    int getColorModeDescriptionsSize() {
        return ColorModePreference.getColorModeDescriptions(mContext).size();
    }
}
