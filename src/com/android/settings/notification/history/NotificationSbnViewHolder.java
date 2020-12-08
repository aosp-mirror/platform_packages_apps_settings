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

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;
import android.view.View;
import android.widget.DateTimeView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.logging.InstanceId;
import com.android.internal.logging.UiEventLogger;
import com.android.settings.R;

public class NotificationSbnViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "SbnViewHolder";

    private final TextView mPkgName;
    private final ImageView mIcon;
    private final DateTimeView mTime;
    private final TextView mTitle;
    private final TextView mSummary;
    private final ImageView mProfileBadge;
    private final View mDivider;

    NotificationSbnViewHolder(View itemView) {
        super(itemView);
        mPkgName = itemView.findViewById(R.id.pkgname);
        mIcon = itemView.findViewById(R.id.icon);
        mTime = itemView.findViewById(R.id.timestamp);
        mTitle = itemView.findViewById(R.id.title);
        mSummary = itemView.findViewById(R.id.text);
        mProfileBadge = itemView.findViewById(R.id.profile_badge);
        mDivider = itemView.findViewById(R.id.divider);
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

    void setPackageLabel(String pkg) {
        mPkgName.setText(pkg);
    }

    void setPostedTime(long postedTime) {
        mTime.setTime(postedTime);
    }

    void setProfileBadge(Drawable badge) {
        mProfileBadge.setImageDrawable(badge);
        mProfileBadge.setVisibility(badge != null ? View.VISIBLE : View.GONE);
    }

    void setDividerVisible(boolean visible) {
        mDivider.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    void addOnClick(int position, String pkg, int uid, int userId, PendingIntent pi,
            InstanceId instanceId,
            boolean isSnoozed, UiEventLogger uiEventLogger) {
        Intent appIntent = itemView.getContext().getPackageManager()
                .getLaunchIntentForPackage(pkg);
        boolean isPendingIntentValid = pi != null && PendingIntent.getActivity(
                itemView.getContext(), 0, pi.getIntent(), PendingIntent.FLAG_NO_CREATE) != null;
        if (isPendingIntentValid || appIntent != null) {
            itemView.setOnClickListener(v -> {
                uiEventLogger.logWithInstanceIdAndPosition(
                        isSnoozed
                                ? NotificationHistoryActivity.NotificationHistoryEvent
                                .NOTIFICATION_HISTORY_SNOOZED_ITEM_CLICK
                                : NotificationHistoryActivity.NotificationHistoryEvent
                                .NOTIFICATION_HISTORY_RECENT_ITEM_CLICK,
                        uid, pkg, instanceId, position);
                if (pi != null && isPendingIntentValid) {
                    try {
                        pi.send();
                    } catch (PendingIntent.CanceledException e) {
                        Slog.e(TAG, "Could not launch", e);
                    }
                } else if (appIntent != null) {
                    appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        itemView.getContext().startActivityAsUser(appIntent, UserHandle.of(userId));
                    } catch (ActivityNotFoundException e) {
                        Slog.e(TAG, "no launch activity", e);
                    }
                }
            });
            ViewCompat.setAccessibilityDelegate(itemView, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(View host,
                        AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    CharSequence description = host.getResources().getText(
                            R.string.notification_history_open_notification);
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat customClick =
                            new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                    AccessibilityNodeInfoCompat.ACTION_CLICK, description);
                    info.addAction(customClick);
                }
            });
        }
    }
}
