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

package com.android.settings.notification.history;

import static android.provider.Settings.EXTRA_APP_PACKAGE;
import static android.provider.Settings.EXTRA_CHANNEL_ID;
import static android.provider.Settings.EXTRA_CONVERSATION_ID;

import android.app.INotificationManager;
import android.app.NotificationHistory.HistoricalNotification;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.logging.UiEventLogger;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class NotificationHistoryAdapter extends
        RecyclerView.Adapter<NotificationHistoryViewHolder> implements
        NotificationHistoryRecyclerView.OnItemSwipeDeleteListener {

    private static String TAG = "NotiHistoryAdapter";

    private INotificationManager mNm;
    private List<HistoricalNotification> mValues;
    private OnItemDeletedListener mListener;
    private UiEventLogger mUiEventLogger;
    public NotificationHistoryAdapter(INotificationManager nm,
            NotificationHistoryRecyclerView listView,
            OnItemDeletedListener listener,
            UiEventLogger uiEventLogger) {
        mValues = new ArrayList<>();
        setHasStableIds(true);
        listView.setOnItemSwipeDeleteListener(this);
        mNm = nm;
        mListener = listener;
        mUiEventLogger = uiEventLogger;
    }

    @Override
    public NotificationHistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification_history_log_row, parent, false);
        return new NotificationHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final @NonNull NotificationHistoryViewHolder holder,
            int position) {
        final HistoricalNotification hn = mValues.get(position);
        holder.setTitle(hn.getTitle());
        holder.setSummary(hn.getText());
        holder.setPostedTime(hn.getPostedTimeMs());
        holder.itemView.setOnClickListener(v -> {
            mUiEventLogger.logWithPosition(NotificationHistoryActivity.NotificationHistoryEvent
                    .NOTIFICATION_HISTORY_OLDER_ITEM_CLICK, hn.getUid(), hn.getPackage(), position);
            Intent intent =  new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(EXTRA_APP_PACKAGE, hn.getPackage())
                    .putExtra(EXTRA_CHANNEL_ID, hn.getChannelId())
                    .putExtra(EXTRA_CONVERSATION_ID, hn.getConversationId());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            holder.itemView.getContext().startActivityAsUser(intent, UserHandle.of(hn.getUserId()));
        });
        holder.itemView.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host,
                    AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                CharSequence description =
                        host.getResources().getText(R.string.notification_history_view_settings);
                AccessibilityNodeInfo.AccessibilityAction customClick =
                        new AccessibilityNodeInfo.AccessibilityAction(
                                AccessibilityNodeInfo.ACTION_CLICK, description);
                info.addAction(customClick);
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_DISMISS);
            }

            @Override
            public boolean performAccessibilityAction(View host, int action, Bundle args) {
                super.performAccessibilityAction(host, action, args);
                if (action == AccessibilityNodeInfo.AccessibilityAction.ACTION_DISMISS.getId()) {
                    int currPosition = mValues.indexOf(hn);
                    onItemSwipeDeleted(currPosition);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public void onRebuildComplete(List<HistoricalNotification> notifications) {
        mValues = notifications;
        mValues.sort((o1, o2) -> Long.compare(o2.getPostedTimeMs(), o1.getPostedTimeMs()));
        notifyDataSetChanged();
    }

    @Override
    public void onItemSwipeDeleted(int position) {
        if (position > (mValues.size() - 1)) {
            Slog.d(TAG, "Tried to swipe element out of list: position: " + position
                    + " size? " + mValues.size());
            return;
        }
        HistoricalNotification hn = mValues.remove(position);
        if (hn != null) {
            try {
                mNm.deleteNotificationHistoryItem(
                        hn.getPackage(), hn.getUid(), hn.getPostedTimeMs());
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to delete item", e);
            }
            mUiEventLogger.logWithPosition(NotificationHistoryActivity.NotificationHistoryEvent
                        .NOTIFICATION_HISTORY_OLDER_ITEM_DELETE, hn.getUid(), hn.getPackage(),
                    position);
        }
        mListener.onItemDeleted(mValues.size());
        notifyItemRemoved(position);
    }

    interface OnItemDeletedListener {
        void onItemDeleted(int newCount);
    }
}
