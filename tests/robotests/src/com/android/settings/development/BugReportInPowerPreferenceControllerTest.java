/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static com.android.settings.development.BugReportInPowerPreferenceController
        .SETTING_VALUE_OFF;
import static com.android.settings.development.BugReportInPowerPreferenceController
        .SETTING_VALUE_ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.os.UserManager;
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
public class BugReportInPowerPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private UserManager mUserManager;
    @Mock
    private Context mContext;
    @Mock
    private SwitchPreference mPreference;

    private ContentResolver mContentResolver;
    private BugReportInPowerPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mController = new BugReportInPowerPreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void isAvailable_hasDebugRestriction_shouldReturnFalse() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_noDebugRestriction_shouldReturnTrue() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceShouldNotBeChecked() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        mController.displayPreference(mScreen);

        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onPreferenceChanged_settingDisabled_shouldDisableBugReportInPowerSetting() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        mController.displayPreference(mScreen);

        mController.onPreferenceChange(mPreference, false /* new value */);
        int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Global.BUGREPORT_IN_POWER_MENU, -1 /* default */);

        assertThat(mode).isEqualTo(SETTING_VALUE_OFF);
    }

    @Test
    public void onPreferenceChanged_settingEnabled_shouldEnableBugReportInPowerSetting() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        mController.displayPreference(mScreen);

        mController.onPreferenceChange(mPreference, true /* new value */);
        int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Global.BUGREPORT_IN_POWER_MENU, -1 /* default */);

        assertThat(mode).isEqualTo(SETTING_VALUE_ON);
    }


    @Test
    public void updateState_settingsOn_preferenceShouldBeChecked() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Global.BUGREPORT_IN_POWER_MENU, SETTING_VALUE_ON);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingsOff_preferenceShouldNotBeChecked() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Global.BUGREPORT_IN_POWER_MENU, SETTING_VALUE_OFF);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldTurnOffPreference() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        mController.displayPreference(mScreen);

        mController.onDeveloperOptionsSwitchDisabled();
        int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Global.BUGREPORT_IN_POWER_MENU, -1 /* default */);

        assertThat(mode).isEqualTo(SETTING_VALUE_OFF);
        verify(mPreference).setChecked(false);
    }
}
