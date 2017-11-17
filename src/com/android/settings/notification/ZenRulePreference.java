/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.service.notification.ZenModeConfig;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;

import com.android.settings.R;
import com.android.settings.utils.ManagedServiceSettings;
import com.android.settings.utils.ZenServiceListing;
import com.android.settingslib.TwoTargetPreference;

import java.util.Map;

public class ZenRulePreference extends TwoTargetPreference {
    private static final ManagedServiceSettings.Config CONFIG =
            ZenModeAutomationSettings.getConditionProviderConfig();
    final CharSequence mName;
    final String mId;
    boolean appExists;
    final PreferenceCategory mParent;
    final Preference mPref;
    final Context mContext;
    final ZenModeBackend mBackend;
    final ZenServiceListing mServiceListing;
    final PackageManager mPm;

    public ZenRulePreference(Context context,
            final Map.Entry<String, AutomaticZenRule> ruleEntry,
            PreferenceCategory prefCategory) {
        super(context);

        mBackend = ZenModeBackend.getInstance(context);
        mContext = context;
        final AutomaticZenRule rule = ruleEntry.getValue();
        mName = rule.getName();
        mId = ruleEntry.getKey();
        mParent = prefCategory;
        mPm = mContext.getPackageManager();
        mServiceListing = new ZenServiceListing(mContext, CONFIG);
        mServiceListing.reloadApprovedServices();
        mPref = this;

        setAttributes(rule);
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.zen_rule_widget;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        View v = view.findViewById(R.id.delete_zen_rule);
        if (v != null) {
            v.setOnClickListener(mDeleteListener);
        }
    }

    private final View.OnClickListener mDeleteListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showDeleteRuleDialog(mId, mName, mParent, mPref);
        }
    };

    private void showDeleteRuleDialog(final String ruleId, final CharSequence ruleName,
            PreferenceCategory parent, Preference pref) {
        new AlertDialog.Builder(mContext)
                .setMessage(mContext.getResources().getString(
                        R.string.zen_mode_delete_rule_confirmation, ruleName))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.zen_mode_delete_rule_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mBackend.removeZenRule(ruleId);
                                parent.removePreference(pref);
                            }
                        })
                .show();
    }

    protected void setAttributes(AutomaticZenRule rule) {
        final boolean isSchedule = ZenModeConfig.isValidScheduleConditionId(
                rule.getConditionId());
        final boolean isEvent = ZenModeConfig.isValidEventConditionId(rule.getConditionId());
        final boolean isSystemRule = isSchedule || isEvent;

        try {
            ApplicationInfo info = mPm.getApplicationInfo(rule.getOwner().getPackageName(), 0);
            setSummary(computeRuleSummary(rule, isSystemRule, info.loadLabel(mPm)));
        } catch (PackageManager.NameNotFoundException e) {
            appExists = false;
            return;
        }

        appExists = true;
        setTitle(rule.getName());
        setPersistent(false);

        final String action = isSchedule ? ZenModeScheduleRuleSettings.ACTION
                : isEvent ? ZenModeEventRuleSettings.ACTION : "";
        ServiceInfo si = mServiceListing.findService(rule.getOwner());
        ComponentName settingsActivity = AbstractZenModeAutomaticRulePreferenceController.
                getSettingsActivity(si);
        setIntent(AbstractZenModeAutomaticRulePreferenceController.getRuleIntent(action,
                settingsActivity, mId));
        setSelectable(settingsActivity != null || isSystemRule);
        setKey(mId);
    }

    private String computeRuleSummary(AutomaticZenRule rule, boolean isSystemRule,
            CharSequence providerLabel) {
        final String mode = computeZenModeCaption(mContext.getResources(),
                rule.getInterruptionFilter());
        final String ruleState = (rule == null || !rule.isEnabled())
                ? mContext.getResources().getString(R.string.switch_off_text)
                : mContext.getResources().getString(
                        R.string.zen_mode_rule_summary_enabled_combination, mode);

        return ruleState;
    }

    private static String computeZenModeCaption(Resources res, int zenMode) {
        switch (zenMode) {
            case NotificationManager.INTERRUPTION_FILTER_ALARMS:
                return res.getString(R.string.zen_mode_option_alarms);
            case NotificationManager.INTERRUPTION_FILTER_PRIORITY:
                return res.getString(R.string.zen_mode_option_important_interruptions);
            case NotificationManager.INTERRUPTION_FILTER_NONE:
                return res.getString(R.string.zen_mode_option_no_interruptions);
            default:
                return null;
        }
    }
}