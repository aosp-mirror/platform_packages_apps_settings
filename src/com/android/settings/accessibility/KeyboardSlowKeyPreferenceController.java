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
import com.android.settings.inputmethod.PhysicalKeyboardFragment;

/**
 * A toggle preference controller for keyboard slow key.
 */
public class KeyboardSlowKeyPreferenceController extends TogglePreferenceController {

    static final String PREF_KEY = "toggle_keyboard_slow_keys";

    public KeyboardSlowKeyPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return InputSettings.isAccessibilitySlowKeysFeatureFlagEnabled()
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return InputSettings.isAccessibilitySlowKeysEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        InputSettings.setAccessibilitySlowKeysThreshold(mContext,
                isChecked ? PhysicalKeyboardFragment.SLOW_KEYS_THRESHOLD
                        : 0);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }
}
