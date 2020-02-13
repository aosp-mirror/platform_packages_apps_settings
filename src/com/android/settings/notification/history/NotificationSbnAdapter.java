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

import static android.content.pm.PackageManager.*;

import android.app.Notification;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationSbnAdapter extends
        RecyclerView.Adapter<NotificationSbnViewHolder> {

    private static final String TAG = "SbnAdapter";
    private List<StatusBarNotification> mValues;
    private Map<Integer, Drawable> mUserBadgeCache;
    private final Context mContext;
    private PackageManager mPm;

    public NotificationSbnAdapter(Context context, PackageManager pm) {
        mContext = context;
        mPm = pm;
        mUserBadgeCache = new HashMap<>();
        mValues = new ArrayList<>();
        setHasStableIds(true);
    }

    @Override
    public NotificationSbnViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification_sbn_log_row, parent, false);
        return new NotificationSbnViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final @NonNull NotificationSbnViewHolder holder,
            int position) {
        final StatusBarNotification sbn = mValues.get(position);
        if (sbn != null) {
            holder.setIcon(loadIcon(sbn));
            holder.setPackageName(loadPackageName(sbn.getPackageName()).toString());
            holder.setTitle(getTitleString(sbn.getNotification()));
            holder.setSummary(getTextString(mContext, sbn.getNotification()));
            holder.setPostedTime(sbn.getPostTime());
            if (!mUserBadgeCache.containsKey(sbn.getUserId())) {
                Drawable profile = mContext.getPackageManager().getUserBadgeForDensity(
                        UserHandle.of(sbn.getUserId()), -1);
                mUserBadgeCache.put(sbn.getUserId(), profile);
            }
            holder.setProfileBadge(mUserBadgeCache.get(sbn.getUserId()));
        } else {
            Slog.w(TAG, "null entry in list at position " + position);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public void onRebuildComplete(List<StatusBarNotification> notifications) {
        // summaries are low content; don't bother showing them
        for (int i = notifications.size() - 1; i >= 0; i--) {
            StatusBarNotification sbn = notifications.get(i);
            if (sbn.isGroup() && sbn.getNotification().isGroupSummary()) {
                notifications.remove(i);
            }
        }
        mValues = notifications;
        notifyDataSetChanged();
    }

    private @NonNull CharSequence loadPackageName(String pkg) {
        try {
            ApplicationInfo info = mPm.getApplicationInfo(pkg,
                    MATCH_ANY_USER);
            if (info != null) return mPm.getApplicationLabel(info);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Cannot load package name", e);
        }
        return pkg;
    }

    private static String getTitleString(Notification n) {
        CharSequence title = null;
        if (n.extras != null) {
            title = n.extras.getCharSequence(Notification.EXTRA_TITLE);
        }
        return title == null? null : String.valueOf(title);
    }

    /**
     * Returns the appropriate substring for this notification based on the style of notification.
     */
    private static String getTextString(Context appContext, Notification n) {
        CharSequence text = null;
        if (n.extras != null) {
            text = n.extras.getCharSequence(Notification.EXTRA_TEXT);

            Notification.Builder nb = Notification.Builder.recoverBuilder(appContext, n);

            if (nb.getStyle() instanceof Notification.BigTextStyle) {
                text = ((Notification.BigTextStyle) nb.getStyle()).getBigText();
            } else if (nb.getStyle() instanceof Notification.MessagingStyle) {
                Notification.MessagingStyle ms = (Notification.MessagingStyle) nb.getStyle();
                final List<Notification.MessagingStyle.Message> messages = ms.getMessages();
                if (messages != null && messages.size() > 0) {
                    text = messages.get(messages.size() - 1).getText();
                }
            }

            if (TextUtils.isEmpty(text)) {
                text = n.extras.getCharSequence(Notification.EXTRA_TEXT);
            }
        }
        return text == null ? null : String.valueOf(text);
    }

    private Drawable loadIcon(StatusBarNotification sbn) {
        Drawable draw = sbn.getNotification().getSmallIcon().loadDrawableAsUser(
                sbn.getPackageContext(mContext), sbn.getUserId());
        if (draw == null) {
            return null;
        }
        draw.mutate();
        draw.setColorFilter(sbn.getNotification().color, PorterDuff.Mode.SRC_ATOP);
        return draw;
    }
}
