/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.Arrays;
import java.util.List;

/** A toggle preference controller for disable animations. */
public class DisableAnimationsPreferenceController extends TogglePreferenceController implements
        LifecycleObserver, OnStart, OnStop {

    @VisibleForTesting
    static final float ANIMATION_ON_VALUE = 1.0f;
    @VisibleForTesting
    static final float ANIMATION_OFF_VALUE = 0.0f;

    // Settings that should be changed when toggling animations
    @VisibleForTesting
    static final List<String> TOGGLE_ANIMATION_TARGETS = Arrays.asList(
            Settings.Global.WINDOW_ANIMATION_SCALE, Settings.Global.TRANSITION_ANIMATION_SCALE,
            Settings.Global.ANIMATOR_DURATION_SCALE
    );

    private final ContentObserver mSettingsContentObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())){
        @Override
        public void onChange(boolean selfChange) {
            updateState(mPreference);
        }
    };

    private final ContentResolver mContentResolver;
    private TwoStatePreference mPreference;

    public DisableAnimationsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public boolean isChecked() {
        boolean allAnimationsDisabled = true;
        for (String animationSetting : TOGGLE_ANIMATION_TARGETS) {
            final float value = Settings.Global.getFloat(mContentResolver, animationSetting,
                    ANIMATION_ON_VALUE);
            if (value > ANIMATION_OFF_VALUE) {
                allAnimationsDisabled = false;
                break;
            }
        }
        return allAnimationsDisabled;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final float newAnimationValue = isChecked ? ANIMATION_OFF_VALUE : ANIMATION_ON_VALUE;
        boolean allAnimationSet = true;
        for (String animationPreference : TOGGLE_ANIMATION_TARGETS) {
            allAnimationSet &= Settings.Global.putFloat(mContentResolver, animationPreference,
                    newAnimationValue);
        }
        return allAnimationSet;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        for (String key : TOGGLE_ANIMATION_TARGETS) {
            mContentResolver.registerContentObserver(Settings.Global.getUriFor(key),
                    false, mSettingsContentObserver, UserHandle.USER_ALL);
        }
    }

    @Override
    public void onStop() {
        mContentResolver.unregisterContentObserver(mSettingsContentObserver);
    }
}
