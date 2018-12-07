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

import static com.android.settings.development.LocalTerminalPreferenceController
        .TERMINAL_APP_PACKAGE;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LocalTerminalPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PackageManager mPackageManager;

    private LocalTerminalPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mUserManager.isAdminUser()).thenReturn(true);
        mController = spy(new LocalTerminalPreferenceController(mContext));
        doReturn(true).when(mController).isAvailable();
        doReturn(mPackageManager).when(mController).getPackageManager();
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void displayPreference_shouldDisablePreferenceWhenNotAdmin() {
        when(mUserManager.isAdminUser()).thenReturn(false);
        mController.displayPreference(mPreferenceScreen);

        verify(mPreference).setEnabled(false);
    }

    @Test
    public void onPreferenceChanged_turnOnTerminal() {
        mController.onPreferenceChange(null, true);

        verify(mPackageManager).setApplicationEnabledSetting(TERMINAL_APP_PACKAGE,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);
    }

    @Test
    public void onPreferenceChanged_turnOffTerminal() {
        mController.onPreferenceChange(null, false);

        verify(mPackageManager).setApplicationEnabledSetting(TERMINAL_APP_PACKAGE,
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0);
    }

    @Test
    public void updateState_preferenceShouldBeChecked() {
        when(mPackageManager.getApplicationEnabledSetting(TERMINAL_APP_PACKAGE)).thenReturn(
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_preferenceShouldNotBeChecked() {
        when(mPackageManager.getApplicationEnabledSetting(TERMINAL_APP_PACKAGE)).thenReturn(
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPackageManager).setApplicationEnabledSetting(TERMINAL_APP_PACKAGE,
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0);
        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(false);
    }

    @Test
    public void onDeveloperOptionsSwitchEnabled_shouldDoNothingWhenNotAdmin() {
        when(mUserManager.isAdminUser()).thenReturn(false);
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference, never()).setEnabled(true);
    }

    @Test
    public void onDeveloperOptionsSwitchEnabled_preferenceShouldBeEnabledWhenAdmin() {
        mController.onDeveloperOptionsSwitchEnabled();

        verify(mPreference).setEnabled(true);
    }
}
