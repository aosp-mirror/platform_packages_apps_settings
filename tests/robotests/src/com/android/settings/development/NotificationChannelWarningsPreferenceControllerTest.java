package com.android.settings.development;

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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
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
public class NotificationChannelWarningsPreferenceControllerTest {

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private NotificationChannelWarningsPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new NotificationChannelWarningsPreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void onPreferenceChange_settingEnabled_shouldEnableNotificationChannelWarnings() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.SHOW_NOTIFICATION_CHANNEL_WARNINGS, -1 /* default */);

        assertThat(mode).isEqualTo(
                NotificationChannelWarningsPreferenceController.SETTING_VALUE_ON);
    }

    @Test
    public void onPreferenceChange_settingDisabled_shouldDisableNotificationChannelWarnings() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.SHOW_NOTIFICATION_CHANNEL_WARNINGS, -1 /* default */);

        assertThat(mode).isEqualTo(
                NotificationChannelWarningsPreferenceController.SETTING_VALUE_OFF);
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.SHOW_NOTIFICATION_CHANNEL_WARNINGS,
                NotificationChannelWarningsPreferenceController.SETTING_VALUE_ON);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldNotBeChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.SHOW_NOTIFICATION_CHANNEL_WARNINGS,
                NotificationChannelWarningsPreferenceController.SETTING_VALUE_OFF);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void updateState_settingUndefinedDebuggingEnabled_preferenceShouldBeChecked() {
        mController = spy(mController);
        doReturn(true).when(mController).isDebuggable();
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.SHOW_NOTIFICATION_CHANNEL_WARNINGS, "NotAnInteger");

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingUndefinedDebuggingDisabled_preferenceShouldNotBeChecked() {
        mController = spy(mController);
        doReturn(false).when(mController).isDebuggable();
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.SHOW_NOTIFICATION_CHANNEL_WARNINGS, "NotAnInteger");

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceShouldBeDisabled() {
        mController.onDeveloperOptionsSwitchDisabled();
        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.SHOW_NOTIFICATION_CHANNEL_WARNINGS, -1 /* default */);

        assertThat(mode).isEqualTo(
                NotificationChannelWarningsPreferenceController.SETTING_VALUE_OFF);
        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(false);
    }
}
