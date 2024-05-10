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
 * limitations under the License.
 */

package com.android.settings.accessibility;

import android.content.Context;
import android.provider.Settings;
import android.view.accessibility.Flags;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/** A toggle preference controller for force invert (force dark). */
public class ToggleForceInvertPreferenceController extends TogglePreferenceController {

    public static final String SETTINGS_KEY =
            Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED;

    @VisibleForTesting
    static final int ON = 1;
    @VisibleForTesting
    static final int OFF = 0;

    public ToggleForceInvertPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(), SETTINGS_KEY, OFF) != OFF;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                SETTINGS_KEY, isChecked ? ON : OFF);
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.forceInvertColor() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }
}
