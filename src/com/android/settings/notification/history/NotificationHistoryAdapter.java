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

import android.app.INotificationManager;
import android.app.NotificationHistory.HistoricalNotification;
import android.os.RemoteException;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class NotificationHistoryAdapter extends
        RecyclerView.Adapter<NotificationHistoryViewHolder> implements
        NotificationHistoryRecyclerView.OnItemSwipeDeleteListener {

    private static String TAG = "NotiHistoryAdapter";

    private INotificationManager mNm;
    private List<HistoricalNotification> mValues;

    public NotificationHistoryAdapter(INotificationManager nm,
            NotificationHistoryRecyclerView listView) {
        mValues = new ArrayList<>();
        setHasStableIds(true);
        listView.setOnItemSwipeDeleteListener(this);
        mNm = nm;
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
        holder.addOnClick(hn.getPackage(), hn.getUserId(), hn.getChannelId(),
                hn.getConversationId());
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
        HistoricalNotification hn = mValues.remove(position);
        if (hn != null) {
            try {
                mNm.deleteNotificationHistoryItem(
                        hn.getPackage(), hn.getUid(), hn.getPostedTimeMs());
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to delete item", e);
            }
        }
        notifyItemRemoved(position);
    }
}
