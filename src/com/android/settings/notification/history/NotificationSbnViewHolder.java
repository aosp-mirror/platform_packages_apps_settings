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

import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.text.TextUtils;
import android.view.View;
import android.widget.DateTimeView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

public class NotificationSbnViewHolder extends RecyclerView.ViewHolder {

    private final TextView mPkgName;
    private final ImageView mIcon;
    private final DateTimeView mTime;
    private final TextView mTitle;
    private final TextView mSummary;
    private final ImageView mProfileBadge;

    NotificationSbnViewHolder(View itemView) {
        super(itemView);
        mPkgName = itemView.findViewById(R.id.pkgname);
        mIcon = itemView.findViewById(R.id.icon);
        mTime = itemView.findViewById(R.id.timestamp);
        mTitle = itemView.findViewById(R.id.title);
        mSummary = itemView.findViewById(R.id.text);
        mProfileBadge = itemView.findViewById(R.id.profile_badge);
    }

    void setSummary(CharSequence summary) {
        mSummary.setVisibility(TextUtils.isEmpty(summary) ? View.GONE : View.VISIBLE);
        mSummary.setText(summary);
    }

    void setTitle(CharSequence title) {
        if (title == null) {
            return;
        }
        mTitle.setText(title);
    }

    void setIcon(Drawable icon) {
        mIcon.setImageDrawable(icon);
    }

    void setPackageName(String pkg) {
        mPkgName.setText(pkg);
    }

    void setPostedTime(long postedTime) {
        mTime.setTime(postedTime);
    }

    void setProfileBadge(Drawable badge) {
        mProfileBadge.setImageDrawable(badge);
    }
}
