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
import android.widget.EditText;

public class UserDictionaryAddWordActivity extends Activity {

    private static final String STATE_KEY_IS_OPEN = "isOpen";

    public static final String MODE_EDIT_ACTION = "com.android.settings.USER_DICTIONARY_EDIT";
    public static final String MODE_INSERT_ACTION = "com.android.settings.USER_DICTIONARY_INSERT";

    private UserDictionaryAddWordContents mContents;

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
            // Override options if we have a saved state.
            args.putAll(savedInstanceState);
        }

        mContents = new UserDictionaryAddWordContents(getWindow().getDecorView(), args);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
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
}
