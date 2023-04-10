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

package com.android.settings.regionalpreferences;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;

import com.android.settings.core.BasePreferenceController;

import java.util.Locale;

/**
 * A controller for the entry of Calendar types' page
 */
public class CalendarTypeController extends BasePreferenceController {
    private static final String CALENDAR_FEATURE_PROPERTY =
            "i18n-feature-locale-preference-calendar";
    private static final String TAG = CalendarTypeController.class.getSimpleName();
    public CalendarTypeController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * @return {@link AvailabilityStatus} for the Setting. This status is used to determine if the
     * Setting should be shown or disabled in Settings. Further, it can be used to produce
     * appropriate error / warning Slice in the case of unavailability.
     * </p>
     * The status is used for the convenience methods: {@link #isAvailable()}, {@link
     * #isSupported()}
     * </p>
     * The inherited class doesn't need to check work profile if android:forWork="true" is set in
     * preference xml.
     */
    @Override
    public int getAvailabilityStatus() {
        return SystemProperties.getBoolean(CALENDAR_FEATURE_PROPERTY, false)
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        String record = Settings.System.getString(
                mContext.getContentResolver(), Settings.System.LOCALE_PREFERENCES);
        String result = "";
        if (record != null) {
            result = LocalePreferences.getCalendarType(Locale.forLanguageTag(record), false);
        }

        if (result.isEmpty()) {
            result = LocalePreferences.getCalendarType(false);
        }

        String inputStr = result.isEmpty() ? RegionalPreferencesDataUtils.DEFAULT_VALUE : result;
        return RegionalPreferencesDataUtils.calendarConverter(mContext, inputStr);
    }
}
