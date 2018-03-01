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

import static com.android.settings.development.ConnectivityMonitorPreferenceController.ENG_BUILD;
import static com.android.settings.development.ConnectivityMonitorPreferenceController
        .USERDEBUG_BUILD;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION, shadows =
        SettingsShadowSystemProperties.class)
public class ConnectivityMonitorPreferenceControllerTest {

    private static final String USER_BUILD = "user";

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;

    private Context mContext;
    private ConnectivityMonitorPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        SettingsShadowSystemProperties.clear();
        mContext = RuntimeEnvironment.application;
        mController = new ConnectivityMonitorPreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void isAvailable_trueShowFlagWithUserdebugBuild_shouldReturnTrue() {
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, USERDEBUG_BUILD);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_trueShowFlagWithEngBuild_shouldReturnTrue() {
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, ENG_BUILD);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_trueShowFlagWithUserBuild_shouldReturnFalse() {
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, USER_BUILD);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_falseShowFlagWithUserdebugBuild_shouldReturnFalse() {
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, USERDEBUG_BUILD);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_falseShowFlagWithEngBuild_shouldReturnFalse() {
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, ENG_BUILD);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_falseShowFlagWithUserBuild_shouldReturnFalse() {
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, USER_BUILD);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateState_connectivityMonitorEnabled_shouldCheckedPreference() {
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.PROPERTY_CONNECTIVITY_MONITOR,
                ConnectivityMonitorPreferenceController.ENABLED_STATUS);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, USERDEBUG_BUILD);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_connectivityMonitorUserEnabled_shouldCheckedPreference() {
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.PROPERTY_CONNECTIVITY_MONITOR,
                ConnectivityMonitorPreferenceController.USER_ENABLED_STATUS);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, USERDEBUG_BUILD);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_connectivityMonitorDisabled_shouldUncheckedPreference() {
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.PROPERTY_CONNECTIVITY_MONITOR,
                ConnectivityMonitorPreferenceController.DISABLED_STATUS);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, USERDEBUG_BUILD);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void updateState_connectivityMonitorUserDisabled_shouldUncheckedPreference() {
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.PROPERTY_CONNECTIVITY_MONITOR,
                ConnectivityMonitorPreferenceController.USER_DISABLED_STATUS);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, USERDEBUG_BUILD);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onPreferenceChange_preferenceChecked_shouldEnableConnectivityMonitor() {
        SystemProperties.set(
                ConnectivityMonitorPreferenceController.PROPERTY_CONNECTIVITY_MONITOR,
                ConnectivityMonitorPreferenceController.USER_ENABLED_STATUS);

        mController.handlePreferenceTreeClick(mPreference);

        assertThat(ConnectivityMonitorPreferenceController.USER_ENABLED_STATUS).isEqualTo(
                SystemProperties.get(
                        ConnectivityMonitorPreferenceController.PROPERTY_CONNECTIVITY_MONITOR,
                        ConnectivityMonitorPreferenceController.DISABLED_STATUS));
    }

    @Test
    public void onPreferenceChange_preferenceUnchecked_shouldDisableConnectivityMonitor() {
        SystemProperties.set(
                ConnectivityMonitorPreferenceController.PROPERTY_CONNECTIVITY_MONITOR,
                ConnectivityMonitorPreferenceController.USER_DISABLED_STATUS);

        mController.handlePreferenceTreeClick(mPreference);

        assertThat(ConnectivityMonitorPreferenceController.USER_DISABLED_STATUS).isEqualTo(
                SystemProperties.get(
                        ConnectivityMonitorPreferenceController.PROPERTY_CONNECTIVITY_MONITOR,
                        ConnectivityMonitorPreferenceController.DISABLED_STATUS));
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        String mode = SystemProperties.get(
                ConnectivityMonitorPreferenceController.PROPERTY_CONNECTIVITY_MONITOR,
                null /* default */);

        assertThat(mode).isEqualTo(ConnectivityMonitorPreferenceController.USER_DISABLED_STATUS);
        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
    }
}
