/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.notification.app;

import android.app.NotificationChannel;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.content.pm.ShortcutInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.ConversationChannelWrapper;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.NotificationBackend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppConversationListPreferenceController extends NotificationPreferenceController {

    private static final String KEY = "conversations";
    public static final String ARG_FROM_SETTINGS = "fromSettings";

    protected List<ConversationChannelWrapper> mConversations = new ArrayList<>();
    protected PreferenceCategory mPreference;

    public AppConversationListPreferenceController(Context context, NotificationBackend backend) {
        super(context, backend);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        if (mAppRow == null) {
            return false;
        }
        if (mAppRow.banned) {
            return false;
        }
        if (mChannel != null) {
            if (mBackend.onlyHasDefaultChannel(mAppRow.pkg, mAppRow.uid)
                    || NotificationChannel.DEFAULT_CHANNEL_ID.equals(mChannel.getId())) {
                return false;
            }
        }
        return mBackend.hasSentValidMsg(mAppRow.pkg, mAppRow.uid) || mBackend.isInInvalidMsgState(
                mAppRow.pkg, mAppRow.uid);
    }

    @Override
    public void updateState(Preference preference) {
        mPreference = (PreferenceCategory) preference;
        loadConversationsAndPopulate();
    }

    protected void loadConversationsAndPopulate() {
        if (mAppRow == null) {
            return;
        }
        // Load channel settings
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                ParceledListSlice<ConversationChannelWrapper> list =
                        mBackend.getConversations(mAppRow.pkg, mAppRow.uid);
                if (list != null) {
                    mConversations = filterAndSortConversations(list.getList());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                if (mContext == null) {
                    return;
                }
                populateList();
            }
        }.execute();
    }

    protected List<ConversationChannelWrapper> filterAndSortConversations(
            List<ConversationChannelWrapper> conversations) {
        Collections.sort(conversations, mConversationComparator);
        return conversations;
    }

    protected int getTitleResId() {
        return R.string.conversations_category_title;
    }

    protected void populateList() {
        if (mPreference == null) {
            return;
        }

        if (!mConversations.isEmpty()) {
            // TODO: if preference has children, compare with newly loaded list
            mPreference.removeAll();
            mPreference.setTitle(getTitleResId());
            populateConversations();
        }
    }

    private void populateConversations() {
        for (ConversationChannelWrapper conversation : mConversations) {
            if (conversation.getNotificationChannel().isDemoted()) {
                continue;
            }
            mPreference.addPreference(createConversationPref(conversation));
        }
    }

    protected Preference createConversationPref(final ConversationChannelWrapper conversation) {
        Preference pref = new Preference(mContext);
        populateConversationPreference(conversation, pref);
        return pref;
    }

    protected void populateConversationPreference(final ConversationChannelWrapper conversation,
            final Preference pref) {
        ShortcutInfo si = conversation.getShortcutInfo();

        pref.setTitle(si != null
                ? si.getLabel()
                : conversation.getNotificationChannel().getName());
        pref.setSummary(conversation.getNotificationChannel().getGroup() != null
                ? mContext.getString(R.string.notification_conversation_summary,
                conversation.getParentChannelLabel(), conversation.getGroupLabel())
                : conversation.getParentChannelLabel());
        if (si != null) {
            pref.setIcon(mBackend.getConversationDrawable(mContext, si, mAppRow.pkg, mAppRow.uid,
                    conversation.getNotificationChannel().isImportantConversation()));
        }
        pref.setKey(conversation.getNotificationChannel().getId());

        Bundle channelArgs = new Bundle();
        channelArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, mAppRow.uid);
        channelArgs.putString(AppInfoBase.ARG_PACKAGE_NAME, mAppRow.pkg);
        channelArgs.putString(Settings.EXTRA_CHANNEL_ID,
                conversation.getNotificationChannel().getParentChannelId());
        channelArgs.putString(Settings.EXTRA_CONVERSATION_ID,
                conversation.getNotificationChannel().getConversationId());
        channelArgs.putBoolean(ARG_FROM_SETTINGS, true);
        pref.setIntent(new SubSettingLauncher(mContext)
                .setDestination(ChannelNotificationSettings.class.getName())
                .setArguments(channelArgs)
                .setExtras(channelArgs)
                .setTitleText(pref.getTitle())
                .setSourceMetricsCategory(SettingsEnums.NOTIFICATION_APP_NOTIFICATION)
                .toIntent());
    }

    protected Comparator<ConversationChannelWrapper> mConversationComparator =
            (left, right) -> {
                if (left.getNotificationChannel().isImportantConversation()
                        != right.getNotificationChannel().isImportantConversation()) {
                    // important first
                    return Boolean.compare(right.getNotificationChannel().isImportantConversation(),
                            left.getNotificationChannel().isImportantConversation());
                }
                return left.getNotificationChannel().getId().compareTo(
                        right.getNotificationChannel().getId());
            };
}
