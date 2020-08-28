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

package com.android.settings.notification.zen;

import static android.app.NotificationManager.Policy.CONVERSATION_SENDERS_ANYONE;
import static android.app.NotificationManager.Policy.CONVERSATION_SENDERS_IMPORTANT;
import static android.app.NotificationManager.Policy.CONVERSATION_SENDERS_NONE;

import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.service.notification.ConversationChannelWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Updates the DND Settings conversations image resource based on the conversations channels.
 */
public class ZenModeConversationsImagePreferenceController
        extends AbstractZenModePreferenceController {
    private static final int MAX_CONVERSATIONS_SHOWN = 5;
    private final int mIconSizePx;
    private final int mIconOffsetPx;
    private final ArrayList<Drawable> mConversationDrawables = new ArrayList<>();
    private final NotificationBackend mNotificationBackend;

    private ViewGroup mViewGroup;
    private LayoutPreference mPreference;

    public ZenModeConversationsImagePreferenceController(Context context, String key,
            Lifecycle lifecycle, NotificationBackend notificationBackend) {
        super(context, key, lifecycle);
        mNotificationBackend = notificationBackend;
        mIconSizePx =
                mContext.getResources().getDimensionPixelSize(R.dimen.zen_conversations_icon_size);
        mIconOffsetPx = mContext.getResources()
                .getDimensionPixelSize(R.dimen.zen_conversations_icon_offset);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (LayoutPreference) screen.findPreference(KEY);
        mViewGroup =
                (ViewGroup) mPreference.findViewById(R.id.zen_mode_settings_senders_overlay_view);
        loadConversations();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void updateState(Preference preference) {
        loadConversations();

        mViewGroup.removeAllViews();
        final int conversationSenders = mBackend.getPriorityConversationSenders();
        if (conversationSenders == CONVERSATION_SENDERS_ANYONE) {
            mViewGroup.setContentDescription(
                    mContext.getResources().getString(R.string.zen_mode_from_all_conversations));
        } else if (conversationSenders == CONVERSATION_SENDERS_IMPORTANT) {
            mViewGroup.setContentDescription(
                    mContext.getResources().getString(
                            R.string.zen_mode_from_important_conversations));
        } else {
            mViewGroup.setContentDescription(null);
            mViewGroup.setVisibility(View.GONE);
            return;
        }

        final int numDrawablesToShow = Math.min(MAX_CONVERSATIONS_SHOWN,
                mConversationDrawables.size());
        for (int i = 0; i < numDrawablesToShow; i++) {
            ImageView iv = new ImageView(mContext);
            iv.setImageDrawable(mConversationDrawables.get(i));
            iv.setLayoutParams(new ViewGroup.LayoutParams(mIconSizePx, mIconSizePx));

            FrameLayout fl = new FrameLayout(mContext);
            fl.addView(iv);
            fl.setPadding((numDrawablesToShow - i - 1) * mIconOffsetPx, 0, 0, 0);
            mViewGroup.addView(fl);
        }

        mViewGroup.setVisibility(numDrawablesToShow > 0 ? View.VISIBLE : View.GONE);
    }

    private void loadConversations() {
        // Load conversations
        new AsyncTask<Void, Void, Void>() {
            private List<Drawable> mDrawables = new ArrayList<>();
            @Override
            protected Void doInBackground(Void... unused) {
                mDrawables.clear();
                final int conversationSenders = mBackend.getPriorityConversationSenders();
                if (conversationSenders == CONVERSATION_SENDERS_NONE) {
                    return null;
                }
                ParceledListSlice<ConversationChannelWrapper> conversations =
                        mNotificationBackend.getConversations(
                                conversationSenders == CONVERSATION_SENDERS_IMPORTANT);
                if (conversations != null) {
                    for (ConversationChannelWrapper conversation : conversations.getList()) {
                        if (!conversation.getNotificationChannel().isDemoted()) {
                            Drawable drawable = mNotificationBackend.getConversationDrawable(
                                    mContext,
                                    conversation.getShortcutInfo(),
                                    conversation.getPkg(),
                                    conversation.getUid(),
                                    conversation.getNotificationChannel()
                                            .isImportantConversation());
                            if (drawable != null) {
                                mDrawables.add(drawable);
                            }
                        }
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                if (mContext == null) {
                    return;
                }
                mConversationDrawables.clear();
                mConversationDrawables.addAll(mDrawables);
                updateState(mPreference);
            }
        }.execute();
    }
}
