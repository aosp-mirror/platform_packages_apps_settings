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

package com.android.settings.utils;

import android.icu.text.ListFormatter;
import android.text.TextUtils;

import java.util.List;
import java.util.Locale;

/**
 * This class implements some common methods to process with locales
 */
public class LocaleUtils {

    /**
     * Returns a character sequence concatenating the items with the localized comma.
     *
     * @param items items to be concatenated
     */
    public static CharSequence getConcatenatedString(List<CharSequence> items) {
        final ListFormatter listFormatter = ListFormatter.getInstance(Locale.getDefault());
        final CharSequence lastItem =  items.get(items.size() - 1);
        items.add("fake last item");

        // For English with "{0}, {1}, and {2}", the pattern is "{0}, {1}, and {2}".
        // To get "{0}, {1}, {2}", we add a {fake item}, then the pattern result would be
        // "{0}, {1}, {2} and {fake item}", then get the substring with the end index of the
        // last item.
        final String formatted = listFormatter.format(items);
        return formatted.subSequence(0, TextUtils.indexOf(formatted, lastItem) + lastItem.length());
    }
}
