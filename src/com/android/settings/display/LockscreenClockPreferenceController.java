/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Preference to enable/disable the large double-line clock on lockscreen
 */
public class LockscreenClockPreferenceController extends TogglePreferenceController {

    private static final String SETTING_KEY = Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK;

    public LockscreenClockPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(), SETTING_KEY,
                getDoublelineClockDefault()) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), SETTING_KEY,
                isChecked ? 1 : 0);
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getText(R.string.lockscreen_double_line_clock_summary);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(true);
        refreshSummary(preference);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    protected int getDoublelineClockDefault() {
        return mContext.getResources()
            .getInteger(com.android.internal.R.integer.config_doublelineClockDefault);
    }
}
