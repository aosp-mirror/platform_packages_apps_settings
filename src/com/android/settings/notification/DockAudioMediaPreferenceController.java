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

package com.android.settings.notification;

import static com.android.settings.notification.SettingPref.TYPE_GLOBAL;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings.Global;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class DockAudioMediaPreferenceController extends SettingPrefController {

    private static final String KEY_DOCK_AUDIO_MEDIA = "dock_audio_media";

    private static final int DOCK_AUDIO_MEDIA_DISABLED = 0;
    private static final int DOCK_AUDIO_MEDIA_ENABLED = 1;
    private static final int DEFAULT_DOCK_AUDIO_MEDIA = DOCK_AUDIO_MEDIA_DISABLED;

    public DockAudioMediaPreferenceController(Context context, SettingsPreferenceFragment parent,
            Lifecycle lifecycle) {
        super(context, parent, lifecycle);
        mPreference = new SettingPref(
            TYPE_GLOBAL, KEY_DOCK_AUDIO_MEDIA, Global.DOCK_AUDIO_MEDIA_ENABLED,
            DEFAULT_DOCK_AUDIO_MEDIA, DOCK_AUDIO_MEDIA_DISABLED, DOCK_AUDIO_MEDIA_ENABLED) {
            @Override
            public boolean isApplicable(Context context) {
                return context.getResources().getBoolean(
                    com.android.settings.R.bool.has_dock_settings);
            }

            @Override
            protected String getCaption(Resources res, int value) {
                switch(value) {
                    case DOCK_AUDIO_MEDIA_DISABLED:
                        return res.getString(
                            com.android.settings.R.string.dock_audio_media_disabled);
                    case DOCK_AUDIO_MEDIA_ENABLED:
                        return res.getString(
                            com.android.settings.R.string.dock_audio_media_enabled);
                    default:
                        throw new IllegalArgumentException();
                }
            }
        };
    }
}
