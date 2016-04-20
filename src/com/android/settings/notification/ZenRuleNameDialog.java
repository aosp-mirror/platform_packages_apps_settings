/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.notification;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.android.settings.R;

public abstract class ZenRuleNameDialog {
    private static final String TAG = "ZenRuleNameDialog";
    private static final boolean DEBUG = ZenModeSettings.DEBUG;

    private final AlertDialog mDialog;
    private final EditText mEditText;
    private final CharSequence mOriginalRuleName;
    private final boolean mIsNew;

    public ZenRuleNameDialog(Context context, CharSequence ruleName) {
        mIsNew = ruleName == null;
        mOriginalRuleName = ruleName;
        final View v = LayoutInflater.from(context).inflate(R.layout.zen_rule_name, null, false);
        mEditText = (EditText) v.findViewById(R.id.rule_name);
        if (!mIsNew) {
            mEditText.setText(ruleName);
        }
        mEditText.setSelectAllOnFocus(true);

        mDialog = new AlertDialog.Builder(context)
                .setTitle(mIsNew ? R.string.zen_mode_add_rule : R.string.zen_mode_rule_name)
                .setView(v)
                .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String newName = trimmedText();
                        if (TextUtils.isEmpty(newName)) {
                            return;
                        }
                        if (!mIsNew && mOriginalRuleName != null
                                && mOriginalRuleName.equals(newName)) {
                            return;  // no change to an existing rule, just dismiss
                        }
                        onOk(newName);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    abstract public void onOk(String ruleName);

    public void show() {
        mDialog.show();
    }

    private String trimmedText() {
        return mEditText.getText() == null ? null : mEditText.getText().toString().trim();
    }
}
