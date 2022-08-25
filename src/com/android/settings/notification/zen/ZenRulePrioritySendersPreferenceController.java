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

import static com.android.settings.notification.zen.ZenPrioritySendersHelper.UNKNOWN;

import android.content.Context;
import android.os.AsyncTask;
import android.service.notification.ZenPolicy;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

/**
 * Shared controller for custom rule priority senders settings for both messages and calls.
 *
 * Most functionality is the same as that of the main zen mode messages and calls settings;
 * these controllers handle which senders are allowed to break through DND for messages or calls,
 * with possible settings options being: starred contacts, all contacts, priority conversations
 * (for messages only), anyone, or no one.
 */
public class ZenRulePrioritySendersPreferenceController
        extends AbstractZenCustomRulePreferenceController {
    private final boolean mIsMessages; // if this is false, then this preference is for calls

    private PreferenceCategory mPreferenceCategory;
    private ZenPrioritySendersHelper mHelper;

    public ZenRulePrioritySendersPreferenceController(Context context, String key,
            Lifecycle lifecycle, boolean isMessages, NotificationBackend notificationBackend) {
        super(context, key, lifecycle);
        mIsMessages = isMessages;

        mHelper = new ZenPrioritySendersHelper(
                context, isMessages, mBackend, notificationBackend, mSelectorClickListener);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mHelper.displayPreference(mPreferenceCategory);
        super.displayPreference(screen);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mRule != null && mRule.getZenPolicy() != null) {
            final int currContactsSetting = getPrioritySenders();
            final int currConversationsSetting = getPriorityConversationSenders();
            mHelper.updateState(currContactsSetting, currConversationsSetting);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIsMessages) {
            updateChannelCounts();
        }
        mHelper.updateSummaries();
    }

    private void updateChannelCounts() {
        // Load conversations
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                mHelper.updateChannelCounts();
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                if (mContext == null) {
                    return;
                }
                updateState(mPreferenceCategory);
            }
        }.execute();
    }

    private int getPrioritySenders() {
        if (mRule == null || mRule.getZenPolicy() == null) {
            return UNKNOWN;
        }
        if (mIsMessages) {
            return ZenModeBackend.getContactSettingFromZenPolicySetting(
                    mRule.getZenPolicy().getPriorityMessageSenders());
        } else {
            return ZenModeBackend.getContactSettingFromZenPolicySetting(
                    mRule.getZenPolicy().getPriorityCallSenders());
        }
    }

    private int getPriorityConversationSenders() {
        if (mRule == null || mRule.getZenPolicy() == null) {
            return UNKNOWN;
        }
        return mRule.getZenPolicy().getPriorityConversationSenders();
    }

    // Returns the ZenPolicySetting enum associated with the provided NotificationManager.Policy.
    static @ZenPolicy.PeopleType int zenPolicySettingFromSender(int senderSetting) {
        return ZenModeBackend.getZenPolicySettingFromPrefKey(
                ZenModeBackend.getKeyFromSetting(senderSetting));
    }

    @VisibleForTesting
    SelectorWithWidgetPreference.OnClickListener mSelectorClickListener =
            new SelectorWithWidgetPreference.OnClickListener() {
                @Override
                public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
                    if (mRule == null || mRule.getZenPolicy() == null) {
                        return;
                    }

                    final int[] settingsToSave = mHelper.settingsToSaveOnClick(preference,
                            getPrioritySenders(), getPriorityConversationSenders());
                    final int prioritySendersSetting = settingsToSave[0];
                    final int priorityConvosSetting = settingsToSave[1];

                    // if both are UNKNOWN then just return
                    if (prioritySendersSetting == UNKNOWN && priorityConvosSetting == UNKNOWN) {
                        return;
                    }

                    if (prioritySendersSetting != UNKNOWN) {
                        if (mIsMessages) {
                            mRule.setZenPolicy(new ZenPolicy.Builder(mRule.getZenPolicy())
                                    .allowMessages(
                                            zenPolicySettingFromSender(prioritySendersSetting))
                                    .build());
                        } else {
                            mRule.setZenPolicy(new ZenPolicy.Builder(mRule.getZenPolicy())
                                    .allowCalls(
                                            zenPolicySettingFromSender(prioritySendersSetting))
                                    .build());
                        }
                    }

                    if (mIsMessages && priorityConvosSetting != UNKNOWN) {
                        mRule.setZenPolicy(new ZenPolicy.Builder(mRule.getZenPolicy())
                                .allowConversations(priorityConvosSetting)
                                .build());
                    }

                    // Save any changes
                    mBackend.updateZenRule(mId, mRule);
                }
            };
}
