/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.hardware.input.InputSettings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * A toggle preference controller for keyboard sticky key.
 */
public class KeyboardStickyKeyPreferenceController extends TogglePreferenceController {

    static final String PREF_KEY = "toggle_keyboard_sticky_keys";

    public KeyboardStickyKeyPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return InputSettings.isAccessibilityStickyKeysFeatureEnabled()
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return InputSettings.isAccessibilityStickyKeysEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        InputSettings.setAccessibilityStickyKeysEnabled(mContext, isChecked);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }
}
