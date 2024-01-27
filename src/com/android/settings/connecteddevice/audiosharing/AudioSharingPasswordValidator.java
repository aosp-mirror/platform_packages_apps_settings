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
 * Validator for Audio Sharing Password, which should be a UTF-8 string that has at least 4 octets
 * and should not exceed 16 octets.
 */
public class AudioSharingPasswordValidator implements ValidatedEditTextPreference.Validator {
    private static final int MIN_OCTETS = 4;
    private static final int MAX_OCTETS = 16;

    @Override
    public boolean isTextValid(String value) {
        if (value == null
                || getOctetsCount(value) < MIN_OCTETS
                || getOctetsCount(value) > MAX_OCTETS) {
            return false;
        }

        return isValidUTF8(value);
    }

    private static int getOctetsCount(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private static boolean isValidUTF8(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        String reconstructedString = new String(bytes, StandardCharsets.UTF_8);
        return value.equals(reconstructedString);
    }
}
