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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.TextReadingPreferenceFragment.EXTRA_LAUNCHED_FROM;
import static com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint.ACCESSIBILITY_SETTINGS;
import static com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint.UNKNOWN_ENTRY;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link TextReadingFragmentBaseController}.
 */
@RunWith(RobolectricTestRunner.class)
public class TextReadingFragmentBaseControllerTest {
    private static final String FRAGMENT_PREF_KEY = "FRAGMENT_PREF_KEY";
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void handlePreferenceClick_getExtraWithA11ySettingsEntryPoint() {
        final Preference a11ySettingsPreference = new Preference(mContext);
        a11ySettingsPreference.setKey(FRAGMENT_PREF_KEY);
        final TextReadingFragmentBaseController mA11ySettingsFragmentController =
                new TextReadingFragmentBaseController(mContext, FRAGMENT_PREF_KEY,
                        ACCESSIBILITY_SETTINGS);

        mA11ySettingsFragmentController.handlePreferenceTreeClick(a11ySettingsPreference);

        assertThat(a11ySettingsPreference.getExtras().getInt(EXTRA_LAUNCHED_FROM,
                UNKNOWN_ENTRY)).isEqualTo(ACCESSIBILITY_SETTINGS);
    }
}
