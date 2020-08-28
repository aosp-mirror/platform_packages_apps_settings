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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RadioGroup;

import com.android.settings.R;

/**
 * Dialog preference to set the Bluetooth A2DP config of LDAC quality
 */
public class BluetoothQualityDialogPreference extends BaseBluetoothDialogPreference implements
        RadioGroup.OnCheckedChangeListener {

    public BluetoothQualityDialogPreference(Context context) {
        super(context);
        initialize(context);
    }

    public BluetoothQualityDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public BluetoothQualityDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    public BluetoothQualityDialogPreference(Context context, AttributeSet attrs, int defStyleAttr,
                                            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    @Override
    protected int getRadioButtonGroupId() {
        return R.id.bluetooth_audio_quality_radio_group;
    }

    @Override
    protected int getDefaultIndex() {
        return 3;
    }

    private void initialize(Context context) {
        mRadioButtonIds.add(R.id.bluetooth_audio_quality_default);
        mRadioButtonIds.add(R.id.bluetooth_audio_quality_optimized_quality);
        mRadioButtonIds.add(R.id.bluetooth_audio_quality_optimized_connection);
        mRadioButtonIds.add(R.id.bluetooth_audio_quality_best_effort);
        String[] stringArray = context.getResources().getStringArray(
                R.array.bluetooth_a2dp_codec_ldac_playback_quality_titles);
        for (int i = 0; i < stringArray.length; i++) {
            mRadioButtonStrings.add(stringArray[i]);
        }
        stringArray = context.getResources().getStringArray(
                R.array.bluetooth_a2dp_codec_ldac_playback_quality_summaries);
        for (int i = 0; i < stringArray.length; i++) {
            mSummaryStrings.add(stringArray[i]);
        }
    }
}
