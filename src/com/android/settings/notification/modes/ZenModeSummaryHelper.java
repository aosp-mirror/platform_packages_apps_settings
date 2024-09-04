/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.notification.modes;

import static android.app.NotificationManager.INTERRUPTION_FILTER_ALL;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_ANYONE;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_IMPORTANT;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_NONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_ANYONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_CONTACTS;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_NONE;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_ALARMS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_CALLS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_CONVERSATIONS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_EVENTS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_MEDIA;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_MESSAGES;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_REMINDERS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_REPEAT_CALLERS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_SYSTEM;
import static android.service.notification.ZenPolicy.STATE_ALLOW;
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_AMBIENT;
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_BADGE;
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_FULL_SCREEN_INTENT;
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_LIGHTS;
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_NOTIFICATION_LIST;
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_PEEK;
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_STATUS_BAR;

import android.content.Context;
import android.icu.text.MessageFormat;
import android.service.notification.ZenDeviceEffects;
import android.service.notification.ZenPolicy;
import android.service.notification.ZenPolicy.ConversationSenders;
import android.service.notification.ZenPolicy.PeopleType;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.text.BidiFormatter;

import com.android.settings.R;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.notification.modes.ZenMode;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

class ZenModeSummaryHelper {

    private final Context mContext;
    private final ZenHelperBackend mBackend;

    ZenModeSummaryHelper(Context context, ZenHelperBackend backend) {
        mContext = context;
        mBackend = backend;
    }

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

    static final ImmutableList</* @PriorityCategory */ Integer> OTHER_SOUND_CATEGORIES =
            ImmutableList.of(
                PRIORITY_CATEGORY_ALARMS,
                PRIORITY_CATEGORY_MEDIA,
                PRIORITY_CATEGORY_SYSTEM,
                PRIORITY_CATEGORY_REMINDERS,
                PRIORITY_CATEGORY_EVENTS);

    String getOtherSoundCategoriesSummary(ZenMode zenMode) {
        List<String> enabledCategories = getEnabledCategories(
                zenMode.getPolicy(),
                OTHER_SOUND_CATEGORIES::contains,
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

    String getCallsSettingSummary(ZenMode zenMode) {
        List<String> enabledCategories = getEnabledCategories(zenMode.getPolicy(),
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

    String getMessagesSettingSummary(ZenPolicy policy) {
        if (policy.getPriorityCategoryMessages() == STATE_ALLOW
                && policy.getPriorityMessageSenders() == PEOPLE_TYPE_ANYONE) {
            // Messages=anyone means anyone. Even if conversation senders is specially configured,
            // saying "Anyone and priority conversations" 1) makes no sense and 2) is incorrect
            // because conversations WILL get through by virtue of also being messages.
            return mContext.getString(R.string.zen_mode_from_anyone);
        }

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

    String getBlockedEffectsSummary(ZenMode zenMode) {
        List<Integer> relevantVisualEffects = new ArrayList<>();
        relevantVisualEffects.add(VISUAL_EFFECT_FULL_SCREEN_INTENT);
        relevantVisualEffects.add(VISUAL_EFFECT_PEEK);
        relevantVisualEffects.add(VISUAL_EFFECT_STATUS_BAR);
        relevantVisualEffects.add(VISUAL_EFFECT_BADGE);
        relevantVisualEffects.add(VISUAL_EFFECT_AMBIENT);
        relevantVisualEffects.add(VISUAL_EFFECT_NOTIFICATION_LIST);
        if (mContext.getResources()
                .getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
            relevantVisualEffects.add(VISUAL_EFFECT_LIGHTS);
        }

        if (shouldShowAllVisualEffects(zenMode.getPolicy(), relevantVisualEffects)) {
            return mContext.getResources().getString(
                    R.string.zen_mode_restrict_notifications_summary_muted);
        } else if (shouldHideAllVisualEffects(zenMode.getPolicy(), relevantVisualEffects)) {
            return mContext.getResources().getString(
                    R.string.zen_mode_restrict_notifications_summary_hidden);
        } else {
            return mContext.getResources().getString(
                    R.string.zen_mode_restrict_notifications_summary_custom);
        }
    }

    private boolean shouldShowAllVisualEffects(ZenPolicy policy, List<Integer> relevantEffects) {
        for (int i = 0; i < relevantEffects.size(); i++) {
            if (!policy.isVisualEffectAllowed(relevantEffects.get(i), false)) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldHideAllVisualEffects(ZenPolicy policy, List<Integer> relevantEffects) {
        for (int i = 0; i < relevantEffects.size(); i++) {
            if (policy.isVisualEffectAllowed(relevantEffects.get(i), false)) {
                return false;
            }
        }
        return true;
    }

    String getDisplayEffectsSummary(ZenMode zenMode) {
        boolean isFirst = true;
        List<String> enabledEffects = new ArrayList<>();
        if (!zenMode.getPolicy().shouldShowAllVisualEffects()
                && zenMode.getInterruptionFilter() != INTERRUPTION_FILTER_ALL) {
            enabledEffects.add(getBlockedEffectsSummary(zenMode));
            isFirst = false;
        }
        ZenDeviceEffects currEffects = zenMode.getDeviceEffects();
        if (currEffects.shouldDisplayGrayscale()) {
            if (isFirst) {
                enabledEffects.add(mContext.getString(R.string.mode_grayscale_title));
            } else {
                enabledEffects.add(mContext.getString(
                        R.string.mode_grayscale_title_secondary_list));
            }
            isFirst = false;
        }
        if (currEffects.shouldSuppressAmbientDisplay()) {
            if (isFirst) {
                enabledEffects.add(mContext.getString(R.string.mode_aod_title));
            } else {
                enabledEffects.add(mContext.getString(
                        R.string.mode_aod_title_secondary_list));
            }
            isFirst = false;
        }
        if (currEffects.shouldDimWallpaper()) {
            if (isFirst) {
                enabledEffects.add(mContext.getString(R.string.mode_wallpaper_title));
            } else {
                enabledEffects.add(mContext.getString(
                        R.string.mode_wallpaper_title_secondary_list));
            }
            isFirst = false;
        }
        if (currEffects.shouldUseNightMode()) {
            if (isFirst) {
                enabledEffects.add(mContext.getString(R.string.mode_dark_theme_title));
            } else {
                enabledEffects.add(mContext.getString(
                        R.string.mode_dark_theme_title_secondary_list));
            }
            isFirst = false;
        }

        int numCategories = enabledEffects.size();
        MessageFormat msgFormat = new MessageFormat(
                mContext.getString(R.string.mode_display_settings_summary),
                Locale.getDefault());
        Map<String, Object> args = new HashMap<>();
        args.put("count", numCategories);
        if (numCategories >= 1) {
            args.put("effect_1", enabledEffects.get(0));
            if (numCategories >= 2) {
                args.put("effect_2", enabledEffects.get(1));
                if (numCategories == 3) {
                    args.put("effect_3", enabledEffects.get(2));
                }
            }
        }
        return msgFormat.format(args);
    }

    private List<String> getEnabledCategories(ZenPolicy policy,
            Predicate<Integer> filteredCategories, boolean capitalizeFirstInList) {
        List<String> enabledCategories = new ArrayList<>();
        for (int category : ALL_PRIORITY_CATEGORIES) {
            boolean isFirst = capitalizeFirstInList && enabledCategories.isEmpty();
            if (filteredCategories.test(category) && policy.isCategoryAllowed(category, false)) {
                if (category == PRIORITY_CATEGORY_REPEAT_CALLERS
                        && policy.isCategoryAllowed(PRIORITY_CATEGORY_CALLS, false)
                        && policy.getPriorityCallSenders() == PEOPLE_TYPE_ANYONE) {
                    continue;
                }

                // For conversations, only the "all/priority conversations" settings are relevant;
                // any other setting is subsumed by the messages-specific messaging.
                if (category == PRIORITY_CATEGORY_CONVERSATIONS
                        && policy.isCategoryAllowed(PRIORITY_CATEGORY_CONVERSATIONS, false)
                        && policy.getPriorityConversationSenders() != CONVERSATION_SENDERS_ANYONE
                        && policy.getPriorityConversationSenders()
                        != CONVERSATION_SENDERS_IMPORTANT) {
                    continue;
                }

                enabledCategories.add(getCategory(category, policy, isFirst));
            }
        }
        return enabledCategories;
    }

    private String getCategory(int category, ZenPolicy policy, boolean isFirst) {
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
        } else if (category == PRIORITY_CATEGORY_MESSAGES) {
            if (policy.getPriorityMessageSenders() == PEOPLE_TYPE_ANYONE) {
                return mContext.getString(R.string.zen_mode_from_anyone);
            } else if (policy.getPriorityMessageSenders() == PEOPLE_TYPE_CONTACTS) {
                return mContext.getString(R.string.zen_mode_from_contacts);
            } else {
                return mContext.getString(R.string.zen_mode_from_starred);
            }
        } else if (category == PRIORITY_CATEGORY_CONVERSATIONS) {
            if (policy.getPriorityConversationSenders() == CONVERSATION_SENDERS_IMPORTANT) {
                if (isFirst) {
                    return mContext.getString(R.string.zen_mode_from_important_conversations);
                } else {
                    return mContext.getString(
                            R.string.zen_mode_from_important_conversations_second);
                }
            } else if (policy.getPriorityConversationSenders() == CONVERSATION_SENDERS_ANYONE) {
                if (isFirst) {
                    return mContext.getString(R.string.zen_mode_from_all_conversations);
                } else {
                    return mContext.getString(R.string.zen_mode_from_all_conversations_second);
                }
            }
        } else if (category == PRIORITY_CATEGORY_EVENTS) {
            if (isFirst) {
                return mContext.getString(R.string.zen_mode_events_list_first);
            } else {
                return mContext.getString(R.string.zen_mode_events_list);
            }
        } else if (category == PRIORITY_CATEGORY_REMINDERS) {
            if (isFirst) {
                return mContext.getString(R.string.zen_mode_reminders_list_first);
            } else {
                return mContext.getString(R.string.zen_mode_reminders_list);
            }
        } else if (category == PRIORITY_CATEGORY_CALLS) {
            if (policy.getPriorityCallSenders() == PEOPLE_TYPE_ANYONE) {
                if (isFirst) {
                    return mContext.getString(R.string.zen_mode_from_anyone);
                }
                return mContext.getString(R.string.zen_mode_all_callers);
            } else if (policy.getPriorityCallSenders() == PEOPLE_TYPE_CONTACTS) {
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
        } else if (category == PRIORITY_CATEGORY_REPEAT_CALLERS) {
            if (isFirst) {
                return mContext.getString(R.string.zen_mode_repeat_callers);
            } else {
                return mContext.getString(R.string.zen_mode_repeat_callers_list);
            }
        }

        return "";
    }

    public String getStarredContactsSummary() {
        List<String> starredContacts = mBackend.getStarredContacts().stream()
                .map(ZenHelperBackend.Contact::displayName)
                .map(name -> Strings.isNullOrEmpty(name)
                        ? mContext.getString(R.string.zen_mode_starred_contacts_empty_name)
                        : name)
                .toList();
        int numStarredContacts = starredContacts.size();
        MessageFormat msgFormat = new MessageFormat(
                mContext.getString(R.string.zen_mode_starred_contacts_summary_contacts),
                Locale.getDefault());
        Map<String, Object> args = new HashMap<>();
        args.put("count", numStarredContacts);
        if (numStarredContacts >= 1) {
            args.put("contact_1", starredContacts.get(0));
            if (numStarredContacts >= 2) {
                args.put("contact_2", starredContacts.get(1));
                if (numStarredContacts == 3) {
                    args.put("contact_3", starredContacts.get(2));
                }
            }
        }
        return msgFormat.format(args);
    }

    public String getContactsNumberSummary() {
        MessageFormat msgFormat = new MessageFormat(
                mContext.getString(R.string.zen_mode_contacts_count),
                Locale.getDefault());
        Map<String, Object> args = new HashMap<>();
        args.put("count", mBackend.getAllContactsCount());
        return msgFormat.format(args);
    }

    public String getPeopleSummary(ZenPolicy policy) {
        @PeopleType int callersAllowed = policy.getPriorityCategoryCalls() == STATE_ALLOW
                ? policy.getPriorityCallSenders() : PEOPLE_TYPE_NONE;
        @PeopleType int messagesAllowed = policy.getPriorityCategoryMessages() == STATE_ALLOW
                ? policy.getPriorityMessageSenders() : PEOPLE_TYPE_NONE;
        @ConversationSenders int conversationsAllowed =
                policy.getPriorityCategoryConversations() == STATE_ALLOW
                        ? policy.getPriorityConversationSenders()
                        : CONVERSATION_SENDERS_NONE;
        final boolean areRepeatCallersAllowed =
                policy.isCategoryAllowed(PRIORITY_CATEGORY_REPEAT_CALLERS, false);

        if (callersAllowed == PEOPLE_TYPE_ANYONE
                && messagesAllowed == PEOPLE_TYPE_ANYONE
                && conversationsAllowed == CONVERSATION_SENDERS_ANYONE) {
            return mContext.getString(R.string.zen_mode_people_all);
        } else if (callersAllowed == PEOPLE_TYPE_NONE
                && messagesAllowed == PEOPLE_TYPE_NONE
                && conversationsAllowed == CONVERSATION_SENDERS_NONE) {
            return mContext.getString(
                    areRepeatCallersAllowed ? R.string.zen_mode_people_repeat_callers
                            : R.string.zen_mode_people_none);
        } else {
            return mContext.getResources().getString(R.string.zen_mode_people_some);
        }
    }

    /**
     * Generates a summary to display under the top level "Apps" preference for a mode, based
     * on the given mode and provided set of apps.
     */
    public @NonNull String getAppsSummary(@NonNull ZenMode zenMode,
            @Nullable List<AppEntry> appsBypassing) {
        if (zenMode.getPolicy().getAllowedChannels() == ZenPolicy.CHANNEL_POLICY_PRIORITY) {
            return formatAppsList(appsBypassing);
        } else if (zenMode.getPolicy().getAllowedChannels() == ZenPolicy.CHANNEL_POLICY_NONE) {
            return mContext.getResources().getString(R.string.zen_mode_apps_none_apps);
        }
        return "";
    }

    /**
     * Generates a formatted string declaring which apps can interrupt in the style of
     * "App, App2, and 4 more can interrupt."
     * Apps selected for explicit mention are picked in order from the provided list.
     */
    @VisibleForTesting
    public @NonNull String formatAppsList(@Nullable List<AppEntry> appsBypassingDnd) {
        if (appsBypassingDnd == null) {
            return mContext.getResources().getString(R.string.zen_mode_apps_priority_apps);
        }
        List<String> appNames = appsBypassingDnd.stream().limit(3)
                .map(app -> {
                    String appName = BidiFormatter.getInstance().unicodeWrap(app.label);
                    if (app.isManagedProfile()) {
                        appName = mContext.getString(R.string.zen_mode_apps_work_app, appName);
                    }
                    return appName;
                })
                .toList();

        MessageFormat msgFormat = new MessageFormat(
                mContext.getString(R.string.zen_mode_apps_subtext),
                Locale.getDefault());
        Map<String, Object> args = new HashMap<>();
        args.put("count", appsBypassingDnd.size());
        if (appNames.size() >= 1) {
            args.put("app_1", appNames.get(0));
            if (appNames.size() >= 2) {
                args.put("app_2", appNames.get(1));
                if (appNames.size() == 3) {
                    args.put("app_3", appNames.get(2));
                }
            }
        }
        return msgFormat.format(args);
    }

    String getModesSummary(List<ZenMode> modes) {
        List<ZenMode> activeModes = modes.stream().filter(ZenMode::isActive).toList();

        if (!activeModes.isEmpty()) {
            MessageFormat msgFormat = new MessageFormat(
                    mContext.getString(R.string.zen_modes_summary_some_active),
                    Locale.getDefault());

            Map<String, Object> args = new HashMap<>();
            args.put("count", activeModes.size());
            args.put("mode_1", activeModes.get(0).getName());
            if (activeModes.size() >= 2) {
                args.put("mode_2", activeModes.get(1).getName());
                if (activeModes.size() == 3) {
                    args.put("mode_3", activeModes.get(2).getName());
                }
            }

            return msgFormat.format(args);
        } else {
            int automaticModeCount = (int) modes.stream()
                    .filter(m -> m.isEnabled() && !m.isManualDnd() && !m.isCustomManual())
                    .count();

            MessageFormat msgFormat = new MessageFormat(
                    mContext.getString(R.string.zen_modes_summary_none_active),
                    Locale.getDefault());
            Map<String, Object> msgArgs = Map.of("count", automaticModeCount);
            return msgFormat.format(msgArgs);
        }
    }

}
