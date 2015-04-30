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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.service.notification.ZenModeConfig.ZenRule;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.notification.ZenRuleNameDialog.RuleInfo;

public class ZenModeExternalRuleSettings extends ZenModeRuleSettingsBase {
    private static final String KEY_TYPE = "type";
    private static final String KEY_CONFIGURE = "configure";

    public static final String ACTION = Settings.ACTION_ZEN_MODE_EXTERNAL_RULE_SETTINGS;
    private static final int REQUEST_CODE_CONFIGURE = 1;

    private static final String MD_RULE_TYPE = "automatic.ruleType";
    private static final String MD_DEFAULT_CONDITION_ID = "automatic.defaultConditionId";
    private static final String MD_CONFIGURATION_ACTIVITY = "automatic.configurationActivity";
    private static final String EXTRA_CONDITION_ID = "automatic.conditionId";

    private Preference mType;
    private Preference mConfigure;

    @Override
    protected boolean setRule(ZenRule rule) {
        return rule != null;
    }

    @Override
    protected String getZenModeDependency() {
        return null;
    }

    @Override
    protected int getEnabledToastText() {
        return 0;
    }

    @Override
    protected void onCreateInternal() {
        addPreferencesFromResource(R.xml.zen_mode_external_rule_settings);
        final PreferenceScreen root = getPreferenceScreen();
        final ServiceInfo si = ServiceListing.findService(mContext,
                ZenModeAutomationSettings.CONFIG, mRule.component);
        if (DEBUG) Log.d(TAG, "ServiceInfo: " + si);
        final RuleInfo ri = getRuleInfo(si);
        if (DEBUG) Log.d(TAG, "RuleInfo: " + ri);
        mType = root.findPreference(KEY_TYPE);
        if (ri == null) {
            mType.setSummary(R.string.zen_mode_rule_type_unknown);
        } else {
            mType.setSummary(ri.caption);
        }

        mConfigure = root.findPreference(KEY_CONFIGURE);
        if (ri == null || ri.configurationActivity == null) {
            mConfigure.setEnabled(false);
        } else {
            mConfigure.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivityForResult(new Intent().setComponent(ri.configurationActivity),
                            REQUEST_CODE_CONFIGURE);
                    return true;
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CONFIGURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                final Uri conditionId = data.getParcelableExtra(EXTRA_CONDITION_ID);
                if (conditionId != null && !conditionId.equals(mRule.conditionId)) {
                    updateRule(conditionId);
                }
            }
        }
    }

    public static RuleInfo getRuleInfo(ServiceInfo si) {
        if (si == null || si.metaData == null) return null;
        final String ruleType = si.metaData.getString(MD_RULE_TYPE);
        final String defaultConditionId = si.metaData.getString(MD_DEFAULT_CONDITION_ID);
        final String configurationActivity = si.metaData.getString(MD_CONFIGURATION_ACTIVITY);
        if (ruleType != null && !ruleType.trim().isEmpty() && defaultConditionId != null) {
            final RuleInfo ri = new RuleInfo();
            ri.serviceComponent = new ComponentName(si.packageName, si.name);
            ri.settingsAction = ZenModeExternalRuleSettings.ACTION;
            ri.caption = ruleType;
            ri.defaultConditionId = Uri.parse(defaultConditionId);
            if (configurationActivity != null) {
                ri.configurationActivity = ComponentName.unflattenFromString(configurationActivity);
            }
            return ri;
        }
        return null;
    }

    @Override
    protected void updateControlsInternal() {
        // everything done up front
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_ZEN_MODE_EXTERNAL_RULE;
    }

}
