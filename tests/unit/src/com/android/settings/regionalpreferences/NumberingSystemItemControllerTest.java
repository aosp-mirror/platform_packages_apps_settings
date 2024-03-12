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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.Looper;
import android.util.AndroidRuntimeException;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.app.LocalePicker;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.TickButtonPreference;

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
    private FakeFeatureFactory mFeatureFactory;

    @Before
    @UiThreadTest
    public void setUp() throws Exception {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mApplicationContext = ApplicationProvider.getApplicationContext();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mFragment = spy(new NumberingPreferencesFragment());
        PreferenceManager preferenceManager = new PreferenceManager(mApplicationContext);
        mPreferenceScreen = preferenceManager.createPreferenceScreen(mApplicationContext);
        mCacheLocale = LocaleList.getDefault();
    }

    @After
    public void tearDown() {
        LocaleList.setDefault(mCacheLocale);
        LocalePicker.updateLocales(mCacheLocale);
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
        verify(mFeatureFactory.metricsFeatureProvider).action(
                mApplicationContext,
                SettingsEnums.ACTION_CHOOSE_LANGUAGE_FOR_NUMBERS_PREFERENCES);
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
        verify(mFeatureFactory.metricsFeatureProvider).action(
                mApplicationContext, SettingsEnums.ACTION_SET_NUMBERS_PREFERENCES);
    }

    @Test
    @UiThreadTest
    public void handlePreferenceTreeClick_numbersSelect_numberingSystemIsUpdated() {
        LocalePicker.updateLocales(LocaleList.forLanguageTags("en-US,zh-TW,ar-BH"));
        Bundle bundle = new Bundle();
        bundle.putString(RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE,
                NumberingSystemItemController.ARG_VALUE_NUMBERING_SYSTEM_SELECT);
        bundle.putString(
                NumberingSystemItemController.KEY_SELECTED_LANGUAGE, "ar-BH");
        TickButtonPreference defaultPreference = new TickButtonPreference(mApplicationContext);
        TickButtonPreference numberPreference = new TickButtonPreference(mApplicationContext);
        defaultPreference.setKey("default");
        numberPreference.setKey("latn");
        mPreferenceScreen.addPreference(defaultPreference);
        mPreferenceScreen.addPreference(numberPreference);
        mController = new NumberingSystemItemController(mApplicationContext, bundle);
        mController.setParentFragment(mFragment);
        mController.displayPreference(mPreferenceScreen);

        mController.handlePreferenceTreeClick(numberPreference);

        assertThat(LocalePicker.getLocales().toLanguageTags()).contains(
                "en-US,zh-TW,ar-BH-u-nu-latn");
    }

    @Test
    @UiThreadTest
    public void displayPreference_languageOptAndHas2LocaleWithSingleNu_showNothing() {
        LocaleList.setDefault(LocaleList.forLanguageTags("en-US,zh-TW"));
        Bundle bundle = new Bundle();
        bundle.putString(RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE,
                NumberingSystemItemController.ARG_VALUE_LANGUAGE_SELECT);
        bundle.putString(
                NumberingSystemItemController.KEY_SELECTED_LANGUAGE, Locale.US.toLanguageTag());
        mController = new NumberingSystemItemController(mApplicationContext, bundle);
        mController.setParentFragment(mFragment);

        mController.displayPreference(mPreferenceScreen);

        assertEquals(0, mPreferenceScreen.getPreferenceCount());
    }

    @Test
    @UiThreadTest
    public void displayPreference_languageOptAndHas2LocaleWithMultiNu_showLocaleWithMultiNuOnly() {
        // ar-JO and dz-BT have multiple numbering systems.
        LocaleList.setDefault(LocaleList.forLanguageTags("en-US,zh-TW,ar-JO,dz-BT"));
        Bundle bundle = new Bundle();
        bundle.putString(RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE,
                NumberingSystemItemController.ARG_VALUE_LANGUAGE_SELECT);
        bundle.putString(
                NumberingSystemItemController.KEY_SELECTED_LANGUAGE, Locale.US.toLanguageTag());
        mController = new NumberingSystemItemController(mApplicationContext, bundle);
        mController.setParentFragment(mFragment);

        mController.displayPreference(mPreferenceScreen);

        assertEquals(2, mPreferenceScreen.getPreferenceCount());
    }

    @Test
    @UiThreadTest
    public void displayPreference_enUsNumbersOpt_show1Option() {
        LocaleList.setDefault(LocaleList.forLanguageTags("en-US,zh-TW"));
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
