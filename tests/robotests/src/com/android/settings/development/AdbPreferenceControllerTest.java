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
public class AdbPreferenceControllerTest {

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;

    private Context mContext;
    private AdbPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new AdbPreferenceController(mContext, mFragment));
        doReturn(true).when(mController).isAvailable();
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsDisabled();
        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ADB_ENABLED, -1);

        assertThat(mode).isEqualTo(AdbPreferenceController.ADB_SETTING_OFF);
        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
    }

    @Test
    public void onAdbDialogConfirmed_shouldEnableAdbSetting() {
        mController.onAdbDialogConfirmed();
        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ADB_ENABLED, -1);

        assertThat(mode).isEqualTo(AdbPreferenceController.ADB_SETTING_ON);
    }

    @Test
    public void onAdbDialogDismissed_preferenceShouldNotBeChecked() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED,
                AdbPreferenceController.ADB_SETTING_OFF);
        mController.onAdbDialogDismissed();

        verify(mPreference).setChecked(false);
    }
}
