/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.preference.Preference;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;

import com.android.internal.inputmethod.InputMethodUtils;

import java.text.Collator;
import java.util.Locale;

/**
 * Input method subtype preference.
 *
 * This preference represents a subtype of an IME. It is used to enable or disable the subtype.
 */
class InputMethodSubtypePreference extends SwitchWithNoTextPreference {
    private final boolean mIsSystemLocale;
    private final boolean mIsSystemLanguage;

    InputMethodSubtypePreference(final Context context, final InputMethodSubtype subtype,
            final InputMethodInfo imi) {
        super(context);
        setPersistent(false);
        setKey(imi.getId() + subtype.hashCode());
        final CharSequence subtypeLabel = subtype.getDisplayName(context,
                imi.getPackageName(), imi.getServiceInfo().applicationInfo);
        setTitle(subtypeLabel);
        final String subtypeLocaleString = subtype.getLocale();
        if (TextUtils.isEmpty(subtypeLocaleString)) {
            mIsSystemLocale = false;
            mIsSystemLanguage = false;
        } else {
            final Locale systemLocale = context.getResources().getConfiguration().locale;
            mIsSystemLocale = subtypeLocaleString.equals(systemLocale.toString());
            mIsSystemLanguage = mIsSystemLocale
                    || InputMethodUtils.getLanguageFromLocaleString(subtypeLocaleString)
                            .equals(systemLocale.getLanguage());
        }
    }

    int compareTo(final Preference rhs, final Collator collator) {
        if (this == rhs) {
            return 0;
        }
        if (rhs instanceof InputMethodSubtypePreference) {
            final InputMethodSubtypePreference pref = (InputMethodSubtypePreference) rhs;
            final CharSequence t0 = getTitle();
            final CharSequence t1 = rhs.getTitle();
            if (TextUtils.equals(t0, t1)) {
                return 0;
            }
            if (mIsSystemLocale) {
                return -1;
            }
            if (pref.mIsSystemLocale) {
                return 1;
            }
            if (mIsSystemLanguage) {
                return -1;
            }
            if (pref.mIsSystemLanguage) {
                return 1;
            }
            if (TextUtils.isEmpty(t0)) {
                return 1;
            }
            if (TextUtils.isEmpty(t1)) {
                return -1;
            }
            return collator.compare(t0.toString(), t1.toString());
        }
        return super.compareTo(rhs);
    }
}
