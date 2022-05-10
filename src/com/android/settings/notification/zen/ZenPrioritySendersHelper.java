/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.app.NotificationManager.Policy.CONVERSATION_SENDERS_ANYONE;
import static android.app.NotificationManager.Policy.CONVERSATION_SENDERS_IMPORTANT;
import static android.app.NotificationManager.Policy.CONVERSATION_SENDERS_NONE;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_ANY;

import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.icu.text.MessageFormat;
import android.provider.Contacts;
import android.service.notification.ConversationChannelWrapper;
import android.view.View;

import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.app.ConversationListSettings;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared class implementing priority senders logic to be used both for zen mode and zen custom
 * rules, governing which senders can break through DND. This helper class controls creating
 * and displaying the relevant preferences for either messages or calls mode, and determining
 * what the priority and conversation senders settings should be given a click.
 *
 * The outer classes govern how those settings are stored -- for instance, where and how they
 *  are saved, and where they're read from to get current status.
 */
public class ZenPrioritySendersHelper {
    public static final String TAG = "ZenPrioritySendersHelper";

    static final int UNKNOWN = -10;
    static final String KEY_ANY = "senders_anyone";
    static final String KEY_CONTACTS = "senders_contacts";
    static final String KEY_STARRED = "senders_starred_contacts";
    static final String KEY_IMPORTANT = "conversations_important";
    static final String KEY_NONE = "senders_none";

    private int mNumImportantConversations = UNKNOWN;

    private static final Intent ALL_CONTACTS_INTENT =
            new Intent(Contacts.Intents.UI.LIST_DEFAULT)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    private static final Intent STARRED_CONTACTS_INTENT =
            new Intent(Contacts.Intents.UI.LIST_STARRED_ACTION)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK  | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    private static final Intent FALLBACK_INTENT = new Intent(Intent.ACTION_MAIN)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

    private final Context mContext;
    private final ZenModeBackend mZenModeBackend;
    private final NotificationBackend mNotificationBackend;
    private final PackageManager mPackageManager;
    private final boolean mIsMessages; // if this is false, then this preference is for calls
    private final SelectorWithWidgetPreference.OnClickListener mSelectorClickListener;

    private PreferenceCategory mPreferenceCategory;
    private List<SelectorWithWidgetPreference> mSelectorPreferences = new ArrayList<>();

    public ZenPrioritySendersHelper(Context context, boolean isMessages,
            ZenModeBackend zenModeBackend, NotificationBackend notificationBackend,
            SelectorWithWidgetPreference.OnClickListener clickListener) {
        mContext = context;
        mIsMessages = isMessages;
        mZenModeBackend = zenModeBackend;
        mNotificationBackend = notificationBackend;
        mSelectorClickListener = clickListener;

        mPackageManager = mContext.getPackageManager();
        if (!FALLBACK_INTENT.hasCategory(Intent.CATEGORY_APP_CONTACTS)) {
            FALLBACK_INTENT.addCategory(Intent.CATEGORY_APP_CONTACTS);
        }
    }

    void displayPreference(PreferenceCategory preferenceCategory) {
        mPreferenceCategory = preferenceCategory;
        if (mPreferenceCategory.getPreferenceCount() == 0) {
            makeSelectorPreference(KEY_STARRED,
                    com.android.settings.R.string.zen_mode_from_starred, mIsMessages);
            makeSelectorPreference(KEY_CONTACTS,
                    com.android.settings.R.string.zen_mode_from_contacts, mIsMessages);
            if (mIsMessages) {
                makeSelectorPreference(KEY_IMPORTANT,
                        com.android.settings.R.string.zen_mode_from_important_conversations, true);
                updateChannelCounts();
            }
            makeSelectorPreference(KEY_ANY,
                    com.android.settings.R.string.zen_mode_from_anyone, mIsMessages);
            makeSelectorPreference(KEY_NONE,
                    com.android.settings.R.string.zen_mode_none_messages, mIsMessages);
            updateSummaries();
        }
    }

    void updateState(int currContactsSetting, int currConversationsSetting) {
        for (SelectorWithWidgetPreference pref : mSelectorPreferences) {
            // for each preference, check whether the current state matches what this state
            // would look like if the button were checked.
            final int[] checkedState = keyToSettingEndState(pref.getKey(), true);
            final int checkedContactsSetting = checkedState[0];
            final int checkedConversationsSetting = checkedState[1];

            boolean match = checkedContactsSetting == currContactsSetting;
            if (mIsMessages && checkedConversationsSetting != UNKNOWN) {
                // "UNKNOWN" in checkedContactsSetting means this preference doesn't govern
                // the priority senders setting, so the full match happens when either
                // the priority senders setting matches or if it's UNKNOWN so only the conversation
                // setting needs to match.
                match = (match || checkedContactsSetting == UNKNOWN)
                        && (checkedConversationsSetting == currConversationsSetting);
            }

            pref.setChecked(match);
        }
    }

    void updateSummaries() {
        for (SelectorWithWidgetPreference pref : mSelectorPreferences) {
            pref.setSummary(getSummary(pref.getKey()));
        }
    }

    // Gets the desired end state of the priority senders and conversations for the given key
    // and whether it is being checked or unchecked. UNKNOWN indicates no change in state.
    //
    // Returns an integer array with 2 entries. The first entry is the setting for priority senders
    // and the second entry is for priority conversation senders; if isMessages is false, then
    // no changes will ever be prescribed for conversation senders.
    int[] keyToSettingEndState(String key, boolean checked) {
        int[] endState = new int[]{ UNKNOWN, UNKNOWN };
        if (!checked) {
            // Unchecking any priority-senders-based state should reset the state to NONE.
            // "Unchecking" the NONE state doesn't do anything, in practice.
            switch (key) {
                case KEY_STARRED:
                case KEY_CONTACTS:
                case KEY_ANY:
                case KEY_NONE:
                    endState[0] = ZenModeBackend.SOURCE_NONE;
            }

            // For messages, unchecking "priority conversations" and "any" should reset conversation
            // state to "NONE" as well.
            if (mIsMessages) {
                switch (key) {
                    case KEY_IMPORTANT:
                    case KEY_ANY:
                    case KEY_NONE:
                        endState[1] = CONVERSATION_SENDERS_NONE;
                }
            }
        } else {
            // All below is for the enabling (checked) state.
            switch (key) {
                case KEY_STARRED:
                    endState[0] = NotificationManager.Policy.PRIORITY_SENDERS_STARRED;
                    break;
                case KEY_CONTACTS:
                    endState[0] = NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS;
                    break;
                case KEY_ANY:
                    endState[0] = NotificationManager.Policy.PRIORITY_SENDERS_ANY;
                    break;
                case KEY_NONE:
                    endState[0] = ZenModeBackend.SOURCE_NONE;
            }

            // In the messages case *only*, also handle changing of conversation settings.
            if (mIsMessages) {
                switch (key) {
                    case KEY_IMPORTANT:
                        endState[1] = CONVERSATION_SENDERS_IMPORTANT;
                        break;
                    case KEY_ANY:
                        endState[1] = CONVERSATION_SENDERS_ANYONE;
                        break;
                    case KEY_NONE:
                        endState[1] = CONVERSATION_SENDERS_NONE;
                }
            }
        }

        // Error case check: if somehow, after all of that, endState is still {UNKNOWN, UNKNOWN},
        // something has gone wrong.
        if (endState[0] == UNKNOWN && endState[1] == UNKNOWN) {
            throw new IllegalArgumentException("invalid key " + key);
        }

        return endState;
    }

    // Returns the preferences, if any, that should be newly saved for the specified setting and
    // checked state in an array where index 0 is the new senders setting and 1 the new
    // conversations setting. A return value of UNKNOWN indicates that nothing should change.
    //
    // The returned conversations setting will always be UNKNOWN (not to change) in the calls case.
    //
    // Checking and unchecking is mostly an operation of setting or unsetting the relevant
    // preference, except for some special handling where the conversation setting overlaps:
    //   - setting or unsetting "priority contacts" or "contacts" has no effect on the
    //     priority conversation setting, and vice versa
    //   - if "priority conversations" is selected, and the user checks "anyone", the conversation
    //     setting is also set to any conversations
    //   - if "anyone" is previously selected, and the user clicks "priority conversations", then
    //     the contacts setting is additionally reset to "none".
    //   - if "anyone" is previously selected, and the user clicks one of the contacts values,
    //     then the conversations setting is additionally reset to "none".
    int[] settingsToSaveOnClick(SelectorWithWidgetPreference preference,
            int currSendersSetting, int currConvosSetting) {
        int[] savedSettings = new int[]{ UNKNOWN, UNKNOWN };

        // If the preference isn't a checkbox, always consider this to be "checking" the setting.
        // Otherwise, toggle.
        final int[] endState = keyToSettingEndState(preference.getKey(),
                preference.isCheckBox() ? !preference.isChecked() : true);
        final int prioritySendersSetting = endState[0];
        final int priorityConvosSetting = endState[1];

        if (prioritySendersSetting != UNKNOWN && prioritySendersSetting != currSendersSetting) {
            savedSettings[0] = prioritySendersSetting;
        }

        // Only handle conversation settings for the messages case. If not messages, there should
        // never be any change to the conversation senders setting.
        if (mIsMessages) {
            if (priorityConvosSetting != UNKNOWN
                    && priorityConvosSetting != currConvosSetting) {
                savedSettings[1] = priorityConvosSetting;
            }

            // Special-case handling for the "priority conversations" checkbox:
            // If a specific selection exists for priority senders (starred, contacts), we leave
            // it untouched. Otherwise (when the senders is set to "any"), set it to NONE.
            if (preference.getKey() == KEY_IMPORTANT
                    && currSendersSetting == PRIORITY_SENDERS_ANY) {
                savedSettings[0] = ZenModeBackend.SOURCE_NONE;
            }

            // Flip-side special case for clicking either "contacts" option: if a specific selection
            // exists for priority conversations, leave it untouched; otherwise, set to none.
            if ((preference.getKey() == KEY_STARRED || preference.getKey() == KEY_CONTACTS)
                    && currConvosSetting == CONVERSATION_SENDERS_ANYONE) {
                savedSettings[1] = CONVERSATION_SENDERS_NONE;
            }
        }

        return savedSettings;
    }

    private String getSummary(String key) {
        switch (key) {
            case KEY_STARRED:
                return mZenModeBackend.getStarredContactsSummary(mContext);
            case KEY_CONTACTS:
                return mZenModeBackend.getContactsNumberSummary(mContext);
            case KEY_IMPORTANT:
                return getConversationSummary();
            case KEY_ANY:
                return mContext.getResources().getString(mIsMessages
                        ? R.string.zen_mode_all_messages_summary
                        : R.string.zen_mode_all_calls_summary);
            case KEY_NONE:
            default:
                return null;
        }
    }

    private String getConversationSummary() {
        final int numConversations = mNumImportantConversations;

        if (numConversations == UNKNOWN) {
            return null;
        } else {
            MessageFormat msgFormat = new MessageFormat(
                    mContext.getString(R.string.zen_mode_conversations_count),
                    Locale.getDefault());
            Map<String, Object> args = new HashMap<>();
            args.put("count", numConversations);
            return msgFormat.format(args);
        }
    }

    void updateChannelCounts() {
        // Load conversations
        ParceledListSlice<ConversationChannelWrapper> impConversations =
                mNotificationBackend.getConversations(true);
        int numImportantConversations = 0;
        if (impConversations != null) {
            for (ConversationChannelWrapper conversation : impConversations.getList()) {
                if (!conversation.getNotificationChannel().isDemoted()) {
                    numImportantConversations++;
                }
            }
        }
        mNumImportantConversations = numImportantConversations;
    }

    private SelectorWithWidgetPreference makeSelectorPreference(String key, int titleId,
            boolean isCheckbox) {
        final SelectorWithWidgetPreference pref =
                new SelectorWithWidgetPreference(mPreferenceCategory.getContext(), isCheckbox);
        pref.setKey(key);
        pref.setTitle(titleId);
        pref.setOnClickListener(mSelectorClickListener);

        View.OnClickListener widgetClickListener = getWidgetClickListener(key);
        if (widgetClickListener != null) {
            pref.setExtraWidgetOnClickListener(widgetClickListener);
        }

        mPreferenceCategory.addPreference(pref);
        mSelectorPreferences.add(pref);
        return pref;
    }

    private View.OnClickListener getWidgetClickListener(String key) {
        if (!KEY_CONTACTS.equals(key) && !KEY_STARRED.equals(key) && !KEY_IMPORTANT.equals(key)) {
            return null;
        }

        if (KEY_STARRED.equals(key) && !isStarredIntentValid()) {
            return null;
        }

        if (KEY_CONTACTS.equals(key) && !isContactsIntentValid()) {
            return null;
        }

        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (KEY_STARRED.equals(key)
                        && STARRED_CONTACTS_INTENT.resolveActivity(mPackageManager) != null) {
                    mContext.startActivity(STARRED_CONTACTS_INTENT);
                } else if (KEY_CONTACTS.equals(key)
                        && ALL_CONTACTS_INTENT.resolveActivity(mPackageManager) != null) {
                    mContext.startActivity(ALL_CONTACTS_INTENT);
                } else if (KEY_IMPORTANT.equals(key)) {
                    new SubSettingLauncher(mContext)
                            .setDestination(ConversationListSettings.class.getName())
                            .setSourceMetricsCategory(SettingsEnums.DND_CONVERSATIONS)
                            .launch();
                } else {
                    mContext.startActivity(FALLBACK_INTENT);
                }
            }
        };
    }

    private boolean isStarredIntentValid() {
        return STARRED_CONTACTS_INTENT.resolveActivity(mPackageManager) != null
                || FALLBACK_INTENT.resolveActivity(mPackageManager) != null;
    }

    private boolean isContactsIntentValid() {
        return ALL_CONTACTS_INTENT.resolveActivity(mPackageManager) != null
                || FALLBACK_INTENT.resolveActivity(mPackageManager) != null;
    }
}
