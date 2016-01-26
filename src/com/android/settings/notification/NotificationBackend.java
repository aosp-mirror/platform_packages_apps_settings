/*
 * Copyright (C) 2015 The Android Open Source Project
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

import com.google.android.collect.Lists;

import android.app.INotificationManager;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.graphics.drawable.Drawable;
import android.os.ServiceManager;
import android.service.notification.NotificationListenerService;
import android.util.Log;
import com.android.settingslib.Utils;

import java.util.List;

public class NotificationBackend {
    private static final String TAG = "NotificationBackend";

    static INotificationManager sINM = INotificationManager.Stub.asInterface(
            ServiceManager.getService(Context.NOTIFICATION_SERVICE));

    public AppRow loadAppRow(PackageManager pm, ApplicationInfo app) {
        final AppRow row = new AppRow();
        row.pkg = app.packageName;
        row.uid = app.uid;
        try {
            row.label = app.loadLabel(pm);
        } catch (Throwable t) {
            Log.e(TAG, "Error loading application label for " + row.pkg, t);
            row.label = row.pkg;
        }
        row.icon = app.loadIcon(pm);
        row.banned = getNotificationsBanned(row.pkg, row.uid);
        row.appImportance = getImportance(row.pkg, row.uid, null);
        row.appBypassDnd = getBypassZenMode(row.pkg, row.uid, null);
        row.appSensitive = getSensitive(row.pkg, row.uid, null);
        return row;
    }

    public AppRow loadAppRow(PackageManager pm, PackageInfo app) {
        final AppRow row = loadAppRow(pm, app.applicationInfo);
        row.systemApp = Utils.isSystemPackage(pm, app);
        return row;
    }

    public TopicRow loadTopicRow(PackageManager pm, PackageInfo app, Notification.Topic topic) {
        final TopicRow row = new TopicRow();
        row.pkg = app.packageName;
        row.uid = app.applicationInfo.uid;
        row.label = topic.getLabel();
        row.icon = app.applicationInfo.loadIcon(pm);
        row.systemApp = Utils.isSystemPackage(pm, app);
        row.topic = topic;
        row.priority = getBypassZenMode(row.pkg, row.uid, row.topic);
        row.sensitive = getSensitive(row.pkg, row.uid, row.topic);
        row.banned = getNotificationsBanned(row.pkg, row.uid);
        row.importance = getImportance(row.pkg, row.uid, row.topic);
        return row;
    }

    public boolean setNotificationsBanned(String pkg, int uid, boolean banned) {
        try {
            sINM.setNotificationsEnabledForPackage(pkg, uid, !banned);
            return true;
        } catch (Exception e) {
           Log.w(TAG, "Error calling NoMan", e);
           return false;
        }
    }

    public boolean getNotificationsBanned(String pkg, int uid) {
        try {
            final boolean enabled = sINM.areNotificationsEnabledForPackage(pkg, uid);
            return !enabled;
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return false;
        }
    }

    public boolean getBypassZenMode(String pkg, int uid, Notification.Topic topic) {
        try {
            return sINM.getPriority(pkg, uid, topic) == Notification.PRIORITY_MAX;
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return false;
        }
    }

    public boolean setBypassZenMode(String pkg, int uid, Notification.Topic topic,
            boolean bypassZen) {
        try {
            sINM.setPriority(pkg, uid, topic,
                    bypassZen ? Notification.PRIORITY_MAX : Notification.PRIORITY_DEFAULT);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return false;
        }
    }

    public boolean getSensitive(String pkg, int uid, Notification.Topic topic) {
        try {
            return sINM.getVisibilityOverride(pkg, uid, topic)
                    == Notification.VISIBILITY_PRIVATE;
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return false;
        }
    }

    public boolean setSensitive(String pkg, int uid, Notification.Topic topic, boolean sensitive) {
        try {
            sINM.setVisibilityOverride(pkg, uid, topic,
                    sensitive ? Notification.VISIBILITY_PRIVATE
                            : NotificationListenerService.Ranking.VISIBILITY_NO_OVERRIDE);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return false;
        }
    }

    public boolean setImportance(String pkg, int uid, Notification.Topic topic, int importance) {
        try {
            sINM.setImportance(pkg, uid, topic, importance);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return false;
        }
    }

    public int getImportance(String pkg, int uid, Notification.Topic topic) {
        try {
            return sINM.getImportance(pkg, uid, topic);
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return NotificationListenerService.Ranking.IMPORTANCE_UNSPECIFIED;
        }
    }

    public List<Notification.Topic> getTopics(String pkg, int uid) {
        try {
            final ParceledListSlice<Notification.Topic> parceledList = sINM.getTopics(pkg, uid);
            return parceledList.getList();
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return Lists.newArrayList();
        }
    }

    static class Row {
        public String section;
    }

    public static class AppRow extends Row {
        public String pkg;
        public int uid;
        public Drawable icon;
        public CharSequence label;
        public Intent settingsIntent;
        public boolean banned;
        public boolean first;  // first app in section
        public boolean systemApp;
        public int appImportance;
        public boolean appBypassDnd;
        public boolean appSensitive;
    }

    public static class TopicRow extends AppRow {
        public Notification.Topic topic;
        public boolean priority;
        public boolean sensitive;
        public int importance;
    }
}
