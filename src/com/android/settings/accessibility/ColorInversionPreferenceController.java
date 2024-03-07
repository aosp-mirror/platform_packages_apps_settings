/*
 * Copyright (C) 2018 The Android Open Source Project
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

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/** Controller that shows the color inversion summary. */
public class ColorInversionPreferenceController extends BasePreferenceController {

    private static final String DISPLAY_INVERSION_ENABLED =
            Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED;

    public ColorInversionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public CharSequence getSummary() {
        return AccessibilityUtil.getSummary(
                mContext,
                DISPLAY_INVERSION_ENABLED,
                R.string.color_inversion_state_on, R.string.color_inversion_state_off);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
