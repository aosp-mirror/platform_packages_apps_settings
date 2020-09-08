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

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog preference controller to set the Bluetooth A2DP config of codec
 */
public class BluetoothCodecDialogPreferenceController extends
        AbstractBluetoothDialogPreferenceController {

    private static final String KEY = "bluetooth_audio_codec_settings";
    private static final String TAG = "BtCodecCtr";

    private final Callback mCallback;

    public BluetoothCodecDialogPreferenceController(Context context, Lifecycle lifecycle,
                                                    BluetoothA2dpConfigStore store,
                                                    Callback callback) {
        super(context, lifecycle, store);
        mCallback = callback;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        ((BaseBluetoothDialogPreference) mPreference).setCallback(this);
    }

    @Override
    public void onHDAudioEnabled(boolean enabled) {
        if (!enabled) {
            // If option codec is disabled, SBC is the only only one available codec.
            onIndexUpdated(convertCfgToBtnIndex(BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC));
        }
    }

    @Override
    public List<Integer> getSelectableIndex() {
        List<Integer> index = new ArrayList<>();
        final BluetoothA2dp bluetoothA2dp = mBluetoothA2dp;

        index.add(getDefaultIndex());
        if (bluetoothA2dp == null) {
            return index;
        }
        final BluetoothDevice activeDevice = bluetoothA2dp.getActiveDevice();
        if (activeDevice == null) {
            Log.d(TAG, "Unable to get selectable index. No Active Bluetooth device");
            return index;
        }
        // Check HD audio is enabled, display the available list.
        if (bluetoothA2dp.isOptionalCodecsEnabled(activeDevice)
                == BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED) {
            BluetoothCodecConfig[] configs = getSelectableConfigs(activeDevice);
            if (configs != null) {
                return getIndexFromConfig(configs);
            }
        }
        // If HD audio is disabled, SBC is the only one available codec.
        index.add(convertCfgToBtnIndex(BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC));
        return index;
    }

    @Override
    protected void writeConfigurationValues(final int index) {
        int codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC; // default
        int codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT;
        switch (index) {
            case 0:
                codecTypeValue = getHighestCodec(getSelectableConfigs(
                    mBluetoothA2dp.getActiveDevice()));
                codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
                break;
            case 1:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC;
                codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
                break;
            case 2:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC;
                codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
                break;
            case 3:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX;
                codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
                break;
            case 4:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD;
                codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
                break;
            case 5:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC;
                codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
                break;
            default:
                break;
        }
        mBluetoothA2dpConfigStore.setCodecType(codecTypeValue);
        mBluetoothA2dpConfigStore.setCodecPriority(codecPriorityValue);

        // Once user changes codec, to reset configs with highest quality.
        final BluetoothCodecConfig config = getSelectableByCodecType(codecTypeValue);
        if (config == null) {
            Log.d(TAG, "Selectable config is null. Unable to reset");
        }
        mBluetoothA2dpConfigStore.setSampleRate(getHighestSampleRate(config));
        mBluetoothA2dpConfigStore.setBitsPerSample(getHighestBitsPerSample(config));
        mBluetoothA2dpConfigStore.setChannelMode(getHighestChannelMode(config));
    }

    @Override
    protected int getCurrentIndexByConfig(BluetoothCodecConfig config) {
        if (config == null) {
            Log.e(TAG, "Unable to get current config index. Config is null.");
        }
        return convertCfgToBtnIndex(config.getCodecType());
    }

    @Override
    public void onIndexUpdated(int index) {
        super.onIndexUpdated(index);
        mCallback.onBluetoothCodecChanged();
    }

    private List<Integer> getIndexFromConfig(BluetoothCodecConfig[] configs) {
        List<Integer> indexArray = new ArrayList<>();
        for (int i = 0; i < configs.length; i++) {
            indexArray.add(convertCfgToBtnIndex(configs[i].getCodecType()));
        }
        return indexArray;
    }

    @VisibleForTesting
    int convertCfgToBtnIndex(int config) {
        int index = getDefaultIndex();
        switch (config) {
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC:
                index = 1;
                break;
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC:
                index = 2;
                break;
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX:
                index = 3;
                break;
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD:
                index = 4;
                break;
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC:
                index = 5;
                break;
            default:
                Log.e(TAG, "Unsupported config:" + config);
                break;
        }
        return index;
    }
}
