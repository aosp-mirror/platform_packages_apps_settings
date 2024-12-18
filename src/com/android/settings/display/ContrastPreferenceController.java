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

import static android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_HIGH;
import static android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_MEDIUM;
import static android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_STANDARD;
import static android.app.UiModeManager.ContrastUtils.toContrastLevel;

import android.app.UiModeManager;
import android.content.Context;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.accessibility.Flags;
import com.android.settings.core.BasePreferenceController;

import java.util.Map;

/**
 * Controller for {@link ColorContrastFragment}.
 */
public class ContrastPreferenceController extends BasePreferenceController {

    public ContrastPreferenceController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableColorContrastControl() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        Map<Integer, Integer> mContrastLevelToResId = Map.ofEntries(
                Map.entry(CONTRAST_LEVEL_STANDARD, R.string.contrast_default),
                Map.entry(CONTRAST_LEVEL_MEDIUM, R.string.contrast_medium),
                Map.entry(CONTRAST_LEVEL_HIGH, R.string.contrast_high)
        );

        float contrastLevel = mContext.getSystemService(UiModeManager.class).getContrast();
        return mContext.getString(mContrastLevelToResId.get(toContrastLevel(contrastLevel)));
    }
}
