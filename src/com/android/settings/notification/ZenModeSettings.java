/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.app.NotificationManager.Policy;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.service.notification.ConditionProviderService;
import android.service.notification.ZenModeConfig;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.utils.ManagedServiceSettings;
import com.android.settings.utils.ZenServiceListing;
import com.android.settingslib.TwoTargetPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ZenModeSettings extends ZenModeSettingsBase implements Indexable {

    public static final String KEY_VISUAL_SETTINGS = "zen_mode_visual_interruptions_settings";
    private static final String KEY_BEHAVIOR_SETTINGS = "zen_mode_behavior_settings";
    private static final String KEY_AUTOMATIC_RULES = "zen_mode_automatic_rules";

    static final ManagedServiceSettings.Config CONFIG = getConditionProviderConfig();

    private PreferenceCategory mAutomaticRules;
    private Preference mBehaviorSettings;
    private Preference mVisualSettings;
    private Policy mPolicy;
    private SummaryBuilder mSummaryBuilder;
    private PackageManager mPm;
    private ZenServiceListing mServiceListing;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.zen_mode_settings);
        final PreferenceScreen root = getPreferenceScreen();

        mAutomaticRules = (PreferenceCategory) root.findPreference(KEY_AUTOMATIC_RULES);
        mBehaviorSettings = root.findPreference(KEY_BEHAVIOR_SETTINGS);
        mVisualSettings = root.findPreference(KEY_VISUAL_SETTINGS);
        mPolicy = NotificationManager.from(mContext).getNotificationPolicy();
        mSummaryBuilder = new SummaryBuilder(getContext());
        mPm = mContext.getPackageManager();
        mServiceListing = new ZenServiceListing(mContext, CONFIG);
        mServiceListing.reloadApprovedServices();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isUiRestricted()) {
            return;
        }
        updateControls();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_ZEN_MODE;
    }

    @Override
    protected void onZenModeChanged() {
        updateControls();
    }

    @Override
    protected void onZenModeConfigChanged() {
        mPolicy = NotificationManager.from(mContext).getNotificationPolicy();
        updateControls();
    }

    private void updateControls() {
        updateBehaviorSettingsSummary();
        updateVisualSettingsSummary();
        updateAutomaticRules();
    }

    private void updateBehaviorSettingsSummary() {
        mBehaviorSettings.setSummary(mSummaryBuilder.getBehaviorSettingSummary(mPolicy, mZenMode));
    }

    private void updateVisualSettingsSummary() {
        mVisualSettings.setSummary(mSummaryBuilder.getVisualSettingSummary(mPolicy));
    }

    private void updateAutomaticRules() {
        mAutomaticRules.removeAll();
        final Map.Entry<String,AutomaticZenRule>[] sortedRules = sortedRules();
        for (Map.Entry<String,AutomaticZenRule> sortedRule : sortedRules) {
            ZenRulePreference pref = new ZenRulePreference(getPrefContext(), sortedRule);
            if (pref.appExists) {
                mAutomaticRules.addPreference(pref);
            }
        }
        final Preference p = new Preference(getPrefContext());
        p.setIcon(R.drawable.ic_menu_add);
        p.setTitle(R.string.zen_mode_add_rule);
        p.setPersistent(false);
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_ZEN_ADD_RULE);
                showAddRuleDialog();
                return true;
            }
        });
        mAutomaticRules.addPreference(p);
    }

    private void showAddRuleDialog() {
        new ZenRuleSelectionDialog(mContext, mServiceListing) {
            @Override
            public void onSystemRuleSelected(ZenRuleInfo ri) {
                showNameRuleDialog(ri);
            }

            @Override
            public void onExternalRuleSelected(ZenRuleInfo ri) {
                Intent intent = new Intent().setComponent(ri.configurationActivity);
                startActivity(intent);
            }
        }.show();
    }

    private String computeRuleSummary(AutomaticZenRule rule, boolean isSystemRule,
            CharSequence providerLabel) {
        final String mode = computeZenModeCaption(getResources(), rule.getInterruptionFilter());
        final String ruleState = (rule == null || !rule.isEnabled())
                ? getString(R.string.switch_off_text)
                : getString(R.string.zen_mode_rule_summary_enabled_combination, mode);

        return ruleState;
    }

    private static ManagedServiceSettings.Config getConditionProviderConfig() {
        final ManagedServiceSettings.Config c = new ManagedServiceSettings.Config();
        c.tag = TAG;
        c.intentAction = ConditionProviderService.SERVICE_INTERFACE;
        c.permission = android.Manifest.permission.BIND_CONDITION_PROVIDER_SERVICE;
        c.noun = "condition provider";
        return c;
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

    public static ZenRuleInfo getRuleInfo(PackageManager pm, ServiceInfo si) {
        if (si == null || si.metaData == null) return null;
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

    private static ComponentName getSettingsActivity(ServiceInfo si) {
        if (si == null || si.metaData == null) return null;
        final String configurationActivity =
                si.metaData.getString(ConditionProviderService.META_DATA_CONFIGURATION_ACTIVITY);
        if (configurationActivity != null) {
            return ComponentName.unflattenFromString(configurationActivity);
        }
        return null;
    }

    private void showNameRuleDialog(final ZenRuleInfo ri) {
        new ZenRuleNameDialog(mContext, null) {
            @Override
            public void onOk(String ruleName) {
                mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_ZEN_ADD_RULE_OK);
                AutomaticZenRule rule = new AutomaticZenRule(ruleName, ri.serviceComponent,
                        ri.defaultConditionId, NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                        true);
                String savedRuleId = addZenRule(rule);
                if (savedRuleId != null) {
                    startActivity(getRuleIntent(ri.settingsAction, null, savedRuleId));
                }
            }
        }.show();
    }

    private void showDeleteRuleDialog(final String ruleId, final CharSequence ruleName) {
        new AlertDialog.Builder(mContext)
                .setMessage(getString(R.string.zen_mode_delete_rule_confirmation, ruleName))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.zen_mode_delete_rule_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mMetricsFeatureProvider.action(mContext,
                                        MetricsEvent.ACTION_ZEN_DELETE_RULE_OK);
                                removeZenRule(ruleId);
                            }
                        })
                .show();
    }

    private Intent getRuleIntent(String settingsAction, ComponentName configurationActivity,
            String ruleId) {
        Intent intent = new Intent()
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ConditionProviderService.EXTRA_RULE_ID, ruleId);
        if (configurationActivity != null) {
            intent.setComponent(configurationActivity);
        } else {
            intent.setAction(settingsAction);
        }
        return intent;
    }

    private Map.Entry<String,AutomaticZenRule>[] sortedRules() {
        final Map.Entry<String,AutomaticZenRule>[] rt =
                mRules.toArray(new Map.Entry[mRules.size()]);
        Arrays.sort(rt, RULE_COMPARATOR);
        return rt;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_interruptions;
    }

    private class ZenRulePreference extends TwoTargetPreference {
        final CharSequence mName;
        final String mId;
        final boolean appExists;

        public ZenRulePreference(Context context,
                final Map.Entry<String, AutomaticZenRule> ruleEntry) {
            super(context);

            final AutomaticZenRule rule = ruleEntry.getValue();
            mName = rule.getName();
            mId = ruleEntry.getKey();

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
            ComponentName settingsActivity = getSettingsActivity(si);
            setIntent(getRuleIntent(action, settingsActivity, mId));
            setSelectable(settingsActivity != null || isSystemRule);
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
                showDeleteRuleDialog(mId, mName);
            }
        };
    }

    public static class SummaryBuilder {

        private Context mContext;

        public SummaryBuilder(Context context) {
            mContext = context;
        }

        private static final int[] ALL_PRIORITY_CATEGORIES = {
                Policy.PRIORITY_CATEGORY_ALARMS,
                Policy.PRIORITY_CATEGORY_MEDIA_SYSTEM_OTHER,
                Policy.PRIORITY_CATEGORY_REMINDERS,
                Policy.PRIORITY_CATEGORY_EVENTS,
                Policy.PRIORITY_CATEGORY_MESSAGES,
                Policy.PRIORITY_CATEGORY_CALLS,
                Policy.PRIORITY_CATEGORY_REPEAT_CALLERS,
        };

        String getBehaviorSettingSummary(Policy policy, int zenMode) {
            List<String> enabledCategories;

            if (zenMode == Settings.Global.ZEN_MODE_NO_INTERRUPTIONS) {
                return mContext.getString(R.string.zen_mode_behavior_total_silence);
            } else if (zenMode == Settings.Global.ZEN_MODE_ALARMS) {
                return mContext.getString(R.string.zen_mode_behavior_alarms_only);
            } else {
                enabledCategories = getEnabledCategories(policy);
            }

            int numCategories = enabledCategories.size();
            if (numCategories == 0) {
                return mContext.getString(R.string.zen_mode_behavior_no_sound);
            }

            String s = enabledCategories.get(0).toLowerCase();
            for (int i = 1; i < numCategories; i++) {
                if (i == numCategories - 1) {
                    s = mContext.getString(R.string.join_many_items_last,
                            s, enabledCategories.get(i).toLowerCase());
                } else {
                    s = mContext.getString(R.string.join_many_items_middle,
                            s, enabledCategories.get(i).toLowerCase());
                }
            }

            return mContext.getString(R.string.zen_mode_behavior_no_sound_except, s);
        }

        String getVisualSettingSummary(Policy policy) {
            String s = mContext.getString(R.string.zen_mode_all_visual_interruptions);
            if (isEffectSuppressed(policy, Policy.SUPPRESSED_EFFECT_SCREEN_ON)
                && isEffectSuppressed(policy, Policy.SUPPRESSED_EFFECT_SCREEN_OFF)) {
                s = mContext.getString(R.string.zen_mode_no_visual_interruptions);
            } else if (isEffectSuppressed(policy, Policy.SUPPRESSED_EFFECT_SCREEN_ON)) {
                s = mContext.getString(R.string.zen_mode_screen_on_visual_interruptions);
            } else if (isEffectSuppressed(policy, Policy.SUPPRESSED_EFFECT_SCREEN_OFF)) {
                s = mContext.getString(R.string.zen_mode_screen_off_visual_interruptions);
            }
            return s;
        }

        String getAutomaticRulesSummary() {
            final int count = getEnabledAutomaticRulesCount();
            return count == 0 ? mContext.getString(R.string.zen_mode_settings_summary_off)
                : mContext.getResources().getQuantityString(
                    R.plurals.zen_mode_settings_summary_on, count, count);
        }

        @VisibleForTesting
        int getEnabledAutomaticRulesCount() {
            int count = 0;
            final Map<String, AutomaticZenRule> ruleMap =
                NotificationManager.from(mContext).getAutomaticZenRules();
            if (ruleMap != null) {
                for (Entry<String, AutomaticZenRule> ruleEntry : ruleMap.entrySet()) {
                    final AutomaticZenRule rule = ruleEntry.getValue();
                    if (rule != null && rule.isEnabled()) {
                        count++;
                    }
                }
            }
            return count;
        }

        private List<String> getEnabledCategories(Policy policy) {
            List<String> enabledCategories = new ArrayList<>();
            for (int category : ALL_PRIORITY_CATEGORIES) {
                if (isCategoryEnabled(policy, category)) {
                    if (category == Policy.PRIORITY_CATEGORY_ALARMS) {
                        enabledCategories.add(mContext.getString(R.string.zen_mode_alarms));
                    } else if (category == Policy.PRIORITY_CATEGORY_MEDIA_SYSTEM_OTHER) {
                        enabledCategories.add(mContext.getString(
                                R.string.zen_mode_media_system_other));
                    } else if (category == Policy.PRIORITY_CATEGORY_REMINDERS) {
                        enabledCategories.add(mContext.getString(R.string.zen_mode_reminders));
                    } else if (category == Policy.PRIORITY_CATEGORY_EVENTS) {
                        enabledCategories.add(mContext.getString(R.string.zen_mode_events));
                    } else if (category == Policy.PRIORITY_CATEGORY_MESSAGES) {
                        if (policy.priorityMessageSenders == Policy.PRIORITY_SENDERS_ANY) {
                            enabledCategories.add(mContext.getString(
                                    R.string.zen_mode_all_messages));
                        } else {
                            enabledCategories.add(mContext.getString(
                                    R.string.zen_mode_selected_messages));
                        }
                    } else if (category == Policy.PRIORITY_CATEGORY_CALLS) {
                        if (policy.priorityCallSenders == Policy.PRIORITY_SENDERS_ANY) {
                            enabledCategories.add(mContext.getString(
                                    R.string.zen_mode_all_callers));
                        } else {
                            enabledCategories.add(mContext.getString(
                                    R.string.zen_mode_selected_callers));
                        }
                    } else if (category == Policy.PRIORITY_CATEGORY_REPEAT_CALLERS) {
                        if (!enabledCategories.contains(mContext.getString(
                                R.string.zen_mode_all_callers))) {
                            enabledCategories.add(mContext.getString(
                                    R.string.zen_mode_repeat_callers));
                        }
                    }
                }
            }
            return enabledCategories;
        }

        private boolean isCategoryEnabled(Policy policy, int categoryType) {
            return (policy.priorityCategories & categoryType) != 0;
        }

        private boolean isEffectSuppressed(Policy policy, int effect) {
            return (policy.suppressedVisualEffects & effect) != 0;
        }
    }

    private static final Comparator<Map.Entry<String,AutomaticZenRule>> RULE_COMPARATOR =
            new Comparator<Map.Entry<String,AutomaticZenRule>>() {
                @Override
                public int compare(Map.Entry<String,AutomaticZenRule> lhs,
                        Map.Entry<String,AutomaticZenRule> rhs) {
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
                            ? 1
                            : ZenModeConfig.isValidEventConditionId(rule.getConditionId())
                                    ? 2
                                    : 3;
                    return type + rule.getName().toString();
                }
            };
    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.zen_mode_settings;
                    return Arrays.asList(sir);
                }
            };
}
