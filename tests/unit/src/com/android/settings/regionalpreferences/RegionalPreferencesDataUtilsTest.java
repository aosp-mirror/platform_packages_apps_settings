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

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.LocaleList;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.internal.app.LocalePicker;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

public class RegionalPreferencesDataUtilsTest {
    private Context mApplicationContext;
    private String mCacheProviderContent = "";
    private Locale mCacheLocale;
    private LocaleList mCacheLocaleList;

    @Before
    public void setUp() throws Exception {
        mApplicationContext = ApplicationProvider.getApplicationContext();
        mCacheProviderContent = Settings.System.getString(
                mApplicationContext.getContentResolver(), Settings.System.LOCALE_PREFERENCES);
        mCacheLocale = Locale.getDefault(Locale.Category.FORMAT);
        mCacheLocaleList = LocaleList.getDefault();
    }

    @After
    public void tearDown() throws Exception {
        RegionalPreferenceTestUtils.setSettingsProviderContent(
                mApplicationContext, mCacheProviderContent);
        Locale.setDefault(mCacheLocale);
        LocalePicker.updateLocales(mCacheLocaleList);
    }

    @Test
    public void getDefaultUnicodeExtensionData_hasProviderValue_resultIsCelsius() {
        RegionalPreferenceTestUtils.setSettingsProviderContent(
                mApplicationContext, "und-u-mu-celsius");

        String unit = RegionalPreferencesDataUtils.getDefaultUnicodeExtensionData(
                mApplicationContext, RegionalPreferencesFragment.TYPE_TEMPERATURE);

        assertEquals(LocalePreferences.TemperatureUnit.CELSIUS, unit);
    }

    @Test
    public void getDefaultUnicodeExtensionData_hasDefaultLocaleSubtag_resultIsCelsius() {
        RegionalPreferenceTestUtils.setSettingsProviderContent(
                mApplicationContext, "und");
        Locale.setDefault(Locale.forLanguageTag("en-US-u-mu-celsius"));

        String unit = RegionalPreferencesDataUtils.getDefaultUnicodeExtensionData(
                mApplicationContext, RegionalPreferencesFragment.TYPE_TEMPERATURE);

        assertEquals(LocalePreferences.TemperatureUnit.CELSIUS, unit);
    }

    @Test
    public void getDefaultUnicodeExtensionData_noSubtag_resultIsDefault() {
        RegionalPreferenceTestUtils.setSettingsProviderContent(
                mApplicationContext, "und");
        Locale.setDefault(Locale.forLanguageTag("en-US"));

        String unit = RegionalPreferencesDataUtils.getDefaultUnicodeExtensionData(
                mApplicationContext, RegionalPreferencesFragment.TYPE_TEMPERATURE);

        assertEquals(RegionalPreferencesFragment.TYPE_DEFAULT, unit);
    }

    @Test
    public void savePreference_saveCalendarIsDangi_success() {
        RegionalPreferencesDataUtils.savePreference(
                mApplicationContext,
                RegionalPreferencesFragment.TYPE_CALENDAR,
                LocalePreferences.CalendarType.DANGI
        );
        String providerContent = Settings.System.getString(
                mApplicationContext.getContentResolver(), Settings.System.LOCALE_PREFERENCES);
        Locale locale = Locale.forLanguageTag(providerContent);


        String result1 = locale.getUnicodeLocaleType(RegionalPreferencesFragment.TYPE_CALENDAR);

        assertEquals(LocalePreferences.CalendarType.DANGI, result1);

        String result2 = Locale.getDefault(Locale.Category.FORMAT)
                .getUnicodeLocaleType(RegionalPreferencesFragment.TYPE_CALENDAR);

        assertEquals(LocalePreferences.CalendarType.DANGI, result2);

    }

    @Test
    public void temperatureUnitsConverter_inputFahrenheit_resultIsFahrenheitString() {
        String result = RegionalPreferencesDataUtils.temperatureUnitsConverter(mApplicationContext,
                LocalePreferences.TemperatureUnit.FAHRENHEIT);

        assertEquals(ResourcesUtils.getResourcesString(
                mApplicationContext, "fahrenheit_temperature_unit"), result);
    }

    @Test
    public void temperatureUnitsConverter_inputDefault_resultIsDefaultString() {
        String result = RegionalPreferencesDataUtils.temperatureUnitsConverter(mApplicationContext,
                RegionalPreferencesFragment.TYPE_DEFAULT);

        assertEquals(ResourcesUtils.getResourcesString(
                mApplicationContext, "default_string_of_regional_preference"), result);
    }

    @Test
    public void dayConverter_inputWed_resultIsWedString() {
        String result = RegionalPreferencesDataUtils.dayConverter(mApplicationContext,
                LocalePreferences.FirstDayOfWeek.WEDNESDAY);

        assertEquals(ResourcesUtils.getResourcesString(
                mApplicationContext, "wednesday_first_day_of_week"), result);
    }

    @Test
    public void dayConverter_inputDefault_resultIsDefaultString() {
        String result = RegionalPreferencesDataUtils.dayConverter(mApplicationContext,
                RegionalPreferencesFragment.TYPE_DEFAULT);

        assertEquals(ResourcesUtils.getResourcesString(
                mApplicationContext, "default_string_of_regional_preference"), result);
    }

    @Test
    public void calendarConverter_inputDefault_resultIsDefaultString() {
        String result = RegionalPreferencesDataUtils.dayConverter(mApplicationContext,
                RegionalPreferencesFragment.TYPE_DEFAULT);

        assertEquals(ResourcesUtils.getResourcesString(
                mApplicationContext, "default_string_of_regional_preference"), result);
    }
}
