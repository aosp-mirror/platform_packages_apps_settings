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

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;

import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint;
import com.android.settings.accessibility.TextReadingResetPreference.ButtonStyle;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.instrumentation.SettingsStatsLog;

/**
 * The controller of the reset button in the text and reading options page.
 */
class TextReadingResetController extends BasePreferenceController {
    @ButtonStyle
    private int mButtonStyle;
    private final View.OnClickListener mOnResetClickListener;

    @EntryPoint
    private int mEntryPoint;

    TextReadingResetController(Context context, String preferenceKey,
            @Nullable View.OnClickListener listener) {
        super(context, preferenceKey);
        mOnResetClickListener = listener;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        final TextReadingResetPreference resetPreference =
                (TextReadingResetPreference) screen.findPreference(getPreferenceKey());
        resetPreference.setOnResetClickListener(v -> {
            if (mOnResetClickListener != null) {
                mOnResetClickListener.onClick(v);

                SettingsStatsLog.write(
                        SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED,
                        AccessibilityStatsLogUtils.convertToItemKeyName(getPreferenceKey()),
                        /* reset */ -1,
                        AccessibilityStatsLogUtils.convertToEntryPoint(mEntryPoint));
            }
        });

        resetPreference.setSetupWizardStyle(mButtonStyle);
    }

    void setSetupWizardStyle() {
        mButtonStyle = ButtonStyle.SUW;
    }

    /**
     * The entry point is used for logging.
     *
     * @param entryPoint from which settings page
     */
    void setEntryPoint(@EntryPoint int entryPoint) {
        mEntryPoint = entryPoint;
    }

    /**
     * Interface for resetting to default state.
     */
    interface ResetStateListener {
        /**
         * Called when the reset button was clicked.
         */
        void resetState();
    }
}
