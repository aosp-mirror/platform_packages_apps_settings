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
import com.android.settings.notification.VolumeSeekBarPreference.Callback;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class MediaVolumePreferenceController extends
    VolumeSeekBarPreferenceController {

    private static final String KEY_MEDIA_VOLUME = "media_volume";

    public MediaVolumePreferenceController(Context context, Callback callback, Lifecycle lifecycle) {
        super(context, callback, lifecycle);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_MEDIA_VOLUME;
    }

    @Override
    public int getAudioStream() {
        return AudioManager.STREAM_MUSIC;
    }

    @Override
    public int getMuteIcon() {
        return com.android.internal.R.drawable.ic_audio_media_mute;
    }

}
