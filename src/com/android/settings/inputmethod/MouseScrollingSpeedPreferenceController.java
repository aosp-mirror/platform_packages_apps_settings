/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.SliderPreferenceController;
import com.android.settings.widget.SeekBarPreference;

public class MouseScrollingSpeedPreferenceController extends SliderPreferenceController {

    public MouseScrollingSpeedPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        SeekBarPreference preference = screen.findPreference(getPreferenceKey());
        preference.setMax(getMax());
        preference.setMin(getMin());
        preference.setProgress(getSliderPosition());
        updateState(preference);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!InputSettings.isMouseScrollingAccelerationFeatureFlagEnabled()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public boolean setSliderPosition(int position) {
        if (position < getMin() || position > getMax()) {
            return false;
        }
        InputSettings.setMouseScrollingSpeed(mContext, position);

        return true;
    }

    @Override
    public int getSliderPosition() {
        return InputSettings.getMouseScrollingSpeed(mContext);
    }

    @Override
    public int getMin() {
        return InputSettings.MIN_MOUSE_SCROLLING_SPEED;
    }

    @Override
    public int getMax() {
        return InputSettings.MAX_MOUSE_SCROLLING_SPEED;
    }
}
