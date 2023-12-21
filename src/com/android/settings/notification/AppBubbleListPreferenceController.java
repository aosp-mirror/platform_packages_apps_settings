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

package com.android.settings.notification;

import static android.app.NotificationChannel.DEFAULT_ALLOW_BUBBLE;
import static android.app.NotificationManager.BUBBLE_PREFERENCE_ALL;
import static android.app.NotificationManager.BUBBLE_PREFERENCE_NONE;
import static android.app.NotificationManager.BUBBLE_PREFERENCE_SELECTED;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.service.notification.ConversationChannelWrapper;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.notification.app.AppConversationListPreferenceController;
import com.android.settingslib.RestrictedLockUtils;

import com.google.common.annotations.VisibleForTesting;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Displays a list of conversations that have either been selected or excluded from bubbling.
 */
public class AppBubbleListPreferenceController extends AppConversationListPreferenceController {

    private static final String KEY = "bubble_conversations";

    public AppBubbleListPreferenceController(Context context, NotificationBackend backend) {
        super(context, backend);
    }

    @Override
    public void updateState(Preference preference) {
        // loading convos is async; hide header until we know we have conversations to show
        preference.setVisible(false);
        super.updateState(preference);
    }

    @Override
    public void onResume(NotificationBackend.AppRow appRow,
            @Nullable NotificationChannel channel, @Nullable NotificationChannelGroup group,
            Drawable conversationDrawable,
            ShortcutInfo conversationInfo,
            RestrictedLockUtils.EnforcedAdmin admin,
            List<String> preferenceFilter) {
        super.onResume(appRow, channel, group, conversationDrawable, conversationInfo, admin,
                preferenceFilter);
        // In case something changed in the foreground (e.g. via bubble button on notification)
        loadConversationsAndPopulate();
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        // copy rather than inherit super's isAvailable because apps can link to this page
        // as part of onboarding, before they send a valid conversation notification
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
        if (mAppRow.bubblePreference == BUBBLE_PREFERENCE_NONE) {
            return false;
        }
        return true;
    }

    @VisibleForTesting
    @Override
    public List<ConversationChannelWrapper> filterAndSortConversations(
            List<ConversationChannelWrapper> conversations) {
        return conversations.stream()
                .sorted(mConversationComparator)
                .filter((c) -> {
                    if (mAppRow.bubblePreference == BUBBLE_PREFERENCE_SELECTED) {
                        return c.getNotificationChannel().canBubble();
                    } else if (mAppRow.bubblePreference == BUBBLE_PREFERENCE_ALL) {
                        return c.getNotificationChannel().getAllowBubbles() == 0;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    @Override
    protected int getTitleResId() {
        // TODO: possible to left align like mocks?
        return mAppRow.bubblePreference == BUBBLE_PREFERENCE_SELECTED
                ? R.string.bubble_app_setting_selected_conversation_title
                : R.string.bubble_app_setting_excluded_conversation_title;
    }

    @VisibleForTesting
    @Override
    public Preference createConversationPref(final ConversationChannelWrapper conversation) {
        final ConversationPreference pref = new ConversationPreference(mContext);
        populateConversationPreference(conversation, pref);
        pref.setOnClickBubblesConversation(mAppRow.bubblePreference == BUBBLE_PREFERENCE_ALL);
        pref.setOnClickListener((v) -> {
            conversation.getNotificationChannel().setAllowBubbles(DEFAULT_ALLOW_BUBBLE);
            mBackend.updateChannel(mAppRow.pkg, mAppRow.uid, conversation.getNotificationChannel());
            mPreference.removePreference(pref);
            if (mPreference.getPreferenceCount() == 0) {
                mPreference.setVisible(false);
            }
        });
        return pref;
    }

    @Override
    protected void populateList() {
        super.populateList();
        if (mPreference == null) {
            return;
        }
        mPreference.setVisible(mPreference.getPreferenceCount() > 0);
    }

    /** Simple preference with a 'x' button at the end. */
    @VisibleForTesting
    public static class ConversationPreference extends Preference implements View.OnClickListener {

        View.OnClickListener mOnClickListener;
        boolean mOnClickBubbles;

        ConversationPreference(Context context) {
            super(context);
            setWidgetLayoutResource(R.layout.bubble_conversation_remove_button);
        }

        @Override
        public void onBindViewHolder(final PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            ImageView view =  holder.itemView.findViewById(R.id.button);
            view.setContentDescription(mOnClickBubbles
                    ? getContext().getString(R.string.bubble_app_setting_bubble_conversation)
                    : getContext().getString(R.string.bubble_app_setting_unbubble_conversation));
            view.setOnClickListener(mOnClickListener);
        }

        public void setOnClickBubblesConversation(boolean enablesBubbles) {
            mOnClickBubbles = enablesBubbles;
        }

        public void setOnClickListener(View.OnClickListener listener) {
            mOnClickListener = listener;
        }

        @Override
        public void onClick(View v) {
            if (mOnClickListener != null) {
                mOnClickListener.onClick(v);
            }
        }
    }
}
