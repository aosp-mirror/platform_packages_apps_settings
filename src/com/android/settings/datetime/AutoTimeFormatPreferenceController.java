/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.datetime;

import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.System;
import android.text.format.DateFormat;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import java.util.Locale;

public class AutoTimeFormatPreferenceController extends TogglePreferenceController {

    public AutoTimeFormatPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return isAutoTimeFormatSelection(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Boolean is24Hour;
        if (isChecked) {
            is24Hour = null;
        } else {
            is24Hour = is24HourLocale(mContext.getResources().getConfiguration().locale);
        }
        TimeFormatPreferenceController.update24HourFormat(mContext, is24Hour);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }

    boolean is24HourLocale(Locale locale) {
        return DateFormat.is24HourLocale(locale);
    }

    /**
     * Returns if the system is currently configured to pick the time format automatically based on
     * the locale.
     */
    static boolean isAutoTimeFormatSelection(Context context) {
        return Settings.System.getString(context.getContentResolver(), System.TIME_12_24) == null;
    }
}
