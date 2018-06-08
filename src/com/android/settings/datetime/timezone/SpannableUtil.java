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

package com.android.settings.datetime.timezone;

import android.annotation.StringRes;
import android.content.res.Resources;
import android.text.Spannable;
import android.text.SpannableStringBuilder;

import java.util.Formatter;
import java.util.Locale;


public class SpannableUtil {

    /**
     * {@class Resources} has no method to format string resource with {@class Spannable} a
     * rguments. It's a helper method for this purpose.
     */
    public static Spannable getResourcesText(Resources res, @StringRes int resId,
            Object... args) {
        final Locale locale = res.getConfiguration().getLocales().get(0);
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        new Formatter(builder, locale).format(res.getString(resId), args);
        return builder;
    }
}
