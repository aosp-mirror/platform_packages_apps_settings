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
import android.text.TextUtils;
import android.text.format.DateFormat;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.Locale;

public class AutoTimeFormatPreferenceController extends AbstractPreferenceController
          implements PreferenceControllerMixin {

    private static final String KEY_AUTO_24_HOUR = "auto_24hour";

    public AutoTimeFormatPreferenceController(Context context, UpdateTimeAndDateCallback callback) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AUTO_24_HOUR;
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof SwitchPreference)) {
            return;
        }
        ((SwitchPreference) preference).setChecked(isAutoTimeFormatSelection(mContext));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!(preference instanceof TwoStatePreference)
            || !TextUtils.equals(KEY_AUTO_24_HOUR, preference.getKey())) {
            return false;
        }
        boolean auto24HourEnabled = ((SwitchPreference) preference).isChecked();
        Boolean is24Hour;
        if (auto24HourEnabled) {
            is24Hour = null;
        } else {
            is24Hour = is24HourLocale(mContext.getResources().getConfiguration().locale);
        }
        TimeFormatPreferenceController.update24HourFormat(mContext, is24Hour);
        return true;
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
