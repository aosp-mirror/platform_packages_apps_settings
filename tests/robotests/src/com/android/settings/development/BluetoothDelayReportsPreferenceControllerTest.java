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


import static com.android.settings.development.BluetoothDelayReportsPreferenceController
        .BLUETOOTH_ENABLE_AVDTP_DELAY_REPORTS_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
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
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {SettingsShadowSystemProperties.class})
public class BluetoothDelayReportsPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Context mContext;
    private SwitchPreference mPreference;
    private BluetoothDelayReportsPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mController = spy(new BluetoothDelayReportsPreferenceController(mContext));
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChanged_settingEnabled_turnOnDelayReports() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        final boolean mode = SettingsShadowSystemProperties.getBoolean(
                BLUETOOTH_ENABLE_AVDTP_DELAY_REPORTS_PROPERTY, false /* default */);

        assertThat(mode).isTrue();
    }

    @Test
    public void onPreferenceChanged_settingDisabled_turnOffDelayReports() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        final boolean mode = SettingsShadowSystemProperties.getBoolean(
                BLUETOOTH_ENABLE_AVDTP_DELAY_REPORTS_PROPERTY, false /* default */);

        assertThat(mode).isFalse();
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        SettingsShadowSystemProperties.set(BLUETOOTH_ENABLE_AVDTP_DELAY_REPORTS_PROPERTY,
                Boolean.toString(true));
        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldNotBeChecked() {
        SettingsShadowSystemProperties.set(BLUETOOTH_ENABLE_AVDTP_DELAY_REPORTS_PROPERTY,
                Boolean.toString(false));
        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsDisabled();

        final boolean mode = SettingsShadowSystemProperties.getBoolean(
                BLUETOOTH_ENABLE_AVDTP_DELAY_REPORTS_PROPERTY, false /* default */);

        assertThat(mode).isFalse();
        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void onDeveloperOptionsEnabled_shouldEnablePreference() {
        mController.onDeveloperOptionsEnabled();

        final boolean mode = SettingsShadowSystemProperties.getBoolean(
                BLUETOOTH_ENABLE_AVDTP_DELAY_REPORTS_PROPERTY, false /* default */);

        assertThat(mode).isFalse();
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isFalse();
    }
}