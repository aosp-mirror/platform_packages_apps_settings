/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import com.android.settings.widget.ValidatedEditTextPreference;

import java.nio.charset.StandardCharsets;

/**
 * Validator for Audio Sharing Name, which should be a UTF-8 encoded string containing a minimum of
 * 4 characters and a maximum of 32 human-readable characters.
 */
public class AudioSharingNameTextValidator implements ValidatedEditTextPreference.Validator {
    private static final int MIN_LENGTH = 4;
    private static final int MAX_LENGTH = 32;

    @Override
    public boolean isTextValid(String value) {
        if (value == null || value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            return false;
        }
        return isValidUTF8(value);
    }

    private static boolean isValidUTF8(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        String reconstructedString = new String(bytes, StandardCharsets.UTF_8);
        return value.equals(reconstructedString);
    }
}
