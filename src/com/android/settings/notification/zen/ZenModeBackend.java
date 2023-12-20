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

import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CONVERSATIONS;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_NONE;

import android.app.ActivityManager;
import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.icu.text.MessageFormat;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenPolicy;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ZenModeBackend {
    @VisibleForTesting
    protected static final String ZEN_MODE_FROM_ANYONE = "zen_mode_from_anyone";
    @VisibleForTesting
    protected static final String ZEN_MODE_FROM_CONTACTS = "zen_mode_from_contacts";
    @VisibleForTesting
    protected static final String ZEN_MODE_FROM_STARRED = "zen_mode_from_starred";
    @VisibleForTesting
    protected static final String ZEN_MODE_FROM_NONE = "zen_mode_from_none";
    protected static final int SOURCE_NONE = -1;

    private static ZenModeBackend sInstance;

    protected int mZenMode;
    /** gets policy last set by updatePolicy **/
    protected NotificationManager.Policy mPolicy;
    private final NotificationManager mNotificationManager;

    private static final String TAG = "ZenModeSettingsBackend";
    private final Context mContext;

    public static ZenModeBackend getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ZenModeBackend(context);
        }
        return sInstance;
    }

    public ZenModeBackend(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        updateZenMode();
        updatePolicy();
    }

    protected void updatePolicy() {
        if (mNotificationManager != null) {
            mPolicy = mNotificationManager.getNotificationPolicy();
        }
    }

    protected void updateZenMode() {
        mZenMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ZEN_MODE, mZenMode);
    }

    protected boolean updateZenRule(String id, AutomaticZenRule rule) {
        if (android.app.Flags.modesApi()) {
            return mNotificationManager.updateAutomaticZenRule(id, rule, /* fromUser= */ true);
        } else {
            return NotificationManager.from(mContext).updateAutomaticZenRule(id, rule);
        }
    }

    protected void setZenMode(int zenMode) {
        if (android.app.Flags.modesApi()) {
            mNotificationManager.setZenMode(zenMode, null, TAG, /* fromUser= */ true);
        } else {
            NotificationManager.from(mContext).setZenMode(zenMode, null, TAG);
        }
        mZenMode = getZenMode();
    }

    protected void setZenModeForDuration(int minutes) {
        Uri conditionId = ZenModeConfig.toTimeCondition(mContext, minutes,
                ActivityManager.getCurrentUser(), true).id;
        if (android.app.Flags.modesApi()) {
            mNotificationManager.setZenMode(Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS,
                        conditionId, TAG, /* fromUser= */ true);
        } else {
            mNotificationManager.setZenMode(Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS,
                    conditionId, TAG);
        }
        mZenMode = getZenMode();
    }

    protected int getZenMode() {
        mZenMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ZEN_MODE, mZenMode);
        return mZenMode;
    }

    protected boolean isVisualEffectSuppressed(int visualEffect) {
        return (mPolicy.suppressedVisualEffects & visualEffect) != 0;
    }

    protected boolean isPriorityCategoryEnabled(int categoryType) {
        return (mPolicy.priorityCategories & categoryType) != 0;
    }

    protected int getNewDefaultPriorityCategories(boolean allow, int categoryType) {
        int priorityCategories = mPolicy.priorityCategories;
        if (allow) {
            priorityCategories |= categoryType;
        } else {
            priorityCategories &= ~categoryType;
        }
        return priorityCategories;
    }

    protected int getPriorityCallSenders() {
        if (isPriorityCategoryEnabled(NotificationManager.Policy.PRIORITY_CATEGORY_CALLS)) {
            return mPolicy.priorityCallSenders;
        }

        return SOURCE_NONE;
    }

    protected int getPriorityMessageSenders() {
        if (isPriorityCategoryEnabled(
                NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES)) {
            return mPolicy.priorityMessageSenders;
        }
        return SOURCE_NONE;
    }

    protected int getPriorityConversationSenders() {
        if (isPriorityCategoryEnabled(PRIORITY_CATEGORY_CONVERSATIONS)) {
            return mPolicy.priorityConversationSenders;
        }
        return CONVERSATION_SENDERS_NONE;
    }

    protected void saveVisualEffectsPolicy(int category, boolean suppress) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ZEN_SETTINGS_UPDATED, 1);

        int suppressedEffects = getNewSuppressedEffects(suppress, category);
        savePolicy(mPolicy.priorityCategories, mPolicy.priorityCallSenders,
                mPolicy.priorityMessageSenders, suppressedEffects,
                mPolicy.priorityConversationSenders);
    }

    protected void saveSoundPolicy(int category, boolean allow) {
        int priorityCategories = getNewDefaultPriorityCategories(allow, category);
        savePolicy(priorityCategories, mPolicy.priorityCallSenders,
                mPolicy.priorityMessageSenders, mPolicy.suppressedVisualEffects,
                mPolicy.priorityConversationSenders);
    }

    protected void savePolicy(int priorityCategories, int priorityCallSenders,
            int priorityMessageSenders, int suppressedVisualEffects,
            int priorityConversationSenders) {
        mPolicy = new NotificationManager.Policy(priorityCategories, priorityCallSenders,
                priorityMessageSenders, suppressedVisualEffects, priorityConversationSenders);
        if (android.app.Flags.modesApi()) {
            mNotificationManager.setNotificationPolicy(mPolicy, /* fromUser= */ true);
        } else {
            mNotificationManager.setNotificationPolicy(mPolicy);
        }
    }


    private int getNewSuppressedEffects(boolean suppress, int effectType) {
        int effects = mPolicy.suppressedVisualEffects;

        if (suppress) {
            effects |= effectType;
        } else {
            effects &= ~effectType;
        }

        return clearDeprecatedEffects(effects);
    }

    private int clearDeprecatedEffects(int effects) {
        return effects & ~(SUPPRESSED_EFFECT_SCREEN_ON | SUPPRESSED_EFFECT_SCREEN_OFF);
    }

    protected boolean isEffectAllowed(int effect) {
        return (mPolicy.suppressedVisualEffects & effect) == 0;
    }

    protected void saveSenders(int category, int val) {
        int priorityCallSenders = getPriorityCallSenders();
        int priorityMessagesSenders = getPriorityMessageSenders();
        int categorySenders = getPrioritySenders(category);

        final boolean allowSenders = val != SOURCE_NONE;
        final int allowSendersFrom = val == SOURCE_NONE ? categorySenders : val;

        String stringCategory = "";
        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_CALLS) {
            stringCategory = "Calls";
            priorityCallSenders = allowSendersFrom;
        }

        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES) {
            stringCategory = "Messages";
            priorityMessagesSenders = allowSendersFrom;
        }

        savePolicy(getNewDefaultPriorityCategories(allowSenders, category),
            priorityCallSenders, priorityMessagesSenders, mPolicy.suppressedVisualEffects,
                mPolicy.priorityConversationSenders);

        if (ZenModeSettingsBase.DEBUG) Log.d(TAG, "onPrefChange allow" +
                stringCategory + "=" + allowSenders + " allow" + stringCategory + "From="
                + ZenModeConfig.sourceToString(allowSendersFrom));
    }

    protected void saveConversationSenders(int val) {
        final boolean allowSenders = val != CONVERSATION_SENDERS_NONE;

        savePolicy(getNewDefaultPriorityCategories(allowSenders, PRIORITY_CATEGORY_CONVERSATIONS),
                mPolicy.priorityCallSenders, mPolicy.priorityMessageSenders,
                mPolicy.suppressedVisualEffects, val);

    }

    private int getPrioritySenders(int category) {
        int categorySenders = -1;

        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_CALLS) {
            return getPriorityCallSenders();
        }

        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES) {
            return getPriorityMessageSenders();
        }

        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_CONVERSATIONS) {
            return getPriorityConversationSenders();
        }

        return categorySenders;
    }

    protected static String getKeyFromZenPolicySetting(int contactType) {
        switch (contactType) {
            case ZenPolicy.PEOPLE_TYPE_ANYONE:
                return ZEN_MODE_FROM_ANYONE;
            case  ZenPolicy.PEOPLE_TYPE_CONTACTS:
                return ZEN_MODE_FROM_CONTACTS;
            case ZenPolicy.PEOPLE_TYPE_STARRED:
                return ZEN_MODE_FROM_STARRED;
            case ZenPolicy.PEOPLE_TYPE_NONE:
            default:
                return ZEN_MODE_FROM_NONE;
        }
    }

    protected static String getKeyFromSetting(int contactType) {
        switch (contactType) {
            case NotificationManager.Policy.PRIORITY_SENDERS_ANY:
                return ZEN_MODE_FROM_ANYONE;
            case NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS:
                return ZEN_MODE_FROM_CONTACTS;
            case NotificationManager.Policy.PRIORITY_SENDERS_STARRED:
                return ZEN_MODE_FROM_STARRED;
            case SOURCE_NONE:
            default:
                return ZEN_MODE_FROM_NONE;
        }
    }

    protected static int getContactSettingFromZenPolicySetting(int setting) {
        switch (setting) {
            case ZenPolicy.PEOPLE_TYPE_ANYONE:
                return NotificationManager.Policy.PRIORITY_SENDERS_ANY;
            case  ZenPolicy.PEOPLE_TYPE_CONTACTS:
                return NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS;
            case ZenPolicy.PEOPLE_TYPE_STARRED:
                return NotificationManager.Policy.PRIORITY_SENDERS_STARRED;
            case ZenPolicy.PEOPLE_TYPE_NONE:
            default:
                return SOURCE_NONE;
        }
    }

    protected int getAlarmsTotalSilencePeopleSummary(int category) {
        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES) {
            return R.string.zen_mode_none_messages;
        } else if (category == NotificationManager.Policy.PRIORITY_CATEGORY_CALLS){
            return R.string.zen_mode_none_calls;
        } else if (category == NotificationManager.Policy.PRIORITY_CATEGORY_CONVERSATIONS) {
            return R.string.zen_mode_from_no_conversations;
        }
        return R.string.zen_mode_from_no_conversations;
    }

    protected int getContactsCallsSummary(ZenPolicy policy) {
        int peopleType = policy.getPriorityCallSenders();
        switch (peopleType) {
            case ZenPolicy.PEOPLE_TYPE_ANYONE:
                return R.string.zen_mode_from_anyone;
            case ZenPolicy.PEOPLE_TYPE_CONTACTS:
                return R.string.zen_mode_from_contacts;
            case ZenPolicy.PEOPLE_TYPE_STARRED:
                return R.string.zen_mode_from_starred;
            case ZenPolicy.PEOPLE_TYPE_NONE:
            default:
                return R.string.zen_mode_none_calls;
        }
    }

    protected int getContactsMessagesSummary(ZenPolicy policy) {
        int peopleType = policy.getPriorityMessageSenders();
        switch (peopleType) {
            case ZenPolicy.PEOPLE_TYPE_ANYONE:
                return R.string.zen_mode_from_anyone;
            case ZenPolicy.PEOPLE_TYPE_CONTACTS:
                return R.string.zen_mode_from_contacts;
            case ZenPolicy.PEOPLE_TYPE_STARRED:
                return R.string.zen_mode_from_starred;
            case ZenPolicy.PEOPLE_TYPE_NONE:
            default:
                return R.string.zen_mode_none_messages;
        }
    }

    protected static int getZenPolicySettingFromPrefKey(String key) {
        switch (key) {
            case ZEN_MODE_FROM_ANYONE:
                return ZenPolicy.PEOPLE_TYPE_ANYONE;
            case ZEN_MODE_FROM_CONTACTS:
                return ZenPolicy.PEOPLE_TYPE_CONTACTS;
            case ZEN_MODE_FROM_STARRED:
                return ZenPolicy.PEOPLE_TYPE_STARRED;
            case ZEN_MODE_FROM_NONE:
            default:
                return ZenPolicy.PEOPLE_TYPE_NONE;
        }
    }

    public boolean removeZenRule(String ruleId) {
        if (android.app.Flags.modesApi()) {
            return mNotificationManager.removeAutomaticZenRule(ruleId, /* fromUser= */ true);
        } else {
            return NotificationManager.from(mContext).removeAutomaticZenRule(ruleId);
        }
    }

    public NotificationManager.Policy getConsolidatedPolicy() {
        return NotificationManager.from(mContext).getConsolidatedNotificationPolicy();
    }

    protected String addZenRule(AutomaticZenRule rule) {
        try {
            if (android.app.Flags.modesApi()) {
                return mNotificationManager.addAutomaticZenRule(rule, /* fromUser= */ true);
            } else {
                return NotificationManager.from(mContext).addAutomaticZenRule(rule);
            }
        } catch (Exception e) {
            return null;
        }
    }

    ZenPolicy setDefaultZenPolicy(ZenPolicy zenPolicy) {
        int calls;
        if (mPolicy.allowCalls()) {
            calls = ZenModeConfig.getZenPolicySenders(mPolicy.allowCallsFrom());
        } else {
            calls = ZenPolicy.PEOPLE_TYPE_NONE;
        }

        int messages;
        if (mPolicy.allowMessages()) {
            messages = ZenModeConfig.getZenPolicySenders(mPolicy.allowMessagesFrom());
        } else {
            messages = ZenPolicy.PEOPLE_TYPE_NONE;
        }

        int conversations;
        if (mPolicy.allowConversations()) {
            // unlike the above, no mapping is needed because the values are the same
            conversations = mPolicy.allowConversationsFrom();
        } else {
            conversations = CONVERSATION_SENDERS_NONE;
        }

        return new ZenPolicy.Builder(zenPolicy)
                .allowAlarms(mPolicy.allowAlarms())
                .allowCalls(calls)
                .allowEvents(mPolicy.allowEvents())
                .allowMedia(mPolicy.allowMedia())
                .allowMessages(messages)
                .allowConversations(conversations)
                .allowReminders(mPolicy.allowReminders())
                .allowRepeatCallers(mPolicy.allowRepeatCallers())
                .allowSystem(mPolicy.allowSystem())
                .showFullScreenIntent(mPolicy.showFullScreenIntents())
                .showLights(mPolicy.showLights())
                .showInAmbientDisplay(mPolicy.showAmbient())
                .showInNotificationList(mPolicy.showInNotificationList())
                .showBadges(mPolicy.showBadges())
                .showPeeking(mPolicy.showPeeking())
                .showStatusBarIcons(mPolicy.showStatusBarIcons())
                .build();
    }

    protected Map.Entry<String, AutomaticZenRule>[] getAutomaticZenRules() {
        Map<String, AutomaticZenRule> ruleMap =
                NotificationManager.from(mContext).getAutomaticZenRules();
        final Map.Entry<String, AutomaticZenRule>[] rt = ruleMap.entrySet().toArray(
                new Map.Entry[ruleMap.size()]);
        Arrays.sort(rt, RULE_COMPARATOR);
        return rt;
    }

    protected AutomaticZenRule getAutomaticZenRule(String id) {
        return NotificationManager.from(mContext).getAutomaticZenRule(id);
    }

    private static List<String> getDefaultRuleIds() {
        return ZenModeConfig.DEFAULT_RULE_IDS;
    }

    NotificationManager.Policy toNotificationPolicy(ZenPolicy policy) {
        ZenModeConfig config = new ZenModeConfig();
        return config.toNotificationPolicy(policy);
    }

    @VisibleForTesting
    List<String> getStarredContacts(Cursor cursor) {
        List<String> starredContacts = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String contact = cursor.getString(0);
                starredContacts.add(contact != null ? contact :
                        mContext.getString(R.string.zen_mode_starred_contacts_empty_name));

            } while (cursor.moveToNext());
        }
        return starredContacts;
    }

    private List<String> getStarredContacts() {
        Cursor cursor = null;
        try {
            cursor = queryStarredContactsData();
            return getStarredContacts(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    String getStarredContactsSummary(Context context) {
        List<String> starredContacts = getStarredContacts();
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

    String getContactsNumberSummary(Context context) {
        MessageFormat msgFormat = new MessageFormat(
                mContext.getString(R.string.zen_mode_contacts_count),
                Locale.getDefault());
        Map<String, Object> args = new HashMap<>();
        args.put("count", queryAllContactsData().getCount());
        return msgFormat.format(args);
    }

    private Cursor queryStarredContactsData() {
        return mContext.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME_PRIMARY},
                ContactsContract.Data.STARRED + "=1", null,
                ContactsContract.Data.TIMES_CONTACTED);
    }

    private Cursor queryAllContactsData() {
        return mContext.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME_PRIMARY},
                null, null, null);
    }

    @VisibleForTesting
    public static final Comparator<Map.Entry<String, AutomaticZenRule>> RULE_COMPARATOR =
            new Comparator<Map.Entry<String, AutomaticZenRule>>() {
                @Override
                public int compare(Map.Entry<String, AutomaticZenRule> lhs,
                        Map.Entry<String, AutomaticZenRule> rhs) {
                    // if it's a default rule, should be at the top of automatic rules
                    boolean lhsIsDefaultRule = getDefaultRuleIds().contains(lhs.getKey());
                    boolean rhsIsDefaultRule = getDefaultRuleIds().contains(rhs.getKey());
                    if (lhsIsDefaultRule != rhsIsDefaultRule) {
                        return lhsIsDefaultRule ? -1 : 1;
                    }

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
                            ? 1 : ZenModeConfig.isValidEventConditionId(rule.getConditionId())
                            ? 2 : 3;
                    return type + rule.getName().toString();
                }
            };
}
