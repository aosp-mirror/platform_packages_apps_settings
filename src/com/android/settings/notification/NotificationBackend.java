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

import android.app.INotificationManager;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.ServiceManager;
import android.service.notification.NotificationListenerService;
import android.util.Log;

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
        row.priority = getHighPriority(row.pkg, row.uid);
        row.peekable = getPeekable(row.pkg, row.uid);
        row.sensitive = getSensitive(row.pkg, row.uid);
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

    public boolean getHighPriority(String pkg, int uid) {
        try {
            return sINM.getPackagePriority(pkg, uid) == Notification.PRIORITY_MAX;
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return false;
        }
    }

    public boolean setHighPriority(String pkg, int uid, boolean highPriority) {
        try {
            sINM.setPackagePriority(pkg, uid,
                    highPriority ? Notification.PRIORITY_MAX : Notification.PRIORITY_DEFAULT);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return false;
        }
    }

    public boolean getPeekable(String pkg, int uid) {
        try {
            return sINM.getPackagePeekable(pkg, uid);
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return false;
        }
    }

    public boolean setPeekable(String pkg, int uid, boolean peekable) {
        try {
            sINM.setPackagePeekable(pkg, uid, peekable);
            return true;
        } catch (Exception e) {
           Log.w(TAG, "Error calling NoMan", e);
           return false;
        }
    }

    public boolean getSensitive(String pkg, int uid) {
        try {
            return sINM.getPackageVisibilityOverride(pkg, uid) == Notification.VISIBILITY_PRIVATE;
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return false;
        }
    }

    public boolean setSensitive(String pkg, int uid, boolean sensitive) {
        try {
            sINM.setPackageVisibilityOverride(pkg, uid,
                    sensitive ? Notification.VISIBILITY_PRIVATE
                            : NotificationListenerService.Ranking.VISIBILITY_NO_OVERRIDE);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return false;
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
        public boolean priority;
        public boolean peekable;
        public boolean sensitive;
        public boolean first;  // first app in section
    }

}
