/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static android.provider.Settings.Global.NOTIFICATION_BUBBLES;

import static com.android.settings.development.BubbleGlobalPreferenceController.OFF;
import static com.android.settings.development.BubbleGlobalPreferenceController.ON;

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
public class BubbleGlobalPreferenceControllerTest {
    private Context mContext;

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private BubbleGlobalPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new BubbleGlobalPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChange_settingEnabled_allowBubbles_shouldBeOn() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        assertThat(isSettingEnabled()).isTrue();
    }

    @Test
    public void onPreferenceChange_settingDisabled_allowBubbles_shouldBeOff() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        assertThat(isSettingEnabled()).isFalse();
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, 1 /* enabled */);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingReset_defaultDisabled_preferenceShouldNotBeChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, 0 /* enabled */);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisable() {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(false);

        assertThat(isSettingEnabled()).isFalse();
    }

    private boolean isSettingEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES,
                OFF /* default off */) == ON;
    }

}
