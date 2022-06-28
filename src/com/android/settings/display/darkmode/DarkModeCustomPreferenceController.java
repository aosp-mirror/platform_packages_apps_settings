/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display.darkmode;

import static android.app.UiModeManager.MODE_NIGHT_CUSTOM;

import android.app.TimePickerDialog;
import android.app.UiModeManager;
import android.content.Context;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

import java.time.LocalTime;

/**
 * Controller for custom mode night mode time settings
 */
public class DarkModeCustomPreferenceController extends BasePreferenceController {
    private static final String START_TIME_KEY = "dark_theme_start_time";
    private static final String END_TIME_KEY = "dark_theme_end_time";
    private final UiModeManager mUiModeManager;
    private TimeFormatter mFormat;
    private DarkModeSettingsFragment mFragmet;

    public DarkModeCustomPreferenceController(Context context, String key) {
        super(context, key);
        mFormat = new TimeFormatter(mContext);
        mUiModeManager = context.getSystemService(UiModeManager.class);
    }

    public DarkModeCustomPreferenceController(
            Context context, String key,
            DarkModeSettingsFragment fragment) {
        this(context, key);
        mFragmet = fragment;
    }

    public DarkModeCustomPreferenceController(
            Context context, String key,
            DarkModeSettingsFragment fragment,
            TimeFormatter format) {
        this(context, key, fragment);
        mFormat = format;
    }

    @Override
    public int getAvailabilityStatus() {
        return mUiModeManager.getNightMode() == MODE_NIGHT_CUSTOM
                && mUiModeManager.getNightModeCustomType()
                == UiModeManager.MODE_NIGHT_CUSTOM_TYPE_SCHEDULE
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    public TimePickerDialog getDialog() {
        final LocalTime initialTime;
        if (TextUtils.equals(getPreferenceKey(), START_TIME_KEY)) {
            initialTime = mUiModeManager.getCustomNightModeStart();
        } else {
            initialTime = mUiModeManager.getCustomNightModeEnd();
        }
        return  new TimePickerDialog(mContext, (view, hourOfDay, minute) -> {
            final LocalTime time = LocalTime.of(hourOfDay, minute);
            if (TextUtils.equals(getPreferenceKey(), START_TIME_KEY)) {
                mUiModeManager.setCustomNightModeStart(time);
            } else {
                mUiModeManager.setCustomNightModeEnd(time);
            }
            if (mFragmet != null) {
                mFragmet.refresh();
            }
        }, initialTime.getHour(), initialTime.getMinute(), mFormat.is24HourFormat());
    }

    @Override
    protected void refreshSummary(Preference preference) {
        final LocalTime time;
        if (TextUtils.equals(getPreferenceKey(), START_TIME_KEY)) {
            time = mUiModeManager.getCustomNightModeStart();
        } else {
            time = mUiModeManager.getCustomNightModeEnd();
        }
        preference.setSummary(mFormat.of(time));
    }
}
