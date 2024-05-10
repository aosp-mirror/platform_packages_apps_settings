/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.UserDictionary;
import android.util.ArraySet;

import androidx.annotation.VisibleForTesting;
import androidx.loader.content.CursorLoader;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class UserDictionaryCursorLoader extends CursorLoader {

    @VisibleForTesting
    static final String[] QUERY_PROJECTION = {
            UserDictionary.Words._ID,
            UserDictionary.Words.WORD,
            UserDictionary.Words.SHORTCUT
    };

    // The index of the shortcut in the above array.
    static final int INDEX_SHORTCUT = 2;

    // Either the locale is empty (means the word is applicable to all locales)
    // or the word equals our current locale
    private static final String QUERY_SELECTION =
            UserDictionary.Words.LOCALE + "=?";
    private static final String QUERY_SELECTION_ALL_LOCALES =
            UserDictionary.Words.LOCALE + " is null";


    // Locale can be any of:
    // - The string representation of a locale, as returned by Locale#toString()
    // - The empty string. This means we want a cursor returning words valid for all locales.
    // - null. This means we want a cursor for the current locale, whatever this is.
    // Note that this contrasts with the data inside the database, where NULL means "all
    // locales" and there should never be an empty string. The confusion is called by the
    // historical use of null for "all locales".
    // TODO: it should be easy to make this more readable by making the special values
    // human-readable, like "all_locales" and "current_locales" strings, provided they
    // can be guaranteed not to match locales that may exist.
    private final String mLocale;

    public UserDictionaryCursorLoader(Context context, String locale) {
        super(context);
        mLocale = locale;
    }

    @Override
    public Cursor loadInBackground() {
        final MatrixCursor result = new MatrixCursor(QUERY_PROJECTION);
        final Cursor candidate;
        if ("".equals(mLocale)) {
            // Case-insensitive sort
            candidate = getContext().getContentResolver().query(
                    UserDictionary.Words.CONTENT_URI, QUERY_PROJECTION,
                    QUERY_SELECTION_ALL_LOCALES, null,
                    "UPPER(" + UserDictionary.Words.WORD + ")");
        } else {
            final String queryLocale = null != mLocale ? mLocale : Locale.getDefault().toString();
            candidate = getContext().getContentResolver().query(UserDictionary.Words.CONTENT_URI,
                    QUERY_PROJECTION, QUERY_SELECTION,
                    new String[]{queryLocale}, "UPPER(" + UserDictionary.Words.WORD + ")");
        }
        final Set<Integer> hashSet = new ArraySet<>();
        for (candidate.moveToFirst(); !candidate.isAfterLast(); candidate.moveToNext()) {
            final int id = candidate.getInt(0);
            final String word = candidate.getString(1);
            final String shortcut = candidate.getString(2);
            final int hash = Objects.hash(word, shortcut);
            if (hashSet.contains(hash)) {
                continue;
            }
            hashSet.add(hash);
            result.addRow(new Object[]{id, word, shortcut});
        }
        // The cursor needs to be closed after use, otherwise it will cause resource leakage
        candidate.close();
        return result;
    }
}
