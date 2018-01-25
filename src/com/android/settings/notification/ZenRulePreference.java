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

import android.app.AutomaticZenRule;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.service.notification.ZenModeConfig;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.utils.ManagedServiceSettings;
import com.android.settings.utils.ZenServiceListing;
import com.android.settingslib.TwoTargetPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.Map;

public class ZenRulePreference extends TwoTargetPreference {
    private static final ManagedServiceSettings.Config CONFIG =
            ZenModeAutomationSettings.getConditionProviderConfig();
    final CharSequence mName;
    final String mId;
    boolean appExists;
    final Fragment mParent;
    final Preference mPref;
    final Context mContext;
    final ZenModeBackend mBackend;
    final ZenServiceListing mServiceListing;
    final PackageManager mPm;
    final MetricsFeatureProvider mMetricsFeatureProvider;

    public ZenRulePreference(Context context,
            final Map.Entry<String, AutomaticZenRule> ruleEntry,
            Fragment parent, MetricsFeatureProvider metricsProvider) {
        super(context);

        mBackend = ZenModeBackend.getInstance(context);
        mContext = context;
        final AutomaticZenRule rule = ruleEntry.getValue();
        mName = rule.getName();
        mId = ruleEntry.getKey();
        mParent = parent;
        mPm = mContext.getPackageManager();
        mServiceListing = new ZenServiceListing(mContext, CONFIG);
        mServiceListing.reloadApprovedServices();
        mPref = this;
        mMetricsFeatureProvider = metricsProvider;

        setAttributes(rule);
    }

    @Override
    protected int getSecondTargetResId() {
        if (mId != null && ZenModeConfig.DEFAULT_RULE_IDS.contains(mId)) {
            return 0;
        }

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
            showDeleteRuleDialog(mParent, mId, mName.toString());
        }
    };

    private void showDeleteRuleDialog(final Fragment parent, final String ruleId,
            final String ruleName) {
        ZenDeleteRuleDialog.show(parent, ruleName, ruleId,
                new ZenDeleteRuleDialog.PositiveClickListener() {
                    @Override
                    public void onOk(String id) {
                        mMetricsFeatureProvider.action(mContext,
                                MetricsProto.MetricsEvent.ACTION_ZEN_DELETE_RULE_OK);
                        mBackend.removeZenRule(id);
                    }
                });
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
        return (rule == null || !rule.isEnabled())
                ? mContext.getResources().getString(R.string.switch_off_text)
                : mContext.getResources().getString(R.string.switch_on_text);
    }
}
