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

package com.android.settings.notification.zen;

import android.app.AutomaticZenRule;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ScheduleInfo;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.utils.ManagedServiceSettings;
import com.android.settings.utils.ZenServiceListing;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.List;
import java.util.Map;

public class ZenRulePreference extends PrimarySwitchPreference {
    private static final String TAG = "ZenRulePreference";
    private static final ManagedServiceSettings.Config CONFIG =
            ZenModeAutomationSettings.getConditionProviderConfig();
    final String mId;
    final Fragment mParent;
    final Preference mPref;
    final Context mContext;
    final ZenModeBackend mBackend;
    final ZenServiceListing mServiceListing;
    final PackageManager mPm;
    final MetricsFeatureProvider mMetricsFeatureProvider;
    AutomaticZenRule mRule;
    CharSequence mName;

    private Intent mIntent;

    private final ZenRuleScheduleHelper mScheduleHelper = new ZenRuleScheduleHelper();

    public ZenRulePreference(Context context,
            final Map.Entry<String, AutomaticZenRule> ruleEntry,
            Fragment parent, MetricsFeatureProvider metricsProvider) {
        super(context);
        mBackend = ZenModeBackend.getInstance(context);
        mContext = context;
        mRule = ruleEntry.getValue();
        mName = mRule.getName();
        mId = ruleEntry.getKey();
        mParent = parent;
        mPm = mContext.getPackageManager();
        mServiceListing = new ZenServiceListing(mContext, CONFIG);
        mServiceListing.reloadApprovedServices();
        mPref = this;
        mMetricsFeatureProvider = metricsProvider;
        setAttributes(mRule);
        setWidgetLayoutResource(getSecondTargetResId());

        // initialize the checked state of the preference
        super.setChecked(mRule.isEnabled());
    }

    public void updatePreference(AutomaticZenRule rule) {
        if (!mRule.getName().equals(rule.getName())) {
            mName = rule.getName();
            setTitle(mName);
        }

        if (mRule.isEnabled() != rule.isEnabled()) {
            setChecked(rule.isEnabled());
        }
        setSummary(computeRuleSummary(rule));
        mRule = rule;
    }

    @Override
    public void onClick() {
        mContext.startActivity(mIntent);
    }

    @Override
    public void setChecked(boolean checked) {
        mRule.setEnabled(checked);
        mBackend.updateZenRule(mId, mRule);
        setAttributes(mRule);
        super.setChecked(checked);
    }

    protected void setAttributes(AutomaticZenRule rule) {
        final boolean isSchedule = ZenModeConfig.isValidScheduleConditionId(
                rule.getConditionId(), true);
        final boolean isEvent = ZenModeConfig.isValidEventConditionId(rule.getConditionId());

        setSummary(computeRuleSummary(rule));

        setTitle(mName);
        setPersistent(false);

        final String action = isSchedule ? ZenModeScheduleRuleSettings.ACTION
                : isEvent ? ZenModeEventRuleSettings.ACTION : "";
        ComponentInfo si = mServiceListing.findService(rule.getOwner());
        ComponentName settingsActivity = AbstractZenModeAutomaticRulePreferenceController.
                getSettingsActivity(mPm, rule, si);
        mIntent = AbstractZenModeAutomaticRulePreferenceController.getRuleIntent(action,
                settingsActivity, mId);
        // If the intent's activity for this rule doesn't exist or resolve to anything, disable the
        // preference and rule.
        List<ResolveInfo> results = mPm.queryIntentActivities(
                mIntent, PackageManager.ResolveInfoFlags.of(0));
        if (mIntent.resolveActivity(mPm) == null || results.size() == 0) {
            Log.w(TAG, "intent for zen rule invalid: " + mIntent);
            mIntent = null;
            setEnabled(false);
        }
        setKey(mId);
    }

    private String computeRuleSummary(AutomaticZenRule rule) {
        if (rule != null) {
            // handle schedule-based rules
            ScheduleInfo schedule =
                    ZenModeConfig.tryParseScheduleConditionId(rule.getConditionId());
            if (schedule != null) {
                String desc = mScheduleHelper.getDaysAndTimeSummary(mContext, schedule);
                return (desc != null) ? desc :
                        mContext.getResources().getString(
                                R.string.zen_mode_schedule_rule_days_none);
            }

            // handle event-based rules
            ZenModeConfig.EventInfo event =
                    ZenModeConfig.tryParseEventConditionId(rule.getConditionId());
            if (event != null) {
                if (event.calName != null) {
                    return event.calName;
                } else {
                    return mContext.getResources().getString(
                            R.string.zen_mode_event_rule_calendar_any);
                }
            }
        }

        return (rule == null || !rule.isEnabled())
                ? mContext.getResources().getString(R.string.switch_off_text)
                : mContext.getResources().getString(R.string.switch_on_text);
    }
}
