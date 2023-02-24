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
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
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

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BluetoothCodecDialogPreferenceControllerTest {

    private static final int SOURCE_CODEC_TYPE_OPUS = 6; // TODO(b/240635097): remove in U

    private static final String DEVICE_ADDRESS = "00:11:22:33:44:55";

    @Mock
    private BluetoothA2dp mBluetoothA2dp;
    @Mock
    private BluetoothAdapter mBluetoothAdapter;
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
    private BluetoothCodecConfig mCodecConfigOPUS;
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
        mController.mBluetoothAdapter = mBluetoothAdapter;
        mPreference = new BluetoothCodecDialogPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
        mCodecConfigSBC = new BluetoothCodecConfig.Builder()
                .setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC)
                .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST)
                .setSampleRate(BluetoothCodecConfig.SAMPLE_RATE_96000
                        | BluetoothCodecConfig.SAMPLE_RATE_176400)
                .setBitsPerSample(BluetoothCodecConfig.BITS_PER_SAMPLE_32)
                .setChannelMode(BluetoothCodecConfig.CHANNEL_MODE_MONO
                        | BluetoothCodecConfig.CHANNEL_MODE_STEREO)
                .build();
        mCodecConfigAAC = new BluetoothCodecConfig.Builder()
                .setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC)
                .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST)
                .setSampleRate(BluetoothCodecConfig.SAMPLE_RATE_48000
                        | BluetoothCodecConfig.SAMPLE_RATE_88200)
                .setBitsPerSample(BluetoothCodecConfig.BITS_PER_SAMPLE_16
                        | BluetoothCodecConfig.BITS_PER_SAMPLE_24)
                .setChannelMode(BluetoothCodecConfig.CHANNEL_MODE_STEREO)
                .build();
        mCodecConfigAPTX = new BluetoothCodecConfig.Builder()
                .setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX)
                .build();
        mCodecConfigAPTXHD = new BluetoothCodecConfig.Builder()
                .setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD)
                .build();
        mCodecConfigLDAC = new BluetoothCodecConfig.Builder()
                .setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC)
                .build();
        mCodecConfigOPUS = new BluetoothCodecConfig.Builder()
                .setCodecType(SOURCE_CODEC_TYPE_OPUS)
                .build();
        when(mBluetoothAdapter.getActiveDevices(eq(BluetoothProfile.A2DP)))
                .thenReturn(Arrays.asList(mActiveDevice));
    }

    @Test
    public void writeConfigurationValues_selectDefault_setHighest() {
        BluetoothCodecConfig[] mCodecConfigs = {mCodecConfigOPUS, mCodecConfigAAC,
                                                mCodecConfigSBC};
        mCodecStatus = new BluetoothCodecStatus.Builder()
                .setCodecConfig(mCodecConfigSBC)
                .setCodecsSelectableCapabilities(Arrays.asList(mCodecConfigs))
                .build();
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        when(mBluetoothA2dp.isOptionalCodecsEnabled(mActiveDevice)).thenReturn(
                BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        mController.writeConfigurationValues(0);
                                                // TODO(b/240635097): update in U
        verify(mBluetoothA2dpConfigStore).setCodecType(SOURCE_CODEC_TYPE_OPUS);
    }

    @Test
    public void writeConfigurationValues_checkCodec() {
        BluetoothCodecConfig[] mCodecConfigs = {mCodecConfigOPUS, mCodecConfigAAC,
                mCodecConfigSBC, mCodecConfigAPTX, mCodecConfigAPTXHD, mCodecConfigLDAC};
        mCodecStatus = new BluetoothCodecStatus.Builder()
                .setCodecConfig(mCodecConfigSBC)
                .setCodecsSelectableCapabilities(Arrays.asList(mCodecConfigs))
                .build();
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

        mController.writeConfigurationValues(7);
                                                // TODO(b/240635097): update in U
        verify(mBluetoothA2dpConfigStore).setCodecType(SOURCE_CODEC_TYPE_OPUS);
    }

    @Test
    public void writeConfigurationValues_resetHighestConfig() {
        BluetoothCodecConfig[] mCodecConfigs = {mCodecConfigAAC, mCodecConfigSBC, mCodecConfigAPTX,
                mCodecConfigAPTXHD, mCodecConfigLDAC, mCodecConfigOPUS};
        mCodecStatus = new BluetoothCodecStatus.Builder()
                .setCodecConfig(mCodecConfigAAC)
                .setCodecsSelectableCapabilities(Arrays.asList(mCodecConfigs))
                .build();
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
    public void getCurrentIndexByConfig_verifyOpusIndex() {
        assertThat(mController.getCurrentIndexByConfig(mCodecConfigOPUS)).isEqualTo(
                mController.convertCfgToBtnIndex(SOURCE_CODEC_TYPE_OPUS));
                                             // TODO(b/240635097): update in U
    }


    @Test
    public void onIndexUpdated_notifyPreference() {
        mController.onIndexUpdated(0);

        verify(mCallback).onBluetoothCodecChanged();
    }

    @Test
    public void onHDAudioEnabled_optionalCodecEnabled_setsCodecTypeAsOpus() {
        List<BluetoothCodecConfig> mCodecConfigs = Arrays.asList(mCodecConfigOPUS,
                                                   mCodecConfigAAC, mCodecConfigSBC);
        mCodecStatus = new BluetoothCodecStatus.Builder()
                .setCodecConfig(mCodecConfigOPUS)
                .setCodecsSelectableCapabilities(mCodecConfigs)
                .build();
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        when(mBluetoothA2dp.isOptionalCodecsEnabled(mActiveDevice)).thenReturn(
                BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        mController.onHDAudioEnabled(/* enabled= */ true);

        verify(mBluetoothA2dpConfigStore, atLeastOnce()).setCodecType(
                eq(SOURCE_CODEC_TYPE_OPUS)); // TODO(b/240635097): update in U
    }

    @Test
    public void onHDAudioEnabled_optionalCodecEnabled_setsCodecTypeAsAAC() {
        List<BluetoothCodecConfig> mCodecConfigs = Arrays.asList(mCodecConfigOPUS,
                                                   mCodecConfigAAC, mCodecConfigSBC);
        mCodecStatus = new BluetoothCodecStatus.Builder()
                .setCodecConfig(mCodecConfigAAC)
                .setCodecsSelectableCapabilities(mCodecConfigs)
                .build();
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        when(mBluetoothA2dp.isOptionalCodecsEnabled(mActiveDevice)).thenReturn(
                BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        mController.onHDAudioEnabled(/* enabled= */ true);

        verify(mBluetoothA2dpConfigStore, atLeastOnce()).setCodecType(
                eq(BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC));
    }
    @Test
    public void onHDAudioEnabled_optionalCodecDisabled_setsCodecTypeAsSBC() {
        List<BluetoothCodecConfig> mCodecConfigs = Arrays.asList(mCodecConfigOPUS,
                                                   mCodecConfigAAC, mCodecConfigSBC);
        mCodecStatus = new BluetoothCodecStatus.Builder()
                .setCodecConfig(mCodecConfigAAC)
                .setCodecsSelectableCapabilities(mCodecConfigs)
                .build();
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        when(mBluetoothA2dp.isOptionalCodecsEnabled(mActiveDevice)).thenReturn(
                BluetoothA2dp.OPTIONAL_CODECS_PREF_DISABLED);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        mController.onHDAudioEnabled(/* enabled= */ false);

        verify(mBluetoothA2dpConfigStore, atLeastOnce()).setCodecType(
                eq(BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC));
    }
}
