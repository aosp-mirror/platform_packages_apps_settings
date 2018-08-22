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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.Session;

import java.util.ArrayList;

/**
 * Common base class for bridging information to ApplicationsState.
 */
public abstract class AppStateBaseBridge implements ApplicationsState.Callbacks {

    protected final ApplicationsState mAppState;
    protected final Session mAppSession;
    protected final Callback mCallback;
    protected final BackgroundHandler mHandler;
    protected final MainHandler mMainHandler;

    public AppStateBaseBridge(ApplicationsState appState, Callback callback) {
        mAppState = appState;
        mAppSession = mAppState != null ? mAppState.newSession(this) : null;
        mCallback = callback;
        // Running on the same background thread as the ApplicationsState lets
        // us run in the background and make sure they aren't doing updates at
        // the same time as us as well.
        mHandler = new BackgroundHandler(mAppState != null ? mAppState.getBackgroundLooper()
                : Looper.getMainLooper());
        mMainHandler = new MainHandler(Looper.getMainLooper());
    }

    public void resume() {
        mHandler.sendEmptyMessage(BackgroundHandler.MSG_LOAD_ALL);
        mAppSession.onResume();
    }

    public void pause() {
        mAppSession.onPause();
    }

    public void release() {
        mAppSession.onDestroy();
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

    protected abstract void loadAllExtraInfo();

    protected abstract void updateExtraInfo(AppEntry app, String pkg, int uid);

    private class MainHandler extends Handler {
        private static final int MSG_INFO_UPDATED = 1;

        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INFO_UPDATED:
                    mCallback.onExtraInfoUpdated();
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
            switch (msg.what) {
                case MSG_LOAD_ALL:
                    loadAllExtraInfo();
                    mMainHandler.sendEmptyMessage(MainHandler.MSG_INFO_UPDATED);
                    break;
                case MSG_FORCE_LOAD_PKG:
                    ArrayList<AppEntry> apps = mAppSession.getAllApps();
                    final int N = apps.size();
                    String pkg = (String) msg.obj;
                    int uid = msg.arg1;
                    for (int i = 0; i < N; i++) {
                        AppEntry app = apps.get(i);
                        if (app.info.uid == uid && pkg.equals(app.info.packageName)) {
                            updateExtraInfo(app, pkg, uid);
                        }
                    }
                    mMainHandler.sendEmptyMessage(MainHandler.MSG_INFO_UPDATED);
                    break;
            }
        }
    }


    public interface Callback {
        void onExtraInfoUpdated();
    }
}
