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

import static android.bluetooth.BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * Abstract class for Bluetooth A2DP config dialog controller in developer option.
 */
public abstract class AbstractBluetoothDialogPreferenceController extends
        AbstractBluetoothPreferenceController implements BaseBluetoothDialogPreference.Callback {

    private static final String TAG = "AbstractBtDlgCtr";

    protected static final int[] CODEC_TYPES = {BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC};
    protected static final int[] SAMPLE_RATES = {BluetoothCodecConfig.SAMPLE_RATE_192000,
            BluetoothCodecConfig.SAMPLE_RATE_176400,
            BluetoothCodecConfig.SAMPLE_RATE_96000,
            BluetoothCodecConfig.SAMPLE_RATE_88200,
            BluetoothCodecConfig.SAMPLE_RATE_48000,
            BluetoothCodecConfig.SAMPLE_RATE_44100};
    protected static final int[] BITS_PER_SAMPLES = {BluetoothCodecConfig.BITS_PER_SAMPLE_32,
            BluetoothCodecConfig.BITS_PER_SAMPLE_24,
            BluetoothCodecConfig.BITS_PER_SAMPLE_16};
    protected static final int[] CHANNEL_MODES = {BluetoothCodecConfig.CHANNEL_MODE_STEREO,
            BluetoothCodecConfig.CHANNEL_MODE_MONO};

    protected final BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;

    public AbstractBluetoothDialogPreferenceController(Context context, Lifecycle lifecycle,
                                                       BluetoothA2dpConfigStore store) {
        super(context, lifecycle, store);
        mBluetoothA2dpConfigStore = store;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
    }

    @Override
    public CharSequence getSummary() {
        return ((BaseBluetoothDialogPreference) mPreference).generateSummary(
                getCurrentConfigIndex());
    }

    @Override
    public void onIndexUpdated(int index) {
        final BluetoothA2dp bluetoothA2dp = mBluetoothA2dp;
        if (bluetoothA2dp == null) {
            return;
        }
        writeConfigurationValues(index);
        final BluetoothCodecConfig codecConfig = mBluetoothA2dpConfigStore.createCodecConfig();
        BluetoothDevice activeDevice = mBluetoothA2dp.getActiveDevice();
        if (activeDevice != null) {
            bluetoothA2dp.setCodecConfigPreference(activeDevice, codecConfig);
        }
        mPreference.setSummary(((BaseBluetoothDialogPreference) mPreference).generateSummary(
                index));
    }

    @Override
    public int getCurrentConfigIndex() {
        final BluetoothCodecConfig codecConfig = getCurrentCodecConfig();
        if (codecConfig == null) {
            Log.d(TAG, "Unable to get current config index. Current codec Config is null.");
            return getDefaultIndex();
        }
        return getCurrentIndexByConfig(codecConfig);
    }

    @Override
    public void onBluetoothServiceConnected(BluetoothA2dp bluetoothA2dp) {
        super.onBluetoothServiceConnected(bluetoothA2dp);
        initConfigStore();
    }

    private void initConfigStore() {
        final BluetoothCodecConfig config = getCurrentCodecConfig();
        if (config == null) {
            return;
        }
        mBluetoothA2dpConfigStore.setCodecType(config.getCodecType());
        mBluetoothA2dpConfigStore.setSampleRate(config.getSampleRate());
        mBluetoothA2dpConfigStore.setBitsPerSample(config.getBitsPerSample());
        mBluetoothA2dpConfigStore.setChannelMode(config.getChannelMode());
        mBluetoothA2dpConfigStore.setCodecPriority(CODEC_PRIORITY_HIGHEST);
        mBluetoothA2dpConfigStore.setCodecSpecific1Value(config.getCodecSpecific1());
    }

    /**
     * Updates the new value to the {@link BluetoothA2dpConfigStore}.
     *
     * @param newValue the new setting value
     */
    protected abstract void writeConfigurationValues(int newValue);

    /**
     * To get the current A2DP index value.
     *
     * @param config for the current {@link BluetoothCodecConfig}.
     * @return the current index.
     */
    protected abstract int getCurrentIndexByConfig(BluetoothCodecConfig config);

    /**
     * @return the default index.
     */
    protected int getDefaultIndex() {
        return ((BaseBluetoothDialogPreference) mPreference).getDefaultIndex();
    }

    /**
     * To get the current A2DP codec config.
     *
     * @return {@link BluetoothCodecConfig}.
     */
    protected BluetoothCodecConfig getCurrentCodecConfig() {
        final BluetoothA2dp bluetoothA2dp = mBluetoothA2dp;
        if (bluetoothA2dp == null) {
            return null;
        }
        BluetoothDevice activeDevice = bluetoothA2dp.getActiveDevice();
        if (activeDevice == null) {
            Log.d(TAG, "Unable to get current codec config. No active device.");
            return null;
        }
        final BluetoothCodecStatus codecStatus =
                bluetoothA2dp.getCodecStatus(activeDevice);
        if (codecStatus == null) {
            Log.d(TAG, "Unable to get current codec config. Codec status is null");
            return null;
        }
        return codecStatus.getCodecConfig();
    }

    /**
     * To get the selectable A2DP configs.
     *
     * @return Array of {@link BluetoothCodecConfig}.
     */
    protected BluetoothCodecConfig[] getSelectableConfigs(BluetoothDevice device) {
        final BluetoothA2dp bluetoothA2dp = mBluetoothA2dp;
        if (bluetoothA2dp == null) {
            return null;
        }
        BluetoothDevice bluetoothDevice =
                (device != null) ? device : bluetoothA2dp.getActiveDevice();
        if (bluetoothDevice == null) {
            return null;
        }
        final BluetoothCodecStatus codecStatus = bluetoothA2dp.getCodecStatus(bluetoothDevice);
        if (codecStatus != null) {
            return codecStatus.getCodecsSelectableCapabilities();
        }
        return null;
    }

    /**
     * To get the selectable A2DP config by codec type.
     *
     * @return {@link BluetoothCodecConfig}.
     */
    protected BluetoothCodecConfig getSelectableByCodecType(int codecTypeValue) {
        BluetoothDevice activeDevice = mBluetoothA2dp.getActiveDevice();
        if (activeDevice == null) {
            Log.d(TAG, "Unable to get selectable config. No active device.");
            return null;
        }
        final BluetoothCodecConfig[] configs = getSelectableConfigs(activeDevice);
        if (configs == null) {
            Log.d(TAG, "Unable to get selectable config. Selectable configs is empty.");
            return null;
        }
        for (BluetoothCodecConfig config : configs) {
            if (config.getCodecType() == codecTypeValue) {
                return config;
            }
        }
        Log.d(TAG, "Unable to find matching codec config, type is " + codecTypeValue);
        return null;
    }

    /**
     * Method to notify controller when the HD audio(optional codec) state is changed.
     *
     * @param enabled Is {@code true} when the setting is enabled.
     */
    public void onHDAudioEnabled(boolean enabled) {}

    static int getHighestCodec(BluetoothCodecConfig[] configs) {
        if (configs == null) {
            Log.d(TAG, "Unable to get highest codec. Configs are empty");
            return BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID;
        }
        for (int i = 0; i < CODEC_TYPES.length; i++) {
            for (int j = 0; j < configs.length; j++) {
                if ((configs[j].getCodecType() == CODEC_TYPES[i])) {
                    return CODEC_TYPES[i];
                }
            }
        }
        return BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID;
    }

    static int getHighestSampleRate(BluetoothCodecConfig config) {
        if (config == null) {
            Log.d(TAG, "Unable to get highest sample rate. Config is empty");
            return BluetoothCodecConfig.SAMPLE_RATE_NONE;
        }
        final int capability = config.getSampleRate();
        for (int i = 0; i < SAMPLE_RATES.length; i++) {
            if ((capability & SAMPLE_RATES[i]) != 0) {
                return SAMPLE_RATES[i];
            }
        }
        return BluetoothCodecConfig.SAMPLE_RATE_NONE;
    }

    static int getHighestBitsPerSample(BluetoothCodecConfig config) {
        if (config == null) {
            Log.d(TAG, "Unable to get highest bits per sample. Config is empty");
            return BluetoothCodecConfig.BITS_PER_SAMPLE_NONE;
        }
        final int capability = config.getBitsPerSample();
        for (int i = 0; i < BITS_PER_SAMPLES.length; i++) {
            if ((capability & BITS_PER_SAMPLES[i]) != 0) {
                return BITS_PER_SAMPLES[i];
            }
        }
        return BluetoothCodecConfig.BITS_PER_SAMPLE_NONE;
    }

    static int getHighestChannelMode(BluetoothCodecConfig config) {
        if (config == null) {
            Log.d(TAG, "Unable to get highest channel mode. Config is empty");
            return BluetoothCodecConfig.CHANNEL_MODE_NONE;
        }
        final int capability = config.getChannelMode();
        for (int i = 0; i < CHANNEL_MODES.length; i++) {
            if ((capability & CHANNEL_MODES[i]) != 0) {
                return CHANNEL_MODES[i];
            }
        }
        return BluetoothCodecConfig.CHANNEL_MODE_NONE;
    }
}
