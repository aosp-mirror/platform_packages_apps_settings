/*
 * Copyright (C) 2018 The Android Open Source Project
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


public class CallVolumePreferenceController extends VolumeSeekBarPreferenceController {

    private AudioManager mAudioManager;

    public CallVolumePreferenceController(Context context, String key) {
        super(context, key);
        mAudioManager = context.getSystemService(AudioManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_call_volume)
                && !mHelper.isSingleVolume() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "call_volume");
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
    public int getAudioStream() {
        if (mAudioManager.isBluetoothScoOn()) {
            return AudioManager.STREAM_BLUETOOTH_SCO;
        }
        return AudioManager.STREAM_VOICE_CALL;
    }

    @Override
    public int getMuteIcon() {
        // User will not be allowed to fully mute the call volume, use original
        // icon for mute icon.
        return R.drawable.ic_local_phone_24_lib;
    }

}
