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
import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.provider.Settings;
import android.service.notification.ConditionProviderService;
import android.util.Log;
import android.util.Slog;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.Map;
import java.util.Objects;

abstract public class AbstractZenModeAutomaticRulePreferenceController extends
        AbstractZenModePreferenceController implements PreferenceControllerMixin {

    protected ZenModeBackend mBackend;
    protected Fragment mParent;
    protected Map.Entry<String, AutomaticZenRule>[] mRules;
    protected PackageManager mPm;

    public AbstractZenModeAutomaticRulePreferenceController(Context context, String key, Fragment
            parent, Lifecycle lifecycle) {
        super(context, key, lifecycle);
        mBackend = ZenModeBackend.getInstance(context);
        mPm = mContext.getPackageManager();
        mParent = parent;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mRules = mBackend.getAutomaticZenRules();
    }

    protected Map.Entry<String, AutomaticZenRule>[] getRules() {
        if (mRules == null) {
            mRules = mBackend.getAutomaticZenRules();
        }
        return mRules;
    }

    protected void showNameRuleDialog(final ZenRuleInfo ri, Fragment parent) {
        ZenRuleNameDialog.show(parent, null, ri.defaultConditionId, new
                RuleNameChangeListener(ri));
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

    public static ZenRuleInfo getRuleInfo(PackageManager pm, ComponentInfo ci) {
        if (ci == null || ci.metaData == null) {
            return null;
        }
        final String ruleType = (ci instanceof ServiceInfo)
                ? ci.metaData.getString(ConditionProviderService.META_DATA_RULE_TYPE)
                : ci.metaData.getString(NotificationManager.META_DATA_AUTOMATIC_RULE_TYPE);

        final ComponentName configurationActivity = getSettingsActivity(pm, null, ci);
        if (ruleType != null && !ruleType.trim().isEmpty() && configurationActivity != null) {
            final ZenRuleInfo ri = new ZenRuleInfo();
            ri.serviceComponent =
                    (ci instanceof ServiceInfo) ? new ComponentName(ci.packageName, ci.name) : null;
            ri.settingsAction = Settings.ACTION_ZEN_MODE_EXTERNAL_RULE_SETTINGS;
            ri.title = ruleType;
            ri.packageName = ci.packageName;
            ri.configurationActivity = configurationActivity;
            ri.packageLabel = ci.applicationInfo.loadLabel(pm);
            ri.ruleInstanceLimit = (ci instanceof ServiceInfo)
                    ? ci.metaData.getInt(ConditionProviderService.META_DATA_RULE_INSTANCE_LIMIT, -1)
                    : ci.metaData.getInt(NotificationManager.META_DATA_RULE_INSTANCE_LIMIT, -1);
            return ri;
        }
        return null;
    }

    protected static ComponentName getSettingsActivity(PackageManager pm, AutomaticZenRule rule,
            ComponentInfo ci) {
        String owner = rule != null ? rule.getPackageName() : ci.packageName;
        ComponentName settingsActivity = null;
        // prefer config activity on the rule itself; fallback to manifest definition
        if (rule != null && rule.getConfigurationActivity() != null) {
            settingsActivity = rule.getConfigurationActivity();
        } else {
            if (ci == null) {
                settingsActivity = null;
            } else if (ci instanceof ActivityInfo) {
                // new activity backed rule
                settingsActivity = new ComponentName(ci.packageName, ci.name);
            } else if (ci.metaData != null) {
                // old service backed rule
                final String configurationActivity = ci.metaData.getString(
                        ConditionProviderService.META_DATA_CONFIGURATION_ACTIVITY);
                if (configurationActivity != null) {
                    settingsActivity = ComponentName.unflattenFromString(configurationActivity);
                }
            }
        }
        if (settingsActivity == null || owner == null) {
            return settingsActivity;
        }
        try {
            int ownerUid = pm.getPackageUid(owner, 0);
            int configActivityOwnerUid = pm.getPackageUid(settingsActivity.getPackageName(), 0);
            if (ownerUid == configActivityOwnerUid) {
                return settingsActivity;
            } else {
                Log.w(TAG, "Config activity not in owner package for " + rule.getName());
                return null;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to find config activity");
            return null;
        }
    }

    public class RuleNameChangeListener implements ZenRuleNameDialog.PositiveClickListener {
        ZenRuleInfo mRuleInfo;

        public RuleNameChangeListener(ZenRuleInfo ruleInfo) {
            mRuleInfo = ruleInfo;
        }

        @Override
        public void onOk(String ruleName, Fragment parent) {
            mMetricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_ZEN_MODE_RULE_NAME_CHANGE_OK);
            AutomaticZenRule rule = new AutomaticZenRule(ruleName, mRuleInfo.serviceComponent,
                    mRuleInfo.configurationActivity, mRuleInfo.defaultConditionId, null,
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY, true);
            String savedRuleId = mBackend.addZenRule(rule);
            if (savedRuleId != null) {
                parent.startActivity(getRuleIntent(mRuleInfo.settingsAction, null,
                        savedRuleId));
            }
        }
    }
}
