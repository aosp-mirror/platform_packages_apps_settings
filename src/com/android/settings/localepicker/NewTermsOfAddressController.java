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

package com.android.settings.localepicker;

import android.content.Context;
import android.os.LocaleList;

import androidx.annotation.NonNull;

import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.flags.Flags;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class NewTermsOfAddressController extends BasePreferenceController {

    public NewTermsOfAddressController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        if (Flags.regionalPreferencesApiEnabled()) {
            if (Flags.termsOfAddressEnabled()) {
                return checkAvailabilityStatus();
            }
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    private int checkAvailabilityStatus() {
        // If language is not available for system language, or if ToA does not apply to
        // system language, we will hide it.
        final Locale defaultLocale = Locale.getDefault();
        LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(defaultLocale);
        final List<String> supportedLanguageList = Arrays.asList(
                mContext.getResources().getStringArray(
                    R.array.terms_of_address_supported_languages));
        final List<String> notSupportedLocaleList = Arrays.asList(
                mContext.getResources().getStringArray(
                    R.array.terms_of_address_unsupported_locales));

        final Locale locale = localeInfo.getLocale().stripExtensions();
        final String language = locale.getLanguage();
        final String localeTag = locale.toLanguageTag();

        // Supported locales:
        // 1. All French is supported except fr-CA.
        // 2. QA language en-XA (LTR pseudo locale), ar_XB (RTL pseudo locale).
        if ((supportedLanguageList.contains(language)
                && !notSupportedLocaleList.contains(localeTag))
                || LocaleList.isPseudoLocale(locale)) {
            return AVAILABLE;
        }

        return CONDITIONALLY_UNAVAILABLE;
    }
}
