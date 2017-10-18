/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.bluetooth.BluetoothCodecConfig;
import android.content.Context;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class BluetoothAudioBitsPerSamplePreferenceController extends
        AbstractBluetoothA2dpPreferenceController {

    private static final int DEFAULT_INDEX = 0;
    private static final String BLUETOOTH_SELECT_A2DP_BITS_PER_SAMPLE_KEY =
            "bluetooth_select_a2dp_bits_per_sample";

    public BluetoothAudioBitsPerSamplePreferenceController(Context context, Lifecycle lifecycle,
            BluetoothA2dpConfigStore store) {
        super(context, lifecycle, store);
    }

    @Override
    public String getPreferenceKey() {
        return BLUETOOTH_SELECT_A2DP_BITS_PER_SAMPLE_KEY;
    }

    @Override
    protected String[] getListValues() {
        return mContext.getResources().getStringArray(
                R.array.bluetooth_a2dp_codec_bits_per_sample_values);
    }

    @Override
    protected String[] getListSummaries() {
        return mContext.getResources().getStringArray(
                R.array.bluetooth_a2dp_codec_bits_per_sample_summaries);
    }

    @Override
    protected int getDefaultIndex() {
        return DEFAULT_INDEX;
    }

    @Override
    protected void writeConfigurationValues(Object newValue) {
        final int index = mPreference.findIndexOfValue(newValue.toString());
        int bitsPerSampleValue = BluetoothCodecConfig.BITS_PER_SAMPLE_NONE; // default
        switch (index) {
            case 0:
                // Reset to default
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
    protected int getCurrentA2dpSettingIndex(BluetoothCodecConfig config) {
        final int bitsPerSample = config.getBitsPerSample();
        int index = DEFAULT_INDEX;
        switch (bitsPerSample) {
            case BluetoothCodecConfig.BITS_PER_SAMPLE_16:
                index = 1;
                break;
            case BluetoothCodecConfig.BITS_PER_SAMPLE_24:
                index = 2;
                break;
            case BluetoothCodecConfig.BITS_PER_SAMPLE_32:
                index = 3;
                break;
            case BluetoothCodecConfig.BITS_PER_SAMPLE_NONE:
            default:
                break;
        }
        return index;
    }
}
