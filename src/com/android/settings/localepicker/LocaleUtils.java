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

package com.android.settings.localepicker;

import android.os.LocaleList;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * A locale utility class.
 */
public class LocaleUtils {
    /**
     * Checks if the languageTag is in the system locale. Since in the current design, the system
     * language list would not show two locales with the same language and region but different
     * numbering system. So, the u extension has to be stripped out in the process of comparison.
     *
     * @param languageTag A language tag
     * @return true if the locale is in the system locale. Otherwise, false.
     */
    public static boolean isInSystemLocale(@NonNull String languageTag) {
        LocaleList systemLocales = LocaleList.getDefault();
        Locale localeWithoutUextension =
                new Locale.Builder()
                        .setLocale(Locale.forLanguageTag(languageTag))
                        .clearExtensions()
                        .build();
        for (int i = 0; i < systemLocales.size(); i++) {
            Locale sysLocaleWithoutUextension =
                    new Locale.Builder().setLocale(systemLocales.get(i)).clearExtensions().build();
            if (localeWithoutUextension.equals(sysLocaleWithoutUextension)) {
                return true;
            }
        }
        return false;
    }
}
