/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ShowTapsPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;

    private Context mContext;

    private ShowTapsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new ShowTapsPreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void updateState_showTapsEnabled_shouldCheckedPreference() {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SHOW_TOUCHES, ShowTapsPreferenceController.SETTING_VALUE_ON);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_showTapsDisabled_shouldUncheckedPreference() {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SHOW_TOUCHES, ShowTapsPreferenceController.SETTING_VALUE_OFF);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onPreferenceChange_preferenceChecked_shouldEnableShowTaps() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        final int showTapsMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SHOW_TOUCHES, -1 /* default */);

        assertThat(showTapsMode).isEqualTo(ShowTapsPreferenceController.SETTING_VALUE_ON);
    }

    @Test
    public void onPreferenceChange__preferenceUnchecked_shouldDisableShowTaps() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        final int showTapsMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SHOW_TOUCHES, -1 /* default */);

        assertThat(showTapsMode).isEqualTo(ShowTapsPreferenceController.SETTING_VALUE_OFF);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceShouldBeEnabled() {
        mController.onDeveloperOptionsSwitchDisabled();

        final int showTapsMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SHOW_TOUCHES, -1 /* default */);

        assertThat(showTapsMode).isEqualTo(ShowTapsPreferenceController.SETTING_VALUE_OFF);
        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
    }
}
