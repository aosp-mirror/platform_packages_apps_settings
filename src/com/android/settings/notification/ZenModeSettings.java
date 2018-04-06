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
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.content.Context;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
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
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle(), getFragmentManager());
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_interruptions;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle, FragmentManager fragmentManager) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ZenModeBehaviorPreferenceController(context, lifecycle));
        controllers.add(new ZenModeBlockedEffectsPreferenceController(context, lifecycle));
        controllers.add(new ZenModeDurationPreferenceController(context, lifecycle,
                fragmentManager));
        controllers.add(new ZenModeAutomationPreferenceController(context));
        controllers.add(new ZenModeButtonPreferenceController(context, lifecycle, fragmentManager));
        controllers.add(new ZenModeSettingsFooterPreferenceController(context, lifecycle));
        return controllers;
    }

    public static class SummaryBuilder {

        private Context mContext;

        public SummaryBuilder(Context context) {
            mContext = context;
        }

        // these should match NotificationManager.Policy#ALL_PRIORITY_CATEGORIES
        private static final int[] ALL_PRIORITY_CATEGORIES = {
                Policy.PRIORITY_CATEGORY_ALARMS,
                Policy.PRIORITY_CATEGORY_MEDIA,
                Policy.PRIORITY_CATEGORY_SYSTEM,
                Policy.PRIORITY_CATEGORY_REMINDERS,
                Policy.PRIORITY_CATEGORY_EVENTS,
                Policy.PRIORITY_CATEGORY_MESSAGES,
                Policy.PRIORITY_CATEGORY_CALLS,
                Policy.PRIORITY_CATEGORY_REPEAT_CALLERS,
        };

        String getBehaviorSettingSummary(Policy policy, int zenMode) {
            List<String> enabledCategories = getEnabledCategories(policy);

            int numCategories = enabledCategories.size();
            if (numCategories == 0) {
                return mContext.getString(R.string.zen_mode_no_exceptions);
            } else if (numCategories == 1) {
                return enabledCategories.get(0);
            } else if (numCategories == 2) {
                return mContext.getString(R.string.join_two_items, enabledCategories.get(0),
                        enabledCategories.get(1).toLowerCase());
            } else if (numCategories == 3){
                String secondaryText = mContext.getString(R.string.join_two_unrelated_items,
                        enabledCategories.get(0), enabledCategories.get(1).toLowerCase());
                return mContext.getString(R.string.join_many_items_last, secondaryText,
                        enabledCategories.get(2).toLowerCase());
            } else {
                String secondaryText = mContext.getString(R.string.join_many_items_middle,
                        enabledCategories.get(0), enabledCategories.get(1).toLowerCase());
                secondaryText = mContext.getString(R.string.join_many_items_middle, secondaryText,
                        enabledCategories.get(2).toLowerCase());
                return mContext.getString(R.string.join_many_items_last, secondaryText,
                        mContext.getString(R.string.zen_mode_other_options));
            }
        }

        String getSoundSummary() {
            int zenMode = NotificationManager.from(mContext).getZenMode();

            if (zenMode != Settings.Global.ZEN_MODE_OFF) {
                ZenModeConfig config = NotificationManager.from(mContext).getZenModeConfig();
                String description = ZenModeConfig.getDescription(mContext, true, config, false);

                if (description == null) {
                    return mContext.getString(R.string.zen_mode_sound_summary_on);
                } else {
                    return mContext.getString(R.string.zen_mode_sound_summary_on_with_info,
                            description);
                }
            } else {
                final int count = getEnabledAutomaticRulesCount();
                if (count > 0) {
                    return mContext.getString(R.string.zen_mode_sound_summary_off_with_info,
                            mContext.getResources().getQuantityString(
                                    R.plurals.zen_mode_sound_summary_summary_off_info,
                                    count, count));
                }

                return mContext.getString(R.string.zen_mode_sound_summary_off);
            }
        }

        String getBlockedEffectsSummary(Policy policy) {
            if (policy.suppressedVisualEffects == 0) {
                return mContext.getResources().getString(
                        R.string.zen_mode_block_effect_summary_sound);
            } else if (Policy.areAllVisualEffectsSuppressed(policy.suppressedVisualEffects)) {
                return mContext.getResources().getString(
                        R.string.zen_mode_block_effect_summary_all);
            }
            return mContext.getResources().getString(
                    R.string.zen_mode_block_effect_summary_some);
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
                    } else if (category == Policy.PRIORITY_CATEGORY_MEDIA) {
                        enabledCategories.add(mContext.getString(
                                R.string.zen_mode_media));
                    } else if (category == Policy.PRIORITY_CATEGORY_SYSTEM) {
                        enabledCategories.add(mContext.getString(
                                R.string.zen_mode_system));
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
                    keys.add(ZenModeDurationPreferenceController.KEY);
                    keys.add(ZenModeButtonPreferenceController.KEY);
                    return keys;
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(context, null, null);
                }
            };
}
