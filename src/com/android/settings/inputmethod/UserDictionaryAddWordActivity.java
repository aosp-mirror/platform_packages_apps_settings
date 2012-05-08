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
import com.android.settings.inputmethod.UserDictionaryAddWordContents.LocaleRenderer;

import java.util.ArrayList;
import java.util.Locale;

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

    private static final String STATE_KEY_IS_OPEN = "isOpen";

    public static final String MODE_EDIT_ACTION = "com.android.settings.USER_DICTIONARY_EDIT";
    public static final String MODE_INSERT_ACTION = "com.android.settings.USER_DICTIONARY_INSERT";

    private static final int[] IDS_SHOWN_ONLY_IN_MORE_OPTIONS_MODE = {
        R.id.user_dictionary_add_word_label,
        R.id.user_dictionary_add_shortcut_label,
        R.id.user_dictionary_add_locale_label,
        R.id.user_dictionary_settings_add_dialog_shortcut,
        R.id.user_dictionary_settings_add_dialog_locale,
    };

    private UserDictionaryAddWordContents mContents;

    private boolean mIsShowingMoreOptions = false;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_dictionary_add_word);
        final Intent intent = getIntent();
        final String action = intent.getAction();
        final int mode;
        if (MODE_EDIT_ACTION.equals(action)) {
            mode = UserDictionaryAddWordContents.MODE_EDIT;
        } else if (MODE_INSERT_ACTION.equals(action)) {
            mode = UserDictionaryAddWordContents.MODE_INSERT;
        } else {
            // Can never come here because we only support these two actions in the manifest
            throw new RuntimeException("Unsupported action: " + action);
        }

        // The following will get the EXTRA_WORD and EXTRA_LOCALE fields that are in the intent.
        // We do need to add the action by hand, because UserDictionaryAddWordContents expects
        // it to be in the bundle, in the EXTRA_MODE key.
        final Bundle args = intent.getExtras();
        args.putInt(UserDictionaryAddWordContents.EXTRA_MODE, mode);

        if (null != savedInstanceState) {
            mIsShowingMoreOptions =
                    savedInstanceState.getBoolean(STATE_KEY_IS_OPEN, mIsShowingMoreOptions);
            // Override options if we have a saved state.
            args.putAll(savedInstanceState);
        }

        mContents = new UserDictionaryAddWordContents(getWindow().getDecorView(), args);

        if (mIsShowingMoreOptions) {
            onClickMoreOptions(findViewById(R.id.user_dictionary_settings_add_dialog_more_options));
        }

        // TODO: The following code enables layout transition for eye-candy, but there is still
        // a jankiness issue with the window moving on one frame, resizing suddenly on the next,
        // and animation only starting afterwards on children.
        final ViewGroup v = (ViewGroup)findViewById(R.id.user_dictionary_add_word_grid);
        final LayoutTransition transition = new LayoutTransition();
        transition.setStartDelay(LayoutTransition.APPEARING, 0);
        v.setLayoutTransition(transition);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putBoolean(STATE_KEY_IS_OPEN, mIsShowingMoreOptions);
        outState.putString(
                UserDictionaryAddWordContents.EXTRA_WORD, mContents.mEditText.getText().toString());
        outState.putString(UserDictionaryAddWordContents.EXTRA_LOCALE, mContents.mLocale);
    }

    public void onClickCancel(final View v) {
        finish();
    }

    public void onClickConfirm(final View v) {
        mContents.apply(this);
        finish();
    }

    public void onClickMoreOptions(final View v) {
        for (final int idToShow : IDS_SHOWN_ONLY_IN_MORE_OPTIONS_MODE) {
            final View viewToShow = findViewById(idToShow);
            viewToShow.setVisibility(View.VISIBLE);
        }
        findViewById(R.id.user_dictionary_settings_add_dialog_more_options)
                .setVisibility(View.GONE);
        findViewById(R.id.user_dictionary_settings_add_dialog_less_options)
                .setVisibility(View.VISIBLE);

        final ArrayList<LocaleRenderer> localesList = mContents.getLocalesList(this);

        final Spinner localeSpinner =
                (Spinner)findViewById(R.id.user_dictionary_settings_add_dialog_locale);
        final ArrayAdapter<LocaleRenderer> adapter = new ArrayAdapter<LocaleRenderer>(this,
                android.R.layout.simple_spinner_item, localesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        localeSpinner.setAdapter(adapter);
        localeSpinner.setOnItemSelectedListener(this);
        mIsShowingMoreOptions = true;
    }

    public void onClickLessOptions(final View v) {
        for (final int idToHide : IDS_SHOWN_ONLY_IN_MORE_OPTIONS_MODE) {
            final View viewToHide = findViewById(idToHide);
            viewToHide.setVisibility(View.GONE);
        }
        findViewById(R.id.user_dictionary_settings_add_dialog_more_options)
                .setVisibility(View.VISIBLE);
        findViewById(R.id.user_dictionary_settings_add_dialog_less_options)
                .setVisibility(View.GONE);
        mIsShowingMoreOptions = false;
    }

    @Override
    public void onItemSelected(final AdapterView<?> parent, final View view, final int pos,
            final long id) {
        final LocaleRenderer locale = (LocaleRenderer)parent.getItemAtPosition(pos);
        mContents.updateLocale(locale.getLocaleString());
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // I'm not sure we can come here, but if we do, that's the right thing to do.
        final Intent intent = getIntent();
        mContents.updateLocale(intent.getStringExtra(UserDictionaryAddWordContents.EXTRA_LOCALE));
    }
}
