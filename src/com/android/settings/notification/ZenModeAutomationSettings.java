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

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings.Global;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ScheduleInfo;
import android.service.notification.ZenModeConfig.ZenRule;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;

public class ZenModeAutomationSettings extends ZenModeSettingsBase {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setHasOptionsMenu(true);

        addPreferencesFromResource(R.xml.zen_mode_automation_settings);
    }

    private void showRule(String ruleId, String ruleName) {
        if (DEBUG) Log.d(TAG, "showRule " + ruleId + " name=" + ruleName);
        mContext.startActivity(new Intent(ZenModeScheduleRuleSettings.ACTION)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ZenModeScheduleRuleSettings.EXTRA_RULE_ID, ruleId));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.zen_mode_automation, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add) {
            showAddRuleDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddRuleDialog() {
        new ZenRuleNameDialog(mContext, "", mConfig.getAutomaticRuleNames()) {
            @Override
            public void onOk(String ruleName) {
                final ScheduleInfo schedule = new ScheduleInfo();
                schedule.days = ZenModeConfig.ALL_DAYS;
                schedule.startHour = 22;
                schedule.endHour = 7;
                final ZenRule rule = new ZenRule();
                rule.name = ruleName;
                rule.enabled = true;
                rule.zenMode = Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
                rule.conditionId = ZenModeConfig.toScheduleConditionId(schedule);
                final ZenModeConfig newConfig = mConfig.copy();
                final String ruleId = newConfig.newRuleId();
                newConfig.automaticRules.put(ruleId, rule);
                if (setZenModeConfig(newConfig)) {
                    showRule(ruleId, rule.name);
                }
            }
        }.show();
    }

    @Override
    protected void onZenModeChanged() {
        // don't care
    }

    @Override
    protected void onZenModeConfigChanged() {
        updateControls();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateControls();
    }

    private void updateControls() {
        final PreferenceScreen root = getPreferenceScreen();
        root.removeAll();

        if (mConfig == null) return;
        for (int i = 0; i < mConfig.automaticRules.size(); i++) {
            final String id = mConfig.automaticRules.keyAt(i);
            final ZenRule rule = mConfig.automaticRules.valueAt(i);
            if (!ZenModeConfig.isValidScheduleConditionId(rule.conditionId)) continue;
            final Preference p = new Preference(mContext);
            p.setTitle(rule.name);
            p.setSummary(rule.enabled ? R.string.switch_on_text : R.string.switch_off_text);
            p.setPersistent(false);
            p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showRule(id, rule.name);
                    return true;
                }
            });
            root.addPreference(p);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_ZEN_MODE_AUTOMATION;
    }

}
