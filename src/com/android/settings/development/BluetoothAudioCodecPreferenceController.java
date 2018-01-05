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

public class BluetoothAudioCodecPreferenceController extends
        AbstractBluetoothA2dpPreferenceController {

    private static final int DEFAULT_INDEX = 0;
    private static final String BLUETOOTH_SELECT_A2DP_CODEC_KEY = "bluetooth_select_a2dp_codec";

    public BluetoothAudioCodecPreferenceController(Context context, Lifecycle lifecycle,
            BluetoothA2dpConfigStore store) {
        super(context, lifecycle, store);
    }

    @Override
    public String getPreferenceKey() {
        return BLUETOOTH_SELECT_A2DP_CODEC_KEY;
    }

    @Override
    protected String[] getListValues() {
        return mContext.getResources().getStringArray(
                R.array.bluetooth_a2dp_codec_values);
    }

    @Override
    protected String[] getListSummaries() {
        return mContext.getResources().getStringArray(
                R.array.bluetooth_a2dp_codec_summaries);
    }

    @Override
    protected int getDefaultIndex() {
        return DEFAULT_INDEX;
    }

    @Override
    protected void writeConfigurationValues(Object newValue) {
        final int index = mPreference.findIndexOfValue(newValue.toString());
        int codecTypeValue = BluetoothCodecConfig.SAMPLE_RATE_NONE; // default
        int codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT;
        switch (index) {
            case 0:
                // Reset the priority of the current codec to default
                final String oldValue = mPreference.getValue();
                switch (mPreference.findIndexOfValue(oldValue)) {
                    case 0:
                        break;      // No current codec
                    case 1:
                        codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC;
                        break;
                    case 2:
                        codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC;
                        break;
                    case 3:
                        codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX;
                        break;
                    case 4:
                        codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD;
                        break;
                    case 5:
                        codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC;
                        break;
                    default:
                        break;
                }
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
            case 6:
                synchronized (mBluetoothA2dpConfigStore) {
                    if (mBluetoothA2dp != null) {
                        mBluetoothA2dp.enableOptionalCodecs(null); // Use current active device
                    }
                }
                return;
            case 7:
                synchronized (mBluetoothA2dpConfigStore) {
                    if (mBluetoothA2dp != null) {
                        mBluetoothA2dp.disableOptionalCodecs(null); // Use current active device
                    }
                }
                return;
            default:
                break;
        }
        mBluetoothA2dpConfigStore.setCodecType(codecTypeValue);
        mBluetoothA2dpConfigStore.setCodecPriority(codecPriorityValue);
    }

    @Override
    protected int getCurrentA2dpSettingIndex(BluetoothCodecConfig config) {
        final int codecType = config.getCodecType();
        int index = DEFAULT_INDEX;
        switch (codecType) {
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
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID:
            default:
                break;
        }
        return index;
    }
}
