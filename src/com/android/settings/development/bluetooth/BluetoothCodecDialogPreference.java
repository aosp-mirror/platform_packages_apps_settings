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
 * Dialog preference to set the Bluetooth A2DP config of codec
 */
public class BluetoothCodecDialogPreference extends BaseBluetoothDialogPreference implements
        RadioGroup.OnCheckedChangeListener {

    public BluetoothCodecDialogPreference(Context context) {
        super(context);
        initialize(context);
    }

    public BluetoothCodecDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public BluetoothCodecDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    public BluetoothCodecDialogPreference(Context context, AttributeSet attrs, int defStyleAttr,
                                          int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    @Override
    protected int getRadioButtonGroupId() {
        return R.id.bluetooth_audio_codec_radio_group;
    }

    private void initialize(Context context) {
        mRadioButtonIds.add(R.id.bluetooth_audio_codec_default);
        mRadioButtonIds.add(R.id.bluetooth_audio_codec_sbc);
        mRadioButtonIds.add(R.id.bluetooth_audio_codec_aac);
        mRadioButtonIds.add(R.id.bluetooth_audio_codec_aptx);
        mRadioButtonIds.add(R.id.bluetooth_audio_codec_aptx_hd);
        mRadioButtonIds.add(R.id.bluetooth_audio_codec_ldac);
        mRadioButtonIds.add(R.id.bluetooth_audio_codec_lc3);
        mRadioButtonIds.add(R.id.bluetooth_audio_codec_opus);
        String[] stringArray = context.getResources().getStringArray(
                com.android.settingslib.R.array.bluetooth_a2dp_codec_titles);
        for (int i = 0; i < stringArray.length; i++) {
            mRadioButtonStrings.add(stringArray[i]);
        }
        stringArray = context.getResources()
                .getStringArray(com.android.settingslib.R.array.bluetooth_a2dp_codec_summaries);
        for (int i = 0; i < stringArray.length; i++) {
            mSummaryStrings.add(stringArray[i]);
        }
    }
}
