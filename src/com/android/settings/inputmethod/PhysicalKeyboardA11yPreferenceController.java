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

package com.android.settings.inputmethod;

import static com.android.settings.keyboard.Flags.keyboardAndTouchpadA11yNewPageEnabled;

import android.content.Context;
import android.view.InputDevice;

import com.android.settings.core.BasePreferenceController;

/** Controller that shows and updates the Physical Keyboard a11y preference. */
public class PhysicalKeyboardA11yPreferenceController extends BasePreferenceController {


    public PhysicalKeyboardA11yPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return keyboardAndTouchpadA11yNewPageEnabled()
                && isAnyHardKeyboardsExist() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    private static boolean isAnyHardKeyboardsExist() {
        for (int deviceId : InputDevice.getDeviceIds()) {
            final InputDevice device = InputDevice.getDevice(deviceId);
            if (device != null && !device.isVirtual() && device.isFullKeyboard()) {
                return true;
            }
        }
        return false;
    }
}
