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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;

public class UserDictionaryAddWordActivity extends Activity {

    public static final String MODE_EDIT_ACTION = "com.android.settings.USER_DICTIONARY_EDIT";
    public static final String MODE_INSERT_ACTION = "com.android.settings.USER_DICTIONARY_INSERT";

    /* package */ static final int CODE_WORD_ADDED = 0;
    /* package */ static final int CODE_CANCEL = 1;
    /* package */ static final int CODE_ALREADY_PRESENT = 2;

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
        mContents.saveStateIntoBundle(outState);
    }

    private void reportBackToCaller(final int resultCode, final Bundle result) {
        final Intent senderIntent = getIntent();
        final Object listener = senderIntent.getExtras().get("listener");
        if (!(listener instanceof Messenger)) return; // This will work if listener is null too.
        final Messenger messenger = (Messenger)listener;

        final Message m = Message.obtain();
        m.obj = result;
        m.what = resultCode;
        try {
            messenger.send(m);
        } catch (RemoteException e) {
            // Couldn't report back, but there is nothing we can do to fix it
        }
    }

    public void onClickCancel(final View v) {
        reportBackToCaller(CODE_CANCEL, null);
        finish();
    }

    public void onClickConfirm(final View v) {
        final Bundle parameters = new Bundle();
        final int resultCode = mContents.apply(this, parameters);
        reportBackToCaller(resultCode, parameters);
        finish();
    }
}
