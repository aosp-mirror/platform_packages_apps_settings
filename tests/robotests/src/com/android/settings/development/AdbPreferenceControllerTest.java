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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AdbPreferenceControllerTest {
    @Mock
    private Context mContext;
    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private UserManager mUserManager;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;

    private ContentResolver mContentResolver;
    private AdbPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        mController = spy(new AdbPreferenceController(mContext, mFragment));
        doNothing().when(mController).notifyStateChanged();
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void isAvailable_notAdmin_shouldBeFalse() {
        when(mUserManager.isAdminUser()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_isAdmin_shouldBeTrue() {
        when(mUserManager.isAdminUser()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onPreferenceChanged_settingDisabled_shouldTurnOffAdb() {
        when(mContext.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        mController.onPreferenceChange(null, false);

        final int mode = Settings.System.getInt(mContentResolver,
                Settings.Global.ADB_ENABLED, -1);

        assertThat(mode).isEqualTo(AdbPreferenceController.ADB_SETTING_OFF);
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        Settings.System.putInt(mContentResolver, Settings.Global.ADB_ENABLED,
                AdbPreferenceController.ADB_SETTING_ON);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldNotBeChecked() {
        Settings.System.putInt(mContentResolver, Settings.Global.ADB_ENABLED,
                AdbPreferenceController.ADB_SETTING_OFF);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldDisablePreference() {
        when(mContext.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        when(mUserManager.isAdminUser()).thenReturn(true);
        mController.onDeveloperOptionsDisabled();
        final int mode = Settings.System.getInt(mContentResolver,
                Settings.Global.ADB_ENABLED, -1);

        assertThat(mode).isEqualTo(AdbPreferenceController.ADB_SETTING_OFF);
        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsEnabled_shouldEnablePreference() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        mController.onDeveloperOptionsEnabled();

        verify(mPreference).setEnabled(true);
    }

    @Test
    public void onAdbDialogConfirmed_shouldEnableAdbSetting() {
        mController.onAdbDialogConfirmed();
        final int mode = Settings.System.getInt(mContentResolver,
                Settings.Global.ADB_ENABLED, -1);

        assertThat(mode).isEqualTo(AdbPreferenceController.ADB_SETTING_ON);
    }

    @Test
    public void onAdbDialogDismissed_preferenceShouldNotBeChecked() {
        Settings.System.putInt(mContentResolver, Settings.Global.ADB_ENABLED,
                AdbPreferenceController.ADB_SETTING_OFF);
        mController.onAdbDialogDismissed();

        verify(mPreference).setChecked(false);
    }
}
