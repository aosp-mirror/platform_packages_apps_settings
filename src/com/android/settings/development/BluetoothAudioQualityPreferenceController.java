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

public class BluetoothAudioQualityPreferenceController extends
        AbstractBluetoothA2dpPreferenceController {

    private static final int DEFAULT_INDEX = 3;
    private static final String BLUETOOTH_SELECT_A2DP_LDAC_PLAYBACK_QUALITY_KEY =
            "bluetooth_select_a2dp_ldac_playback_quality";

    public BluetoothAudioQualityPreferenceController(Context context, Lifecycle lifecycle,
            BluetoothA2dpConfigStore store) {
        super(context, lifecycle, store);
    }

    @Override
    public String getPreferenceKey() {
        return BLUETOOTH_SELECT_A2DP_LDAC_PLAYBACK_QUALITY_KEY;
    }

    @Override
    protected String[] getListValues() {
        return mContext.getResources().getStringArray(
                R.array.bluetooth_a2dp_codec_ldac_playback_quality_values);
    }

    @Override
    protected String[] getListSummaries() {
        return mContext.getResources().getStringArray(
                R.array.bluetooth_a2dp_codec_ldac_playback_quality_summaries);
    }

    @Override
    protected int getDefaultIndex() {
        return DEFAULT_INDEX;
    }

    @Override
    protected void writeConfigurationValues(Object newValue) {
        final int index = mPreference.findIndexOfValue(newValue.toString());
        int codecSpecific1Value = 0; // default
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
    protected int getCurrentA2dpSettingIndex(BluetoothCodecConfig config) {
        // The actual values are 0, 1, 2 - those are extracted
        // as mod-10 remainders of a larger value.
        // The reason is because within BluetoothCodecConfig we cannot use
        // a codec-specific value of zero.
        int index = (int) config.getCodecSpecific1();
        if (index > 0) {
            index %= 10;
        } else {
            index = DEFAULT_INDEX;
        }
        switch (index) {
            case 0:
            case 1:
            case 2:
            case 3:
                break;
            default:
                index = DEFAULT_INDEX;
                break;
        }
        return index;
    }
}
