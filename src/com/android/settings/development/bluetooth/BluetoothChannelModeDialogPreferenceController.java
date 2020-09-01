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
 * Dialog preference controller to set the Bluetooth A2DP config of audio channel mode
 */
public class BluetoothChannelModeDialogPreferenceController extends
        AbstractBluetoothDialogPreferenceController {

    private static final String KEY = "bluetooth_channel_mode_settings";
    private static final String TAG = "BtChannelModeCtr";

    public BluetoothChannelModeDialogPreferenceController(Context context, Lifecycle lifecycle,
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
        int channelModeValue = BluetoothCodecConfig.CHANNEL_MODE_NONE; // default
        switch (index) {
            case 0:
                final BluetoothCodecConfig currentConfig = getCurrentCodecConfig();
                if (currentConfig != null) {
                    channelModeValue = getHighestChannelMode(getSelectableByCodecType(
                            currentConfig.getCodecType()));
                }
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
    protected int getCurrentIndexByConfig(BluetoothCodecConfig config) {
        if (config == null) {
            Log.e(TAG, "Unable to get current config index. Config is null.");
        }
        return convertCfgToBtnIndex(config.getChannelMode());
    }

    @Override
    public List<Integer> getSelectableIndex() {
        List<Integer> selectableIndex = new ArrayList<>();
        selectableIndex.add(getDefaultIndex());
        final BluetoothCodecConfig currentConfig = getCurrentCodecConfig();
        if (currentConfig != null) {
            final int configs =
                    getSelectableByCodecType(currentConfig.getCodecType()).getChannelMode();
            for (int i = 0; i < CHANNEL_MODES.length; i++) {
                if ((configs & CHANNEL_MODES[i]) != 0) {
                    selectableIndex.add(convertCfgToBtnIndex(CHANNEL_MODES[i]));
                }
            }
        }
        return selectableIndex;
    }

    @VisibleForTesting
    int convertCfgToBtnIndex(int config) {
        int index = getDefaultIndex();
        switch (config) {
            case BluetoothCodecConfig.CHANNEL_MODE_MONO:
                index = 1;
                break;
            case BluetoothCodecConfig.CHANNEL_MODE_STEREO:
                index = 2;
                break;
            default:
                Log.e(TAG, "Unsupported config:" + config);
                break;
        }
        return index;
    }
}
