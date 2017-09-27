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

import android.content.Context;
import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * deprecated in favor of {@link ConnectivityMonitorPreferenceControllerV2}
 */
@Deprecated
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ConnectivityMonitorPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;

    private ConnectivityMonitorPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        SettingsShadowSystemProperties.clear();
        mController = new ConnectivityMonitorPreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void isAvailable_trueShowFlagWithUserdebugBuild_shouldReturnTrue() {
        when(mContext.getResources().getBoolean(R.bool.config_show_connectivity_monitor))
                .thenReturn(true);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, "userdebug");

        assertThat(mController.isAvailable()).isTrue();
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void isAvailable_trueShowFlagWithEngBuild_shouldReturnTrue() {
        when(mContext.getResources().getBoolean(R.bool.config_show_connectivity_monitor))
                .thenReturn(true);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, "eng");

        assertThat(mController.isAvailable()).isTrue();
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void isAvailable_trueShowFlagWithUserBuild_shouldReturnFalse() {
        when(mContext.getResources().getBoolean(R.bool.config_show_connectivity_monitor))
                .thenReturn(true);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, "user");

        assertThat(mController.isAvailable()).isFalse();
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void isAvailable_falseShowFlagWithUserdebugBuild_shouldReturnFalse() {
        when(mContext.getResources().getBoolean(R.bool.config_show_connectivity_monitor))
                .thenReturn(false);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, "userdebug");

        assertThat(mController.isAvailable()).isFalse();
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void isAvailable_falseShowFlagWithEngBuild_shouldReturnFalse() {
        when(mContext.getResources().getBoolean(R.bool.config_show_connectivity_monitor))
                .thenReturn(false);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, "eng");

        assertThat(mController.isAvailable()).isFalse();
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void isAvailable_falseShowFlagWithUserBuild_shouldReturnFalse() {
        when(mContext.getResources().getBoolean(R.bool.config_show_connectivity_monitor))
                .thenReturn(false);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, "user");

        assertThat(mController.isAvailable()).isFalse();
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void displayPreference_connectivityMonitorEnabled_shouldCheckedPreference() {
        when(mContext.getResources().getBoolean(R.bool.config_show_connectivity_monitor))
                .thenReturn(true);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.PROPERTY_CONNECTIVITY_MONITOR,
                ConnectivityMonitorPreferenceController.ENABLED_STATUS);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, "userdebug");

        mController.displayPreference(mScreen);

        verify(mPreference).setChecked(true);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void displayPreference_connectivityMonitorUserEnabled_shouldCheckedPreference() {
        when(mContext.getResources().getBoolean(R.bool.config_show_connectivity_monitor))
                .thenReturn(true);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.PROPERTY_CONNECTIVITY_MONITOR,
                ConnectivityMonitorPreferenceController.USER_ENABLED_STATUS);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, "userdebug");

        mController.displayPreference(mScreen);

        verify(mPreference).setChecked(true);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void displayPreference_connectivityMonitorDisabled_shouldUncheckedPreference() {
        when(mContext.getResources().getBoolean(R.bool.config_show_connectivity_monitor))
                .thenReturn(true);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.PROPERTY_CONNECTIVITY_MONITOR,
                ConnectivityMonitorPreferenceController.DISABLED_STATUS);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, "userdebug");

        mController.displayPreference(mScreen);

        verify(mPreference).setChecked(false);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void displayPreference_connectivityMonitorUserDisabled_shouldUncheckedPreference() {
        when(mContext.getResources().getBoolean(R.bool.config_show_connectivity_monitor))
                .thenReturn(true);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.PROPERTY_CONNECTIVITY_MONITOR,
                ConnectivityMonitorPreferenceController.USER_DISABLED_STATUS);
        SettingsShadowSystemProperties.set(
                ConnectivityMonitorPreferenceController.BUILD_TYPE, "userdebug");

        mController.displayPreference(mScreen);

        verify(mPreference).setChecked(false);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void handlePreferenceTreeClick_preferenceChecked_shouldEnableConnectivityMonitor() {
        when(mPreference.isChecked()).thenReturn(true);

        when(mContext.getResources().getString(R.string.connectivity_monitor_toast))
                .thenReturn("To apply connectivity monitor change, reboot device");

        mController.handlePreferenceTreeClick(mPreference);

        assertThat(ConnectivityMonitorPreferenceController.USER_ENABLED_STATUS.equals(
                SystemProperties.get(
                        ConnectivityMonitorPreferenceController.PROPERTY_CONNECTIVITY_MONITOR,
                        ConnectivityMonitorPreferenceController.DISABLED_STATUS))).isTrue();
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void handlePreferenceTreeClick_preferenceUnchecked_shouldDisableConnectivityMonitor() {
        when(mPreference.isChecked()).thenReturn(false);

        when(mContext.getResources().getString(R.string.connectivity_monitor_toast))
                .thenReturn("To apply connectivity monitor change, reboot device");

        mController.handlePreferenceTreeClick(mPreference);

        assertThat(ConnectivityMonitorPreferenceController.USER_DISABLED_STATUS.equals(
                SystemProperties.get(
                        ConnectivityMonitorPreferenceController.PROPERTY_CONNECTIVITY_MONITOR,
                        ConnectivityMonitorPreferenceController.DISABLED_STATUS))).isTrue();
    }

}
