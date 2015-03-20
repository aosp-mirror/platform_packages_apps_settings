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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.settings.applications.ApplicationsState.AppEntry;
import com.android.settings.applications.ApplicationsState.AppFilter;
import com.android.settings.applications.ApplicationsState.Session;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.NotificationBackend.AppRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Connects the info provided by ApplicationsState and the NotificationBackend.
 * Also provides app filters that can use the notification data.
 */
public class AppStateNotificationBridge implements ApplicationsState.Callbacks {

    private final ApplicationsState mAppState;
    private final NotificationBackend mNotifBackend;
    private final Session mAppSession;
    private final Callback mCallback;
    private final BackgroundHandler mHandler;
    private final MainHandler mMainHandler;
    private final PackageManager mPm;

    public AppStateNotificationBridge(PackageManager pm, ApplicationsState appState,
            NotificationBackend notifBackend, Callback callback) {
        mAppState = appState;
        mPm = pm;
        mAppSession = mAppState.newSession(this);
        mNotifBackend = notifBackend;
        mCallback = callback;
        // Running on the same background thread as the ApplicationsState lets
        // us run in the background and make sure they aren't doing updates at
        // the same time as us as well.
        mHandler = new BackgroundHandler(mAppState.getBackgroundLooper());
        mMainHandler = new MainHandler();
        mHandler.sendEmptyMessage(BackgroundHandler.MSG_LOAD_ALL);
    }

    public void resume() {
        mHandler.sendEmptyMessage(BackgroundHandler.MSG_LOAD_ALL);
        mAppSession.resume();
    }

    public void pause() {
        mAppSession.pause();
    }

    public void release() {
        mAppSession.release();
    }

    public void forceUpdate(String pkg, int uid) {
        mHandler.obtainMessage(BackgroundHandler.MSG_FORCE_LOAD_PKG, uid, 0, pkg).sendToTarget();
    }

    @Override
    public void onPackageListChanged() {
        mHandler.sendEmptyMessage(BackgroundHandler.MSG_LOAD_ALL);
    }

    @Override
    public void onLoadEntriesCompleted() {
        mHandler.sendEmptyMessage(BackgroundHandler.MSG_LOAD_ALL);
    }

    @Override
    public void onRunningStateChanged(boolean running) {
        // No op.
    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
        // No op.
    }

    @Override
    public void onPackageIconChanged() {
        // No op.
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
        // No op.
    }

    @Override
    public void onAllSizesComputed() {
        // No op.
    }

    @Override
    public void onLauncherInfoChanged() {
        // No op.
    }

    private class MainHandler extends Handler {
        private static final int MSG_NOTIF_UPDATED = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_NOTIF_UPDATED:
                    mCallback.onNotificationInfoUpdated();
                    break;
            }
        }
    }

    private class BackgroundHandler extends Handler {
        private static final int MSG_LOAD_ALL = 1;
        private static final int MSG_FORCE_LOAD_PKG = 2;

        public BackgroundHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            List<AppEntry> apps = mAppSession.getAllApps();
            final int N = apps.size();
            switch (msg.what) {
                case MSG_LOAD_ALL:
                    for (int i = 0; i < N; i++) {
                        AppEntry app = apps.get(i);
                        app.extraInfo = mNotifBackend.loadAppRow(mPm, app.info);
                    }
                    mMainHandler.sendEmptyMessage(MainHandler.MSG_NOTIF_UPDATED);
                    break;
                case MSG_FORCE_LOAD_PKG:
                    String pkg = (String) msg.obj;
                    int uid = msg.arg1;
                    for (int i = 0; i < N; i++) {
                        AppEntry app = apps.get(i);
                        if (app.info.uid == uid && pkg.equals(app.info.packageName)) {
                            app.extraInfo = mNotifBackend.loadAppRow(mPm, app.info);
                            break;
                        }
                    }
                    mMainHandler.sendEmptyMessage(MainHandler.MSG_NOTIF_UPDATED);
                    break;
            }
        }
    }

    public interface Callback {
        void onNotificationInfoUpdated();
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
}
