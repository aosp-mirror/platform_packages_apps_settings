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

import static android.bluetooth.BluetoothStatusCodes.FEATURE_SUPPORTED;

import static com.android.settings.development.BluetoothLeAudioAllowListPreferenceController
        .BYPASS_LE_AUDIO_ALLOWLIST_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.SystemProperties;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothLeAudioAllowListPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;
    @Mock
    private BluetoothAdapter mBluetoothAdapter;
    @Mock
    private SwitchPreference mPreference;
    private Context mContext;
    private BluetoothLeAudioAllowListPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new BluetoothLeAudioAllowListPreferenceController(mContext, mFragment));
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.mBluetoothAdapter = mBluetoothAdapter;
        mController.displayPreference(mPreferenceScreen);
        when(mBluetoothAdapter.isLeAudioSupported())
            .thenReturn(FEATURE_SUPPORTED);
    }

    @Test
    public void onPreferenceChange_setCheck_shouldBypassLeAudioAllowlist() {
        mController.onPreferenceChange(mPreference, Boolean.TRUE);
        assertThat(SystemProperties.getBoolean(BYPASS_LE_AUDIO_ALLOWLIST_PROPERTY,
                false)).isTrue();
    }

    @Test
    public void onPreferenceChange_setUnCheck_shouldNotBypassLeAudioAllowlist() {
        mController.onPreferenceChange(mPreference, Boolean.FALSE);
        assertThat(SystemProperties.getBoolean(BYPASS_LE_AUDIO_ALLOWLIST_PROPERTY,
                true)).isFalse();
    }

    @Test
    public void updateState_bluetoothOff_shouldDisableToggle() {
        mController.mBluetoothAdapter = null;
        mController.updateState(mPreference);
        verify(mPreference).setEnabled(false);
    }

    @Test
    public void updateState_bluetoothOn_shouldShowStatus() {
        SystemProperties.set(BYPASS_LE_AUDIO_ALLOWLIST_PROPERTY, Boolean.toString(true));
        mController.updateState(mPreference);
        verify(mPreference).setChecked(true);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldSetBypassLeAudioAllowlistToFalse() {
        SystemProperties.set(BYPASS_LE_AUDIO_ALLOWLIST_PROPERTY, Boolean.toString(true));
        mController.onDeveloperOptionsSwitchDisabled();
        verify(mPreference).setEnabled(false);
        assertThat(SystemProperties.getBoolean(BYPASS_LE_AUDIO_ALLOWLIST_PROPERTY, true)).isFalse();
    }
}
