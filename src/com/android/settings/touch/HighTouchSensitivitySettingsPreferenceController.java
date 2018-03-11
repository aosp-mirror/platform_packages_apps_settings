/*
 * Copyright (C) 2020 PixelExperience
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
 * limitations under the License
 */

package com.android.settings.custom.touch;

import android.content.Context;
import com.android.internal.axiom.hardware.LineageHardwareManager;
import com.android.settings.core.BasePreferenceController;

public class HighTouchSensitivitySettingsPreferenceController extends BasePreferenceController {

    public static final String KEY = "high_touch_sensitivity_enable";

    private final LineageHardwareManager mHardware;

    public HighTouchSensitivitySettingsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mHardware = LineageHardwareManager.getInstance(context);
    }

    public HighTouchSensitivitySettingsPreferenceController(Context context) {
        this(context, KEY);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mHardware.isSupported(LineageHardwareManager.FEATURE_HIGH_TOUCH_SENSITIVITY)){
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }
}
