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
package com.android.settings.applications;

import android.content.pm.PackageManager;

import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

import java.util.ArrayList;

/**
 * Connects the info provided by ApplicationsState and the NotificationBackend.
 * Also provides app filters that can use the notification data.
 */
public class AppStateNotificationBridge extends AppStateBaseBridge {

    private final NotificationBackend mNotifBackend;
    private final PackageManager mPm;

    public AppStateNotificationBridge(PackageManager pm, ApplicationsState appState,
            Callback callback, NotificationBackend notifBackend) {
        super(appState, callback);
        mPm = pm;
        mNotifBackend = notifBackend;
    }

    @Override
    protected void loadAllExtraInfo() {
        ArrayList<AppEntry> apps = mAppSession.getAllApps();
        final int N = apps.size();
        for (int i = 0; i < N; i++) {
            AppEntry app = apps.get(i);
            app.extraInfo = mNotifBackend.loadAppRow(mPm, app.info);
        }
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        app.extraInfo = mNotifBackend.loadAppRow(mPm, app.info);
    }

    public static final AppFilter FILTER_APP_NOTIFICATION_BLOCKED = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            return info.extraInfo != null && ((AppRow) info.extraInfo).banned;
        }
    };

    public static final AppFilter FILTER_APP_NOTIFICATION_PRIORITY = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            return info.extraInfo != null && ((AppRow) info.extraInfo).priority;
        }
    };

    public static final AppFilter FILTER_APP_NOTIFICATION_SENSITIVE = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            return info.extraInfo != null && ((AppRow) info.extraInfo).sensitive;
        }
    };

    public static final AppFilter FILTER_APP_NOTIFICATION_NO_PEEK = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            return info.extraInfo != null && !((AppRow) info.extraInfo).peekable;
        }
    };
}
