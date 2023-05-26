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

package com.android.settings.notification.zen;

import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CALLS;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CONVERSATIONS;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_EVENTS;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_REMINDERS;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_SYSTEM;

import android.app.Activity;
import android.app.Application;
import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.icu.text.MessageFormat;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

@SearchIndexable
public class ZenModeSettings extends ZenModeSettingsBase {
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_ZEN_MODE;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final Activity activity = getActivity();
        return buildPreferenceControllers(context, getSettingsLifecycle(), getFragmentManager(),
                activity != null ? activity.getApplication() : null, this);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_interruptions;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle, FragmentManager fragmentManager, Application app,
            Fragment fragment) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ZenModeButtonPreferenceController(context, lifecycle, fragmentManager));
        controllers.add(new ZenModePeoplePreferenceController(context, lifecycle,
                "zen_mode_behavior_people"));
        controllers.add(new ZenModeBypassingAppsPreferenceController(context, app,
                fragment, lifecycle));
        controllers.add(new ZenModeSoundVibrationPreferenceController(context, lifecycle,
                "zen_sound_vibration_settings"));
        controllers.add(new ZenModeAutomationPreferenceController(context));
        controllers.add(new ZenModeDurationPreferenceController(context, lifecycle));
        controllers.add(new ZenModeBlockedEffectsPreferenceController(context, lifecycle));
        controllers.add(new ZenModeSettingsFooterPreferenceController(context, lifecycle,
                fragmentManager));
        return controllers;
    }

    public static class SummaryBuilder {

        private Context mContext;

        public SummaryBuilder(Context context) {
            mContext = context;
        }

        // these should match NotificationManager.Policy#ALL_PRIORITY_CATEGORIES
        private static final int[] ALL_PRIORITY_CATEGORIES = {
                PRIORITY_CATEGORY_ALARMS,
                PRIORITY_CATEGORY_MEDIA,
                PRIORITY_CATEGORY_SYSTEM,
                PRIORITY_CATEGORY_MESSAGES,
                PRIORITY_CATEGORY_CONVERSATIONS,
                PRIORITY_CATEGORY_EVENTS,
                PRIORITY_CATEGORY_REMINDERS,
                PRIORITY_CATEGORY_CALLS,
                PRIORITY_CATEGORY_REPEAT_CALLERS,
        };

        String getOtherSoundCategoriesSummary(Policy policy) {
            List<String> enabledCategories = getEnabledCategories(
                    policy,
                    category -> PRIORITY_CATEGORY_ALARMS == category
                            || PRIORITY_CATEGORY_MEDIA == category
                            || PRIORITY_CATEGORY_SYSTEM == category
                            || PRIORITY_CATEGORY_REMINDERS == category
                            || PRIORITY_CATEGORY_EVENTS == category,
                    true);
            int numCategories = enabledCategories.size();
            MessageFormat msgFormat = new MessageFormat(
                    mContext.getString(R.string.zen_mode_other_sounds_summary),
                    Locale.getDefault());
            Map<String, Object> args = new HashMap<>();
            args.put("count", numCategories);
            if (numCategories >= 1) {
                args.put("sound_category_1", enabledCategories.get(0));
                if (numCategories >= 2) {
                    args.put("sound_category_2", enabledCategories.get(1));
                    if (numCategories == 3) {
                        args.put("sound_category_3", enabledCategories.get(2));
                    }
                }
            }
            return msgFormat.format(args);
        }

        String getCallsSettingSummary(Policy policy) {
            List<String> enabledCategories = getEnabledCategories(policy,
                    category -> PRIORITY_CATEGORY_CALLS == category
                            || PRIORITY_CATEGORY_REPEAT_CALLERS == category, true);
            int numCategories = enabledCategories.size();
            if (numCategories == 0) {
                return mContext.getString(R.string.zen_mode_none_calls);
            } else if (numCategories == 1) {
                return mContext.getString(R.string.zen_mode_calls_summary_one,
                        enabledCategories.get(0));
            } else {
                return mContext.getString(R.string.zen_mode_calls_summary_two,
                        enabledCategories.get(0),
                        enabledCategories.get(1));
            }
        }

        String getMessagesSettingSummary(Policy policy) {
            List<String> enabledCategories = getEnabledCategories(policy,
                    category -> PRIORITY_CATEGORY_MESSAGES == category
                           || PRIORITY_CATEGORY_CONVERSATIONS == category, true);
            int numCategories = enabledCategories.size();
            if (numCategories == 0) {
                return mContext.getString(R.string.zen_mode_none_messages);
            } else if (numCategories == 1) {
                return enabledCategories.get(0);
            } else {
                // While this string name seems like a slight misnomer: it's borrowing the analogous
                // calls-summary functionality to combine two permissions.
                return mContext.getString(R.string.zen_mode_calls_summary_two,
                        enabledCategories.get(0),
                        enabledCategories.get(1));
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
                MessageFormat msgFormat = new MessageFormat(
                        mContext.getString(R.string.zen_mode_sound_summary_off),
                        Locale.getDefault());
                Map<String, Object> msgArgs = new HashMap<>();
                msgArgs.put("count", getEnabledAutomaticRulesCount());
                return msgFormat.format(msgArgs);
            }
        }

        String getBlockedEffectsSummary(Policy policy) {
            if (policy.suppressedVisualEffects == 0) {
                return mContext.getResources().getString(
                        R.string.zen_mode_restrict_notifications_summary_muted);
            } else if (Policy.areAllVisualEffectsSuppressed(policy.suppressedVisualEffects)) {
                return mContext.getResources().getString(
                        R.string.zen_mode_restrict_notifications_summary_hidden);
            } else {
                return mContext.getResources().getString(
                        R.string.zen_mode_restrict_notifications_summary_custom);
            }
        }

        String getAutomaticRulesSummary() {
            MessageFormat msgFormat = new MessageFormat(
                    mContext.getString(R.string.zen_mode_settings_schedules_summary),
                    Locale.getDefault());
            Map<String, Object> msgArgs = new HashMap<>();
            msgArgs.put("count", getEnabledAutomaticRulesCount());
            return msgFormat.format(msgArgs);
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

        private List<String> getEnabledCategories(Policy policy,
                Predicate<Integer> filteredCategories, boolean capitalizeFirstInList) {
            List<String> enabledCategories = new ArrayList<>();
            for (int category : ALL_PRIORITY_CATEGORIES) {
                boolean isFirst = capitalizeFirstInList && enabledCategories.isEmpty();
                if (filteredCategories.test(category) && isCategoryEnabled(policy, category)) {
                    if (category == Policy.PRIORITY_CATEGORY_REPEAT_CALLERS
                            && isCategoryEnabled(policy, Policy.PRIORITY_CATEGORY_CALLS)
                            && policy.priorityCallSenders == Policy.PRIORITY_SENDERS_ANY) {
                        continue;
                    }

                    // For conversations, only the "priority conversations" setting is relevant; any
                    // other setting is subsumed by the messages-specific messaging.
                    if (category == Policy.PRIORITY_CATEGORY_CONVERSATIONS
                            && isCategoryEnabled(policy, Policy.PRIORITY_CATEGORY_CONVERSATIONS)
                            && policy.priorityConversationSenders
                                    != Policy.CONVERSATION_SENDERS_IMPORTANT) {
                        continue;
                    }

                    enabledCategories.add(getCategory(category, policy, isFirst));
                }
            }
            return enabledCategories;
        }

        private boolean isCategoryEnabled(Policy policy, int categoryType) {
            return (policy.priorityCategories & categoryType) != 0;
        }

        private String getCategory(int category, Policy policy, boolean isFirst) {
            if (category == PRIORITY_CATEGORY_ALARMS) {
                if (isFirst) {
                    return mContext.getString(R.string.zen_mode_alarms_list_first);
                } else {
                    return mContext.getString(R.string.zen_mode_alarms_list);
                }
            } else if (category == PRIORITY_CATEGORY_MEDIA) {
                if (isFirst) {
                    return mContext.getString(R.string.zen_mode_media_list_first);
                } else {
                    return mContext.getString(R.string.zen_mode_media_list);
                }
            } else if (category == PRIORITY_CATEGORY_SYSTEM) {
                if (isFirst) {
                    return mContext.getString(R.string.zen_mode_system_list_first);
                } else {
                    return mContext.getString(R.string.zen_mode_system_list);
                }
            } else if (category == Policy.PRIORITY_CATEGORY_MESSAGES) {
                if (policy.priorityMessageSenders == Policy.PRIORITY_SENDERS_ANY) {
                    return mContext.getString(R.string.zen_mode_from_anyone);
                } else if (policy.priorityMessageSenders == Policy.PRIORITY_SENDERS_CONTACTS) {
                    return mContext.getString(R.string.zen_mode_from_contacts);
                } else {
                    return mContext.getString(R.string.zen_mode_from_starred);
                }
            } else if (category == Policy.PRIORITY_CATEGORY_CONVERSATIONS
                    && policy.priorityConversationSenders
                            == Policy.CONVERSATION_SENDERS_IMPORTANT) {
                if (isFirst) {
                    return mContext.getString(R.string.zen_mode_from_important_conversations);
                } else {
                    return mContext.getString(
                            R.string.zen_mode_from_important_conversations_second);
                }
            } else if (category == Policy.PRIORITY_CATEGORY_EVENTS) {
                if (isFirst) {
                    return mContext.getString(R.string.zen_mode_events_list_first);
                } else {
                    return mContext.getString(R.string.zen_mode_events_list);
                }
            } else if (category == Policy.PRIORITY_CATEGORY_REMINDERS) {
                if (isFirst) {
                    return mContext.getString(R.string.zen_mode_reminders_list_first);
                } else {
                    return mContext.getString(R.string.zen_mode_reminders_list);
                }
            } else if (category == Policy.PRIORITY_CATEGORY_CALLS) {
                if (policy.priorityCallSenders == Policy.PRIORITY_SENDERS_ANY) {
                    if (isFirst) {
                        return mContext.getString(R.string.zen_mode_from_anyone);
                    }
                    return mContext.getString(R.string.zen_mode_all_callers);
                } else if (policy.priorityCallSenders == Policy.PRIORITY_SENDERS_CONTACTS){
                    if (isFirst) {
                        return mContext.getString(R.string.zen_mode_from_contacts);
                    }
                    return mContext.getString(R.string.zen_mode_contacts_callers);
                } else {
                    if (isFirst) {
                        return mContext.getString(R.string.zen_mode_from_starred);
                    }
                    return mContext.getString(R.string.zen_mode_starred_callers);
                }
            } else if (category == Policy.PRIORITY_CATEGORY_REPEAT_CALLERS) {
                if (isFirst) {
                    return mContext.getString(R.string.zen_mode_repeat_callers);
                } else {
                    return mContext.getString(R.string.zen_mode_repeat_callers_list);
                }
            }

            return "";
        }
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.zen_mode_settings) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    keys.add(ZenModeDurationPreferenceController.KEY);
                    return keys;
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(context, null, null,
                            null, null);
                }
            };
}
