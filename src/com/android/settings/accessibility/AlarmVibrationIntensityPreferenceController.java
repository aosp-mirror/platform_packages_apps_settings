/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.os.VibrationAttributes;
import android.provider.Settings;

/** Preference controller for alarm vibration intensity */
public class AlarmVibrationIntensityPreferenceController
        extends VibrationIntensityPreferenceController {

    /** General configuration for alarm vibration intensity settings. */
    public static final class AlarmVibrationPreferenceConfig extends VibrationPreferenceConfig {

        public AlarmVibrationPreferenceConfig(Context context) {
            super(context, Settings.System.ALARM_VIBRATION_INTENSITY,
                    VibrationAttributes.USAGE_ALARM);
        }
    }

    public AlarmVibrationIntensityPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey, new AlarmVibrationPreferenceConfig(context));
    }

    protected AlarmVibrationIntensityPreferenceController(Context context, String preferenceKey,
            int supportedIntensityLevels) {
        super(context, preferenceKey, new AlarmVibrationPreferenceConfig(context),
                supportedIntensityLevels);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
