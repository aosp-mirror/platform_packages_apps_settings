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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.service.notification.NotificationListenerService;
import android.util.Log;

import com.android.internal.widget.LockPatternUtils;
import com.android.settingslib.Utils;

public class NotificationBackend {
    private static final String TAG = "NotificationBackend";

    static INotificationManager sINM = INotificationManager.Stub.asInterface(
            ServiceManager.getService(Context.NOTIFICATION_SERVICE));

    public AppRow loadAppRow(Context context, PackageManager pm, ApplicationInfo app) {
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
        row.appImportance = getImportance(row.pkg, row.uid);
        row.appBypassDnd = getBypassZenMode(row.pkg, row.uid);
        row.appVisOverride = getVisibilityOverride(row.pkg, row.uid);
        row.lockScreenSecure = new LockPatternUtils(context).isSecure(
                UserHandle.myUserId());
        return row;
    }

    public AppRow loadAppRow(Context context, PackageManager pm, PackageInfo app) {
        final AppRow row = loadAppRow(context, pm, app.applicationInfo);
        row.cantBlock = Utils.isSystemPackage(context.getResources(), pm, app);
        final String[] nonBlockablePkgs = context.getResources().getStringArray(
                    com.android.internal.R.array.config_nonBlockableNotificationPackages);
        if (nonBlockablePkgs != null) {
            int N = nonBlockablePkgs.length;
            for (int i = 0; i < N; i++) {
                if (app.packageName.equals(nonBlockablePkgs[i])) {
                    row.cantBlock = row.cantSilence = true;
                }
            }
        }
        return row;
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

    public boolean getBypassZenMode(String pkg, int uid) {
        try {
            return sINM.getPriority(pkg, uid) == Notification.PRIORITY_MAX;
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return false;
        }
    }

    public boolean setBypassZenMode(String pkg, int uid, boolean bypassZen) {
        try {
            sINM.setPriority(pkg, uid,
                    bypassZen ? Notification.PRIORITY_MAX : Notification.PRIORITY_DEFAULT);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return false;
        }
    }

    public int getVisibilityOverride(String pkg, int uid) {
        try {
            return sINM.getVisibilityOverride(pkg, uid);
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return NotificationListenerService.Ranking.VISIBILITY_NO_OVERRIDE;
        }
    }

    public boolean setVisibilityOverride(String pkg, int uid, int override) {
        try {
            sINM.setVisibilityOverride(pkg, uid, override);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return false;
        }
    }

    public boolean setImportance(String pkg, int uid, int importance) {
        try {
            sINM.setImportance(pkg, uid, importance);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return false;
        }
    }

    public int getImportance(String pkg, int uid) {
        try {
            return sINM.getImportance(pkg, uid);
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return NotificationListenerService.Ranking.IMPORTANCE_UNSPECIFIED;
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
        public boolean cantBlock;
        public boolean cantSilence;
        public int appImportance;
        public boolean appBypassDnd;
        public int appVisOverride;
        public boolean lockScreenSecure;
    }
}
