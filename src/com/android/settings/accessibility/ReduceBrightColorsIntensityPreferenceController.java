/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.hardware.display.ColorDisplayManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.SliderPreferenceController;
import com.android.settings.widget.SeekBarPreference;

/** PreferenceController for feature intensity. */
public class ReduceBrightColorsIntensityPreferenceController extends SliderPreferenceController {

    private static final int INVERSE_PERCENTAGE_BASE = 100;
    private final ColorDisplayManager mColorDisplayManager;

    public ReduceBrightColorsIntensityPreferenceController(Context context, String key) {
        super(context, key);
        mColorDisplayManager = context.getSystemService(ColorDisplayManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!ColorDisplayManager.isReduceBrightColorsAvailable(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (!mColorDisplayManager.isReduceBrightColorsActivated()) {
            return DISABLED_DEPENDENT_SETTING;
        }
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final SeekBarPreference preference = screen.findPreference(getPreferenceKey());
        preference.setContinuousUpdates(true);
        preference.setMax(getMax());
        preference.setMin(getMin());
        preference.setHapticFeedbackMode(SeekBarPreference.HAPTIC_FEEDBACK_MODE_ON_ENDS);
        updateState(preference);
    }


    @Override
    public final void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(mColorDisplayManager.isReduceBrightColorsActivated());
    }

    @Override
    public int getSliderPosition() {
        return INVERSE_PERCENTAGE_BASE - mColorDisplayManager.getReduceBrightColorsStrength();
    }

    @Override
    public boolean setSliderPosition(int position) {
        return mColorDisplayManager.setReduceBrightColorsStrength(
                INVERSE_PERCENTAGE_BASE - position);
    }

    @Override
    public int getMax() {
        return INVERSE_PERCENTAGE_BASE
                - ColorDisplayManager.getMinimumReduceBrightColorsStrength(mContext);
    }

    @Override
    public int getMin() {
        return INVERSE_PERCENTAGE_BASE
                - ColorDisplayManager.getMaximumReduceBrightColorsStrength(mContext);
    }
}
