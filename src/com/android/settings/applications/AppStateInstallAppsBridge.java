/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.util.ArrayUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

import java.util.List;

/**
 * Connects app op info to the ApplicationsState. Wraps around the generic AppStateBaseBridge
 * class to tailor to the semantics of {@link AppOpsManager#OP_REQUEST_INSTALL_PACKAGES}
 * Also provides app filters that can use the info.
 */
public class AppStateInstallAppsBridge extends AppStateBaseBridge {

    private static final String TAG = AppStateInstallAppsBridge.class.getSimpleName();

    private final IPackageManager mIpm;
    private final AppOpsManager mAppOpsManager;

    public AppStateInstallAppsBridge(Context context, ApplicationsState appState,
            Callback callback) {
        super(appState, callback);
        mIpm = AppGlobals.getPackageManager();
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String packageName, int uid) {
        app.extraInfo = createInstallAppsStateFor(packageName, uid);
    }

    @Override
    protected void loadAllExtraInfo() {
        // TODO: consider making this a batch operation with a single binder call
        final List<AppEntry> allApps = mAppSession.getAllApps();
        for (int i = 0; i < allApps.size(); i++) {
            AppEntry currentEntry = allApps.get(i);
            updateExtraInfo(currentEntry, currentEntry.info.packageName, currentEntry.info.uid);
        }
    }

    private boolean hasRequestedAppOpPermission(String permission, String packageName) {
        try {
            String[] packages = mIpm.getAppOpPermissionPackages(permission);
            return ArrayUtils.contains(packages, packageName);
        } catch (RemoteException exc) {
            Log.e(TAG, "PackageManager dead. Cannot get permission info");
            return false;
        }
    }

    private boolean hasPermission(String permission, int uid) {
        try {
            int result = mIpm.checkUidPermission(permission, uid);
            return result == PackageManager.PERMISSION_GRANTED;
        } catch (RemoteException e) {
            Log.e(TAG, "PackageManager dead. Cannot get permission info");
            return false;
        }
    }

    private int getAppOpMode(int appOpCode, int uid, String packageName) {
        return mAppOpsManager.checkOpNoThrow(appOpCode, uid, packageName);
    }

    public InstallAppsState createInstallAppsStateFor(String packageName, int uid) {
        final InstallAppsState appState = new InstallAppsState();
        appState.permissionRequested = hasRequestedAppOpPermission(
                Manifest.permission.REQUEST_INSTALL_PACKAGES, packageName);
        appState.appOpMode = getAppOpMode(AppOpsManager.OP_REQUEST_INSTALL_PACKAGES, uid,
                packageName);
        return appState;
    }

    /**
     * Collection of information to be used as {@link AppEntry#extraInfo} objects
     */
    public static class InstallAppsState {
        boolean permissionRequested;
        int appOpMode;

        public InstallAppsState() {
            this.appOpMode = AppOpsManager.MODE_DEFAULT;
        }

        public boolean canInstallApps() {
            return appOpMode == AppOpsManager.MODE_ALLOWED;
        }

        public boolean isPotentialAppSource() {
            return appOpMode != AppOpsManager.MODE_DEFAULT || permissionRequested;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[permissionRequested: " + permissionRequested);
            sb.append(", appOpMode: " + appOpMode);
            sb.append("]");
            return sb.toString();
        }
    }

    public static final AppFilter FILTER_APP_SOURCES = new AppFilter() {

        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            if (info.extraInfo == null || !(info.extraInfo instanceof InstallAppsState)) {
                return false;
            }
            InstallAppsState state = (InstallAppsState) info.extraInfo;
            return state.isPotentialAppSource();
        }
    };
}
