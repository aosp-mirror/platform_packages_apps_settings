/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.provider.Settings.Secure.CAMERA_EXTENSIONS_FALLBACK;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Toggle for camera extensions software fallback
 */
public class CameraExtensionsFallbackPreferenceController extends TogglePreferenceController {
    public CameraExtensionsFallbackPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public boolean isChecked() {
        int val = Settings.Secure.getInt(mContext.getContentResolver(),
                CAMERA_EXTENSIONS_FALLBACK, 1);
        return val == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        int val = isChecked ? 1 : 0;
        return Settings.Secure.putInt(mContext.getContentResolver(),
                CAMERA_EXTENSIONS_FALLBACK, val);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isSliceable() {
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed since it's not sliceable
        return NO_RES;
    }
}
