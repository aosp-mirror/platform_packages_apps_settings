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
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog preference controller to set the Bluetooth A2DP config of LDAC quality
 */
public class BluetoothQualityDialogPreferenceController extends
        AbstractBluetoothDialogPreferenceController {

    private static final String KEY = "bluetooth_a2dp_ldac_playback_quality";
    private static final String TAG = "BtQualityCtr";

    public BluetoothQualityDialogPreferenceController(Context context, Lifecycle lifecycle,
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
        long codecSpecific1Value = 0; // default
        switch (index) {
            case 0:
            case 1:
            case 2:
            case 3:
                codecSpecific1Value = 1000 + index;
                break;
            default:
                break;
        }
        mBluetoothA2dpConfigStore.setCodecSpecific1Value(codecSpecific1Value);
    }

    @Override
    protected int getCurrentIndexByConfig(BluetoothCodecConfig config) {
        if (config == null) {
            Log.e(TAG, "Unable to get current config index. Config is null.");
        }
        return convertCfgToBtnIndex((int) config.getCodecSpecific1());
    }

    @Override
    public List<Integer> getSelectableIndex() {
        List<Integer> selectableIndex = new ArrayList<>();
        // All four items of LDAC are available.
        for (int i = 0; i < 4; i++) {
            selectableIndex.add(i);
        }
        return selectableIndex;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        // Enable preference when current codec type is LDAC. For other cases, disable it.
        final BluetoothCodecConfig currentConfig = getCurrentCodecConfig();
        if (currentConfig != null
                && currentConfig.getCodecType() == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC) {
            preference.setEnabled(true);
        } else {
            preference.setEnabled(false);
            preference.setSummary("");
        }
    }

    @Override
    public void onHDAudioEnabled(boolean enabled) {
        mPreference.setEnabled(false);
    }

    @VisibleForTesting
    int convertCfgToBtnIndex(int config) {
        int index = config - 1000;
        if (index < 0) {
            index = getDefaultIndex();
        }
        return index;
    }
}
