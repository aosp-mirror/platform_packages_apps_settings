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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.Looper;
import android.util.AndroidRuntimeException;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

public class NumberingSystemItemControllerTest {
    private Context mApplicationContext;
    private NumberingSystemItemController mController;
    private NumberingPreferencesFragment mFragment;
    private PreferenceScreen mPreferenceScreen;
    private LocaleList mCacheLocale;

    @Before
    @UiThreadTest
    public void setUp() throws Exception {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mApplicationContext = ApplicationProvider.getApplicationContext();
        mFragment = spy(new NumberingPreferencesFragment());
        PreferenceManager preferenceManager = new PreferenceManager(mApplicationContext);
        mPreferenceScreen = preferenceManager.createPreferenceScreen(mApplicationContext);
        mCacheLocale = LocaleList.getDefault();
    }

    @After
    public void tearDown() {
        LocaleList.setDefault(mCacheLocale);
    }

    @Test
    @UiThreadTest
    public void handlePreferenceTreeClick_languageSelect_launchFragment() {
        Bundle bundle = new Bundle();
        bundle.putString(RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE,
                NumberingSystemItemController.ARG_VALUE_LANGUAGE_SELECT);
        bundle.putString(
                NumberingSystemItemController.KEY_SELECTED_LANGUAGE, Locale.US.toLanguageTag());
        TickButtonPreference preference = new TickButtonPreference(mApplicationContext);
        preference.setKey("I_am_the_key");
        mPreferenceScreen.addPreference(preference);
        mController = new NumberingSystemItemController(mApplicationContext, bundle);
        mController.setParentFragment(mFragment);
        mController.displayPreference(mPreferenceScreen);

        boolean isCallingStartActivity = false;
        try {
            mController.handlePreferenceTreeClick(preference);
        } catch (AndroidRuntimeException exception) {
            isCallingStartActivity = true;
        }

        assertTrue(isCallingStartActivity);
    }

    @Test
    @UiThreadTest
    public void handlePreferenceTreeClick_numbersSelect_preferenceHasTick() {
        Bundle bundle = new Bundle();
        bundle.putString(RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE,
                NumberingSystemItemController.ARG_VALUE_NUMBERING_SYSTEM_SELECT);
        bundle.putString(
                NumberingSystemItemController.KEY_SELECTED_LANGUAGE, Locale.US.toLanguageTag());
        TickButtonPreference preference = new TickButtonPreference(mApplicationContext);
        preference.setKey("test_key");
        mPreferenceScreen.addPreference(preference);
        mController = new NumberingSystemItemController(mApplicationContext, bundle);
        mController.setParentFragment(mFragment);
        mController.displayPreference(mPreferenceScreen);

        mController.handlePreferenceTreeClick(preference);

        verify(mFragment).setArguments(any());
    }

    @Test
    @UiThreadTest
    public void displayPreference_languageOptAndHas2Locale_show2Options() {
        LocaleList.setDefault(LocaleList.forLanguageTags("en-US, zh-TW"));
        Bundle bundle = new Bundle();
        bundle.putString(RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE,
                NumberingSystemItemController.ARG_VALUE_LANGUAGE_SELECT);
        bundle.putString(
                NumberingSystemItemController.KEY_SELECTED_LANGUAGE, Locale.US.toLanguageTag());
        mController = new NumberingSystemItemController(mApplicationContext, bundle);
        mController.setParentFragment(mFragment);

        mController.displayPreference(mPreferenceScreen);

        assertEquals(LocaleList.getDefault().size(), mPreferenceScreen.getPreferenceCount());
    }

    @Test
    @UiThreadTest
    public void displayPreference_enUsNumbersOpt_show1Option() {
        LocaleList.setDefault(LocaleList.forLanguageTags("en-US, zh-TW"));
        Bundle bundle = new Bundle();
        bundle.putString(RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE,
                NumberingSystemItemController.ARG_VALUE_NUMBERING_SYSTEM_SELECT);
        bundle.putString(
                NumberingSystemItemController.KEY_SELECTED_LANGUAGE, Locale.US.toLanguageTag());
        mController = new NumberingSystemItemController(mApplicationContext, bundle);
        mController.setParentFragment(mFragment);

        mController.displayPreference(mPreferenceScreen);

        // en-US only has 1 numbering system.
        assertEquals(1, mPreferenceScreen.getPreferenceCount());
    }
}
