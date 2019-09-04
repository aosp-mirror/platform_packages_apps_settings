/*
 * Copyright (C) 2016 The Android Open Source Project
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

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.datetime.ZoneGetter;

import java.util.Calendar;

public class TimeZonePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_TIMEZONE = "timezone";

    private final AutoTimeZonePreferenceController mAutoTimeZonePreferenceController;

    public TimeZonePreferenceController(Context context,
            AutoTimeZonePreferenceController autoTimeZonePreferenceController) {
        super(context);
        mAutoTimeZonePreferenceController = autoTimeZonePreferenceController;
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof RestrictedPreference)) {
            return;
        }
        preference.setSummary(getTimeZoneOffsetAndName());
        if( !((RestrictedPreference) preference).isDisabledByAdmin()) {
            preference.setEnabled(!mAutoTimeZonePreferenceController.isEnabled());
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TIMEZONE;
    }

    @VisibleForTesting
    CharSequence getTimeZoneOffsetAndName() {
        final Calendar now = Calendar.getInstance();
        return ZoneGetter.getTimeZoneOffsetAndName(mContext,
                now.getTimeZone(), now.getTime());
    }
}
