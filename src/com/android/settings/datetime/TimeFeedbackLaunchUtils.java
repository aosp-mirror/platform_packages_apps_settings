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
package com.android.settings.datetime;

import static android.provider.DeviceConfig.NAMESPACE_SETTINGS_UI;

import android.provider.DeviceConfig;

import com.android.settings.flags.Flags;

/** A class to avoid duplication of launch-control logic for "time feedback" support. */
final class TimeFeedbackLaunchUtils {
    /**
     * A {@link DeviceConfig} flag that influences whether the settings entries related to help and
     * feedback are supported on this device / for this user.
     */
    public static final String KEY_HELP_AND_FEEDBACK_FEATURE_SUPPORTED =
            "time_help_and_feedback_feature_supported";

    private TimeFeedbackLaunchUtils() {}

    static boolean isFeedbackFeatureSupported() {
        // Support is determined according to:
        // 1) A build-time flag to determine release feature availability.
        // 2) A runtime / server-side flag to determine which devices / who gets to see the feature.
        //    This is launch control for limiting the feedback to droidfooding.
        return isFeatureSupportedThisRelease() && isFeatureSupportedOnThisDevice();
    }

    private static boolean isFeatureSupportedThisRelease() {
        return Flags.datetimeFeedback();
    }

    private static boolean isFeatureSupportedOnThisDevice() {
        boolean defaultIsSupported = false;
        return DeviceConfig.getBoolean(
                NAMESPACE_SETTINGS_UI, KEY_HELP_AND_FEEDBACK_FEATURE_SUPPORTED, defaultIsSupported);
    }
}
