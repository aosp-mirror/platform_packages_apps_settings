/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.content.res.Resources;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class MagnificationOneFingerPanningPreferenceController
        extends TogglePreferenceController {
    static final String PREF_KEY = Settings.Secure.ACCESSIBILITY_SINGLE_FINGER_PANNING_ENABLED;

    @Nullable
    private TwoStatePreference mSwitchPreference;

    @VisibleForTesting
    final boolean mDefaultValue;

    public MagnificationOneFingerPanningPreferenceController(Context context) {
        super(context, PREF_KEY);
        boolean defaultValue;
        try {
            defaultValue = context.getResources().getBoolean(
                    com.android.internal.R.bool.config_enable_a11y_magnification_single_panning);
        } catch (Resources.NotFoundException e) {
            defaultValue = false;
        }
        mDefaultValue = defaultValue;
    }

    @Override
    public int getAvailabilityStatus() {
        return (Flags.enableMagnificationOneFingerPanningGesture())
                ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(
                mContext.getContentResolver(),
                PREF_KEY,
                (mDefaultValue) ? ON : OFF) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        var toReturn = Settings.Secure.putInt(mContext.getContentResolver(),
                PREF_KEY,
                (isChecked ? ON : OFF));
        if (mSwitchPreference != null) {
            refreshSummary(mSwitchPreference);
        }
        return toReturn;
    }

    @Override
    public CharSequence getSummary() {
        return (isChecked())
                ? mContext.getString(
                        R.string.accessibility_magnification_one_finger_panning_summary_on)
                : mContext.getString(
                        R.string.accessibility_magnification_one_finger_panning_summary_off);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSwitchPreference = screen.findPreference(getPreferenceKey());
        refreshSummary(mSwitchPreference);
    }
}
