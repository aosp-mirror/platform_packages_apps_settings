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

import android.arch.lifecycle.LifecycleOwner;
import android.bluetooth.BluetoothCodecConfig;
import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class BluetoothAudioQualityPreferenceControllerTest {

    @Mock
    private BluetoothCodecConfig mBluetoothCodecConfig;
    @Mock
    private ListPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;

    /**
     * 0: Optimized for Audio Quality (990kbps/909kbps)
     * 1: Balanced Audio And Connection Quality (660kbps/606kbps)
     * 2: Stereo
     */
    private String[] mListValues;
    private Context mContext;
    private BluetoothAudioQualityPreferenceController mController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = spy(new BluetoothAudioQualityPreferenceController(mContext,
                mLifecycle, mBluetoothA2dpConfigStore));
        mListValues = mController.getListValues();
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void writeConfigurationValues_option3_shouldWrite1003ToSharedStore() {
        when(mPreference.findIndexOfValue(mListValues[3])).thenReturn(3);
        mController.writeConfigurationValues(mListValues[3]);

        verify(mBluetoothA2dpConfigStore).setCodecSpecific1Value(1003);
    }

    @Test
    public void writeConfigurationValues_default_shouldSetDefault() {
        when(mPreference.findIndexOfValue(mListValues[0])).thenReturn(0);
        mController.writeConfigurationValues(mListValues[0]);

        verify(mBluetoothA2dpConfigStore).setCodecSpecific1Value(1000);
    }

    @Test
    public void getCurrentA2dpSettingIndex_option2_shouldReturnSecondIndex() {
        when(mBluetoothCodecConfig.getCodecSpecific1()).thenReturn(Long.valueOf(2));

        final int index = mController.getCurrentA2dpSettingIndex(mBluetoothCodecConfig);

        assertThat(index).isEqualTo(2);
    }

    @Test
    public void getCurrentA2dpSettingIndex_unknownOption_shouldReturnDefault() {
        when(mBluetoothCodecConfig.getCodecType()).thenReturn(1381391835);

        final int index = mController.getCurrentA2dpSettingIndex(mBluetoothCodecConfig);

        assertThat(index).isEqualTo(3);
    }
}
