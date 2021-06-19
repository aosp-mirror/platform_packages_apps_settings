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
        mTitle = itemView.findViewById(R.id.title);
        mSummary = itemView.findViewById(R.id.text);
    }

    void setSummary(CharSequence summary) {
        mSummary.setText(summary);
        mSummary.setVisibility(summary != null ? View.VISIBLE : View.GONE);
    }

    void setTitle(CharSequence title) {
        mTitle.setText(title);
        mTitle.setVisibility(title != null ? View.VISIBLE : View.INVISIBLE);
    }

    void setPostedTime(long postedTime) {
        mTime.setTime(postedTime);
    }
}
