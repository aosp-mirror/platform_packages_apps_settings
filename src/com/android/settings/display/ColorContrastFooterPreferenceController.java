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

package com.android.settings.display;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityFooterPreferenceController;

/** Preference controller for footer in color contrast page. */
public class ColorContrastFooterPreferenceController extends
        AccessibilityFooterPreferenceController {
    public ColorContrastFooterPreferenceController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
    }

    @Override
    protected String getIntroductionTitle() {
        return mContext.getString(R.string.color_contrast_about_title);
    }
}
