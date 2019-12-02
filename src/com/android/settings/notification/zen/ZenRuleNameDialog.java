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

package com.android.settings.notification.zen;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class ZenRuleNameDialog extends InstrumentedDialogFragment {
    protected static final String TAG = "ZenRuleNameDialog";
    private static final String EXTRA_ZEN_RULE_NAME = "zen_rule_name";
    private static final String EXTRA_CONDITION_ID = "extra_zen_condition_id";
    protected static PositiveClickListener mPositiveClickListener;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_ZEN_MODE_RULE_NAME_DIALOG;
    }

    /**
     * The interface we expect a listener to implement.
     */
    public interface PositiveClickListener {
        void onOk(String newName, Fragment parent);
    }

    public static void show(Fragment parent, String ruleName, Uri conditionId, PositiveClickListener
            listener) {
        final Bundle args = new Bundle();
        args.putString(EXTRA_ZEN_RULE_NAME, ruleName);
        args.putParcelable(EXTRA_CONDITION_ID, conditionId);
        mPositiveClickListener = listener;

        ZenRuleNameDialog dialog = new ZenRuleNameDialog();
        dialog.setArguments(args);
        dialog.setTargetFragment(parent, 0);
        dialog.show(parent.getFragmentManager(), TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        Uri conditionId = arguments.getParcelable(EXTRA_CONDITION_ID);
        String ruleName = arguments.getString(EXTRA_ZEN_RULE_NAME);

        boolean isNew = ruleName == null;
        CharSequence originalRuleName = ruleName;
        Context context = getContext();
        final View v = LayoutInflater.from(context).inflate(R.layout.zen_rule_name, null,
                false);
        EditText editText = (EditText) v.findViewById(R.id.zen_mode_rule_name);
        if (!isNew) {
            // set text to current rule name
            editText.setText(ruleName);
            // move cursor to end of text
            editText.setSelection(editText.getText().length());
        }
        editText.setSelectAllOnFocus(true);
        return new AlertDialog.Builder(context)
                .setTitle(getTitleResource(conditionId, isNew))
                .setView(v)
                .setPositiveButton(isNew ? R.string.zen_mode_add : R.string.okay,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final String newName = trimmedText(editText);
                                if (TextUtils.isEmpty(newName)) {
                                    return;
                                }
                                if (!isNew && originalRuleName != null
                                        && originalRuleName.equals(newName)) {
                                    return;  // no change to an existing rule, just dismiss
                                }
                               mPositiveClickListener.onOk(newName, getTargetFragment());
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    private String trimmedText(EditText editText) {
        return editText.getText() == null ? null : editText.getText().toString().trim();
    }

    private int getTitleResource(Uri conditionId, boolean isNew) {
        final boolean isEvent = ZenModeConfig.isValidEventConditionId(conditionId);
        final boolean isTime = ZenModeConfig.isValidScheduleConditionId(conditionId);
        int titleResource =  R.string.zen_mode_rule_name;
        if (isNew) {
            if (isEvent) {
                titleResource = R.string.zen_mode_add_event_rule;
            } else if (isTime) {
                titleResource = R.string.zen_mode_add_time_rule;
            }
        }
        return titleResource;
    }
}
