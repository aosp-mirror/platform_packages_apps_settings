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

package com.android.settings.notification;

import static android.app.NotificationManager.IMPORTANCE_UNSPECIFIED;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.INotificationManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationStation extends SettingsPreferenceFragment {
    private static final String TAG = NotificationStation.class.getSimpleName();

    private static final boolean DEBUG = false;
    private static final boolean DUMP_EXTRAS = true;
    private static final boolean DUMP_PARCEL = true;
    private Handler mHandler;

    private static class HistoricalNotificationInfo {
        public String key;
        public String channel;
        public String pkg;
        public Drawable pkgicon;
        public CharSequence pkgname;
        public Drawable icon;
        public CharSequence title;
        public int priority;
        public int user;
        public long timestamp;
        public boolean active;
        public CharSequence extra;
    }

    private PackageManager mPm;
    private INotificationManager mNoMan;
    private RankingMap mRanking;

    private Runnable mRefreshListRunnable = new Runnable() {
        @Override
        public void run() {
            refreshList();
        }
    };

    private final NotificationListenerService mListener = new NotificationListenerService() {
        @Override
        public void onNotificationPosted(StatusBarNotification sbn, RankingMap ranking) {
            logd("onNotificationPosted: %s, with update for %d", sbn.getNotification(),
                    ranking == null ? 0 : ranking.getOrderedKeys().length);
            mRanking = ranking;
            scheduleRefreshList();
        }

        @Override
        public void onNotificationRemoved(StatusBarNotification notification, RankingMap ranking) {
            logd("onNotificationRankingUpdate with update for %d",
                    ranking == null ? 0 : ranking.getOrderedKeys().length);
            mRanking = ranking;
            scheduleRefreshList();
        }

        @Override
        public void onNotificationRankingUpdate(RankingMap ranking) {
            logd("onNotificationRankingUpdate with update for %d",
                    ranking == null ? 0 : ranking.getOrderedKeys().length);
            mRanking = ranking;
            scheduleRefreshList();
        }

        @Override
        public void onListenerConnected() {
            mRanking = getCurrentRanking();
            logd("onListenerConnected with update for %d",
                    mRanking == null ? 0 : mRanking.getOrderedKeys().length);
            scheduleRefreshList();
        }
    };

    private void scheduleRefreshList() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mRefreshListRunnable);
            mHandler.postDelayed(mRefreshListRunnable, 100);
        }
    }

    private Context mContext;

    private final Comparator<HistoricalNotificationInfo> mNotificationSorter
            = new Comparator<HistoricalNotificationInfo>() {
                @Override
                public int compare(HistoricalNotificationInfo lhs,
                                   HistoricalNotificationInfo rhs) {
                    return Long.compare(rhs.timestamp, lhs.timestamp);
                }
            };

    @Override
    public void onAttach(Activity activity) {
        logd("onAttach(%s)", activity.getClass().getSimpleName());
        super.onAttach(activity);
        mHandler = new Handler(activity.getMainLooper());
        mContext = activity;
        mPm = mContext.getPackageManager();
        mNoMan = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
    }

    @Override
    public void onDetach() {
        logd("onDetach()");
        mHandler.removeCallbacks(mRefreshListRunnable);
        mHandler = null;
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
        refreshList();
    }

    private void refreshList() {
        List<HistoricalNotificationInfo> infos = loadNotifications();
        if (infos != null) {
            final int N = infos.size();
            logd("adding %d infos", N);
            Collections.sort(infos, mNotificationSorter);
            if (getPreferenceScreen() == null) {
                setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getContext()));
            }
            getPreferenceScreen().removeAll();
            for (int i = 0; i < N; i++) {
                getPreferenceScreen().addPreference(
                        new HistoricalNotificationPreference(getPrefContext(), infos.get(i)));
            }
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
            if (TextUtils.isEmpty(title)) {
                title = n.extras.getCharSequence(Notification.EXTRA_TEXT);
            }
        }
        if (TextUtils.isEmpty(title) && !TextUtils.isEmpty(n.tickerText)) {
            title = n.tickerText;
        }
        return String.valueOf(title);
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

    private List<HistoricalNotificationInfo> loadNotifications() {
        final int currentUserId = ActivityManager.getCurrentUser();
        try {
            StatusBarNotification[] active = mNoMan.getActiveNotifications(
                    mContext.getPackageName());
            StatusBarNotification[] dismissed = mNoMan.getHistoricalNotifications(
                    mContext.getPackageName(), 50);

            List<HistoricalNotificationInfo> list
                    = new ArrayList<HistoricalNotificationInfo>(active.length + dismissed.length);

            for (StatusBarNotification[] resultset
                    : new StatusBarNotification[][] { active, dismissed }) {
                for (StatusBarNotification sbn : resultset) {
                    if (sbn.getUserId() != UserHandle.USER_ALL & sbn.getUserId() != currentUserId) {
                        continue;
                    }

                    final Notification n = sbn.getNotification();
                    final HistoricalNotificationInfo info = new HistoricalNotificationInfo();
                    info.pkg = sbn.getPackageName();
                    info.user = sbn.getUserId();
                    info.icon = loadIconDrawable(info.pkg, info.user, n.icon);
                    info.pkgicon = loadPackageIconDrawable(info.pkg, info.user);
                    info.pkgname = loadPackageName(info.pkg);
                    info.title = getTitleString(n);
                    if (TextUtils.isEmpty(info.title)) {
                        info.title = getString(R.string.notification_log_no_title);
                    }
                    info.timestamp = sbn.getPostTime();
                    info.priority = n.priority;
                    info.channel = n.getChannelId();
                    info.key = sbn.getKey();

                    info.active = (resultset == active);

                    info.extra = generateExtraText(sbn, info);

                    logd("   [%d] %s: %s", info.timestamp, info.pkg, info.title);
                    list.add(info);
                }
            }

            return list;
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot load Notifications: ", e);
        }
        return null;
    }

    private CharSequence generateExtraText(StatusBarNotification sbn,
                                           HistoricalNotificationInfo info) {
        final Ranking rank = new Ranking();

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
                .append(bold("channelId"))
                .append(delim)
                .append(String.valueOf(n.getChannelId()));
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
        if (info.active) {
            // mRanking only applies to active notifications
            if (mRanking != null && mRanking.getRanking(sbn.getKey(), rank)) {
                if (rank.getLastAudiblyAlertedMillis() > 0) {
                    sb.append("\n")
                            .append(bold(getString(R.string.notification_log_details_alerted)));
                }
            }
        }
        try {
            NotificationChannel channel = mNoMan.getNotificationChannelForPackage(
                    sbn.getPackageName(), sbn.getUid(), n.getChannelId(), false);
            sb.append("\n")
                    .append(bold(getString(R.string.notification_log_details_sound)))
                    .append(delim);
            if (channel == null || channel.getImportance() == IMPORTANCE_UNSPECIFIED) {

                if (0 != (n.defaults & Notification.DEFAULT_SOUND)) {
                    sb.append(getString(R.string.notification_log_details_default));
                } else if (n.sound != null) {
                    sb.append(n.sound.toString());
                } else {
                    sb.append(getString(R.string.notification_log_details_none));
                }
            } else {
                sb.append(String.valueOf(channel.getSound()));
            }
            sb.append("\n")
                    .append(bold(getString(R.string.notification_log_details_vibrate)))
                    .append(delim);
            if (channel == null || channel.getImportance() == IMPORTANCE_UNSPECIFIED) {
                if (0 != (n.defaults & Notification.DEFAULT_VIBRATE)) {
                    sb.append(getString(R.string.notification_log_details_default));
                } else if (n.vibrate != null) {
                    sb.append(getString(R.string.notification_log_details_vibrate_pattern));
                } else {
                    sb.append(getString(R.string.notification_log_details_none));
                }
            } else {
                if (channel.getVibrationPattern() != null) {
                    sb.append(getString(R.string.notification_log_details_vibrate_pattern));
                } else {
                    sb.append(getString(R.string.notification_log_details_none));
                }
            }
        } catch (RemoteException e) {
            Log.d(TAG, "cannot read channel info", e);
        }
        sb.append("\n")
                .append(bold(getString(R.string.notification_log_details_visibility)))
                .append(delim)
                .append(Notification.visibilityToString(n.visibility));
        if (n.publicVersion != null) {
            sb.append("\n")
                    .append(bold(getString(
                            R.string.notification_log_details_public_version)))
                    .append(delim)
                    .append(getTitleString(n.publicVersion));
        }
        sb.append("\n")
                .append(bold(getString(R.string.notification_log_details_priority)))
                .append(delim)
                .append(Notification.priorityToString(n.priority));
        if (info.active) {
            // mRanking only applies to active notifications
            if (mRanking != null && mRanking.getRanking(sbn.getKey(), rank)) {
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

    private Resources getResourcesForUserPackage(String pkg, int userId) {
        Resources r = null;

        if (pkg != null) {
            try {
                if (userId == UserHandle.USER_ALL) {
                    userId = UserHandle.USER_SYSTEM;
                }
                r = mPm.getResourcesForApplicationAsUser(pkg, userId);
            } catch (PackageManager.NameNotFoundException ex) {
                Log.e(TAG, "Icon package not found: " + pkg, ex);
                return null;
            }
        } else {
            r = mContext.getResources();
        }
        return r;
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

    private Drawable loadIconDrawable(String pkg, int userId, int resId) {
        Resources r = getResourcesForUserPackage(pkg, userId);

        if (resId == 0) {
            return null;
        }

        try {
            return r.getDrawable(resId, null);
        } catch (RuntimeException e) {
            Log.w(TAG, "Icon not found in "
                    + (pkg != null ? resId : "<system>")
                    + ": " + Integer.toHexString(resId), e);
        }

        return null;
    }

    private static class HistoricalNotificationPreference extends Preference {
        private final HistoricalNotificationInfo mInfo;
        private static long sLastExpandedTimestamp; // quick hack to keep things from collapsing

        public HistoricalNotificationPreference(Context context, HistoricalNotificationInfo info) {
            super(context);
            setLayoutResource(R.layout.notification_log_row);
            mInfo = info;
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder row) {
            super.onBindViewHolder(row);

            if (mInfo.icon != null) {
                ((ImageView) row.findViewById(R.id.icon)).setImageDrawable(mInfo.icon);
            }
            if (mInfo.pkgicon != null) {
                ((ImageView) row.findViewById(R.id.pkgicon)).setImageDrawable(mInfo.pkgicon);
            }

            ((DateTimeView) row.findViewById(R.id.timestamp)).setTime(mInfo.timestamp);
            ((TextView) row.findViewById(R.id.title)).setText(mInfo.title);
            ((TextView) row.findViewById(R.id.pkgname)).setText(mInfo.pkgname);

            final TextView extra = (TextView) row.findViewById(R.id.extra);
            extra.setText(mInfo.extra);
            extra.setVisibility(mInfo.timestamp == sLastExpandedTimestamp
                    ? View.VISIBLE : View.GONE);

            row.itemView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            extra.setVisibility(extra.getVisibility() == View.VISIBLE
                                    ? View.GONE : View.VISIBLE);
                            sLastExpandedTimestamp = mInfo.timestamp;
                        }
                    });

            row.itemView.setAlpha(mInfo.active ? 1.0f : 0.5f);
        }

        @Override
        public void performClick() {
//            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
//                    Uri.fromParts("package", mInfo.pkg, null));
//            intent.setComponent(intent.resolveActivity(getContext().getPackageManager()));
//            getContext().startActivity(intent);
        }
    }
}
