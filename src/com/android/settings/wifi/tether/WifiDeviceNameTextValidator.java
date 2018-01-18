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

package com.android.settings.wifi.tether;

import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settings.wifi.WifiUtils;

/**
 * Validates a text field for a valid Wi-Fi SSID name.
 */
public class WifiDeviceNameTextValidator implements ValidatedEditTextPreference.Validator {
    @Override
    public boolean isTextValid(String value) {
        return !WifiUtils.isSSIDTooLong(value) && !WifiUtils.isSSIDTooShort(value);
    }
}
