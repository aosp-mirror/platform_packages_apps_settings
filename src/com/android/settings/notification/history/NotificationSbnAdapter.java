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

import static android.app.Notification.COLOR_DEFAULT;
import static android.content.pm.PackageManager.MATCH_ANY_USER;
import static android.content.pm.PackageManager.NameNotFoundException;
import static android.os.UserHandle.USER_ALL;
import static android.provider.Settings.EXTRA_APP_PACKAGE;
import static android.provider.Settings.EXTRA_CHANNEL_ID;
import static android.provider.Settings.EXTRA_CONVERSATION_ID;

import android.annotation.ColorInt;
import android.annotation.UserIdInt;
import android.app.ActivityManager;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.logging.UiEventLogger;
import com.android.internal.util.ContrastColorUtil;
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
    private @ColorInt int mBackgroundColor;
    private boolean mInNightMode;
    private @UserIdInt int mCurrentUser;
    private List<Integer> mEnabledProfiles = new ArrayList<>();
    private boolean mIsSnoozed;
    private UiEventLogger mUiEventLogger;

    public NotificationSbnAdapter(Context context, PackageManager pm, UserManager um,
            boolean isSnoozed, UiEventLogger uiEventLogger) {
        mContext = context;
        mPm = pm;
        mUserBadgeCache = new HashMap<>();
        mValues = new ArrayList<>();
        mBackgroundColor = mContext.getColor(
                com.android.internal.R.color.notification_material_background_color);
        Configuration currentConfig = mContext.getResources().getConfiguration();
        mInNightMode = (currentConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        mCurrentUser = ActivityManager.getCurrentUser();
        int[] enabledUsers = um.getEnabledProfileIds(mCurrentUser);
        for (int id : enabledUsers) {
            if (!um.isQuietModeEnabled(UserHandle.of(id))) {
                mEnabledProfiles.add(id);
            }
        }
        setHasStableIds(true);
        // If true, this is the panel for snoozed notifs, otherwise the one for dismissed notifs.
        mIsSnoozed = isSnoozed;
        mUiEventLogger = uiEventLogger;
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
            holder.setPackageLabel(loadPackageLabel(sbn.getPackageName()).toString());
            holder.setTitle(getTitleString(sbn.getNotification()));
            holder.setSummary(getTextString(mContext, sbn.getNotification()));
            holder.setPostedTime(sbn.getPostTime());
            holder.setDividerVisible(position < (mValues.size() -1));
            int userId = normalizeUserId(sbn);
            if (!mUserBadgeCache.containsKey(userId)) {
                Drawable profile = mContext.getPackageManager().getUserBadgeForDensity(
                        UserHandle.of(userId), -1);
                mUserBadgeCache.put(userId, profile);
            }
            holder.setProfileBadge(mUserBadgeCache.get(userId));
            holder.addOnClick(position, sbn.getPackageName(), sbn.getUid(), sbn.getUserId(),
                    sbn.getNotification().contentIntent, sbn.getInstanceId(), mIsSnoozed,
                    mUiEventLogger);
            holder.itemView.setOnLongClickListener(v -> {
                Intent intent =  new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(EXTRA_APP_PACKAGE, sbn.getPackageName())
                        .putExtra(EXTRA_CHANNEL_ID, sbn.getNotification().getChannelId())
                        .putExtra(EXTRA_CONVERSATION_ID, sbn.getNotification().getShortcutId());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                holder.itemView.getContext().startActivityAsUser(intent, UserHandle.of(userId));
                return true;
            });
        } else {
            Slog.w(TAG, "null entry in list at position " + position);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public void onRebuildComplete(List<StatusBarNotification> notifications) {
        for (int i = notifications.size() - 1; i >= 0; i--) {
            StatusBarNotification sbn = notifications.get(i);
            if (!shouldShowSbn(sbn)) {
                notifications.remove(i);
            }
        }
        mValues = notifications;
        notifyDataSetChanged();
    }

    public void addSbn(StatusBarNotification sbn) {
        if (!shouldShowSbn(sbn)) {
            return;
        }
        mValues.add(0, sbn);
        notifyDataSetChanged();
    }

    private boolean shouldShowSbn(StatusBarNotification sbn) {
        // summaries are low content; don't bother showing them
        if (sbn.isGroup() && sbn.getNotification().isGroupSummary()) {
            return false;
        }
        // also don't show profile notifications if the profile is currently disabled
        if (!mEnabledProfiles.contains(normalizeUserId(sbn))) {
            return false;
        }
        return true;
    }

    private @NonNull CharSequence loadPackageLabel(String pkg) {
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
                sbn.getPackageContext(mContext), normalizeUserId(sbn));
        if (draw == null) {
            return null;
        }
        draw.mutate();
        draw.setColorFilter(getContrastedColor(sbn.getNotification()), PorterDuff.Mode.SRC_ATOP);
        return draw;
    }

    private int normalizeUserId(StatusBarNotification sbn) {
        int userId = sbn.getUserId();
        if (userId == USER_ALL) {
            userId = mCurrentUser;
        }
        return userId;
    }

    private int getContrastedColor(Notification n) {
        int rawColor = n.color;
        if (rawColor != COLOR_DEFAULT) {
            rawColor |= 0xFF000000; // no alpha for custom colors
        }
        return ContrastColorUtil.resolveContrastColor(
                mContext, rawColor, mBackgroundColor, mInNightMode);
    }
}
