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

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.Utils;
import com.android.settings.notification.VolumeSeekBarPreference.Callback;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class NotificationVolumePreferenceController extends
    RingVolumePreferenceController {

    private static final String KEY_NOTIFICATION_VOLUME = "notification_volume";
    private AudioHelper mHelper;

    public NotificationVolumePreferenceController(Context context, Callback callback,
        Lifecycle lifecycle) {
        this(context, callback, lifecycle, new AudioHelper(context));
    }

    @VisibleForTesting
    NotificationVolumePreferenceController(Context context,
        Callback callback, Lifecycle lifecycle, AudioHelper helper) {
        super(context, callback, lifecycle);
        mHelper = helper;
    }


    @Override
    public boolean isAvailable() {
        return !Utils.isVoiceCapable(mContext) && !mHelper.isSingleVolume();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_NOTIFICATION_VOLUME;
    }

    @Override
    public int getAudioStream() {
        return AudioManager.STREAM_NOTIFICATION;
    }

    @Override
    public int getMuteIcon() {
        return com.android.internal.R.drawable.ic_audio_ring_notif_mute;
    }

}
