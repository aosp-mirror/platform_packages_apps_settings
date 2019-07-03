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

import com.android.settings.R;

public class AccessibilityUtil {
    /**
     * Return On/Off string according to the setting which specifies the integer value 1 or 0. This
     * setting is defined in the secure system settings {@link android.provider.Settings.Secure}.
     */
    static CharSequence getSummary(Context context, String settingsSecureKey) {
        final boolean enabled = Settings.Secure.getInt(context.getContentResolver(),
                settingsSecureKey, 0) == 1;
        final int resId = enabled ? R.string.accessibility_feature_state_on
                : R.string.accessibility_feature_state_off;
        return context.getResources().getText(resId);
    }
}
