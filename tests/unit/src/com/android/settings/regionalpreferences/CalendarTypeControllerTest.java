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

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.icu.util.ULocale;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.ResourcesUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

public class CalendarTypeControllerTest {
    private Context mApplicationContext;
    private CalendarTypeController mController;
    private String mCacheProviderContent = "";
    private Locale mCacheLocale;

    @Before
    public void setUp() throws Exception {
        mApplicationContext = ApplicationProvider.getApplicationContext();
        mController = new CalendarTypeController(mApplicationContext, "key");
        mCacheProviderContent = Settings.System.getString(
                mApplicationContext.getContentResolver(), Settings.System.LOCALE_PREFERENCES);
        mCacheLocale = Locale.getDefault(Locale.Category.FORMAT);
    }

    @After
    public void tearDown() throws Exception {
        RegionalPreferenceTestUtils.setSettingsProviderContent(
                mApplicationContext, mCacheProviderContent);
        Locale.setDefault(mCacheLocale);
    }

    @Test
    public void getSummary_hasProviderValue_resultIsChineseCalendar() {
        RegionalPreferenceTestUtils.setSettingsProviderContent(
                mApplicationContext, "und-u-ca-chinese");

        String summary = mController.getSummary().toString();

        assertEquals(getDisplayKeywordValue(LocalePreferences.CalendarType.CHINESE), summary);
    }

    @Test
    public void getSummary_hasProviderValue_resultIsDangiCalendar() {
        RegionalPreferenceTestUtils.setSettingsProviderContent(
                mApplicationContext, "und-u-ca-dangi");

        String summary = mController.getSummary().toString();

        assertEquals(getDisplayKeywordValue(LocalePreferences.CalendarType.DANGI), summary);
    }

    @Test
    public void getSummary_noProviderValueButHasDefaultLocaleWithSubtag_resultIsChineseCalendar() {
        RegionalPreferenceTestUtils.setSettingsProviderContent(mApplicationContext, "");
        Locale.setDefault(Locale.forLanguageTag("en-US-u-ca-chinese"));

        String summary = mController.getSummary().toString();

        assertEquals(getDisplayKeywordValue(LocalePreferences.CalendarType.CHINESE), summary);
    }

    @Test
    public void getSummary_noProviderValueAndDefaultLocaleWithoutSubtag_resultIsEmpty() {
        RegionalPreferenceTestUtils.setSettingsProviderContent(mApplicationContext, "");
        Locale.setDefault(Locale.forLanguageTag("en-US"));

        String summary = mController.getSummary().toString();

        assertEquals(ResourcesUtils.getResourcesString(
                mApplicationContext, "default_string_of_regional_preference"), summary);
    }

    private static String getDisplayKeywordValue(String value) {
        String languageTag = new Locale.Builder()
                .setUnicodeLocaleKeyword(
                        RegionalPreferencesFragment.TYPE_CALENDAR, value).build().toLanguageTag();
        return ULocale.getDisplayKeywordValue(languageTag, "calendar",
                ULocale.forLocale(Locale.getDefault(Locale.Category.FORMAT)));
    }
}
