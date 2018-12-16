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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothCodecConfig;
import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothAudioSampleRatePreferenceControllerTest {

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
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private Context mContext;
    private BluetoothAudioSampleRatePreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = spy(new BluetoothAudioSampleRatePreferenceController(mContext, mLifecycle,
                mBluetoothA2dpConfigStore));
        mListValues = mController.getListValues();
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void writeConfigurationValues_option2_shouldWriteOption2ToSharedStore() {
        when(mPreference.findIndexOfValue(mListValues[2])).thenReturn(2);
        mController.writeConfigurationValues(mListValues[2]);

        verify(mBluetoothA2dpConfigStore).setSampleRate(BluetoothCodecConfig.SAMPLE_RATE_48000);
    }

    @Test
    public void getCurrentA2dpSettingIndex_option2_shouldReturnSecondIndex() {
        when(mBluetoothCodecConfig.getSampleRate()).thenReturn(
                BluetoothCodecConfig.SAMPLE_RATE_48000);

        final int index = mController.getCurrentA2dpSettingIndex(mBluetoothCodecConfig);

        assertThat(index).isEqualTo(2);
    }

    @Test
    public void getCurrentA2dpSettingIndex_unknownOption_shouldReturnDefault() {
        when(mBluetoothCodecConfig.getSampleRate()).thenReturn(1381391835);

        final int index = mController.getCurrentA2dpSettingIndex(mBluetoothCodecConfig);

        assertThat(index).isEqualTo(0);
    }
}
