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

import static com.android.settings.development.BluetoothAudioSampleRatePreferenceController
        .STREAMING_LABEL_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BluetoothAudioSampleRatePreferenceControllerTest {

    @Mock
    private BluetoothA2dp mBluetoothA2dp;
    @Mock
    private BluetoothCodecConfig mBluetoothCodecConfig;
    @Mock
    private ListPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;

    /**
     * 0: Use System Selection (Default)
     * 1: 44.1 kHz
     * 2: 48.0 kHz
     * 3: 88.2 kHz
     * 4: 96.0 kHz
     */
    private String[] mListValues;
    private String[] mListSummaries;
    private Lifecycle mLifecycle;
    private Context mContext;
    private BluetoothAudioSampleRatePreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLifecycle = new Lifecycle();
        mController = spy(new BluetoothAudioSampleRatePreferenceController(mContext, mLifecycle,
                new Object(), mBluetoothA2dpConfigStore));
        doReturn(mBluetoothCodecConfig).when(mController).getCodecConfig();
        doNothing().when(mController).setCodecConfigPreference(any());
        when(mBluetoothA2dpConfigStore.createCodecConfig()).thenReturn(mBluetoothCodecConfig);
        mListValues = mContext.getResources().getStringArray(
                R.array.bluetooth_a2dp_codec_sample_rate_values);
        mListSummaries = mContext.getResources().getStringArray(
                R.array.bluetooth_a2dp_codec_sample_rate_summaries);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void updateState_nothingSet_shouldUpdateToDefault() {
        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[0]);
        verify(mPreference).setSummary(
                mContext.getResources().getString(STREAMING_LABEL_ID, mListSummaries[0]));
    }

    @Test
    public void updateState_option2Set_shouldUpdateToOption2() {
        when(mBluetoothCodecConfig.getSampleRate()).thenReturn(
                BluetoothCodecConfig.SAMPLE_RATE_48000);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[2]);
        verify(mPreference).setSummary(
                mContext.getResources().getString(STREAMING_LABEL_ID, mListSummaries[2]));
        verify(mBluetoothA2dpConfigStore).setSampleRate(BluetoothCodecConfig.SAMPLE_RATE_48000);
    }

    @Test
    public void onPreferenceChange_bluetoothConnected_shouldUpdateCodec() {
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        mController.onPreferenceChange(mPreference, "" /* new value */);

        verify(mController).setCodecConfigPreference(any());
    }

    @Test
    public void onPreferenceChange_bluetoothNotConnected_shouldUpdateCodec() {
        mController.onBluetoothServiceDisconnected();

        mController.onPreferenceChange(mPreference, "" /* new value */);

        verify(mController, never()).setCodecConfigPreference(any());
    }

    @Test
    public void onBluetoothServiceConnected_shouldUpdateState() {
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        verify(mController).updateState(mPreference);
    }

    @Test
    public void onBluetoothCodecUpdated_shouldUpdateState() {
        mController.onBluetoothCodecUpdated();

        verify(mController).updateState(mPreference);
    }

    @Test
    public void onDeveloperOptionsSwitchEnabled_shouldEnablePreference() {
        mController.onDeveloperOptionsSwitchEnabled();

        verify(mPreference).setEnabled(true);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setEnabled(false);
    }

}
