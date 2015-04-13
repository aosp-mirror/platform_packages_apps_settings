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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ScheduleInfo;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.android.settings.R;

import java.util.List;

public abstract class ZenRuleNameDialog {
    private static final String TAG = ZenModeSettings.TAG;
    private static final boolean DEBUG = ZenModeSettings.DEBUG;

    private final AlertDialog mDialog;
    private final EditText mEditText;
    private final RadioGroup mTypes;
    private final ArraySet<String> mExistingNames;
    private final ServiceListing mServiceListing;
    private final RuleInfo[] mExternalRules = new RuleInfo[3];

    public ZenRuleNameDialog(Context context, ServiceListing serviceListing, String ruleName,
            ArraySet<String> existingNames) {
        mServiceListing = serviceListing;
        final View v = LayoutInflater.from(context).inflate(R.layout.zen_rule_name, null, false);
        mEditText = (EditText) v.findViewById(R.id.rule_name);
        if (ruleName != null) {
            mEditText.setText(ruleName);
        }
        mEditText.setSelectAllOnFocus(true);
        mTypes = (RadioGroup) v.findViewById(R.id.rule_types);
        if (mServiceListing != null) {
            bindType(R.id.rule_type_schedule, defaultNewSchedule());
            bindExternalRules();
            mServiceListing.addCallback(mServiceListingCallback);
            mServiceListing.reload();
        }
        mDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.zen_mode_rule_name)
                .setView(v)
                .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onOk(trimmedText(), selectedRuleInfo());
                    }
                })
                .setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (mServiceListing != null) {
                            mServiceListing.removeCallback(mServiceListingCallback);
                        }
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

    abstract public void onOk(String ruleName, RuleInfo ruleInfo);

    public void show() {
        mDialog.show();
        updatePositiveButton();
    }

    private void bindType(int id, RuleInfo ri) {
        final RadioButton rb = (RadioButton) mTypes.findViewById(id);
        if (ri == null) {
            rb.setVisibility(View.GONE);
            return;
        }
        rb.setVisibility(View.VISIBLE);
        if (ri.caption != null) {
            rb.setText(ri.caption);
        }
        rb.setTag(ri);
    }

    private RuleInfo selectedRuleInfo() {
        final int id = mTypes.getCheckedRadioButtonId();
        if (id == -1) return null;
        final RadioButton rb = (RadioButton) mTypes.findViewById(id);
        return (RuleInfo) rb.getTag();
    }

    private String trimmedText() {
        return mEditText.getText() == null ? null : mEditText.getText().toString().trim();
    }

    private void updatePositiveButton() {
        final String name = trimmedText();
        final boolean validName = !TextUtils.isEmpty(name)
                && !mExistingNames.contains(name.toLowerCase());
        mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(validName);
    }

    private static RuleInfo defaultNewSchedule() {
        final ScheduleInfo schedule = new ScheduleInfo();
        schedule.days = ZenModeConfig.ALL_DAYS;
        schedule.startHour = 22;
        schedule.endHour = 7;
        final RuleInfo rt = new RuleInfo();
        rt.settingsAction = ZenModeScheduleRuleSettings.ACTION;
        rt.defaultConditionId = ZenModeConfig.toScheduleConditionId(schedule);
        return rt;
    }

    private void bindExternalRules() {
        bindType(R.id.rule_type_2, mExternalRules[0]);
        bindType(R.id.rule_type_3, mExternalRules[1]);
        bindType(R.id.rule_type_4, mExternalRules[2]);
        // show radio group if we have at least one external rule type
        mTypes.setVisibility(mExternalRules[0] != null ? View.VISIBLE : View.GONE);
    }

    private final ServiceListing.Callback mServiceListingCallback = new ServiceListing.Callback() {
        @Override
        public void onServicesReloaded(List<ServiceInfo> services) {
            if (DEBUG) Log.d(TAG, "Services reloaded: count=" + services.size());
            mExternalRules[0] = mExternalRules[1] = mExternalRules[2] = null;
            int i = 0;
            for (ServiceInfo si : services) {
                final RuleInfo ri = ZenModeExternalRuleSettings.getRuleInfo(si);
                if (ri != null) {
                    mExternalRules[i] = ri;
                    i++;
                    if (i == mExternalRules.length) {
                        break;
                    }
                }
            }
            bindExternalRules();
        }
    };

    public static class RuleInfo {
        public String caption;
        public String settingsAction;
        public Uri defaultConditionId;
        public ComponentName serviceComponent;
        public ComponentName configurationActivity;
    }

}