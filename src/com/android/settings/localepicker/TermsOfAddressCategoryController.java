/**
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.localepicker;

import static com.android.settings.flags.Flags.termsOfAddressEnabled;

import android.content.Context;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.widget.PreferenceCategoryController;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TermsOfAddressCategoryController extends PreferenceCategoryController {

    private static final String TAG = "TermsOfAddressCategoryController";
    private static final String KEY_CATEGORY_TERMS_OF_ADDRESS = "key_category_terms_of_address";
    private static final String KEY_TERMS_OF_ADDRESS = "key_terms_of_address";

    public TermsOfAddressCategoryController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        PreferenceCategory category = screen.findPreference(KEY_CATEGORY_TERMS_OF_ADDRESS);
        if (category == null) {
            Log.d(TAG, "displayPreference(), can not find the category.");
            return;
        }

        boolean isAvailable = isAvailable();
        if (isAvailable) {
            TermsOfAddressController termsOfAddressController = new TermsOfAddressController(
                    mContext, KEY_TERMS_OF_ADDRESS);
            termsOfAddressController.displayPreference(screen);
        }
    }

    @Override
    public int getAvailabilityStatus() {

        if (!termsOfAddressEnabled()) {
            return CONDITIONALLY_UNAVAILABLE;
        }

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

        final Locale locale = localeInfo.getLocale();
        final String language = locale.getLanguage();
        final String localeTag = locale.toLanguageTag();
        Log.d(TAG, "current language: " + language);
        Log.d(TAG, "current locale tag: " + localeTag);

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
