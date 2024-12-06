/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.content.Context;
import android.hardware.input.InputSettings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class TouchpadSystemGesturesPreferenceController extends TogglePreferenceController {

    public TouchpadSystemGesturesPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return InputSettings.isTouchpadSystemGestureDisableFeatureFlagEnabled()
                && InputPeripheralsSettingsUtils.isTouchpad() ? AVAILABLE
                                                              : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return InputSettings.useTouchpadSystemGestures(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        InputSettings.setTouchpadSystemGesturesEnabled(mContext, isChecked);
        // TODO(b/353947750): add a metric for when the setting changes.
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }
}
