/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.notification;

import android.content.Context;
import android.media.AudioManager;
import android.text.TextUtils;

import com.android.settings.R;

public class AlarmVolumePreferenceController extends VolumeSeekBarPreferenceController {

    private static final String KEY_ALARM_VOLUME = "alarm_volume";

    public AlarmVolumePreferenceController(Context context) {
        super(context, KEY_ALARM_VOLUME);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_alarm_volume)
                && !mHelper.isSingleVolume() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "alarm_volume");
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ALARM_VOLUME;
    }

    @Override
    public int getAudioStream() {
        return AudioManager.STREAM_ALARM;
    }

    @Override
    public int getMuteIcon() {
        return com.android.internal.R.drawable.ic_audio_alarm_mute;
    }
}
