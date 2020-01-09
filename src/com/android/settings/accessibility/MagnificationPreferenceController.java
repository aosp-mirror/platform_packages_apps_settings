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
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class MagnificationPreferenceController extends BasePreferenceController {

    private Preference mPreference;

    public MagnificationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final boolean tripleTapEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, 0) == 1;
        final boolean buttonEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, 0) == 1;

        int summaryResId = 0;
        if (!tripleTapEnabled && !buttonEnabled) {
            summaryResId = R.string.accessibility_feature_state_off;
        } else if (!tripleTapEnabled && buttonEnabled) {
            summaryResId = R.string.accessibility_screen_magnification_navbar_title;
        } else if (tripleTapEnabled && !buttonEnabled) {
            summaryResId = R.string.accessibility_screen_magnification_gestures_title;
        } else {
            summaryResId = R.string.accessibility_screen_magnification_state_navbar_gesture;
        }
        return mContext.getResources().getText(summaryResId);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        configureMagnificationPreferenceIfNeeded();
    }

    private void configureMagnificationPreferenceIfNeeded() {
        mPreference.setFragment(ToggleScreenMagnificationPreferenceFragment.class.getName());
        final Bundle extras = mPreference.getExtras();
        MagnificationGesturesPreferenceController
                .populateMagnificationGesturesPreferenceExtras(extras, mContext);
    }
}
