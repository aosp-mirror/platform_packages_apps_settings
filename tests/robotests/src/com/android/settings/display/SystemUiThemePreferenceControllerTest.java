/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.display;

import static android.provider.Settings.Secure.THEME_MODE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class SystemUiThemePreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private ListPreference mListPreference;
    private Context mContext;
    private SystemUiThemePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mListPreference);
        CharSequence[] entries = mContext.getResources().getStringArray(
                R.array.systemui_theme_entries);
        when(mListPreference.getEntries()).thenReturn(entries);
        mController = spy(new SystemUiThemePreferenceController(mContext, "systemui_theme"));
    }

    @Test
    public void displayPreference_readsSetting() {
        Settings.Secure.putInt(mContext.getContentResolver(), THEME_MODE, 2);
        mController.displayPreference(mPreferenceScreen);
        verify(mListPreference).setValue(eq("2"));
    }

    @Test
    public void onPreferenceChange_writesSetting() {
        Settings.Secure.putInt(mContext.getContentResolver(), THEME_MODE, 2);
        mController.displayPreference(mPreferenceScreen);
        mController.onPreferenceChange(mListPreference, "0");
        int value = Settings.Secure.getInt(mContext.getContentResolver(), THEME_MODE, 2);
        assertThat(value).isEqualTo(0);
    }

    @Test
    public void onPreferenceChange_updatesSummary() {
        mController.displayPreference(mPreferenceScreen);
        mController.onPreferenceChange(mListPreference, "0");
        verify(mController).getSummary();
    }

}