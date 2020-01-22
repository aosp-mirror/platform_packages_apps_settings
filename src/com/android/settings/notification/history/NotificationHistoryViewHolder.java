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

import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;
import android.widget.DateTimeView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

public class NotificationHistoryViewHolder extends RecyclerView.ViewHolder {

    private final DateTimeView mTime;
    private final TextView mTitle;
    private final TextView mSummary;

    NotificationHistoryViewHolder(View itemView) {
        super(itemView);
        mTime = itemView.findViewById(R.id.timestamp);
        mTime.setShowRelativeTime(true);
        mTitle = itemView.findViewById(R.id.title);
        mSummary = itemView.findViewById(R.id.text);
    }

    void setSummary(CharSequence summary) {
        mSummary.setText(summary);
        mSummary.setVisibility(summary != null ? View.VISIBLE : View.GONE);
    }

    void setTitle(CharSequence title) {
        mTitle.setText(title);
        mTitle.setVisibility(title != null ? View.VISIBLE : View.GONE);
    }

    void setPostedTime(long postedTime) {
        mTime.setTime(postedTime);
    }

    void addOnClick(String pkg, int userId, String channelId) {
        itemView.setOnClickListener(v -> {
            Intent intent =  new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(EXTRA_APP_PACKAGE, pkg)
                    .putExtra(EXTRA_CHANNEL_ID, channelId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            itemView.getContext().startActivityAsUser(intent, UserHandle.of(userId));
        });
    }
}
