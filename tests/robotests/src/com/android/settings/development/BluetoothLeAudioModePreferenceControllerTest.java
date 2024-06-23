/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.android.settings.development.BluetoothLeAudioModePreferenceController
        .LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.SystemProperties;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothLeAudioModePreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;
    @Mock
    private BluetoothAdapter mBluetoothAdapter;
    @Mock
    private ListPreference mPreference;

    private Context mContext;
    private BluetoothLeAudioModePreferenceController mController;
    private String[] mListValues;
    private String[] mListSummaries;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mListValues = mContext.getResources().getStringArray(
                R.array.bluetooth_leaudio_mode_values);
        mListSummaries = mContext.getResources().getStringArray(
                R.array.bluetooth_leaudio_mode);
        mController = spy(new BluetoothLeAudioModePreferenceController(mContext, mFragment));
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.mBluetoothAdapter = mBluetoothAdapter;
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onRebootDialogConfirmed_changeLeAudioMode_shouldSetLeAudioMode() {
        mController.mChanged = true;
        SystemProperties.set(LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY, mListValues[0]);
        mController.mNewMode = mListValues[1];

        mController.onRebootDialogConfirmed();
        assertThat(SystemProperties.get(LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY, mListValues[0])
                        .equals(mController.mNewMode)).isTrue();
    }

    @Test
    public void onRebootDialogConfirmed_notChangeLeAudioMode_shouldNotSetLeAudioMode() {
        mController.mChanged = false;
        SystemProperties.set(LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY, mListValues[0]);
        mController.mNewMode = mListValues[1];

        mController.onRebootDialogConfirmed();
        assertThat(SystemProperties.get(LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY, mListValues[0])
                        .equals(mController.mNewMode)).isFalse();
    }

    @Test
    public void onRebootDialogCanceled_shouldNotSetLeAudioMode() {
        mController.mChanged = true;
        SystemProperties.set(LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY, mListValues[0]);
        mController.mNewMode = mListValues[1];

        mController.onRebootDialogCanceled();
        assertThat(SystemProperties.get(LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY, mListValues[0])
                        .equals(mController.mNewMode)).isFalse();
    }
}
