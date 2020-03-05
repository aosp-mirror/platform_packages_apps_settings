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

import static org.mockito.Mockito.atLeastOnce;
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
public class BluetoothCodecDialogPreferenceControllerTest {

    private static final String DEVICE_ADDRESS = "00:11:22:33:44:55";

    @Mock
    private BluetoothA2dp mBluetoothA2dp;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private AbstractBluetoothPreferenceController.Callback mCallback;

    private BluetoothCodecDialogPreferenceController mController;
    private BluetoothCodecDialogPreference mPreference;
    private BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;
    private BluetoothCodecStatus mCodecStatus;
    private BluetoothCodecConfig mCodecConfigAAC;
    private BluetoothCodecConfig mCodecConfigSBC;
    private BluetoothCodecConfig mCodecConfigAPTX;
    private BluetoothCodecConfig mCodecConfigAPTXHD;
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
        mController = new BluetoothCodecDialogPreferenceController(mContext, mLifecycle,
                mBluetoothA2dpConfigStore, mCallback);
        mPreference = new BluetoothCodecDialogPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
        mCodecConfigSBC = new BluetoothCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC,
                BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST,
                BluetoothCodecConfig.SAMPLE_RATE_96000 | BluetoothCodecConfig.SAMPLE_RATE_176400,
                BluetoothCodecConfig.BITS_PER_SAMPLE_32,
                BluetoothCodecConfig.CHANNEL_MODE_MONO | BluetoothCodecConfig.CHANNEL_MODE_STEREO,
                0, 0, 0, 0);
        mCodecConfigAAC = new BluetoothCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC,
                BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST,
                BluetoothCodecConfig.SAMPLE_RATE_48000 | BluetoothCodecConfig.SAMPLE_RATE_88200,
                BluetoothCodecConfig.BITS_PER_SAMPLE_16 | BluetoothCodecConfig.BITS_PER_SAMPLE_24,
                BluetoothCodecConfig.CHANNEL_MODE_STEREO,
                0, 0, 0, 0);
        mCodecConfigAPTX = new BluetoothCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX);
        mCodecConfigAPTXHD = new BluetoothCodecConfig(
                BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD);
        mCodecConfigLDAC = new BluetoothCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC);
        when(mBluetoothA2dp.getActiveDevice()).thenReturn(mActiveDevice);
    }

    @Test
    public void writeConfigurationValues_selectDefault_setHighest() {
        BluetoothCodecConfig[] mCodecConfigs = {mCodecConfigAAC, mCodecConfigSBC};
        mCodecStatus = new BluetoothCodecStatus(mCodecConfigSBC, null, mCodecConfigs);
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        mController.writeConfigurationValues(0);
        verify(mBluetoothA2dpConfigStore).setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC);
    }

    @Test
    public void writeConfigurationValues_checkCodec() {
        BluetoothCodecConfig[] mCodecConfigs = {mCodecConfigAAC, mCodecConfigSBC, mCodecConfigAPTX,
                mCodecConfigAPTXHD, mCodecConfigLDAC, mCodecConfigAAC, mCodecConfigSBC};
        mCodecStatus = new BluetoothCodecStatus(mCodecConfigSBC, null, mCodecConfigs);
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        mController.writeConfigurationValues(1);
        verify(mBluetoothA2dpConfigStore, atLeastOnce()).setCodecType(
                BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC);

        mController.writeConfigurationValues(2);
        verify(mBluetoothA2dpConfigStore).setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC);

        mController.writeConfigurationValues(3);
        verify(mBluetoothA2dpConfigStore).setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX);

        mController.writeConfigurationValues(4);
        verify(mBluetoothA2dpConfigStore).setCodecType(
                BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD);

        mController.writeConfigurationValues(5);
        verify(mBluetoothA2dpConfigStore).setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC);

    }

    @Test
    public void writeConfigurationValues_resetHighestConfig() {
        BluetoothCodecConfig[] mCodecConfigs = {mCodecConfigAAC, mCodecConfigSBC, mCodecConfigAPTX,
                mCodecConfigAPTXHD, mCodecConfigLDAC, mCodecConfigAAC, mCodecConfigSBC};
        mCodecStatus = new BluetoothCodecStatus(mCodecConfigAAC, null, mCodecConfigs);
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        mController.writeConfigurationValues(2);

        verify(mBluetoothA2dpConfigStore, atLeastOnce()).setCodecPriority(
                BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST);
        verify(mBluetoothA2dpConfigStore, atLeastOnce()).setSampleRate(
                BluetoothCodecConfig.SAMPLE_RATE_88200);
        verify(mBluetoothA2dpConfigStore, atLeastOnce()).setBitsPerSample(
                BluetoothCodecConfig.BITS_PER_SAMPLE_24);
        verify(mBluetoothA2dpConfigStore, atLeastOnce()).setChannelMode(
                BluetoothCodecConfig.CHANNEL_MODE_STEREO);
    }

    @Test
    public void getCurrentIndexByConfig_verifyIndex() {
        assertThat(mController.getCurrentIndexByConfig(mCodecConfigAAC)).isEqualTo(
                mController.convertCfgToBtnIndex(BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC));
    }

    @Test
    public void onIndexUpdated_notifyPreference() {
        mController.onIndexUpdated(0);

        verify(mCallback).onBluetoothCodecChanged();
    }
}
