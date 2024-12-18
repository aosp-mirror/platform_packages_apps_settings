/**
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.regionalpreferences;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.LocaleList;
import android.os.Looper;
import android.provider.Settings;

import com.android.internal.app.LocalePicker;
import com.android.settings.widget.TickButtonPreference;

import androidx.preference.PreferenceManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class FirstDayOfWeekItemListControllerTest {

    private static final String KEY_PREFERENCE_CATEGORY_FIRST_DAY_OF_WEEK_ITEM =
            "first_day_of_week_item_category";
    private static final String KEY_PREFERENCE_FIRST_DAY_OF_WEEK_ITEM =
            "first_day_of_week_item_list";

    private Context mContext;
    private PreferenceManager mPreferenceManager;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;
    private FirstDayOfWeekItemListController mController;
    private LocaleList mCacheLocaleList;
    private Locale mCacheLocale;
    private String mCacheProviderContent = "";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreferenceCategory = new PreferenceCategory(mContext);
        mPreferenceCategory.setKey(KEY_PREFERENCE_CATEGORY_FIRST_DAY_OF_WEEK_ITEM);
        mPreferenceScreen.addPreference(mPreferenceCategory);
        mController = new FirstDayOfWeekItemListController(mContext,
                KEY_PREFERENCE_FIRST_DAY_OF_WEEK_ITEM);
        mController.displayPreference(mPreferenceScreen);
        mCacheProviderContent = Settings.System.getString(
                mContext.getContentResolver(), Settings.System.LOCALE_PREFERENCES);
        mCacheLocale = Locale.getDefault(Locale.Category.FORMAT);
        mCacheLocaleList = LocaleList.getDefault();
    }

    @After
    public void tearDown() throws Exception {
        RegionalPreferenceTestUtils.setSettingsProviderContent(
                mContext, mCacheProviderContent);
        Locale.setDefault(mCacheLocale);
        LocalePicker.updateLocales(mCacheLocaleList);
    }

    @Test
    public void displayPreference_setSelectPreferredFirstDayOfWeekIsDefault() {
        TickButtonPreference pref = (TickButtonPreference) mPreferenceCategory.getPreference(0);
        pref.performClick();
        String record = Settings.System.getString(
                mContext.getContentResolver(), Settings.System.LOCALE_PREFERENCES);

        assertThat(pref.getKey()).isEqualTo("default");
        assertThat(
                RegionalPreferencesDataUtils.dayConverter(mContext, record)).contains("default");
    }

    @Test
    public void displayPreference_setSelectPreferredFirstDayOfWeekIsSunday() {
        TickButtonPreference pref = (TickButtonPreference) mPreferenceCategory.getPreference(1);
        pref.performClick();
        String record = Settings.System.getString(
                mContext.getContentResolver(), Settings.System.LOCALE_PREFERENCES);

        assertThat(pref.getKey()).isEqualTo("sun");
        assertThat(record).contains("sun");
    }

    @Test
    public void displayPreference_setSelectPreferredFirstDayOfWeekIsMonday() {
        TickButtonPreference pref = (TickButtonPreference) mPreferenceCategory.getPreference(2);
        pref.performClick();
        String record = Settings.System.getString(
                mContext.getContentResolver(), Settings.System.LOCALE_PREFERENCES);

        assertThat(pref.getKey()).isEqualTo("mon");
        assertThat(record).contains("mon");
    }
}
