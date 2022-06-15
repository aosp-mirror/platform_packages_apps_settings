/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.development.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothQualityDialogPreferenceControllerTest {

    private static final String DEVICE_ADDRESS = "00:11:22:33:44:55";

    @Mock
    private BluetoothA2dp mBluetoothA2dp;
    @Mock
    private PreferenceScreen mScreen;

    private BluetoothQualityDialogPreferenceController mController;
    private BluetoothQualityDialogPreference mPreference;
    private BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;
    private BluetoothCodecStatus mCodecStatus;
    private BluetoothCodecConfig mCodecConfigAAC;
    private BluetoothCodecConfig mCodecConfigLDAC;
    private BluetoothDevice mActiveDevice;
    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mBluetoothA2dpConfigStore = spy(new BluetoothA2dpConfigStore());
        mActiveDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(DEVICE_ADDRESS);
        mController = new BluetoothQualityDialogPreferenceController(mContext, mLifecycle,
                mBluetoothA2dpConfigStore);
        mPreference = new BluetoothQualityDialogPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
        mCodecConfigAAC = new BluetoothCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC,
                BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT,
                BluetoothCodecConfig.SAMPLE_RATE_48000 | BluetoothCodecConfig.SAMPLE_RATE_88200,
                BluetoothCodecConfig.BITS_PER_SAMPLE_NONE,
                BluetoothCodecConfig.CHANNEL_MODE_NONE,
                0, 0, 0, 0);
        mCodecConfigLDAC = new BluetoothCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC,
                BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT,
                BluetoothCodecConfig.SAMPLE_RATE_96000,
                BluetoothCodecConfig.BITS_PER_SAMPLE_NONE,
                BluetoothCodecConfig.CHANNEL_MODE_NONE,
                1001, 0, 0, 0);
        when(mBluetoothA2dp.getActiveDevice()).thenReturn(mActiveDevice);
    }

    @Test
    public void writeConfigurationValues_checkSampleRate() {
        mController.writeConfigurationValues(0);
        verify(mBluetoothA2dpConfigStore).setCodecSpecific1Value(1000);

        mController.writeConfigurationValues(1);
        verify(mBluetoothA2dpConfigStore).setCodecSpecific1Value(1001);

        mController.writeConfigurationValues(2);
        verify(mBluetoothA2dpConfigStore).setCodecSpecific1Value(1002);

        mController.writeConfigurationValues(3);
        verify(mBluetoothA2dpConfigStore).setCodecSpecific1Value(1003);
    }

    @Test
    public void getCurrentIndexByConfig_verifyIndex() {
        assertThat(mController.getCurrentIndexByConfig(mCodecConfigLDAC)).isEqualTo(
                mController.convertCfgToBtnIndex(1001));
    }

    @Test
    public void updateState_codeTypeIsLDAC_enablePreference() {
        BluetoothCodecConfig[] mCodecConfigs = {mCodecConfigAAC, mCodecConfigLDAC};
        mCodecStatus = new BluetoothCodecStatus(mCodecConfigLDAC, null, mCodecConfigs);
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_codeTypeAAC_disablePreference() {
        BluetoothCodecConfig[] mCodecConfigs = {mCodecConfigAAC, mCodecConfigLDAC};
        mCodecStatus = new BluetoothCodecStatus(mCodecConfigAAC, null, mCodecConfigs);
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }
}
