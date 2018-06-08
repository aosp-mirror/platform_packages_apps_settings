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

public class BluetoothAudioSampleRatePreferenceController extends
        AbstractBluetoothA2dpPreferenceController {

    private static final int DEFAULT_INDEX = 0;
    private static final String BLUETOOTH_SELECT_A2DP_SAMPLE_RATE_KEY =
            "bluetooth_select_a2dp_sample_rate";

    public BluetoothAudioSampleRatePreferenceController(Context context, Lifecycle lifecycle,
            BluetoothA2dpConfigStore store) {
        super(context, lifecycle, store);
    }

    @Override
    public String getPreferenceKey() {
        return BLUETOOTH_SELECT_A2DP_SAMPLE_RATE_KEY;
    }

    @Override
    protected String[] getListValues() {
        return mContext.getResources().getStringArray(
                R.array.bluetooth_a2dp_codec_sample_rate_values);
    }

    @Override
    protected String[] getListSummaries() {
        return mContext.getResources().getStringArray(
                R.array.bluetooth_a2dp_codec_sample_rate_summaries);
    }

    @Override
    protected int getDefaultIndex() {
        return DEFAULT_INDEX;
    }

    @Override
    protected void writeConfigurationValues(Object newValue) {
        final int index = mPreference.findIndexOfValue(newValue.toString());
        int sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_NONE; // default
        switch (index) {
            case 0:
                sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_NONE;
                break;
            case 1:
                sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_44100;
                break;
            case 2:
                sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_48000;
                break;
            case 3:
                sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_88200;
                break;
            case 4:
                sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_96000;
                break;
            default:
                break;
        }
        mBluetoothA2dpConfigStore.setSampleRate(sampleRateValue);
    }

    @Override
    protected int getCurrentA2dpSettingIndex(BluetoothCodecConfig config) {
        final int sampleRate = config.getSampleRate();
        int index = DEFAULT_INDEX;
        switch (sampleRate) {
            case BluetoothCodecConfig.SAMPLE_RATE_44100:
                index = 1;
                break;
            case BluetoothCodecConfig.SAMPLE_RATE_48000:
                index = 2;
                break;
            case BluetoothCodecConfig.SAMPLE_RATE_88200:
                index = 3;
                break;
            case BluetoothCodecConfig.SAMPLE_RATE_96000:
                index = 4;
                break;
            case BluetoothCodecConfig.SAMPLE_RATE_176400:
            case BluetoothCodecConfig.SAMPLE_RATE_192000:
            case BluetoothCodecConfig.SAMPLE_RATE_NONE:
            default:
                break;
        }
        return index;
    }
}
