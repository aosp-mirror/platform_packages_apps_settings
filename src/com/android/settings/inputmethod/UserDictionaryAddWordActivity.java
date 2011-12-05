/*
 * Copyright (C) 2011 The Android Open Source Project
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

import com.android.settings.R;
import com.android.settings.UserDictionarySettings;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.UserDictionary;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class UserDictionaryAddWordActivity extends Activity
        implements AdapterView.OnItemSelectedListener {
    public static final String EXTRA_WORD = "word";
    public static final String EXTRA_LOCALE = "locale";
    private static final int FREQUENCY_FOR_USER_DICTIONARY_ADDS = 250;

    public static final String MODE_EDIT_ACTION = "com.android.settings.USER_DICTIONARY_EDIT";
    public static final String MODE_INSERT_ACTION = "com.android.settings.USER_DICTIONARY_INSERT";
    private static final int MODE_EDIT = 0;
    private static final int MODE_INSERT = 1;

    private EditText mEditText;
    private int mMode; // Either MODE_EDIT or MODE_INSERT
    private String mOldWord;
    private String mLocale; // may be null

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_dictionary_add_word);
        final Intent intent = getIntent();
        final String action = intent.getAction();
        if (MODE_EDIT_ACTION.equals(action)) {
            mMode = MODE_EDIT;
        } else if (MODE_INSERT_ACTION.equals(action)) {
            mMode = MODE_INSERT;
        } else {
            // Can never come here because we only support these two actions in the manifest
            throw new RuntimeException("Unsupported action: " + action);
        }
        mOldWord = intent.getStringExtra(EXTRA_WORD);
        mLocale = intent.getStringExtra(EXTRA_LOCALE); // this may be null
        mEditText = (EditText)findViewById(R.id.user_dictionary_add_word_text);
        if (null != mOldWord) {
            mEditText.setText(mOldWord);
            mEditText.setSelection(mOldWord.length());
        }
        final ViewGroup v = (ViewGroup)findViewById(R.id.user_dict_settings_add_dialog_top);
        final LayoutTransition transition = new LayoutTransition();
        transition.setStartDelay(LayoutTransition.APPEARING, 0);
        v.setLayoutTransition(transition);
    }

    public void onClickCancel(final View v) {
        finish();
    }

    public void onClickManage(final View v) {
        final Intent intent = new Intent(v.getContext(), UserDictionarySettings.class);
        startActivity(intent);
        finish();
    }

    public void onClickConfirm(final View v) {
        if (MODE_EDIT == mMode && !TextUtils.isEmpty(mOldWord)) {
            UserDictionarySettings.deleteWord(mOldWord, this.getContentResolver());
        }
        final String newWord = mEditText.getText().toString();
        if (TextUtils.isEmpty(newWord)) {
            // If the word is somehow empty, don't insert it.
            // TODO: grey out the Ok button when the text is empty?
            finish();
            return;
        }
        // Disallow duplicates.
        // TODO: Redefine the logic when we support shortcuts.
        UserDictionarySettings.deleteWord(newWord, this.getContentResolver());

        if (null == mLocale) {
            // Null means insert with the default system locale.
            UserDictionary.Words.addWord(this, newWord.toString(),
                    FREQUENCY_FOR_USER_DICTIONARY_ADDS, UserDictionary.Words.LOCALE_TYPE_CURRENT);
        } else if ("".equals(mLocale)) {
            // Empty string means insert for all languages.
            UserDictionary.Words.addWord(this, newWord.toString(),
                    FREQUENCY_FOR_USER_DICTIONARY_ADDS, UserDictionary.Words.LOCALE_TYPE_ALL);
        } else {
            // TODO: fix the framework so that it can accept a locale when we add a word
            // to the user dictionary instead of querying the system locale.
            final Locale prevLocale = Locale.getDefault();
            Locale.setDefault(Utils.createLocaleFromString(mLocale));
            UserDictionary.Words.addWord(this, newWord.toString(),
                    FREQUENCY_FOR_USER_DICTIONARY_ADDS, UserDictionary.Words.LOCALE_TYPE_CURRENT);
            Locale.setDefault(prevLocale);
        }
        finish();
    }

    private static class LocaleRenderer {
        private final String mLocaleString;
        private final String mDescription;
        // LocaleString may NOT be null.
        public LocaleRenderer(final Context context, final String localeString) {
            mLocaleString = localeString;
            if ("".equals(localeString)) {
                mDescription = context.getString(R.string.user_dict_settings_all_languages);
            } else {
                mDescription = Utils.createLocaleFromString(localeString).getDisplayName();
            }
        }
        public String toString() {
            return mDescription;
        }
        public String getLocaleString() {
            return mLocaleString;
        }
    }

    private static void addLocaleDisplayNameToList(final Context context,
            final List<LocaleRenderer> list, final String locale) {
        if (null != locale) {
            list.add(new LocaleRenderer(context, locale));
        }
    }

    public void onClickOptions(final View v) {
        final View moreOptionsView =
                findViewById(R.id.user_dict_settings_add_dialog_shortcut_interface);
        moreOptionsView.setVisibility(View.VISIBLE);
        findViewById(R.id.user_dictionary_settings_add_dialog_options).setVisibility(View.GONE);
        findViewById(R.id.user_dictionary_settings_add_dialog_manage).setVisibility(View.VISIBLE);

        final Set<String> locales = UserDictionaryList.getUserDictionaryLocalesList(this);
        if (null != mLocale && locales.contains(mLocale)) {
            // Remove our locale if it's in, because we're always gonna put it at the top
            locales.remove(mLocale);
        }
        final String systemLocale = Locale.getDefault().toString();
        if (null != systemLocale && locales.contains(systemLocale)) {
            // The system locale should be inside. We want it at the 2nd spot.
            locales.remove(systemLocale);
        }
        final ArrayList<LocaleRenderer> localesList = new ArrayList<LocaleRenderer>();
        // Add the passed locale, then the system locale at the top of the list. Add an
        // "all languages" entry at the bottom of the list.
        addLocaleDisplayNameToList(this, localesList, mLocale);
        addLocaleDisplayNameToList(this, localesList, systemLocale);
        for (final String l : locales) {
            // TODO: sort in unicode order
            addLocaleDisplayNameToList(this, localesList, l);
        }
        localesList.add(new LocaleRenderer(this, ""));
        //TODO: Do we need an option "more..." to show all locales in the world?
        final Spinner localeSpinner =
                (Spinner)findViewById(R.id.user_dictionary_settings_add_dialog_locale);
        final ArrayAdapter<LocaleRenderer> adapter = new ArrayAdapter<LocaleRenderer>(this,
                android.R.layout.simple_spinner_item, localesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        localeSpinner.setAdapter(adapter);
        localeSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(final AdapterView<?> parent, final View view, final int pos,
            final long id) {
        final LocaleRenderer locale = (LocaleRenderer)parent.getItemAtPosition(pos);
        mLocale = locale.getLocaleString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // I'm not sure we can come here, but if we do, that's the right thing to do.
        mLocale = null;
    }
}
