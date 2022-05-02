/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.privacy;

import android.content.Context;

import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.TogglePreferenceController;
import com.android.settings.R;

import static android.provider.Settings.Secure.DISABLE_SCREENSHOT_TIMESTAMP_EXIF;
import static android.provider.Settings.Secure.ENABLE_SCREENSHOT_TIMESTAMP_EXIF;

/**
 * Controller for preference to toggle whether screenshot EXIF timestamp should be added.
 */
public class ScreenshotTimestampExifPreferenceController extends TogglePreferenceController {
    public ScreenshotTimestampExifPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SCREENSHOT_TIMESTAMP_EXIF, DISABLE_SCREENSHOT_TIMESTAMP_EXIF)
                == ENABLE_SCREENSHOT_TIMESTAMP_EXIF;
    }

    @Override
    public boolean setChecked(boolean checked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SCREENSHOT_TIMESTAMP_EXIF, checked ?
                ENABLE_SCREENSHOT_TIMESTAMP_EXIF : DISABLE_SCREENSHOT_TIMESTAMP_EXIF);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_privacy;
    }
}