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

package com.android.settings.development.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothCodecType;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.platform.test.annotations.RequiresFlagsEnabled;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settings.development.Flags;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BluetoothCodecListPreferenceControllerTest {
    private static final String DEVICE_ADDRESS = "00:11:22:33:44:55";

    @Mock private BluetoothA2dp mBluetoothA2dp;
    @Mock private BluetoothAdapter mBluetoothAdapter;
    @Mock private PreferenceScreen mScreen;
    @Mock private AbstractBluetoothPreferenceController.Callback mCallback;

    private BluetoothCodecListPreferenceController mController;
    private ListPreference mPreference;
    private BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;
    private BluetoothCodecStatus mCodecStatus;
    private BluetoothCodecType mCodecTypeAAC;
    private BluetoothCodecType mCodecTypeSBC;
    private BluetoothCodecType mCodecTypeAPTX;
    private BluetoothCodecType mCodecTypeLDAC;
    private BluetoothCodecType mCodecTypeOPUS;
    private List<BluetoothCodecType> mCodecTypes;

    private BluetoothCodecConfig mCodecConfigAAC;
    private BluetoothCodecConfig mCodecConfigSBC;
    private BluetoothCodecConfig mCodecConfigAPTX;
    private BluetoothCodecConfig mCodecConfigAPTXHD;
    private BluetoothCodecConfig mCodecConfigLDAC;
    private BluetoothCodecConfig mCodecConfigOPUS;
    private List<BluetoothCodecConfig> mCodecConfigs;
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
        mController =
                new BluetoothCodecListPreferenceController(
                        mContext, mLifecycle, mBluetoothA2dpConfigStore, mCallback);
        mController.mBluetoothAdapter = mBluetoothAdapter;
        mPreference = new ListPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);

        mCodecTypeAAC =
                BluetoothCodecType.createFromType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC);
        mCodecTypeSBC =
                BluetoothCodecType.createFromType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC);
        mCodecTypeAPTX =
                BluetoothCodecType.createFromType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX);
        mCodecTypeLDAC =
                BluetoothCodecType.createFromType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC);
        mCodecTypeOPUS =
                BluetoothCodecType.createFromType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_OPUS);

        mCodecTypes = new ArrayList<>();
        mCodecTypes.addAll(
                Arrays.asList(
                        mCodecTypeSBC,
                        mCodecTypeAAC,
                        mCodecTypeAPTX,
                        mCodecTypeLDAC,
                        mCodecTypeOPUS));

        mCodecConfigSBC =
                new BluetoothCodecConfig.Builder()
                        .setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC)
                        .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST)
                        .setSampleRate(
                                BluetoothCodecConfig.SAMPLE_RATE_96000
                                        | BluetoothCodecConfig.SAMPLE_RATE_176400)
                        .setBitsPerSample(BluetoothCodecConfig.BITS_PER_SAMPLE_32)
                        .setChannelMode(
                                BluetoothCodecConfig.CHANNEL_MODE_MONO
                                        | BluetoothCodecConfig.CHANNEL_MODE_STEREO)
                        .build();
        mCodecConfigAAC =
                new BluetoothCodecConfig.Builder()
                        .setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC)
                        .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST)
                        .setSampleRate(
                                BluetoothCodecConfig.SAMPLE_RATE_48000
                                        | BluetoothCodecConfig.SAMPLE_RATE_88200)
                        .setBitsPerSample(
                                BluetoothCodecConfig.BITS_PER_SAMPLE_16
                                        | BluetoothCodecConfig.BITS_PER_SAMPLE_24)
                        .setChannelMode(BluetoothCodecConfig.CHANNEL_MODE_STEREO)
                        .build();
        mCodecConfigAPTX =
                new BluetoothCodecConfig.Builder()
                        .setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX)
                        .build();
        mCodecConfigAPTXHD =
                new BluetoothCodecConfig.Builder()
                        .setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD)
                        .build();
        mCodecConfigLDAC =
                new BluetoothCodecConfig.Builder()
                        .setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC)
                        .build();
        mCodecConfigOPUS =
                new BluetoothCodecConfig.Builder()
                        .setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_OPUS)
                        .build();

        mCodecConfigs = new ArrayList<>();
        mCodecConfigs.addAll(
                Arrays.asList(
                        mCodecConfigOPUS,
                        mCodecConfigAAC,
                        mCodecConfigSBC,
                        mCodecConfigAPTX,
                        mCodecConfigAPTXHD,
                        mCodecConfigLDAC));

        when(mBluetoothAdapter.getActiveDevices(eq(BluetoothProfile.A2DP)))
                .thenReturn(Arrays.asList(mActiveDevice));
        when(mBluetoothA2dp.getSupportedCodecTypes()).thenReturn(mCodecTypes);
    }

    @Test
    public void writeConfigurationValues_selectDefault() {
        mCodecStatus =
                new BluetoothCodecStatus.Builder()
                        .setCodecConfig(mCodecConfigSBC)
                        .setCodecsSelectableCapabilities(mCodecConfigs)
                        .build();
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        when(mBluetoothA2dp.isOptionalCodecsEnabled(mActiveDevice))
                .thenReturn(BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED);

        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        mController.writeConfigurationValues(String.valueOf(mController.DEFAULT_VALUE_INT));
        verify(mBluetoothA2dpConfigStore, times(2)).setCodecType(mCodecTypeSBC);
    }

    @Test
    public void writeConfigurationValues_checkCodec() {
        mCodecStatus =
                new BluetoothCodecStatus.Builder()
                        .setCodecConfig(mCodecConfigSBC)
                        .setCodecsSelectableCapabilities(mCodecConfigs)
                        .build();
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        mController.writeConfigurationValues(String.valueOf(mCodecTypeSBC.getCodecId()));
        verify(mBluetoothA2dpConfigStore, atLeastOnce()).setCodecType(mCodecTypeSBC);

        mController.writeConfigurationValues(String.valueOf(mCodecTypeAAC.getCodecId()));
        verify(mBluetoothA2dpConfigStore).setCodecType(mCodecTypeAAC);

        mController.writeConfigurationValues(String.valueOf(mCodecTypeAPTX.getCodecId()));
        verify(mBluetoothA2dpConfigStore).setCodecType(mCodecTypeAPTX);

        mController.writeConfigurationValues(String.valueOf(mCodecTypeLDAC.getCodecId()));
        verify(mBluetoothA2dpConfigStore).setCodecType(mCodecTypeLDAC);

        mController.writeConfigurationValues(String.valueOf(mCodecTypeOPUS.getCodecId()));
        verify(mBluetoothA2dpConfigStore).setCodecType(mCodecTypeOPUS);
    }

    @Test
    public void writeConfigurationValues_chooseHighestConfig() {
        mCodecStatus =
                new BluetoothCodecStatus.Builder()
                        .setCodecConfig(mCodecConfigSBC)
                        .setCodecsSelectableCapabilities((mCodecConfigs))
                        .build();
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        mController.writeConfigurationValues(String.valueOf(mCodecTypeAAC.getCodecId()));

        verify(mBluetoothA2dpConfigStore, atLeastOnce())
                .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST);
        verify(mBluetoothA2dpConfigStore, atLeastOnce())
                .setSampleRate(BluetoothCodecConfig.SAMPLE_RATE_88200);
        verify(mBluetoothA2dpConfigStore, atLeastOnce())
                .setBitsPerSample(BluetoothCodecConfig.BITS_PER_SAMPLE_24);
        verify(mBluetoothA2dpConfigStore, atLeastOnce())
                .setChannelMode(BluetoothCodecConfig.CHANNEL_MODE_STEREO);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_A2DP_OFFLOAD_CODEC_EXTENSIBILITY_SETTINGS)
    public void onPreferenceChange_notifyPreference() {
        assertFalse(
                mController.onPreferenceChange(
                        mPreference, String.valueOf(mCodecTypeAAC.getCodecId())));

        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        assertTrue(
                mController.onPreferenceChange(
                        mPreference, String.valueOf(mCodecTypeAAC.getCodecId())));

        verify(mCallback).onBluetoothCodecChanged();
    }

    @Test
    public void onHDAudioEnabled_setsPreferenceEnabled() {
        mController.onHDAudioEnabled(/* enabled= */ true);
        assertThat(mPreference.isEnabled()).isTrue();
    }
}
