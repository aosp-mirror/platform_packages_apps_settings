/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.INotificationManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.NotificationListenerService.Ranking;
import android.service.notification.NotificationListenerService.RankingMap;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DateTimeView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class NotificationStation extends SettingsPreferenceFragment {
    private static final String TAG = NotificationStation.class.getSimpleName();

    private static final boolean DEBUG = false;
    private static final boolean DUMP_EXTRAS = true;
    private static final boolean DUMP_PARCEL = true;

    private static class HistoricalNotificationInfo {
        public String key;
        public NotificationChannel channel;
        // Historical notifications don't have Ranking information. for most fields that's ok
        // but we need channel id to launch settings.
        public String channelId;
        public String pkg;
        public Drawable pkgicon;
        public CharSequence pkgname;
        public Drawable icon;
        public boolean badged;
        public CharSequence title;
        public CharSequence text;
        public int priority;
        public int user;
        public long timestamp;
        public boolean active;
        public CharSequence notificationExtra;
        public CharSequence rankingExtra;
        public boolean alerted;
        public boolean visuallyInterruptive;

        public void updateFrom(HistoricalNotificationInfo updatedInfo) {
            this.channel = updatedInfo.channel;
            this.icon = updatedInfo.icon;
            this.title = updatedInfo.title;
            this.text = updatedInfo.text;
            this.priority = updatedInfo.priority;
            this.timestamp = updatedInfo.timestamp;
            this.active = updatedInfo.active;
            this.alerted = updatedInfo.alerted;
            this.visuallyInterruptive = updatedInfo.visuallyInterruptive;
            this.notificationExtra = updatedInfo.notificationExtra;
            this.rankingExtra = updatedInfo.rankingExtra;
        }
    }

    private PackageManager mPm;
    private INotificationManager mNoMan;
    private RankingMap mRanking;
    private LinkedList<HistoricalNotificationInfo> mNotificationInfos;

    private final NotificationListenerService mListener = new NotificationListenerService() {
        @Override
        public void onNotificationPosted(StatusBarNotification sbn, RankingMap ranking) {
            logd("onNotificationPosted: %s, with update for %d", sbn.getNotification(),
                    ranking == null ? 0 : ranking.getOrderedKeys().length);
            mRanking = ranking;
            if (sbn.getNotification().isGroupSummary()) {
                return;
            }
            addOrUpdateNotification(sbn);
        }

        @Override
        public void onNotificationRemoved(StatusBarNotification sbn, RankingMap ranking) {
            logd("onNotificationRankingUpdate with update for %d",
                    ranking == null ? 0 : ranking.getOrderedKeys().length);
            mRanking = ranking;
            if (sbn.getNotification().isGroupSummary()) {
                return;
            }
            markNotificationAsDismissed(sbn);
        }

        @Override
        public void onNotificationRankingUpdate(RankingMap ranking) {
            logd("onNotificationRankingUpdate with update for %d",
                    ranking == null ? 0 : ranking.getOrderedKeys().length);
            mRanking = ranking;
            updateNotificationsFromRanking();
        }

        @Override
        public void onListenerConnected() {
            mRanking = getCurrentRanking();
            logd("onListenerConnected with update for %d",
                    mRanking == null ? 0 : mRanking.getOrderedKeys().length);
            populateNotifications();
        }
    };

    private Context mContext;

    private final Comparator<HistoricalNotificationInfo> mNotificationSorter
            = (lhs, rhs) -> Long.compare(rhs.timestamp, lhs.timestamp);

    @Override
    public void onAttach(Activity activity) {
        logd("onAttach(%s)", activity.getClass().getSimpleName());
        super.onAttach(activity);
        mContext = activity;
        mPm = mContext.getPackageManager();
        mNoMan = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        mNotificationInfos = new LinkedList<>();
    }

    @Override
    public void onDetach() {
        logd("onDetach()");
        super.onDetach();
    }

    @Override
    public void onPause() {
        try {
            mListener.unregisterAsSystemService();
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot unregister listener", e);
        }
        super.onPause();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_STATION;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        logd("onActivityCreated(%s)", savedInstanceState);
        super.onActivityCreated(savedInstanceState);

        RecyclerView listView = getListView();
        Utils.forceCustomPadding(listView, false /* non additive padding */);
    }

    @Override
    public void onResume() {
        logd("onResume()");
        super.onResume();
        try {
            mListener.registerAsSystemService(mContext, new ComponentName(mContext.getPackageName(),
                    this.getClass().getCanonicalName()), ActivityManager.getCurrentUser());
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot register listener", e);
        }
    }

    /**
     * Adds all current and historical notifications when the NLS connects.
     */
    private void populateNotifications() {
        loadNotifications();
        final int N = mNotificationInfos.size();
        logd("adding %d infos", N);
        if (getPreferenceScreen() == null) {
            setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getContext()));
        }
        getPreferenceScreen().removeAll();
        for (int i = 0; i < N; i++) {
            getPreferenceScreen().addPreference(new HistoricalNotificationPreference(
                    getPrefContext(), mNotificationInfos.get(i), i));
        }
    }

    /**
     * Finds and dims the given notification in the preferences list.
     */
    private void markNotificationAsDismissed(StatusBarNotification sbn) {
        final int N = mNotificationInfos.size();
        for (int i = 0; i < N; i++) {
            final HistoricalNotificationInfo info = mNotificationInfos.get(i);
            if (TextUtils.equals(info.key, sbn.getKey())) {
                info.active = false;
                ((HistoricalNotificationPreference) getPreferenceScreen().findPreference(
                        sbn.getKey())).updatePreference(info);
               break;
            }
        }
    }

    /**
     * Either updates a notification with its latest information or (if it's something the user
     * would consider a new notification) adds a new entry at the start of the list.
     */
    private void addOrUpdateNotification(StatusBarNotification sbn) {
        HistoricalNotificationInfo newInfo = createFromSbn(sbn, true);
        boolean needsAdd = true;
        final int N = mNotificationInfos.size();
        for (int i = 0; i < N; i++) {
            final HistoricalNotificationInfo info = mNotificationInfos.get(i);
            if (TextUtils.equals(info.key, sbn.getKey()) && info.active
                    && !newInfo.alerted && !newInfo.visuallyInterruptive) {
                info.updateFrom(newInfo);

                ((HistoricalNotificationPreference) getPreferenceScreen().findPreference(
                        sbn.getKey())).updatePreference(info);
                needsAdd = false;
                break;
            }
        }
        if (needsAdd) {
            mNotificationInfos.addFirst(newInfo);
            getPreferenceScreen().addPreference(new HistoricalNotificationPreference(
                    getPrefContext(), mNotificationInfos.peekFirst(),
                    -1 * mNotificationInfos.size()));
        }
    }

    /**
     * Updates all notifications in the list based on new information in the ranking.
     */
    private void updateNotificationsFromRanking() {
        Ranking rank = new Ranking();
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            final HistoricalNotificationPreference p =
                    (HistoricalNotificationPreference) getPreferenceScreen().getPreference(i);
            final HistoricalNotificationInfo info = mNotificationInfos.get(i);
            mRanking.getRanking(p.getKey(), rank);

            updateFromRanking(info);
            ((HistoricalNotificationPreference) getPreferenceScreen().findPreference(
                    info.key)).updatePreference(info);
        }
    }

    private static void logd(String msg, Object... args) {
        if (DEBUG) {
            Log.d(TAG, args == null || args.length == 0 ? msg : String.format(msg, args));
        }
    }

    private static CharSequence bold(CharSequence cs) {
        if (cs.length() == 0) return cs;
        SpannableString ss = new SpannableString(cs);
        ss.setSpan(new StyleSpan(Typeface.BOLD), 0, cs.length(), 0);
        return ss;
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

    private Drawable loadIcon(HistoricalNotificationInfo info, StatusBarNotification sbn) {
        Drawable draw = sbn.getNotification().getSmallIcon().loadDrawableAsUser(
                sbn.getPackageContext(mContext), info.user);
        if (draw == null) {
            return null;
        }
        draw.mutate();
        draw.setColorFilter(sbn.getNotification().color, PorterDuff.Mode.SRC_ATOP);
        return draw;
    }

    private static String formatPendingIntent(PendingIntent pi) {
        final StringBuilder sb = new StringBuilder();
        final IntentSender is = pi.getIntentSender();
        sb.append("Intent(pkg=").append(is.getCreatorPackage());
        try {
            final boolean isActivity =
                    ActivityManager.getService().isIntentSenderAnActivity(is.getTarget());
            if (isActivity) sb.append(" (activity)");
        } catch (RemoteException ex) {}
        sb.append(")");
        return sb.toString();
    }

    /**
     * Reads all current and past notifications (up to the system limit, since the device was
     * booted), stores the data we need to present them, and sorts them chronologically for display.
     */
    private void loadNotifications() {
        try {
            StatusBarNotification[] active = mNoMan.getActiveNotificationsWithAttribution(
                    mContext.getPackageName(), mContext.getAttributionTag());
            StatusBarNotification[] dismissed = mNoMan.getHistoricalNotificationsWithAttribution(
                    mContext.getPackageName(), mContext.getAttributionTag(), 50, false);

            List<HistoricalNotificationInfo> list
                    = new ArrayList<>(active.length + dismissed.length);

            for (StatusBarNotification[] resultSet
                    : new StatusBarNotification[][] { active, dismissed }) {
                for (StatusBarNotification sbn : resultSet) {
                    if (sbn.getNotification().isGroupSummary()) {
                        continue;
                    }
                    final HistoricalNotificationInfo info = createFromSbn(sbn, resultSet == active);
                    logd("   [%d] %s: %s", info.timestamp, info.pkg, info.title);
                    list.add(info);
                }
            }

            // notifications are given to us in the same order as the shade; sorted by inferred
            // priority. Resort chronologically for our display.
            list.sort(mNotificationSorter);
            mNotificationInfos = new LinkedList<>(list);

        } catch (RemoteException e) {
            Log.e(TAG, "Cannot load Notifications: ", e);
        }
    }

    private HistoricalNotificationInfo createFromSbn(StatusBarNotification sbn, boolean active) {
        final Notification n = sbn.getNotification();
        final HistoricalNotificationInfo info = new HistoricalNotificationInfo();
        info.pkg = sbn.getPackageName();
        info.user = sbn.getUserId() == UserHandle.USER_ALL
                ? UserHandle.USER_SYSTEM : sbn.getUserId();
        info.badged = info.user != ActivityManager.getCurrentUser();
        info.icon = loadIcon(info, sbn);
        if (info.icon == null) {
            info.icon = loadPackageIconDrawable(info.pkg, info.user);
        }
        info.pkgname = loadPackageName(info.pkg);
        info.title = getTitleString(n);
        info.text = getTextString(sbn.getPackageContext(mContext), n);
        info.timestamp = sbn.getPostTime();
        info.priority = n.priority;
        info.key = sbn.getKey();
        info.channelId = sbn.getNotification().getChannelId();

        info.active = active;
        info.notificationExtra = generateExtraText(sbn, info);

        updateFromRanking(info);

        return info;
    }

    private void updateFromRanking(HistoricalNotificationInfo info) {
        Ranking rank = new Ranking();
        if (mRanking == null) {
            return;
        }
        mRanking.getRanking(info.key, rank);
        info.alerted = rank.getLastAudiblyAlertedMillis() > 0;
        info.visuallyInterruptive = rank.visuallyInterruptive();
        info.channel = rank.getChannel();
        info.rankingExtra = generateRankingExtraText(info);
    }

    /**
     * Generates a string of debug information for this notification based on the RankingMap
     */
    private CharSequence generateRankingExtraText(HistoricalNotificationInfo info) {
        final SpannableStringBuilder sb = new SpannableStringBuilder();
        final String delim = getString(R.string.notification_log_details_delimiter);

        Ranking rank = new Ranking();
        if (mRanking != null && mRanking.getRanking(info.key, rank)) {
            if (info.active && info.alerted) {
                sb.append("\n")
                        .append(bold(getString(R.string.notification_log_details_alerted)));
            }
            sb.append("\n")
                    .append(bold(getString(R.string.notification_log_channel)))
                    .append(delim)
                    .append(info.channel.toString());
            sb.append("\n")
                    .append(bold("getShortcutInfo"))
                    .append(delim)
                    .append(String.valueOf(rank.getShortcutInfo()));
            sb.append("\n")
                    .append(bold("isConversation"))
                    .append(delim)
                    .append(rank.isConversation() ? "true" : "false");
            sb.append("\n")
                    .append(bold("isBubble"))
                    .append(delim)
                    .append(rank.isBubble() ? "true" : "false");
            if (info.active) {
                sb.append("\n")
                        .append(bold(getString(
                                R.string.notification_log_details_importance)))
                        .append(delim)
                        .append(Ranking.importanceToString(rank.getImportance()));
                if (rank.getImportanceExplanation() != null) {
                    sb.append("\n")
                            .append(bold(getString(
                                    R.string.notification_log_details_explanation)))
                            .append(delim)
                            .append(rank.getImportanceExplanation());
                }
                sb.append("\n")
                        .append(bold(getString(
                                R.string.notification_log_details_badge)))
                        .append(delim)
                        .append(Boolean.toString(rank.canShowBadge()));
            }
        } else {
            if (mRanking == null) {
                sb.append("\n")
                        .append(bold(getString(
                                R.string.notification_log_details_ranking_null)));
            } else {
                sb.append("\n")
                        .append(bold(getString(
                                R.string.notification_log_details_ranking_none)));
            }
        }

        return sb;
    }

    /**
     * Generates a string of debug information for this notification
     */
    private CharSequence generateExtraText(StatusBarNotification sbn,
                                           HistoricalNotificationInfo info) {
        final Notification n = sbn.getNotification();
        final SpannableStringBuilder sb = new SpannableStringBuilder();
        final String delim = getString(R.string.notification_log_details_delimiter);
        sb.append(bold(getString(R.string.notification_log_details_package)))
                .append(delim)
                .append(info.pkg)
                .append("\n")
                .append(bold(getString(R.string.notification_log_details_key)))
                .append(delim)
                .append(sbn.getKey());
        sb.append("\n")
                .append(bold(getString(R.string.notification_log_details_icon)))
                .append(delim)
                .append(String.valueOf(n.getSmallIcon()));
        sb.append("\n")
                .append(bold("postTime"))
                .append(delim)
                .append(String.valueOf(sbn.getPostTime()));
        if (n.getTimeoutAfter() != 0) {
            sb.append("\n")
                    .append(bold("timeoutAfter"))
                    .append(delim)
                    .append(String.valueOf(n.getTimeoutAfter()));
        }
        if (sbn.isGroup()) {
            sb.append("\n")
                    .append(bold(getString(R.string.notification_log_details_group)))
                    .append(delim)
                    .append(String.valueOf(sbn.getGroupKey()));
            if (n.isGroupSummary()) {
                sb.append(bold(
                        getString(R.string.notification_log_details_group_summary)));
            }
        }
        if (n.publicVersion != null) {
            sb.append("\n")
                    .append(bold(getString(
                            R.string.notification_log_details_public_version)))
                    .append(delim)
                    .append(getTitleString(n.publicVersion));
        }

        if (n.contentIntent != null) {
            sb.append("\n")
                    .append(bold(getString(
                            R.string.notification_log_details_content_intent)))
                    .append(delim)
                    .append(formatPendingIntent(n.contentIntent));
        }
        if (n.deleteIntent != null) {
            sb.append("\n")
                    .append(bold(getString(
                            R.string.notification_log_details_delete_intent)))
                    .append(delim)
                    .append(formatPendingIntent(n.deleteIntent));
        }
        if (n.fullScreenIntent != null) {
            sb.append("\n")
                    .append(bold(getString(
                            R.string.notification_log_details_full_screen_intent)))
                    .append(delim)
                    .append(formatPendingIntent(n.fullScreenIntent));
        }
        if (n.actions != null && n.actions.length > 0) {
            sb.append("\n")
                    .append(bold(getString(R.string.notification_log_details_actions)));
            for (int ai=0; ai<n.actions.length; ai++) {
                final Notification.Action action = n.actions[ai];
                sb.append("\n  ").append(String.valueOf(ai)).append(' ')
                        .append(bold(getString(
                                R.string.notification_log_details_title)))
                        .append(delim)
                        .append(action.title);
                if (action.actionIntent != null) {
                    sb.append("\n    ")
                            .append(bold(getString(
                                    R.string.notification_log_details_content_intent)))
                            .append(delim)
                            .append(formatPendingIntent(action.actionIntent));
                }
                if (action.getRemoteInputs() != null) {
                    sb.append("\n    ")
                            .append(bold(getString(
                                    R.string.notification_log_details_remoteinput)))
                            .append(delim)
                            .append(String.valueOf(action.getRemoteInputs().length));
                }
            }
        }
        if (n.contentView != null) {
            sb.append("\n")
                    .append(bold(getString(
                            R.string.notification_log_details_content_view)))
                    .append(delim)
                    .append(n.contentView.toString());
        }
        if (n.getBubbleMetadata() != null) {
            sb.append("\n")
                    .append(bold("bubbleMetadata"))
                    .append(delim)
                    .append(String.valueOf(n.getBubbleMetadata()));
        }
        if (n.getShortcutId() != null) {
            sb.append("\n")
                    .append(bold("shortcutId"))
                    .append(delim)
                    .append(String.valueOf(n.getShortcutId()));
        }

        if (DUMP_EXTRAS) {
            if (n.extras != null && n.extras.size() > 0) {
                sb.append("\n")
                        .append(bold(getString(
                                R.string.notification_log_details_extras)));
                for (String extraKey : n.extras.keySet()) {
                    String val = String.valueOf(n.extras.get(extraKey));
                    if (val.length() > 100) val = val.substring(0, 100) + "...";
                    sb.append("\n  ").append(extraKey).append(delim).append(val);
                }
            }
        }
        if (DUMP_PARCEL) {
            final Parcel p = Parcel.obtain();
            n.writeToParcel(p, 0);
            sb.append("\n")
                    .append(bold(getString(R.string.notification_log_details_parcel)))
                    .append(delim)
                    .append(String.valueOf(p.dataPosition()))
                    .append(' ')
                    .append(bold(getString(R.string.notification_log_details_ashmem)))
                    .append(delim)
                    .append(String.valueOf(p.getBlobAshmemSize()))
                    .append("\n");
        }
        return sb;
    }

    private Drawable loadPackageIconDrawable(String pkg, int userId) {
        Drawable icon = null;
        try {
            icon = mPm.getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot get application icon", e);
        }

        return icon;
    }

    private CharSequence loadPackageName(String pkg) {
        try {
            ApplicationInfo info = mPm.getApplicationInfo(pkg,
                    PackageManager.MATCH_ANY_USER);
            if (info != null) return mPm.getApplicationLabel(info);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot load package name", e);
        }
        return pkg;
    }

    private static class HistoricalNotificationPreference extends Preference {
        private final HistoricalNotificationInfo mInfo;
        private static long sLastExpandedTimestamp; // quick hack to keep things from collapsing
        public ViewGroup mItemView; // hack to update prefs fast;
        private Context mContext;

        public HistoricalNotificationPreference(Context context, HistoricalNotificationInfo info,
                int order) {
            super(context);
            setLayoutResource(R.layout.notification_log_row);
            setOrder(order);
            setKey(info.key);
            mInfo = info;
            mContext = context;
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder row) {
            super.onBindViewHolder(row);

            mItemView = (ViewGroup) row.itemView;

            updatePreference(mInfo);

            row.findViewById(R.id.timestamp).setOnLongClickListener(v -> {
                final View extras = row.findViewById(R.id.extra);
                extras.setVisibility(extras.getVisibility() == View.VISIBLE
                        ? View.GONE : View.VISIBLE);
                sLastExpandedTimestamp = mInfo.timestamp;
                return false;
            });
        }

        public void updatePreference(HistoricalNotificationInfo info) {
            if (mItemView == null) {
                return;
            }
            if (info.icon != null) {
                ((ImageView) mItemView.findViewById(R.id.icon)).setImageDrawable(mInfo.icon);
            }
            ((TextView) mItemView.findViewById(R.id.pkgname)).setText(mInfo.pkgname);
            ((DateTimeView) mItemView.findViewById(R.id.timestamp)).setTime(info.timestamp);
            if (!TextUtils.isEmpty(info.title)) {
                ((TextView) mItemView.findViewById(R.id.title)).setText(info.title);
                mItemView.findViewById(R.id.title).setVisibility(View.VISIBLE);
            } else {
                mItemView.findViewById(R.id.title).setVisibility(View.GONE);
            }
            if (!TextUtils.isEmpty(info.text)) {
                ((TextView) mItemView.findViewById(R.id.text)).setText(info.text);
                mItemView.findViewById(R.id.text).setVisibility(View.VISIBLE);
            } else {
                mItemView.findViewById(R.id.text).setVisibility(View.GONE);
            }
            if (info.icon != null) {
                ((ImageView) mItemView.findViewById(R.id.icon)).setImageDrawable(info.icon);
            }

            ImageView profileBadge = mItemView.findViewById(R.id.profile_badge);
            Drawable profile = mContext.getPackageManager().getUserBadgeForDensity(
                    UserHandle.of(info.user), -1);
            profileBadge.setImageDrawable(profile);
            profileBadge.setVisibility(info.badged ? View.VISIBLE : View.GONE);

            ((DateTimeView) mItemView.findViewById(R.id.timestamp)).setTime(mInfo.timestamp);

            ((TextView) mItemView.findViewById(R.id.notification_extra))
                    .setText(mInfo.notificationExtra);
            ((TextView) mItemView.findViewById(R.id.ranking_extra))
                    .setText(mInfo.rankingExtra);

            mItemView.findViewById(R.id.extra).setVisibility(
                    mInfo.timestamp == sLastExpandedTimestamp ? View.VISIBLE : View.GONE);

            mItemView.setAlpha(mInfo.active ? 1.0f : 0.5f);

            mItemView.findViewById(R.id.alerted_icon).setVisibility(
                    mInfo.alerted ? View.VISIBLE : View.GONE);
        }

        @Override
        public void performClick() {
            Intent intent =  new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(EXTRA_APP_PACKAGE, mInfo.pkg)
                    .putExtra(EXTRA_CHANNEL_ID,
                            mInfo.channel != null ? mInfo.channel.getId() : mInfo.channelId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        }
    }
}
