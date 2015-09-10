/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.content.Context;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.Utils;

import java.util.Locale;

/**
 * Utilities of the user dictionary settings
 */
public class UserDictionarySettingsUtils {
    public static String getLocaleDisplayName(Context context, String localeStr) {
        if (TextUtils.isEmpty(localeStr)) {
            // CAVEAT: localeStr should not be null because a null locale stands for the system
            // locale in UserDictionary.Words.addWord.
            return context.getResources().getString(R.string.user_dict_settings_all_languages);
        }
        final Locale locale = Utils.createLocaleFromString(localeStr);
        final Locale systemLocale = context.getResources().getConfiguration().locale;
        return locale.getDisplayName(systemLocale);
    }
}
