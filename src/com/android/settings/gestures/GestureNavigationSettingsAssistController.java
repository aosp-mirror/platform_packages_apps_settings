/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.gestures;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

/**
 * Configures behaviour of corner swipe to invoke assistant app gesture.
 */
public class GestureNavigationSettingsAssistController extends TogglePreferenceController {

    // This value is based on SystemUI/src/com/android/systemui/navigationbar/NavigationBar.java
    // behaviour. We assume that the gestures are enabled by default.
    private static final int ASSIST_TOUCH_GESTURE_DEFAULT_VALUE = 1;

    public GestureNavigationSettingsAssistController(Context context, String key) {
        super(context, key);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ASSIST_TOUCH_GESTURE_ENABLED, ASSIST_TOUCH_GESTURE_DEFAULT_VALUE)
                == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ASSIST_TOUCH_GESTURE_ENABLED, isChecked ? 1 : 0);
    }

    @Override
    public int getAvailabilityStatus() {
        return SystemNavigationPreferenceController.isGestureAvailable(mContext) ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }
}
