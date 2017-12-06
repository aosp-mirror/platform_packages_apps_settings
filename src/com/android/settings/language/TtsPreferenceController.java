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

package com.android.settings.language;

import android.content.Context;
import android.speech.tts.TtsEngines;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class TtsPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_VOICE_CATEGORY = "voice_category";
    private static final String KEY_TTS_SETTINGS = "tts_settings_summary";

    private final TtsEngines mTtsEngines;

    public TtsPreferenceController(Context context, TtsEngines ttsEngines) {
        super(context);
        mTtsEngines = ttsEngines;
    }

    @Override
    public boolean isAvailable() {
        return !mTtsEngines.getEngines().isEmpty();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TTS_SETTINGS;
    }
}
