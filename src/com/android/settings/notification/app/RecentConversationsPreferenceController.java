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
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.widget.Button;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RecentConversationsPreferenceController extends AbstractPreferenceController {

    private static final String TAG = "RecentConversationsPC";
    private static final String KEY = "recent_conversations";
    private static final String CLEAR_ALL_KEY_SUFFIX = "_clear_all";
    private final IPeopleManager mPs;
    private final NotificationBackend mBackend;
    private PreferenceGroup mPreferenceGroup;

    public RecentConversationsPreferenceController(
            Context context, NotificationBackend backend, IPeopleManager ps) {
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

    //TODO(b/233325816): Use ButtonPreference instead.
    LayoutPreference getClearAll(PreferenceGroup parent) {
        LayoutPreference pref = new LayoutPreference(
                mContext, R.layout.conversations_clear_recents);
        pref.setKey(getPreferenceKey() + CLEAR_ALL_KEY_SUFFIX);
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
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceGroup = screen.findPreference(getPreferenceKey());
    }

    /**
     * Updates the conversation list.
     *
     * @return true if this controller has content to display.
     */
    boolean updateList() {
        // Load conversations
        List<ConversationChannel> conversations = Collections.emptyList();
        try {
            conversations = mPs.getRecentConversations().getList();
        } catch (RemoteException e) {
            Slog.w(TAG, "Could not get recent conversations", e);
        }

        return populateList(conversations);
    }

    @VisibleForTesting
    boolean populateList(List<ConversationChannel> conversations) {
        mPreferenceGroup.removeAll();
        boolean hasClearable = false;
        if (conversations != null) {
            hasClearable = populateConversations(conversations);
        }

        boolean hashContent = mPreferenceGroup.getPreferenceCount() != 0;
        mPreferenceGroup.setVisible(hashContent);
        if (hashContent && hasClearable) {
            Preference clearAll = getClearAll(mPreferenceGroup);
            if (clearAll != null) {
                mPreferenceGroup.addPreference(clearAll);
            }
        }
        return hashContent;
    }

    protected boolean populateConversations(List<ConversationChannel> conversations) {
        AtomicInteger order = new AtomicInteger(100);
        AtomicBoolean hasClearable = new AtomicBoolean(false);
        conversations.stream()
                .filter(conversation ->
                        conversation.getNotificationChannel().getImportance() != IMPORTANCE_NONE
                                && (conversation.getNotificationChannelGroup() == null
                                || !conversation.getNotificationChannelGroup().isBlocked()))
                .sorted(mConversationComparator)
                .map(this::createConversationPref)
                .forEachOrdered(pref -> {
                    pref.setOrder(order.getAndIncrement());
                    mPreferenceGroup.addPreference(pref);
                    if (pref.hasClearListener()) {
                        hasClearable.set(true);
                    }
                });
        return hasClearable.get();
    }

    protected RecentConversationPreference createConversationPref(
            final ConversationChannel conversation) {
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
                    mPreferenceGroup.removePreference(pref);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Could not clear recent", e);
                }
            });
        }

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

    @VisibleForTesting
    Comparator<ConversationChannel> mConversationComparator =
            new Comparator<ConversationChannel>() {
                private final Collator sCollator = Collator.getInstance();

                @Override
                public int compare(ConversationChannel o1, ConversationChannel o2) {
                    int labelComparison = 0;
                    if (o1.getShortcutInfo().getLabel() != null
                            && o2.getShortcutInfo().getLabel() != null) {
                        labelComparison = sCollator.compare(
                                o1.getShortcutInfo().getLabel().toString(),
                                o2.getShortcutInfo().getLabel().toString());
                    }

                    if (labelComparison == 0) {
                        return o1.getNotificationChannel().getId().compareTo(
                                o2.getNotificationChannel().getId());
                    }

                    return labelComparison;
                }
            };
}
