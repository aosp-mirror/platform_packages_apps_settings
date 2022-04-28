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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.Context;
import android.os.VibrationAttributes;
import android.os.Vibrator;
import android.provider.Settings;

/** Preference controller for haptic feedback intensity */
public class HapticFeedbackIntensityPreferenceController
        extends VibrationIntensityPreferenceController {

    /** General configuration for haptic feedback intensity settings. */
    public static final class HapticFeedbackVibrationPreferenceConfig
            extends VibrationPreferenceConfig {

        public HapticFeedbackVibrationPreferenceConfig(Context context) {
            super(context, Settings.System.HAPTIC_FEEDBACK_INTENSITY,
                    VibrationAttributes.USAGE_TOUCH);
        }

        @Override
        public int readIntensity() {
            final int hapticFeedbackEnabled = Settings.System.getInt(mContentResolver,
                    Settings.System.HAPTIC_FEEDBACK_ENABLED, ON);

            if (hapticFeedbackEnabled == OFF) {
                // HAPTIC_FEEDBACK_ENABLED is deprecated but should still be applied if the user has
                // turned it off already.
                return Vibrator.VIBRATION_INTENSITY_OFF;
            }

            return super.readIntensity();
        }

        @Override
        public boolean updateIntensity(int intensity) {
            final boolean success = super.updateIntensity(intensity);
            final boolean isIntensityOff = intensity == Vibrator.VIBRATION_INTENSITY_OFF;

            Settings.System.putInt(mContentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED,
                    isIntensityOff ? OFF : ON);
            // HAPTIC_FEEDBACK_ENABLED is deprecated but should still reflect the intensity setting.

            // HARDWARE_HAPTIC_FEEDBACK_INTENSITY is dependent on this setting, but should not be
            // disabled by it.
            Settings.System.putInt(mContentResolver,
                    Settings.System.HARDWARE_HAPTIC_FEEDBACK_INTENSITY,
                    isIntensityOff ? getDefaultIntensity() : intensity);

            return success;
        }
    }

    public HapticFeedbackIntensityPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey, new HapticFeedbackVibrationPreferenceConfig(context));
    }

    protected HapticFeedbackIntensityPreferenceController(Context context, String preferenceKey,
            int supportedIntensityLevels) {
        super(context, preferenceKey, new HapticFeedbackVibrationPreferenceConfig(context),
                supportedIntensityLevels);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
