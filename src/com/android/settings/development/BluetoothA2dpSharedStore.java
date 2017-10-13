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
public class BluetoothA2dpSharedStore {

    // init default values
    private static int sCodecType = BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID;
    private static int sCodecPriority = BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT;
    private static int sSampleRate = BluetoothCodecConfig.SAMPLE_RATE_NONE;
    private static int sBitsPerSample = BluetoothCodecConfig.BITS_PER_SAMPLE_NONE;
    private static int sChannelMode = BluetoothCodecConfig.CHANNEL_MODE_NONE;
    private static long sCodecSpecific1Value = 0;
    private static long sCodecSpecific2Value = 0;
    private static long sCodecSpecific3Value = 0;
    private static long sCodecSpecific4Value = 0;

    public static int getCodecType() {
        return sCodecType;
    }

    public static int getCodecPriority() {
        return sCodecPriority;
    }

    public static int getSampleRate() {
        return sSampleRate;
    }

    public static int getBitsPerSample() {
        return sBitsPerSample;
    }

    public static int getChannelMode() {
        return sChannelMode;
    }

    public static long getCodecSpecific1Value() {
        return sCodecSpecific1Value;
    }

    public static long getCodecSpecific2Value() {
        return sCodecSpecific2Value;
    }

    public static long getCodecSpecific3Value() {
        return sCodecSpecific3Value;
    }

    public static long getCodecSpecific4Value() {
        return sCodecSpecific4Value;
    }

    public static void setCodecType(int codecType) {
        sCodecType = codecType;
    }

    public static void setCodecPriority(int codecPriority) {
        sCodecPriority = codecPriority;
    }

    public static void setSampleRate(int sampleRate) {
        sSampleRate = sampleRate;
    }

    public static void setBitsPerSample(int bitsPerSample) {
        sBitsPerSample = bitsPerSample;
    }

    public static void setChannelMode(int channelMode) {
        sChannelMode = channelMode;
    }

    public static void setCodecSpecific1Value(int codecSpecific1Value) {
        sCodecSpecific1Value = codecSpecific1Value;
    }

    public static void setCodecSpecific2Value(int codecSpecific2Value) {
        sCodecSpecific2Value = codecSpecific2Value;
    }

    public static void setCodecSpecific3Value(int codecSpecific3Value) {
        sCodecSpecific3Value = codecSpecific3Value;
    }

    public static void setCodecSpecific4Value(int codecSpecific4Value) {
        sCodecSpecific4Value = codecSpecific4Value;
    }
}
