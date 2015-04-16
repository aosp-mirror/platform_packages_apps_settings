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

import static android.service.notification.ZenModeConfig.ALL_DAYS;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.service.notification.ConditionProviderService;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ScheduleInfo;
import android.service.notification.ZenModeConfig.ZenRule;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.notification.ManagedServiceSettings.Config;
import com.android.settings.notification.ZenRuleNameDialog.RuleInfo;
import com.android.settings.widget.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TreeSet;

public class ZenModeAutomationSettings extends ZenModeSettingsBase {
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("EEE");

    static final Config CONFIG = getConditionProviderConfig();

    private final Calendar mCalendar = Calendar.getInstance();

    private ServiceListing mServiceListing;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.zen_mode_automation_settings);
        mServiceListing = new ServiceListing(mContext, CONFIG);
        mServiceListing.addCallback(mServiceListingCallback);
        mServiceListing.reload();
        mServiceListing.setListening(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final FloatingActionButton fab = getFloatingActionButton();
        fab.setVisibility(View.VISIBLE);
        fab.setImageResource(R.drawable.ic_menu_add_white);
        fab.setContentDescription(getString(R.string.zen_mode_time_add_rule));
        fab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddRuleDialog();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mServiceListing.setListening(false);
        mServiceListing.removeCallback(mServiceListingCallback);
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

    private void showAddRuleDialog() {
        new ZenRuleNameDialog(mContext, mServiceListing, null, mConfig.getAutomaticRuleNames()) {
            @Override
            public void onOk(String ruleName, RuleInfo ri) {
                final ZenRule rule = new ZenRule();
                rule.name = ruleName;
                rule.enabled = true;
                rule.zenMode = Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
                rule.conditionId = ri.defaultConditionId;
                rule.component = ri.serviceComponent;
                final ZenModeConfig newConfig = mConfig.copy();
                final String ruleId = newConfig.newRuleId();
                newConfig.automaticRules.put(ruleId, rule);
                if (setZenModeConfig(newConfig)) {
                    showRule(ri.settingsAction, ri.configurationActivity, ruleId, rule.name);
                }
            }
        }.show();
    }

    private void showRule(String settingsAction, ComponentName configurationActivity,
            String ruleId, String ruleName) {
        if (DEBUG) Log.d(TAG, "showRule " + ruleId + " name=" + ruleName);
        mContext.startActivity(new Intent(settingsAction)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ZenModeRuleSettingsBase.EXTRA_RULE_ID, ruleId));
    }

    private void updateControls() {
        final PreferenceScreen root = getPreferenceScreen();
        root.removeAll();
        if (mConfig == null) return;
        for (int i = 0; i < mConfig.automaticRules.size(); i++) {
            final String id = mConfig.automaticRules.keyAt(i);
            final ZenRule rule = mConfig.automaticRules.valueAt(i);
            final boolean isSchedule = ZenModeConfig.isValidScheduleConditionId(rule.conditionId);
            final Preference p = new Preference(mContext);
            p.setTitle(rule.name);
            p.setSummary(computeRuleSummary(rule));
            p.setPersistent(false);
            p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final String action = isSchedule ? ZenModeScheduleRuleSettings.ACTION
                            : ZenModeExternalRuleSettings.ACTION;
                    showRule(action, null, id, rule.name);
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

    private String computeRuleSummary(ZenRule rule) {
        if (rule == null || !rule.enabled) return getString(R.string.switch_off_text);
        final ScheduleInfo schedule = ZenModeConfig.tryParseScheduleConditionId(rule.conditionId);
        final String mode = ZenModeSettings.computeZenModeCaption(getResources(), rule.zenMode);
        String summary = getString(R.string.switch_on_text);
        if (schedule != null) {
            final String days = computeContiguousDayRanges(schedule.days);
            final String start = getTime(schedule.startHour, schedule.startMinute);
            final String end = getTime(schedule.endHour, schedule.endMinute);
            final String time = getString(R.string.summary_range_verbal_combination, start, end);
            summary = getString(R.string.zen_mode_rule_summary_combination, days, time);
        }
        return getString(R.string.zen_mode_rule_summary_combination, summary, mode);
    }

    private String getTime(int hour, int minute) {
        mCalendar.set(Calendar.HOUR_OF_DAY, hour);
        mCalendar.set(Calendar.MINUTE, minute);
        return DateFormat.getTimeFormat(mContext).format(mCalendar.getTime());
    }

    private String computeContiguousDayRanges(int[] days) {
        final TreeSet<Integer> daySet = new TreeSet<>();
        for (int i = 0; days != null && i < days.length; i++) {
            daySet.add(days[i]);
        }
        if (daySet.isEmpty()) {
            return getString(R.string.zen_mode_schedule_rule_days_none);
        }
        final int N = ALL_DAYS.length;
        if (daySet.size() == N) {
            return getString(R.string.zen_mode_schedule_rule_days_all);
        }
        String rt = null;
        for (int i = 0; i < N; i++) {
            final int startDay = ALL_DAYS[i];
            final boolean active = daySet.contains(startDay);
            if (!active) continue;
            int end = 0;
            while (daySet.contains(ALL_DAYS[(i + end + 1) % N])) {
                end++;
            }
            if (!(i == 0 && daySet.contains(ALL_DAYS[N - 1]))) {
                final String v = end == 0 ? dayString(startDay)
                        : getString(R.string.summary_range_symbol_combination,
                                dayString(startDay),
                                dayString(ALL_DAYS[(i + end) % N]));
                rt = rt == null ? v : getString(R.string.summary_divider_text, rt, v);
            }
            i += end;
        }
        return rt;
    }

    private String dayString(int day) {
        mCalendar.set(Calendar.DAY_OF_WEEK, day);
        return DAY_FORMAT.format(mCalendar.getTime());
    }

    private static Config getConditionProviderConfig() {
        final Config c = new Config();
        c.tag = TAG;
        c.setting = Settings.Secure.ENABLED_CONDITION_PROVIDERS;
        c.intentAction = ConditionProviderService.SERVICE_INTERFACE;
        c.permission = android.Manifest.permission.BIND_CONDITION_PROVIDER_SERVICE;
        c.noun = "condition provider";
        return c;
    }

    private final ServiceListing.Callback mServiceListingCallback = new ServiceListing.Callback() {
        @Override
        public void onServicesReloaded(List<ServiceInfo> services) {
            for (ServiceInfo service : services) {
                final RuleInfo ri = ZenModeExternalRuleSettings.getRuleInfo(service);
                if (ri != null && ri.serviceComponent != null
                        && ri.settingsAction == ZenModeExternalRuleSettings.ACTION) {
                    if (!mServiceListing.isEnabled(ri.serviceComponent)) {
                        Log.i(TAG, "Enabling external condition provider: " + ri.serviceComponent);
                        mServiceListing.setEnabled(ri.serviceComponent, true);
                    }
                }
            }
        }
    };

}
