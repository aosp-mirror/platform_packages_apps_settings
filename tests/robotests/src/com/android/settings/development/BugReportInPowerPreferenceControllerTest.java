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

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.os.UserManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BugReportInPowerPreferenceControllerTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private SwitchPreference mPreference;
    private BugReportInPowerPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowContext = ShadowApplication.getInstance();
        shadowContext.setSystemService(Context.USER_SERVICE, mUserManager);
        mContext = spy(shadowContext.getApplicationContext());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mPreference = new SwitchPreference(mContext);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        mController = new BugReportInPowerPreferenceController(mContext);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void displayPreference_hasDebugRestriction_shouldRemovePreference() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(true);
        when(mScreen.getPreferenceCount()).thenReturn(1);
        when(mScreen.getPreference(0)).thenReturn(mPreference);

        mController.displayPreference(mScreen);

        verify(mScreen).removePreference(any(Preference.class));
    }

    @Test
    public void displayPreference_noDebugRestriction_shouldNotRemovePreference() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);

        mController.displayPreference(mScreen);

        verify(mScreen, never()).removePreference(any(Preference.class));
    }

    @Test
    public void enablePreference_hasDebugRestriction_shouldNotEnable() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(true);
        mController.displayPreference(mScreen);
        mPreference.setEnabled(false);

        mController.enablePreference(true);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void enablePreference_noDebugRestriction_shouldEnable() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        mController.displayPreference(mScreen);
        mPreference.setEnabled(false);

        mController.enablePreference(true);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void resetPreference_shouldUncheck() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        mController.displayPreference(mScreen);
        mPreference.setChecked(true);

        mController.resetPreference();

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_shouldUpdateSettings() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        Settings.Secure.putInt(mContext.getContentResolver(),
            Settings.Global.BUGREPORT_IN_POWER_MENU, 0);
        mPreference.setChecked(true);
        mController.displayPreference(mScreen);

        mController.handlePreferenceTreeClick(mPreference);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
            Settings.Global.BUGREPORT_IN_POWER_MENU, 0)).isEqualTo(1);
    }

    @Test
    public void updateState_settingsOn_shouldCheck() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        Settings.Secure.putInt(mContext.getContentResolver(),
            Settings.Global.BUGREPORT_IN_POWER_MENU, 1);
        mPreference.setChecked(false);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_settingsOff_shouldUncheck() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        Settings.Secure.putInt(mContext.getContentResolver(),
            Settings.Global.BUGREPORT_IN_POWER_MENU, 0);
        mPreference.setChecked(true);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updateBugreportOptions_shouldEnable() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        mPreference.setEnabled(false);
        mController.displayPreference(mScreen);

        mController.updateBugreportOptions();

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateBugreportOptions_shouldEnableBugReportStorage() {
        final ComponentName componentName = new ComponentName("com.android.shell",
            "com.android.shell.BugreportStorageProvider");
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        mController.displayPreference(mScreen);

        mController.updateBugreportOptions();

        verify(mPackageManager).setComponentEnabledSetting(eq(componentName), anyInt(), anyInt());
    }
}
