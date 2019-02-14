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
 * limitations under the License
 */

package com.android.settings.biometrics.face;

import android.content.Context;

import androidx.preference.PreferenceScreen;

import com.android.settings.widget.VideoPreference;
import com.android.settings.widget.VideoPreferenceController;

/**
 * Preference controller for the video for face settings.
 */
public class FaceSettingsVideoPreferenceController extends VideoPreferenceController {

    private static final String KEY_VIDEO = "security_settings_face_video";

    private VideoPreference mVideoPreference;

    public FaceSettingsVideoPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    public FaceSettingsVideoPreferenceController(Context context) {
        this(context, KEY_VIDEO);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mVideoPreference = screen.findPreference(KEY_VIDEO);
        mVideoPreference.onViewVisible(false /* paused */);
    }
}
