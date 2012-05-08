/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.UserDictionary;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.UserDictionarySettings;
import com.android.settings.Utils;

import java.util.Locale;

/**
 * A container class to factor common code to UserDictionaryAddWordFragment
 * and UserDictionaryAddWordActivity.
 */
public class UserDictionaryAddWordContents {
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_WORD = "word";
    public static final String EXTRA_LOCALE = "locale";

    public static final int MODE_EDIT = 0;
    public static final int MODE_INSERT = 1;

    private static final int FREQUENCY_FOR_USER_DICTIONARY_ADDS = 250;

    /* package */ final int mMode; // Either MODE_EDIT or MODE_INSERT
    /* package */ final EditText mEditText;
    /* package */ String mLocale;
    /* package */ String mOldWord;

    /* package */ UserDictionaryAddWordContents(final View view, final Bundle args) {
        mEditText = (EditText)view.findViewById(R.id.user_dictionary_add_word_text);
        final String word = args.getString(EXTRA_WORD);
        if (null != word) {
            mEditText.setText(word);
            mEditText.setSelection(word.length());
        }
        mMode = args.getInt(EXTRA_MODE); // default return value for #getInt() is 0 = MODE_EDIT
        mOldWord = args.getString(EXTRA_WORD);
        updateLocale(args.getString(EXTRA_LOCALE));
    }

    // locale may be null, this means default locale
    // It may also be the empty string, which means "all locales"
    /* package */ void updateLocale(final String locale) {
        mLocale = null == locale ? Locale.getDefault().toString() : locale;
    }

    /* package */ void apply(final Context context) {
        final ContentResolver resolver = context.getContentResolver();
        if (UserDictionaryAddWordContents.MODE_EDIT == mMode && !TextUtils.isEmpty(mOldWord)) {
            UserDictionarySettings.deleteWord(mOldWord, resolver);
        }
        final String newWord = mEditText.getText().toString();
        if (TextUtils.isEmpty(newWord)) {
            // If the word is somehow empty, don't insert it.
            return;
        }
        // Disallow duplicates.
        // TODO: Redefine the logic when we support shortcuts.
        UserDictionarySettings.deleteWord(newWord, resolver);

        // In this class we use the empty string to represent 'all locales' and mLocale cannot
        // be null. However the addWord method takes null to mean 'all locales'.
        UserDictionary.Words.addWord(context, newWord.toString(),
                FREQUENCY_FOR_USER_DICTIONARY_ADDS, null /* shortcut */,
                TextUtils.isEmpty(mLocale) ? null : Utils.createLocaleFromString(mLocale));
    }
}
