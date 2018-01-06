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

import static com.android.settings.development.BluetoothMaxConnectedAudioDevicesPreferenceController
        .BLUETOOTH_MAX_CONNECTED_AUDIO_DEVICES_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;

import org.junit.After;
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
public class BluetoothMaxConnectedAudioDevicesPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Context mContext;
    private ListPreference mPreference;
    private BluetoothMaxConnectedAudioDevicesPreferenceController mController;

    /**
     * 0: 1 device maximum (Default)
     * 1: 2 devices maximum
     * 2: 3 devices maximum
     * 3: 4 devices maximum
     * 4: 5 devices maximum
     */
    private String[] mListValues;
    private String[] mListSummaries;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new ListPreference(mContext);
        mListValues = mContext.getResources().getStringArray(
                R.array.bluetooth_max_connected_audio_devices_values);
        mListSummaries = mContext.getResources().getStringArray(
                R.array.bluetooth_max_connected_audio_devices);
        mController = new BluetoothMaxConnectedAudioDevicesPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @After
    public void teardown() {
        SettingsShadowSystemProperties.clear();
    }

    @Test
    public void onPreferenceChange_setNumberOfDevices() {
        for (int numberOfDevices = 0; numberOfDevices < mListValues.length; numberOfDevices++) {
            mController.onPreferenceChange(mPreference, mListValues[numberOfDevices]);

            final String currentValue = SystemProperties.get(
                    BLUETOOTH_MAX_CONNECTED_AUDIO_DEVICES_PROPERTY);

            assertThat(currentValue).isEqualTo(mListValues[numberOfDevices]);
            assertThat(mPreference.getValue()).isEqualTo(mListValues[numberOfDevices]);
            assertThat(mPreference.getSummary()).isEqualTo(mListSummaries[numberOfDevices]);
        }
    }

    @Test
    public void updateState_NumberOfDevicesUpdated_shouldSetPreference() {
        for (int numberOfDevices = 0; numberOfDevices < mListValues.length; numberOfDevices++) {
            SystemProperties.set(BLUETOOTH_MAX_CONNECTED_AUDIO_DEVICES_PROPERTY,
                    mListValues[numberOfDevices]);

            mController.updateState(mPreference);

            assertThat(mPreference.getValue()).isEqualTo(mListValues[numberOfDevices]);
            assertThat(mPreference.getSummary()).isEqualTo(mListSummaries[numberOfDevices]);
        }
    }

    @Test
    public void updateState_noValueSet_shouldSetDefaultTo1device() {
        SystemProperties.set(BLUETOOTH_MAX_CONNECTED_AUDIO_DEVICES_PROPERTY, "garbage");
        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(mListValues[0]);
        assertThat(mPreference.getSummary()).isEqualTo(mListSummaries[0]);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getValue()).isEqualTo(mListValues[0]);
        assertThat(mPreference.getSummary()).isEqualTo(mListSummaries[0]);
        final String currentValue = SystemProperties.get(
                BLUETOOTH_MAX_CONNECTED_AUDIO_DEVICES_PROPERTY);
        assertThat(currentValue).isEqualTo(mListValues[0]);
    }

    @Test
    public void onDeveloperOptionsSwitchEnabled_shouldEnablePreference() {
        for (int numberOfDevices = 0; numberOfDevices < mListValues.length; numberOfDevices++) {
            mController.onDeveloperOptionsSwitchDisabled();
            assertThat(mPreference.isEnabled()).isFalse();

            SystemProperties.set(BLUETOOTH_MAX_CONNECTED_AUDIO_DEVICES_PROPERTY,
                    mListValues[numberOfDevices]);
            mController.onDeveloperOptionsSwitchEnabled();

            assertThat(mPreference.isEnabled()).isTrue();
            assertThat(mPreference.getValue()).isEqualTo(mListValues[numberOfDevices]);
            assertThat(mPreference.getSummary()).isEqualTo(mListSummaries[numberOfDevices]);
        }
    }
}
