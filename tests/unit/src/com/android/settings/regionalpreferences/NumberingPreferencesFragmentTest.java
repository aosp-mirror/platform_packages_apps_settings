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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

public class NumberingPreferencesFragmentTest {
    private Context mApplicationContext;
    private NumberingPreferencesFragment mFragment;

    @Before
    @UiThreadTest
    public void setUp() throws Exception {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mApplicationContext = ApplicationProvider.getApplicationContext();
        mFragment = new NumberingPreferencesFragment();
    }

    @Test
    @UiThreadTest
    public void initTitle_optionIsLanguageSelection_titleIsNumbers() {
        Bundle bundle = new Bundle();
        bundle.putString(RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE,
                NumberingSystemItemController.ARG_VALUE_LANGUAGE_SELECT);
        mFragment.setArguments(bundle);

        String result = mFragment.initTitle();

        assertEquals(ResourcesUtils.getResourcesString(
                mApplicationContext, "numbers_preferences_title"), result);
    }

    @Test
    @UiThreadTest
    public void initTitle_optionIsNumberingSystemSelection_titleIsLocaleDisplayName() {
        Locale expectedLocale = Locale.forLanguageTag("en-US");
        Bundle bundle = new Bundle();
        bundle.putString(RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE,
                NumberingSystemItemController.ARG_VALUE_NUMBERING_SYSTEM_SELECT);
        bundle.putString(
                NumberingSystemItemController.KEY_SELECTED_LANGUAGE,
                expectedLocale.toLanguageTag());
        mFragment.setArguments(bundle);

        String result = mFragment.initTitle();

        assertEquals(expectedLocale.getDisplayName(expectedLocale), result);
    }

    @Test
    @UiThreadTest
    public void getMetricsCategory_optionIsLanguageSelection_resultIsLanguageSelection() {
        Bundle extras = new Bundle();
        extras.putString(RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE,
                NumberingSystemItemController.ARG_VALUE_LANGUAGE_SELECT);
        mFragment.setArguments(extras);

        int result = mFragment.getMetricsCategory();

        assertEquals(SettingsEnums.NUMBERING_SYSTEM_LANGUAGE_SELECTION_PREFERENCE, result);
    }

    @Test
    @UiThreadTest
    public void getMetricsCategory_optionIsNumberSelection_resultIsNumberSelection() {
        Bundle extras = new Bundle();
        extras.putString(RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE,
                NumberingSystemItemController.ARG_VALUE_NUMBERING_SYSTEM_SELECT);
        mFragment.setArguments(extras);

        int result = mFragment.getMetricsCategory();

        assertEquals(SettingsEnums.NUMBERING_SYSTEM_NUMBER_FORMAT_SELECTION_PREFERENCE, result);
    }
}
