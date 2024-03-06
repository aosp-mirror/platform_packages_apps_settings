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

import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.core.instrumentation.SettingsStatsLog;

/**
 * PreferenceController for displaying all text in high contrast style.
 */
public class HighTextContrastPreferenceController extends TogglePreferenceController implements
        TextReadingResetController.ResetStateListener {
    private TwoStatePreference mSwitchPreference;

    @EntryPoint
    private int mEntryPoint;

    public HighTextContrastPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, 0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        SettingsStatsLog.write(
                SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED,
                AccessibilityStatsLogUtils.convertToItemKeyName(getPreferenceKey()),
                isChecked ? 1 : 0,
                AccessibilityStatsLogUtils.convertToEntryPoint(mEntryPoint));

        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, (isChecked ? 1 : 0));
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSwitchPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void resetState() {
        setChecked(false);
        updateState(mSwitchPreference);
    }

    /**
     * The entry point is used for logging.
     *
     * @param entryPoint from which settings page
     */
    void setEntryPoint(@EntryPoint int entryPoint) {
        mEntryPoint = entryPoint;
    }
}
