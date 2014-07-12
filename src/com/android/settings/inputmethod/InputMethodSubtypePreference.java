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
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
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
//TODO: Make this non-persistent.
class InputMethodSubtypePreference extends CheckBoxPreference {
    private static final String TAG = InputMethodSubtypePreference.class.getSimpleName();

    private final boolean mIsSystemLocale;
    private final boolean mIsSystemLanguage;
    private final Collator mCollator;

    public InputMethodSubtypePreference(final Context context, final InputMethodSubtype subtype,
            final InputMethodInfo imi, final Collator collator) {
        super(context);
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
        mCollator = collator;
    }

    @Override
    public int compareTo(Preference p) {
        if (p instanceof InputMethodSubtypePreference) {
            final InputMethodSubtypePreference pref = ((InputMethodSubtypePreference)p);
            final CharSequence t0 = getTitle();
            final CharSequence t1 = pref.getTitle();
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
            return mCollator.compare(t0.toString(), t1.toString());
        }
        Log.w(TAG, "Illegal preference type.");
        return super.compareTo(p);
    }
}
