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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.android.settings.R;

public abstract class ZenRuleNameDialog {
    private final AlertDialog mDialog;
    private final EditText mEditText;
    private final ArraySet<String> mExistingNames;

    public ZenRuleNameDialog(Context context, String ruleName, ArraySet<String> existingNames) {
        final View v = LayoutInflater.from(context).inflate(R.layout.zen_rule_name, null, false);
        mEditText = (EditText) v.findViewById(R.id.rule_name);
        mEditText.setText(ruleName);
        mEditText.setSelectAllOnFocus(true);
        mDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.zen_mode_rule_name)
                .setView(v)
                .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onOk(trimmedText());
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // noop
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // noop
            }

            @Override
            public void afterTextChanged(Editable s) {
                updatePositiveButton();
            }
        });
        mExistingNames = new ArraySet<String>(existingNames.size());
        for (String existingName : existingNames) {
            mExistingNames.add(existingName.toLowerCase());
        }
    }

    abstract public void onOk(String ruleName);

    private String trimmedText() {
        return mEditText.getText() == null ? null : mEditText.getText().toString().trim();
    }

    public void show() {
        mDialog.show();
        updatePositiveButton();
    }

    private void updatePositiveButton() {
        final String name = trimmedText();
        final boolean validName = !TextUtils.isEmpty(name)
                && !mExistingNames.contains(name.toLowerCase());
        mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(validName);
    }

}