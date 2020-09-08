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
 * Dialog preference controller to set the Bluetooth A2DP config of sample rate
 */
public class BluetoothSampleRateDialogPreferenceController extends
        AbstractBluetoothDialogPreferenceController {

    private static final String KEY = "bluetooth_sample_rate_settings";
    private static final String TAG = "BtSampleRateCtr";

    public BluetoothSampleRateDialogPreferenceController(Context context, Lifecycle lifecycle,
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
        int sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_NONE; // default
        switch (index) {
            case 0:
                final BluetoothCodecConfig currentConfig = getCurrentCodecConfig();
                if (currentConfig != null) {
                    sampleRateValue = getHighestSampleRate(getSelectableByCodecType(
                            currentConfig.getCodecType()));
                }
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
    protected int getCurrentIndexByConfig(BluetoothCodecConfig config) {
        if (config == null) {
            Log.e(TAG, "Unable to get current config index. Config is null.");
        }
        return convertCfgToBtnIndex(config.getSampleRate());
    }

    @Override
    public List<Integer> getSelectableIndex() {
        List<Integer> selectableIndex = new ArrayList<>();
        selectableIndex.add(getDefaultIndex());
        final BluetoothCodecConfig currentConfig = getCurrentCodecConfig();
        if (currentConfig != null) {
            final int configs =
                    getSelectableByCodecType(currentConfig.getCodecType()).getSampleRate();
            for (int sampleRate : SAMPLE_RATES) {
                if ((configs & sampleRate) != 0) {
                    selectableIndex.add(convertCfgToBtnIndex(sampleRate));
                }
            }
        }
        return selectableIndex;
    }

    @VisibleForTesting
    int convertCfgToBtnIndex(int config) {
        int index = getDefaultIndex();
        switch (config) {
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
            default:
                Log.e(TAG, "Unsupported config:" + config);
                break;
        }
        return index;
    }
}
