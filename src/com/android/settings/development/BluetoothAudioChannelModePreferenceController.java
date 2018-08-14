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

public class BluetoothAudioChannelModePreferenceController extends
        AbstractBluetoothA2dpPreferenceController {

    private static final int DEFAULT_INDEX = 0;
    private static final String BLUETOOTH_SELECT_A2DP_CHANNEL_MODE_KEY =
            "bluetooth_select_a2dp_channel_mode";

    public BluetoothAudioChannelModePreferenceController(Context context, Lifecycle lifecycle,
            BluetoothA2dpConfigStore store) {
        super(context, lifecycle, store);
    }

    @Override
    public String getPreferenceKey() {
        return BLUETOOTH_SELECT_A2DP_CHANNEL_MODE_KEY;
    }

    @Override
    protected String[] getListValues() {
        return mContext.getResources().getStringArray(
                R.array.bluetooth_a2dp_codec_channel_mode_values);
    }

    @Override
    protected String[] getListSummaries() {
        return mContext.getResources().getStringArray(
                R.array.bluetooth_a2dp_codec_channel_mode_summaries);
    }

    @Override
    protected int getDefaultIndex() {
        return DEFAULT_INDEX;
    }

    @Override
    protected void writeConfigurationValues(Object newValue) {
        final int index = mPreference.findIndexOfValue(newValue.toString());
        int channelModeValue = BluetoothCodecConfig.CHANNEL_MODE_NONE; // default
        switch (index) {
            case 0:
                // Reset to default
                break;
            case 1:
                channelModeValue = BluetoothCodecConfig.CHANNEL_MODE_MONO;
                break;
            case 2:
                channelModeValue = BluetoothCodecConfig.CHANNEL_MODE_STEREO;
                break;
            default:
                break;
        }
        mBluetoothA2dpConfigStore.setChannelMode(channelModeValue);
    }

    @Override
    protected int getCurrentA2dpSettingIndex(BluetoothCodecConfig config) {
        final int channelMode = config.getChannelMode();
        int index = DEFAULT_INDEX;
        switch (channelMode) {
            case BluetoothCodecConfig.CHANNEL_MODE_MONO:
                index = 1;
                break;
            case BluetoothCodecConfig.CHANNEL_MODE_STEREO:
                index = 2;
                break;
            case BluetoothCodecConfig.CHANNEL_MODE_NONE:
            default:
                break;
        }
        return index;
    }
}
