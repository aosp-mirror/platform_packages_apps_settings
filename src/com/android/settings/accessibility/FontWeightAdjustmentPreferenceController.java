/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.graphics.fonts.FontStyle;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.core.instrumentation.SettingsStatsLog;

/** PreferenceController for displaying all text in bold. */
public class FontWeightAdjustmentPreferenceController extends TogglePreferenceController implements
        TextReadingResetController.ResetStateListener {
    static final int BOLD_TEXT_ADJUSTMENT =
            FontStyle.FONT_WEIGHT_BOLD - FontStyle.FONT_WEIGHT_NORMAL;

    @EntryPoint
    private int mEntryPoint;

    public FontWeightAdjustmentPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.FONT_WEIGHT_ADJUSTMENT, 0) == BOLD_TEXT_ADJUSTMENT;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        SettingsStatsLog.write(
                SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED,
                AccessibilityStatsLogUtils.convertToItemKeyName(getPreferenceKey()),
                isChecked ? 1 : 0,
                AccessibilityStatsLogUtils.convertToEntryPoint(mEntryPoint));

        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FONT_WEIGHT_ADJUSTMENT, (isChecked ? BOLD_TEXT_ADJUSTMENT : 0));
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    @Override
    public void resetState() {
        setChecked(false);
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
