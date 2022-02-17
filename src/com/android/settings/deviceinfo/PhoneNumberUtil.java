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
package com.android.settings.deviceinfo;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.TtsSpan;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Helper class to detect format of phone number.
 */
public class PhoneNumberUtil {

    /**
     * Convert given text to support phone number talkback.
     * @param text given
     * @return converted text
     */
    public static CharSequence expandByTts(CharSequence text) {
        if ((text == null) || (text.length() <= 0)
            || (!isPhoneNumberDigits(text))) {
            return text;
        }
        Spannable spannable = new SpannableStringBuilder(text);
        TtsSpan span = new TtsSpan.DigitsBuilder(text.toString()).build();
        spannable.setSpan(span, 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    /**
     * Check if given text may contains a phone id related numbers.
     * (ex: phone number, IMEI, ICCID)
     * @param text given
     * @return true when given text is a phone id related number.
     */
    private static boolean isPhoneNumberDigits(CharSequence text) {
        long textLength = (long)text.length();
        return (textLength == text.chars()
                .filter(c -> isPhoneNumberDigit(c)).count());
    }

    private static boolean isPhoneNumberDigit(int c) {
        return ((c >= (int)'0') && (c <= (int)'9'))
            || (c == (int)'-') || (c == (int)'+')
            || (c == (int)'(') || (c == (int)')');
    }
}
