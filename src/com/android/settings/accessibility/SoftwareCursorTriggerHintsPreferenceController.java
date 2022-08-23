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

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/** Controller class that control accessibility software cursor trigger hints settings. */
public class SoftwareCursorTriggerHintsPreferenceController extends TogglePreferenceController {

    private static final String SETTINGS_VALUE =
            Settings.Secure.ACCESSIBILITY_SOFTWARE_CURSOR_TRIGGER_HINTS_ENABLED;

    private final ContentResolver mContentResolver;

    public SoftwareCursorTriggerHintsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContentResolver, SETTINGS_VALUE, OFF) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putInt(mContentResolver, SETTINGS_VALUE, isChecked ? ON : OFF);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        SwitchPreference preference = screen.findPreference(getPreferenceKey());
        if (preference != null) {
            preference.setOnPreferenceChangeListener(this);
            preference.setChecked(isChecked());
        }
    }
}
