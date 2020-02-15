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

import android.app.NotificationHistory;
import android.app.NotificationHistory.HistoricalNotification;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class NotificationHistoryAdapter extends
        RecyclerView.Adapter<NotificationHistoryViewHolder> {

    private List<HistoricalNotification> mValues;

    public NotificationHistoryAdapter() {
        mValues = new ArrayList<>();
        setHasStableIds(true);
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
        holder.addOnClick(hn.getPackage(), hn.getUserId(), hn.getChannelId());
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
}
