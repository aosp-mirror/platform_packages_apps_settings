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

/**
 * Utility class for storing current Bluetooth A2DP profile values
 */
public class BluetoothA2dpConfigStore {

    // init default values
    private int mCodecType = BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID;
    private int mCodecPriority = BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT;
    private int mSampleRate = BluetoothCodecConfig.SAMPLE_RATE_NONE;
    private int mBitsPerSample = BluetoothCodecConfig.BITS_PER_SAMPLE_NONE;
    private int mChannelMode = BluetoothCodecConfig.CHANNEL_MODE_NONE;
    private long mCodecSpecific1Value;
    private long mCodecSpecific2Value;
    private long mCodecSpecific3Value;
    private long mCodecSpecific4Value;

    public void setCodecType(int codecType) {
        mCodecType = codecType;
    }

    public void setCodecPriority(int codecPriority) {
        mCodecPriority = codecPriority;
    }

    public void setSampleRate(int sampleRate) {
        mSampleRate = sampleRate;
    }

    public void setBitsPerSample(int bitsPerSample) {
        mBitsPerSample = bitsPerSample;
    }

    public void setChannelMode(int channelMode) {
        mChannelMode = channelMode;
    }

    public void setCodecSpecific1Value(long codecSpecific1Value) {
        mCodecSpecific1Value = codecSpecific1Value;
    }

    public void setCodecSpecific2Value(int codecSpecific2Value) {
        mCodecSpecific2Value = codecSpecific2Value;
    }

    public void setCodecSpecific3Value(int codecSpecific3Value) {
        mCodecSpecific3Value = codecSpecific3Value;
    }

    public void setCodecSpecific4Value(int codecSpecific4Value) {
        mCodecSpecific4Value = codecSpecific4Value;
    }

    public BluetoothCodecConfig createCodecConfig() {
        return new BluetoothCodecConfig(mCodecType, mCodecPriority,
                mSampleRate, mBitsPerSample,
                mChannelMode, mCodecSpecific1Value,
                mCodecSpecific2Value, mCodecSpecific3Value,
                mCodecSpecific4Value);
    }
}
