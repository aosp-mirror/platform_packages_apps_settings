/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.icu.util.ULocale;
import android.os.LocaleList;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.internal.app.LocalePicker;
import com.android.settings.R;

import java.util.Locale;

/** Provides utils for regional preferences. */
public class RegionalPreferencesDataUtils {
    static final String DISPLAY_KEYWORD_OF_CALENDAR = "calendar";
    static final String DEFAULT_VALUE = "default";

    static String getDefaultUnicodeExtensionData(Context contxt, String type) {
        // 1. Check cache data in Settings provider.
        String record = Settings.System.getString(
                contxt.getContentResolver(), Settings.System.LOCALE_PREFERENCES);
        String result = "";

        if (!TextUtils.isEmpty(record)) {
            result = Locale.forLanguageTag(record).getUnicodeLocaleType(type);
        }
        // 2. Check cache data in default Locale(ICU lib).
        if (TextUtils.isEmpty(result)) {
            result = Locale.getDefault(Locale.Category.FORMAT).getUnicodeLocaleType(type);
        }

        return result == null ? DEFAULT_VALUE : result;
    }

    static void savePreference(Context context, String type, String value) {
        saveToSettingsProvider(context, type, value);
        saveToSystem(type, value);
    }

    private static void saveToSettingsProvider(Context context, String type, String value) {
        String record = Settings.System.getString(
                context.getContentResolver(), Settings.System.LOCALE_PREFERENCES);

        record = record == null ? "" : record;

        Settings.System.putString(
                context.getContentResolver(),
                Settings.System.LOCALE_PREFERENCES,
                addUnicodeKeywordToLocale(record, type, value).toLanguageTag());
    }

    private static void saveToSystem(String type, String value) {
        LocaleList localeList = LocaleList.getDefault();
        Locale[] resultLocales = new Locale[localeList.size()];
        for (int i = 0; i < localeList.size(); i++) {
            resultLocales[i] = addUnicodeKeywordToLocale(localeList.get(i), type, value);
        }
        LocalePicker.updateLocales(new LocaleList(resultLocales));
    }

    private static Locale addUnicodeKeywordToLocale(Locale locale, String type, String value) {
        return new Locale.Builder()
                .setLocale(locale)
                .setUnicodeLocaleKeyword(type, value)
                .build();
    }

    private static Locale addUnicodeKeywordToLocale(String languageTag, String type, String value) {
        return addUnicodeKeywordToLocale(Locale.forLanguageTag(languageTag), type, value);
    }

    static String calendarConverter(Context context, String calendarType) {
        if (calendarType.equals(DEFAULT_VALUE)) {
            return context.getString(R.string.default_string_of_regional_preference);
        }

        Locale locale = new Locale.Builder()
                .setUnicodeLocaleKeyword(ExtensionTypes.CALENDAR, calendarType)
                .build();
        return ULocale.getDisplayKeywordValue(locale.toLanguageTag(), DISPLAY_KEYWORD_OF_CALENDAR,
                ULocale.forLocale(Locale.getDefault(Locale.Category.FORMAT)));
    }

    static String temperatureUnitsConverter(Context context, String unit) {
        switch (unit) {
            case LocalePreferences.TemperatureUnit.CELSIUS:
                return context.getString(R.string.celsius_temperature_unit);
            case LocalePreferences.TemperatureUnit.FAHRENHEIT:
                return context.getString(R.string.fahrenheit_temperature_unit);
            default:
                return context.getString(R.string.default_string_of_regional_preference);
        }
    }

    static String dayConverter(Context context, String day) {
        switch (day) {
            case LocalePreferences.FirstDayOfWeek.MONDAY:
                return context.getString(R.string.monday_first_day_of_week);
            case LocalePreferences.FirstDayOfWeek.TUESDAY:
                return context.getString(R.string.tuesday_first_day_of_week);
            case LocalePreferences.FirstDayOfWeek.WEDNESDAY:
                return context.getString(R.string.wednesday_first_day_of_week);
            case LocalePreferences.FirstDayOfWeek.THURSDAY:
                return context.getString(R.string.thursday_first_day_of_week);
            case LocalePreferences.FirstDayOfWeek.FRIDAY:
                return context.getString(R.string.friday_first_day_of_week);
            case LocalePreferences.FirstDayOfWeek.SATURDAY:
                return context.getString(R.string.saturday_first_day_of_week);
            case LocalePreferences.FirstDayOfWeek.SUNDAY:
                return context.getString(R.string.sunday_first_day_of_week);
            default:
                return context.getString(R.string.default_string_of_regional_preference);
        }
    }
}
