/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.Context;
import android.provider.Settings;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/** Controller that accesses and switches the preference status of following typing feature */
public class MagnificationFollowTypingPreferenceController extends TogglePreferenceController
        implements LifecycleObserver {

    private static final String TAG =
            MagnificationFollowTypingPreferenceController.class.getSimpleName();
    static final String PREF_KEY = "magnification_follow_typing";

    private TwoStatePreference mFollowTypingPreference;

    public MagnificationFollowTypingPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_FOLLOW_TYPING_ENABLED, ON) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_FOLLOW_TYPING_ENABLED,
                (isChecked ? ON : OFF));
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mFollowTypingPreference = screen.findPreference(getPreferenceKey());
    }

    // TODO(b/186731461): Remove it when this controller is used in DashBoardFragment only.
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    void onResume() {
        updateState();
    }

    /**
     * Updates the state of preference components which has been displayed by
     * {@link MagnificationFollowTypingPreferenceController#displayPreference}.
     */
    void updateState() {
        updateState(mFollowTypingPreference);
    }
}
