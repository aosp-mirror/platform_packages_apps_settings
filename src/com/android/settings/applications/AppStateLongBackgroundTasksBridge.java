/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.Manifest;
import android.app.AppGlobals;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

import libcore.util.EmptyArray;

import java.util.List;

/**
 * Connects app op info to the ApplicationsState. Extends {@link AppStateAppOpsBridge} to tailor
 * to the semantics of {@link Manifest.permission#RUN_USER_INITIATED_JOBS}.
 * Also provides app filters that can use the info.
 */
public class AppStateLongBackgroundTasksBridge extends AppStateBaseBridge {
    private static final String PERMISSION = Manifest.permission.RUN_USER_INITIATED_JOBS;
    private static final String TAG = "LongBackgroundTasksBridge";

    @VisibleForTesting
    JobScheduler mJobScheduler;
    @VisibleForTesting
    String[] mRequesterPackages;

    public AppStateLongBackgroundTasksBridge(Context context, ApplicationsState appState,
            Callback callback) {
        super(appState, callback);

        mJobScheduler = context.getSystemService(JobScheduler.class);
        final IPackageManager iPm = AppGlobals.getPackageManager();
        try {
            mRequesterPackages = iPm.getAppOpPermissionPackages(PERMISSION, context.getUserId());
        } catch (RemoteException re) {
            Log.e(TAG, "Cannot reach package manager", re);
            mRequesterPackages = EmptyArray.STRING;
        }
    }

    /**
     * Returns information regarding {@link Manifest.permission#RUN_USER_INITIATED_JOBS} for the
     * given package and uid.
     */
    public LongBackgroundTasksState createPermissionState(String packageName, int uid) {
        final int userId = UserHandle.getUserId(uid);

        final boolean permissionRequested = ArrayUtils.contains(mRequesterPackages, packageName);
        final boolean permissionGranted = mJobScheduler.hasRunUserInitiatedJobsPermission(
                packageName, userId);
        return new LongBackgroundTasksState(permissionRequested, permissionGranted);
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        app.extraInfo = createPermissionState(pkg, uid);
    }

    @Override
    protected void loadAllExtraInfo() {
        final List<AppEntry> allApps = mAppSession.getAllApps();
        for (int i = 0; i < allApps.size(); i++) {
            final AppEntry currentEntry = allApps.get(i);
            updateExtraInfo(currentEntry, currentEntry.info.packageName, currentEntry.info.uid);
        }
    }

    public static final AppFilter FILTER_LONG_JOBS_APPS = new AppFilter() {

        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            if (info.extraInfo instanceof LongBackgroundTasksState) {
                final LongBackgroundTasksState state = (LongBackgroundTasksState) info.extraInfo;
                return state.shouldBeVisible();
            }
            return false;
        }
    };

    /**
     * Class to denote the state of an app regarding
     * {@link Manifest.permission#RUN_USER_INITIATED_JOBS}.
     */
    public static class LongBackgroundTasksState {
        private boolean mPermissionRequested;
        private boolean mPermissionGranted;

        LongBackgroundTasksState(boolean permissionRequested, boolean permissionGranted) {
            mPermissionRequested = permissionRequested;
            mPermissionGranted = permissionGranted;
        }

        /** Should the app associated with this state appear on the Settings screen */
        public boolean shouldBeVisible() {
            return mPermissionRequested;
        }

        /** Is the permission granted to the app associated with this state */
        public boolean isAllowed() {
            return mPermissionGranted;
        }
    }
}
