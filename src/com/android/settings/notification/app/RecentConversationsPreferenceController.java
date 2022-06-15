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

import static android.app.NotificationManager.IMPORTANCE_NONE;

import android.app.people.ConversationChannel;
import android.app.people.IPeopleManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.widget.Button;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.LayoutPreference;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RecentConversationsPreferenceController extends AbstractPreferenceController {

    private static final String TAG = "RecentConversationsPC";
    private static final String KEY = "recent_conversations";
    private List<ConversationChannel> mConversations;
    private final IPeopleManager mPs;
    private final NotificationBackend mBackend;

    public RecentConversationsPreferenceController(Context context, NotificationBackend backend,
            IPeopleManager ps) {
        super(context);
        mBackend = backend;
        mPs = ps;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    LayoutPreference getClearAll(PreferenceGroup parent) {
        LayoutPreference pref = new LayoutPreference(
                mContext, R.layout.conversations_clear_recents);
        pref.setOrder(1);
        Button button = pref.findViewById(R.id.conversation_settings_clear_recents);
        button.setOnClickListener(v -> {
            try {
                mPs.removeAllRecentConversations();
                // Removing recents is asynchronous, so we can't immediately reload the list from
                // the backend. Instead, proactively remove all of items that were marked as
                // clearable, so long as we didn't get an error

                for (int i = parent.getPreferenceCount() - 1; i >= 0; i--) {
                    Preference p = parent.getPreference(i);
                    if (p instanceof RecentConversationPreference) {
                        if (((RecentConversationPreference) p).hasClearListener()) {
                            parent.removePreference(p);
                        }
                    }
                }
                button.announceForAccessibility(mContext.getString(R.string.recent_convos_removed));
            } catch (RemoteException e) {
                Slog.w(TAG, "Could not clear recents", e);
            }
        });
        return pref;
    }

    @Override
    public void updateState(Preference preference) {
        PreferenceCategory pref = (PreferenceCategory) preference;
        // Load conversations
        try {
            mConversations = mPs.getRecentConversations().getList();
        } catch (RemoteException e) {
            Slog.w(TAG, "Could get recents", e);
        }
        Collections.sort(mConversations, mConversationComparator);

        populateList(mConversations, pref);

    }

    protected void populateList(List<ConversationChannel> conversations,
            PreferenceGroup containerGroup) {
        containerGroup.removeAll();
        boolean hasClearable = false;
        if (conversations != null) {
            hasClearable = populateConversations(conversations, containerGroup);
        }

        if (containerGroup.getPreferenceCount() == 0) {
            containerGroup.setVisible(false);
        } else {
            containerGroup.setVisible(true);
            if (hasClearable) {
                Preference clearAll = getClearAll(containerGroup);
                if (clearAll != null) {
                    containerGroup.addPreference(clearAll);
                }
            }
        }
    }

    protected boolean populateConversations(List<ConversationChannel> conversations,
            PreferenceGroup containerGroup) {
        int order = 100;
        boolean hasClearable = false;
        for (ConversationChannel conversation : conversations) {
            if (conversation.getNotificationChannel().getImportance() == IMPORTANCE_NONE
                    || (conversation.getNotificationChannelGroup() != null
                    && conversation.getNotificationChannelGroup().isBlocked())) {
                continue;
            }
            RecentConversationPreference pref =
                    createConversationPref(containerGroup, conversation, order++);
            containerGroup.addPreference(pref);
            if (pref.hasClearListener()) {
                hasClearable = true;
            }
        }
        return hasClearable;
    }

    protected RecentConversationPreference createConversationPref(PreferenceGroup parent,
            final ConversationChannel conversation, int order) {
        final String pkg = conversation.getShortcutInfo().getPackage();
        final int uid = conversation.getUid();
        final String conversationId = conversation.getShortcutInfo().getId();
        RecentConversationPreference pref = new RecentConversationPreference(mContext);

        if (!conversation.hasActiveNotifications()) {
            pref.setOnClearClickListener(() -> {
                try {
                    mPs.removeRecentConversation(pkg, UserHandle.getUserId(uid), conversationId);
                    pref.getClearView().announceForAccessibility(
                            mContext.getString(R.string.recent_convo_removed));
                    parent.removePreference(pref);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Could not clear recent", e);
                }
            });
        }
        pref.setOrder(order);

        pref.setTitle(getTitle(conversation));
        pref.setSummary(getSummary(conversation));
        pref.setIcon(mBackend.getConversationDrawable(mContext, conversation.getShortcutInfo(),
                pkg, uid, false));
        pref.setKey(conversation.getNotificationChannel().getId()
                + ":" + conversationId);
        pref.setOnPreferenceClickListener(preference -> {
            mBackend.createConversationNotificationChannel(
                    pkg, uid,
                    conversation.getNotificationChannel(),
                    conversationId);
            getSubSettingLauncher(conversation, pref.getTitle()).launch();
            return true;
        });

        return pref;
    }

    CharSequence getSummary(ConversationChannel conversation) {
        return conversation.getNotificationChannelGroup() == null
                ? conversation.getNotificationChannel().getName()
                : mContext.getString(R.string.notification_conversation_summary,
                        conversation.getNotificationChannel().getName(),
                        conversation.getNotificationChannelGroup().getName());
    }

    CharSequence getTitle(ConversationChannel conversation) {
        ShortcutInfo si = conversation.getShortcutInfo();
        return si.getLabel();
    }

    SubSettingLauncher getSubSettingLauncher(ConversationChannel conversation,
            CharSequence title) {
        Bundle channelArgs = new Bundle();
        channelArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, conversation.getUid());
        channelArgs.putString(AppInfoBase.ARG_PACKAGE_NAME,
                conversation.getShortcutInfo().getPackage());
        channelArgs.putString(Settings.EXTRA_CHANNEL_ID,
                conversation.getNotificationChannel().getId());
        channelArgs.putString(Settings.EXTRA_CONVERSATION_ID,
                conversation.getShortcutInfo().getId());

        return new SubSettingLauncher(mContext)
                .setDestination(ChannelNotificationSettings.class.getName())
                .setArguments(channelArgs)
                .setExtras(channelArgs)
                .setUserHandle(UserHandle.getUserHandleForUid(conversation.getUid()))
                .setTitleText(title)
                .setSourceMetricsCategory(SettingsEnums.NOTIFICATION_CONVERSATION_LIST_SETTINGS);
    }

    protected Comparator<ConversationChannel> mConversationComparator =
            new Comparator<ConversationChannel>() {
                private final Collator sCollator = Collator.getInstance();
                @Override
                public int compare(ConversationChannel o1, ConversationChannel o2) {
                    int labelComparison = sCollator.compare(o1.getShortcutInfo().getLabel(),
                            o2.getShortcutInfo().getLabel());

                    if (labelComparison == 0) {
                        return o1.getNotificationChannel().getId().compareTo(
                                o2.getNotificationChannel().getId());
                    }

                    return labelComparison;
                }
            };
}
