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

import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.content.Context;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ZenModeSettings extends ZenModeSettingsBase {
    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_settings;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_ZEN_MODE;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_interruptions;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ZenModeBehaviorPreferenceController(context, lifecycle));
        controllers.add(new ZenModeAutomationPreferenceController(context));
        controllers.add(new ZenModeButtonPreferenceController(context, lifecycle));
        return controllers;
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

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.zen_mode_settings;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    keys.add(ZenModeButtonPreferenceController.KEY);
                    return keys;
                }

                @Override
                public List<AbstractPreferenceController> getPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(context, null);
                }
            };
}
