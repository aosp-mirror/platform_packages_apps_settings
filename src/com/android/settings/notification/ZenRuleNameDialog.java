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
import android.app.AutomaticZenRule;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.ArraySet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.android.settings.R;

import java.util.List;

public abstract class ZenRuleNameDialog {
    private static final String TAG = "ZenRuleNameDialog";
    private static final boolean DEBUG = ZenModeSettings.DEBUG;

    private final AlertDialog mDialog;
    private final EditText mEditText;
    private final View mWarning;
    private final ColorStateList mWarningTint;
    private final ColorStateList mOriginalTint;
    private final String mOriginalRuleName;
    private final ArraySet<String> mExistingNames;
    private final ServiceListing mServiceListing;
    private final boolean mIsNew;

    public ZenRuleNameDialog(Context context, ServiceListing serviceListing, String ruleName,
            List<AutomaticZenRule> rules) {
        mServiceListing = serviceListing;
        mIsNew = ruleName == null;
        mOriginalRuleName = ruleName;
        mWarningTint = ColorStateList.valueOf(context.getColor(R.color.zen_rule_name_warning));
        final View v = LayoutInflater.from(context).inflate(R.layout.zen_rule_name, null, false);
        mEditText = (EditText) v.findViewById(R.id.rule_name);
        mWarning = v.findViewById(R.id.rule_name_warning);
        if (!mIsNew) {
            mEditText.setText(ruleName);
        }
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorAccent, outValue, true);
        mOriginalTint = ColorStateList.valueOf(outValue.data);
        mEditText.setSelectAllOnFocus(true);

        mDialog = new AlertDialog.Builder(context)
                .setTitle(mIsNew ? R.string.zen_mode_add_rule : R.string.zen_mode_rule_name)
                .setView(v)
                .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String newName = trimmedText();
                        if (!mIsNew && mOriginalRuleName != null
                                && mOriginalRuleName.equalsIgnoreCase(newName)) {
                            return;  // no change to an existing rule, just dismiss
                        }
                        onOk(newName);
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
                updatePositiveButtonAndWarning();
            }
        });
        mExistingNames = getAutomaticRuleNames(rules);
    }

    abstract public void onOk(String ruleName);

    public void show() {
        mDialog.show();
        updatePositiveButtonAndWarning();
    }

    public ArraySet<String> getAutomaticRuleNames(List<AutomaticZenRule> rules) {
        final ArraySet<String> rt = new ArraySet<String>(rules.size());
        for (int i = 0; i < rules.size(); i++) {
            rt.add(rules.get(i).getName().toLowerCase());
        }
        return rt;
    }

    private String trimmedText() {
        return mEditText.getText() == null ? null : mEditText.getText().toString().trim();
    }

    private void updatePositiveButtonAndWarning() {
        final String name = trimmedText();
        final boolean validName = !TextUtils.isEmpty(name)
                && (name.equalsIgnoreCase(mOriginalRuleName)
                        || !mExistingNames.contains(name.toLowerCase()));
        mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(validName);
        final boolean showWarning = !TextUtils.isEmpty(name) && !validName;
        mWarning.setVisibility(showWarning ? View.VISIBLE : View.INVISIBLE);
        mEditText.setBackgroundTintList(showWarning ? mWarningTint : mOriginalTint);
    }
}