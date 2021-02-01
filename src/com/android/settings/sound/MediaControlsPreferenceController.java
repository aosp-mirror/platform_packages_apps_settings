/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.sound;

import static android.provider.Settings.Secure.MEDIA_CONTROLS_RESUME;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

/**
 * Toggle for media controls settings
 */
public class MediaControlsPreferenceController extends TogglePreferenceController {

    public MediaControlsPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public boolean isChecked() {
        int val = Settings.Secure.getInt(mContext.getContentResolver(), MEDIA_CONTROLS_RESUME, 1);
        return val == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        int val = isChecked ? 1 : 0;
        return Settings.Secure.putInt(mContext.getContentResolver(), MEDIA_CONTROLS_RESUME, val);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
