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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.INotificationManager;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.DateTimeView;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
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

    private static class HistoricalNotificationInfo {
        public String pkg;
        public Drawable pkgicon;
        public CharSequence pkgname;
        public Drawable icon;
        public CharSequence title;
        public int priority;
        public int user;
        public long timestamp;
        public boolean active;
    }

    private PackageManager mPm;
    private INotificationManager mNoMan;

    private Runnable mRefreshListRunnable = new Runnable() {
        @Override
        public void run() {
            refreshList();
        }
    };

    private NotificationListenerService mListener = new NotificationListenerService() {
        @Override
        public void onNotificationPosted(StatusBarNotification notification) {
            logd("onNotificationPosted: %s", notification);
            final Handler h = getListView().getHandler();
            h.removeCallbacks(mRefreshListRunnable);
            h.postDelayed(mRefreshListRunnable, 100);
        }

        @Override
        public void onNotificationRemoved(StatusBarNotification notification) {
            final Handler h = getListView().getHandler();
            h.removeCallbacks(mRefreshListRunnable);
            h.postDelayed(mRefreshListRunnable, 100);
        }
    };

    private Context mContext;

    private final Comparator<HistoricalNotificationInfo> mNotificationSorter
            = new Comparator<HistoricalNotificationInfo>() {
                @Override
                public int compare(HistoricalNotificationInfo lhs,
                                   HistoricalNotificationInfo rhs) {
                    return (int)(rhs.timestamp - lhs.timestamp);
                }
            };

    @Override
    public void onAttach(Activity activity) {
        logd("onAttach(%s)", activity.getClass().getSimpleName());
        super.onAttach(activity);
        mContext = activity;
        mPm = mContext.getPackageManager();
        mNoMan = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        try {
            mListener.registerAsSystemService(mContext, new ComponentName(mContext.getPackageName(),
                    this.getClass().getCanonicalName()), ActivityManager.getCurrentUser());
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot register listener", e);
        }
    }

    @Override
    public void onDetach() {
        try {
            mListener.unregisterAsSystemService();
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot unregister listener", e);
        }
        super.onDetach();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_STATION;
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
                    final HistoricalNotificationInfo info = new HistoricalNotificationInfo();
                    info.pkg = sbn.getPackageName();
                    info.user = sbn.getUserId();
                    info.icon = loadIconDrawable(info.pkg, info.user, sbn.getNotification().icon);
                    info.pkgicon = loadPackageIconDrawable(info.pkg, info.user);
                    info.pkgname = loadPackageName(info.pkg);
                    if (sbn.getNotification().extras != null) {
                        info.title = sbn.getNotification().extras.getString(
                                Notification.EXTRA_TITLE);
                        if (info.title == null || "".equals(info.title)) {
                            info.title = sbn.getNotification().extras.getString(
                                    Notification.EXTRA_TEXT);
                        }
                    }
                    if (info.title == null || "".equals(info.title)) {
                        info.title = sbn.getNotification().tickerText;
                    }
                    // still nothing? come on, give us something!
                    if (info.title == null || "".equals(info.title)) {
                        info.title = info.pkgname;
                    }
                    info.timestamp = sbn.getPostTime();
                    info.priority = sbn.getNotification().priority;
                    logd("   [%d] %s: %s", info.timestamp, info.pkg, info.title);

                    info.active = (resultset == active);

                    if (info.user == UserHandle.USER_ALL
                            || info.user == currentUserId) {
                        list.add(info);
                    }
                }
            }

            return list;
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot load Notifications: ", e);
        }
        return null;
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
                    PackageManager.GET_UNINSTALLED_PACKAGES);
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

        public HistoricalNotificationPreference(Context context, HistoricalNotificationInfo info) {
            super(context);
            setLayoutResource(R.layout.notification_log_row);
            mInfo = info;
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder row) {
            if (mInfo.icon != null) {
                ((ImageView) row.findViewById(android.R.id.icon)).setImageDrawable(mInfo.icon);
            }
            if (mInfo.pkgicon != null) {
                ((ImageView) row.findViewById(R.id.pkgicon)).setImageDrawable(mInfo.pkgicon);
            }

            ((DateTimeView) row.findViewById(R.id.timestamp)).setTime(mInfo.timestamp);
            ((TextView) row.findViewById(android.R.id.title)).setText(mInfo.title);
            ((TextView) row.findViewById(R.id.pkgname)).setText(mInfo.pkgname);

            row.findViewById(R.id.extra).setVisibility(View.GONE);
            row.itemView.setAlpha(mInfo.active ? 1.0f : 0.5f);
        }

        @Override
        public void performClick() {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", mInfo.pkg, null));
            intent.setComponent(intent.resolveActivity(getContext().getPackageManager()));
            getContext().startActivity(intent);
        }
    }
}
