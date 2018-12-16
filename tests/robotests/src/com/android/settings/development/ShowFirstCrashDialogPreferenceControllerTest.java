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
 * limitations under the License.
 */

package com.android.settings.development;

import static com.android.settings.development.ShowFirstCrashDialogPreferenceController
        .SETTING_VALUE_OFF;
import static com.android.settings.development.ShowFirstCrashDialogPreferenceController
        .SETTING_VALUE_ON;

import static com.google.common.truth.Truth.assertThat;

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
public class ShowFirstCrashDialogPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Context mContext;
    private SwitchPreference mPreference;
    private ShowFirstCrashDialogPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mController = new ShowFirstCrashDialogPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChange_settingEnabled_showFirstCrashDialogShouldBeOn() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        final int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SHOW_FIRST_CRASH_DIALOG_DEV_OPTION, -1 /* default */);

        assertThat(mode).isEqualTo(SETTING_VALUE_ON);
    }

    @Test
    public void onPreferenceChange_settingDisabled_showFirstCrashDialogShouldBeOff() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        final int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SHOW_FIRST_CRASH_DIALOG_DEV_OPTION, -1 /* default */);

        assertThat(mode).isEqualTo(SETTING_VALUE_OFF);
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldNotBeChecked() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SHOW_FIRST_CRASH_DIALOG_DEV_OPTION, SETTING_VALUE_OFF);
        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SHOW_FIRST_CRASH_DIALOG_DEV_OPTION, SETTING_VALUE_ON);
        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        final int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SHOW_FIRST_CRASH_DIALOG_DEV_OPTION, -1 /* default */);

        assertThat(mode).isEqualTo(SETTING_VALUE_OFF);
        assertThat(mPreference.isChecked()).isFalse();
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void onShowFirstCrashDialogGlobalOff_shouldEnablePreference() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.SHOW_FIRST_CRASH_DIALOG, SETTING_VALUE_OFF);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mController.isAvailable()).isTrue();
        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void onShowFirstCrashDialogGlobalOn_shouldDisablePreference() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.SHOW_FIRST_CRASH_DIALOG, SETTING_VALUE_ON);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mController.isAvailable()).isFalse();
        assertThat(mPreference.isVisible()).isFalse();
    }
}
