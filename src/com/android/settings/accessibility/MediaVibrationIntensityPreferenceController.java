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

import com.android.settings.R;

/** Preference controller for am vibration intensity */
public class MediaVibrationIntensityPreferenceController
        extends VibrationIntensityPreferenceController {

    /** General configuration for alarm vibration intensity settings. */
    public static final class MediaVibrationPreferenceConfig extends VibrationPreferenceConfig {

        public MediaVibrationPreferenceConfig(Context context) {
            super(context, Settings.System.MEDIA_VIBRATION_INTENSITY,
                    VibrationAttributes.USAGE_MEDIA);
        }
    }

    public MediaVibrationIntensityPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey, new MediaVibrationPreferenceConfig(context));
    }

    protected MediaVibrationIntensityPreferenceController(Context context, String preferenceKey,
            int supportedIntensityLevels) {
        super(context, preferenceKey, new MediaVibrationPreferenceConfig(context),
                supportedIntensityLevels);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_media_vibration_supported) ?
                AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
