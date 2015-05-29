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
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.service.notification.ConditionProviderService;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.EventInfo;
import android.service.notification.ZenModeConfig.ScheduleInfo;
import android.service.notification.ZenModeConfig.ZenRule;
import android.text.format.DateFormat;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.notification.ManagedServiceSettings.Config;
import com.android.settings.notification.ZenModeEventRuleSettings.CalendarInfo;
import com.android.settings.notification.ZenRuleNameDialog.RuleInfo;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class ZenModeAutomationSettings extends ZenModeSettingsBase {

    static final Config CONFIG = getConditionProviderConfig();

    // per-instance to ensure we're always using the current locale
    private final SimpleDateFormat mDayFormat = new SimpleDateFormat("EEE");
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
                MetricsLogger.action(mContext, MetricsLogger.ACTION_ZEN_ADD_RULE_OK);
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

    private ZenRuleInfo[] sortedRules() {
        final ZenRuleInfo[] rt = new ZenRuleInfo[mConfig.automaticRules.size()];
        for (int i = 0; i < rt.length; i++) {
            final ZenRuleInfo zri = new ZenRuleInfo();
            zri.id = mConfig.automaticRules.keyAt(i);
            zri.rule = mConfig.automaticRules.valueAt(i);
            rt[i] = zri;
        }
        Arrays.sort(rt, RULE_COMPARATOR);
        return rt;
    }

    private void updateControls() {
        final PreferenceScreen root = getPreferenceScreen();
        root.removeAll();
        if (mConfig == null) return;
        final ZenRuleInfo[] sortedRules = sortedRules();
        for (int i = 0; i < sortedRules.length; i++) {
            final String id = sortedRules[i].id;
            final ZenRule rule = sortedRules[i].rule;
            final boolean isSchedule = ZenModeConfig.isValidScheduleConditionId(rule.conditionId);
            final boolean isEvent = ZenModeConfig.isValidEventConditionId(rule.conditionId);
            final Preference p = new Preference(mContext);
            p.setIcon(isSchedule ? R.drawable.ic_schedule
                    : isEvent ? R.drawable.ic_event
                    : R.drawable.ic_label);
            p.setTitle(rule.name);
            p.setSummary(computeRuleSummary(rule));
            p.setPersistent(false);
            p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final String action = isSchedule ? ZenModeScheduleRuleSettings.ACTION
                            : isEvent ? ZenModeEventRuleSettings.ACTION
                            : ZenModeExternalRuleSettings.ACTION;
                    showRule(action, null, id, rule.name);
                    return true;
                }
            });
            root.addPreference(p);
        }
        final Preference p = new Preference(mContext);
        p.setIcon(R.drawable.ic_add);
        p.setTitle(R.string.zen_mode_add_rule);
        p.setPersistent(false);
        p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MetricsLogger.action(mContext, MetricsLogger.ACTION_ZEN_ADD_RULE);
                showAddRuleDialog();
                return true;
            }
        });
        root.addPreference(p);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_ZEN_MODE_AUTOMATION;
    }

    private String computeRuleSummary(ZenRule rule) {
        if (rule == null || !rule.enabled) return getString(R.string.switch_off_text);
        final String mode = computeZenModeCaption(getResources(), rule.zenMode);
        String summary = getString(R.string.switch_on_text);
        final ScheduleInfo schedule = ZenModeConfig.tryParseScheduleConditionId(rule.conditionId);
        final EventInfo event = ZenModeConfig.tryParseEventConditionId(rule.conditionId);
        if (schedule != null) {
            summary = computeScheduleRuleSummary(schedule);
        } else if (event != null) {
            summary = computeEventRuleSummary(event);
        }
        return getString(R.string.zen_mode_rule_summary_combination, summary, mode);
    }

    private String computeScheduleRuleSummary(ScheduleInfo schedule) {
        final String days = computeContiguousDayRanges(schedule.days);
        final String start = getTime(schedule.startHour, schedule.startMinute);
        final String end = getTime(schedule.endHour, schedule.endMinute);
        final String time = getString(R.string.summary_range_verbal_combination, start, end);
        return getString(R.string.zen_mode_rule_summary_combination, days, time);
    }

    private String computeEventRuleSummary(EventInfo event) {
        final String calendar = getString(R.string.zen_mode_event_rule_summary_calendar_template,
                computeCalendarName(event));
        final String reply = getString(R.string.zen_mode_event_rule_summary_reply_template,
                getString(computeReply(event)));
        return getString(R.string.zen_mode_rule_summary_combination, calendar, reply);
    }

    private String computeCalendarName(EventInfo event) {
        return event.calendar != null ? event.calendar
                : getString(R.string.zen_mode_event_rule_summary_any_calendar);
    }

    private int computeReply(EventInfo event) {
        switch (event.reply) {
            case EventInfo.REPLY_ANY_EXCEPT_NO:
                return R.string.zen_mode_event_rule_reply_any_except_no;
            case EventInfo.REPLY_YES:
                return R.string.zen_mode_event_rule_reply_yes;
            case EventInfo.REPLY_YES_OR_MAYBE:
                return R.string.zen_mode_event_rule_reply_yes_or_maybe;
            default:
                throw new IllegalArgumentException("Bad reply: " + event.reply);
        }
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
        return mDayFormat.format(mCalendar.getTime());
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

    private static String computeZenModeCaption(Resources res, int zenMode) {
        switch (zenMode) {
            case Global.ZEN_MODE_ALARMS:
                return res.getString(R.string.zen_mode_option_alarms);
            case Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                return res.getString(R.string.zen_mode_option_important_interruptions);
            case Global.ZEN_MODE_NO_INTERRUPTIONS:
                return res.getString(R.string.zen_mode_option_no_interruptions);
            default:
                return null;
        }
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

    private static final Comparator<ZenRuleInfo> RULE_COMPARATOR = new Comparator<ZenRuleInfo>() {
        @Override
        public int compare(ZenRuleInfo lhs, ZenRuleInfo rhs) {
            return key(lhs).compareTo(key(rhs));
        }

        private String key(ZenRuleInfo zri) {
            final ZenRule rule = zri.rule;
            final int type = ZenModeConfig.isValidScheduleConditionId(rule.conditionId) ? 1
                    : ZenModeConfig.isValidEventConditionId(rule.conditionId) ? 2
                    : 3;
            return type + rule.name;
        }
    };

    private static class ZenRuleInfo {
        String id;
        ZenRule rule;
    }

}
