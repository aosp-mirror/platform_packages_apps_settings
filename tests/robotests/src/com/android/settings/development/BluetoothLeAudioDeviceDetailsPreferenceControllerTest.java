/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static com.android.settings.development.BluetoothLeAudioDeviceDetailsPreferenceController
        .LE_AUDIO_TOGGLE_VISIBLE_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.os.SystemProperties;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class})
public class BluetoothLeAudioDeviceDetailsPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private BluetoothAdapter mBluetoothAdapter;
    @Mock
    private SwitchPreference mPreference;

    private Context mContext;
    private BluetoothLeAudioDeviceDetailsPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new BluetoothLeAudioDeviceDetailsPreferenceController(mContext));
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.mBluetoothAdapter = mBluetoothAdapter;
        mController.displayPreference(mPreferenceScreen);
    }

    @After
    public void tearDown() {
        ShadowDeviceConfig.reset();
    }

    @Test
    public void onPreferenceChanged_settingEnabled_shouldTurnOnLeAudioDeviceDetailSetting() {
        mController.sLeAudioSupportedStateCache = BluetoothStatusCodes.FEATURE_SUPPORTED;
        mController.onPreferenceChange(mPreference, true /* new value */);
        final boolean isEnabled = SystemProperties.getBoolean(
                LE_AUDIO_TOGGLE_VISIBLE_PROPERTY, false);
        assertThat(isEnabled).isTrue();
    }

    @Test
    public void onPreferenceChanged_settingDisabled_shouldTurnOffLeAudioDeviceDetailSetting() {
        mController.sLeAudioSupportedStateCache = BluetoothStatusCodes.FEATURE_SUPPORTED;
        mController.onPreferenceChange(mPreference, false /* new value */);
        final boolean isEnabled = SystemProperties.getBoolean(
                LE_AUDIO_TOGGLE_VISIBLE_PROPERTY, true);
        assertThat(isEnabled).isFalse();
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        mController.sLeAudioSupportedStateCache = BluetoothStatusCodes.FEATURE_SUPPORTED;
        SystemProperties.set(LE_AUDIO_TOGGLE_VISIBLE_PROPERTY, "true");
        mController.mLeAudioEnabledByDefault = false;
        mController.updateState(mPreference);
        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldNotBeChecked() {
        mController.sLeAudioSupportedStateCache = BluetoothStatusCodes.FEATURE_SUPPORTED;
        SystemProperties.set(LE_AUDIO_TOGGLE_VISIBLE_PROPERTY, "false");
        mController.mLeAudioEnabledByDefault = false;
        mController.updateState(mPreference);
        verify(mPreference).setChecked(false);
    }

    @Test
    public void isAvailable_leAudioSupported() {
        mController.mLeAudioEnabledByDefault = false;
        mController.sLeAudioSupportedStateCache = BluetoothStatusCodes.ERROR_UNKNOWN;
        when(mBluetoothAdapter.isLeAudioSupported())
                .thenReturn(BluetoothStatusCodes.FEATURE_SUPPORTED);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_leAudioNotSupported() {
        mController.mLeAudioEnabledByDefault = false;
        mController.sLeAudioSupportedStateCache = BluetoothStatusCodes.ERROR_UNKNOWN;
        when(mBluetoothAdapter.isLeAudioSupported())
                .thenReturn(BluetoothStatusCodes.FEATURE_NOT_SUPPORTED);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isUnAvailable_ifLeAudioConnectionByDefault() {
        mController.mLeAudioEnabledByDefault = true;
        assertThat(mController.isAvailable()).isFalse();
    }
}
