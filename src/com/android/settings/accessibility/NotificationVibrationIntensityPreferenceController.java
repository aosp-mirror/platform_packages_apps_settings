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

import androidx.annotation.VisibleForTesting;

public class NotificationVibrationIntensityPreferenceController
        extends VibrationIntensityPreferenceController {

    @VisibleForTesting
    static final String PREF_KEY = "notification_vibration_preference_screen";

    public NotificationVibrationIntensityPreferenceController(Context context) {
        super(context, PREF_KEY, Settings.System.NOTIFICATION_VIBRATION_INTENSITY, "");
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    protected int getDefaultIntensity() {
        return mVibrator.getDefaultNotificationVibrationIntensity();
    }
}
