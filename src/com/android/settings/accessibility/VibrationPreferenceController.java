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
import android.os.Vibrator;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class VibrationPreferenceController extends BasePreferenceController {

    private final Vibrator mVibrator;

    public VibrationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mVibrator = mContext.getSystemService(Vibrator.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        int ringIntensity = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.RING_VIBRATION_INTENSITY,
                mVibrator.getDefaultRingVibrationIntensity());
        if (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.VIBRATE_WHEN_RINGING, 0) == 0
                && !AccessibilitySettings.isRampingRingerEnabled(mContext)) {
            ringIntensity = Vibrator.VIBRATION_INTENSITY_OFF;
        }
        final CharSequence ringIntensityString =
                VibrationIntensityPreferenceController.getIntensityString(mContext, ringIntensity);

        final int notificationIntensity = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_VIBRATION_INTENSITY,
                mVibrator.getDefaultNotificationVibrationIntensity());
        final CharSequence notificationIntensityString =
                VibrationIntensityPreferenceController.getIntensityString(mContext,
                        notificationIntensity);

        int touchIntensity = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_INTENSITY,
                mVibrator.getDefaultHapticFeedbackIntensity());
        if (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) == 0) {
            touchIntensity = Vibrator.VIBRATION_INTENSITY_OFF;
        }
        final CharSequence touchIntensityString =
                VibrationIntensityPreferenceController.getIntensityString(mContext, touchIntensity);

        if (ringIntensity == touchIntensity && ringIntensity == notificationIntensity) {
            return ringIntensityString;
        } else {
            return mContext.getString(R.string.accessibility_vibration_summary, ringIntensityString,
                    notificationIntensityString, touchIntensityString);
        }
    }
}
