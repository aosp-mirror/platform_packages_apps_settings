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
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import com.android.settings.core.TogglePreferenceController;

public class DisableAnimationsPreferenceController extends TogglePreferenceController {

    @VisibleForTesting
    static final String ANIMATION_ON_VALUE = "1";
    @VisibleForTesting
    static final String ANIMATION_OFF_VALUE = "0";

    // Settings that should be changed when toggling animations
    @VisibleForTesting
    static final String[] TOGGLE_ANIMATION_TARGETS = {
            Settings.Global.WINDOW_ANIMATION_SCALE, Settings.Global.TRANSITION_ANIMATION_SCALE,
            Settings.Global.ANIMATOR_DURATION_SCALE
    };

    public DisableAnimationsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        boolean allAnimationsDisabled = true;
        for (String animationSetting : TOGGLE_ANIMATION_TARGETS) {
            if (!TextUtils.equals(
                    Settings.Global.getString(mContext.getContentResolver(), animationSetting),
                    ANIMATION_OFF_VALUE)) {
                allAnimationsDisabled = false;
                break;
            }
        }
        return allAnimationsDisabled;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final String newAnimationValue = isChecked ? ANIMATION_OFF_VALUE : ANIMATION_ON_VALUE;
        boolean allAnimationSet = true;
        for (String animationPreference : TOGGLE_ANIMATION_TARGETS) {
            allAnimationSet &= Settings.Global.putString(mContext.getContentResolver(),
                    animationPreference, newAnimationValue);
        }
        return allAnimationSet;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
