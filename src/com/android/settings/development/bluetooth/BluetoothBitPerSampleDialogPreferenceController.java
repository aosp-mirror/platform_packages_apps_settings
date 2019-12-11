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

import android.bluetooth.BluetoothCodecConfig;
import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog preference controller to set the Bluetooth A2DP config of bit per sample
 */
public class BluetoothBitPerSampleDialogPreferenceController extends
        AbstractBluetoothDialogPreferenceController {

    private static final String KEY = "bluetooth_bit_per_sample_settings";
    private static final String TAG = "BtBitPerSampleCtr";

    public BluetoothBitPerSampleDialogPreferenceController(Context context, Lifecycle lifecycle,
                                                           BluetoothA2dpConfigStore store) {
        super(context, lifecycle, store);
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
    protected void writeConfigurationValues(final int index) {
        int bitsPerSampleValue = BluetoothCodecConfig.BITS_PER_SAMPLE_NONE;
        switch (index) {
            case 0:
                final BluetoothCodecConfig currentConfig = getCurrentCodecConfig();
                if (currentConfig != null) {
                    bitsPerSampleValue = getHighestBitsPerSample(getSelectableByCodecType(
                            currentConfig.getCodecType()));
                }
                break;
            case 1:
                bitsPerSampleValue = BluetoothCodecConfig.BITS_PER_SAMPLE_16;
                break;
            case 2:
                bitsPerSampleValue = BluetoothCodecConfig.BITS_PER_SAMPLE_24;
                break;
            case 3:
                bitsPerSampleValue = BluetoothCodecConfig.BITS_PER_SAMPLE_32;
                break;
            default:
                break;
        }
        mBluetoothA2dpConfigStore.setBitsPerSample(bitsPerSampleValue);
    }

    @Override
    protected int getCurrentIndexByConfig(BluetoothCodecConfig config) {
        if (config == null) {
            Log.e(TAG, "Unable to get current config index. Config is null.");
        }
        return convertCfgToBtnIndex(config.getBitsPerSample());
    }

    @Override
    public List<Integer> getSelectableIndex() {
        List<Integer> selectableIndex = new ArrayList<>();
        selectableIndex.add(getDefaultIndex());
        final BluetoothCodecConfig currentConfig = getCurrentCodecConfig();
        if (currentConfig != null) {
            final int configs =
                    getSelectableByCodecType(currentConfig.getCodecType()).getBitsPerSample();
            for (int i = 0; i < BITS_PER_SAMPLES.length; i++) {
                if ((configs & BITS_PER_SAMPLES[i]) != 0) {
                    selectableIndex.add(convertCfgToBtnIndex(BITS_PER_SAMPLES[i]));
                }
            }
        }
        return selectableIndex;
    }

    @VisibleForTesting
    int convertCfgToBtnIndex(int config) {
        int index = getDefaultIndex();
        switch (config) {
            case BluetoothCodecConfig.BITS_PER_SAMPLE_16:
                index = 1;
                break;
            case BluetoothCodecConfig.BITS_PER_SAMPLE_24:
                index = 2;
                break;
            case BluetoothCodecConfig.BITS_PER_SAMPLE_32:
                index = 3;
                break;
            default:
                Log.e(TAG, "Unsupported config:" + config);
                break;
        }
        return index;
    }
}
