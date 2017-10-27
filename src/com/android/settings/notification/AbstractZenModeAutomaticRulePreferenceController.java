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
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.provider.Settings;
import android.service.notification.ConditionProviderService;
import android.service.notification.ZenModeConfig;
import android.support.v7.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

abstract public class AbstractZenModeAutomaticRulePreferenceController extends
        AbstractPreferenceController implements PreferenceControllerMixin {

    private static final String TAG = "ZenModeAutomaticRule";
    protected ZenModeBackend mBackend;
    protected Fragment mParent;
    protected Set<Map.Entry<String, AutomaticZenRule>> mRules;
    protected PackageManager mPm;

    public AbstractZenModeAutomaticRulePreferenceController(Context context, Fragment parent) {
        super(context);
        mBackend = ZenModeBackend.getInstance(context);
        mParent = parent;
        mPm = mContext.getPackageManager();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mRules = getZenModeRules();
    }

    private Set<Map.Entry<String, AutomaticZenRule>> getZenModeRules() {
        Map<String, AutomaticZenRule> ruleMap =
                NotificationManager.from(mContext).getAutomaticZenRules();
        return ruleMap.entrySet();
    }

    protected void showNameRuleDialog(final ZenRuleInfo ri) {
        new ZenRuleNameDialog(mContext, null, ri.defaultConditionId) {
            @Override
            public void onOk(String ruleName) {
                AutomaticZenRule rule = new AutomaticZenRule(ruleName, ri.serviceComponent,
                        ri.defaultConditionId, NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                        true);
                String savedRuleId = mBackend.addZenRule(rule);
                if (savedRuleId != null) {
                    mParent.startActivity(getRuleIntent(ri.settingsAction, null, savedRuleId));
                }
            }
        }.show();
    }

    protected Map.Entry<String, AutomaticZenRule>[] sortedRules() {
        if (mRules == null) {
            mRules = getZenModeRules();
        }
        final Map.Entry<String, AutomaticZenRule>[] rt =
                mRules.toArray(new Map.Entry[mRules.size()]);
        Arrays.sort(rt, RULE_COMPARATOR);
        return rt;
    }

    protected static Intent getRuleIntent(String settingsAction,
            ComponentName configurationActivity, String ruleId) {
        final Intent intent = new Intent()
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ConditionProviderService.EXTRA_RULE_ID, ruleId);
        if (configurationActivity != null) {
            intent.setComponent(configurationActivity);
        } else {
            intent.setAction(settingsAction);
        }
        return intent;
    }

    private static final Comparator<Map.Entry<String, AutomaticZenRule>> RULE_COMPARATOR =
            new Comparator<Map.Entry<String, AutomaticZenRule>>() {
                @Override
                public int compare(Map.Entry<String, AutomaticZenRule> lhs,
                        Map.Entry<String, AutomaticZenRule> rhs) {
                    int byDate = Long.compare(lhs.getValue().getCreationTime(),
                            rhs.getValue().getCreationTime());
                    if (byDate != 0) {
                        return byDate;
                    } else {
                        return key(lhs.getValue()).compareTo(key(rhs.getValue()));
                    }
                }

                private String key(AutomaticZenRule rule) {
                    final int type = ZenModeConfig.isValidScheduleConditionId(rule.getConditionId())
                            ? 1 : ZenModeConfig.isValidEventConditionId(rule.getConditionId())
                            ? 2 : 3;
                    return type + rule.getName().toString();
                }
            };

    public static ZenRuleInfo getRuleInfo(PackageManager pm, ServiceInfo si) {
        if (si == null || si.metaData == null) {
            return null;
        }
        final String ruleType = si.metaData.getString(ConditionProviderService.META_DATA_RULE_TYPE);
        final ComponentName configurationActivity = getSettingsActivity(si);
        if (ruleType != null && !ruleType.trim().isEmpty() && configurationActivity != null) {
            final ZenRuleInfo ri = new ZenRuleInfo();
            ri.serviceComponent = new ComponentName(si.packageName, si.name);
            ri.settingsAction = Settings.ACTION_ZEN_MODE_EXTERNAL_RULE_SETTINGS;
            ri.title = ruleType;
            ri.packageName = si.packageName;
            ri.configurationActivity = getSettingsActivity(si);
            ri.packageLabel = si.applicationInfo.loadLabel(pm);
            ri.ruleInstanceLimit =
                    si.metaData.getInt(ConditionProviderService.META_DATA_RULE_INSTANCE_LIMIT, -1);
            return ri;
        }
        return null;
    }

    protected static ComponentName getSettingsActivity(ServiceInfo si) {
        if (si == null || si.metaData == null) {
            return null;
        }
        final String configurationActivity =
                si.metaData.getString(ConditionProviderService.META_DATA_CONFIGURATION_ACTIVITY);
        if (configurationActivity != null) {
            return ComponentName.unflattenFromString(configurationActivity);
        }
        return null;
    }
}
