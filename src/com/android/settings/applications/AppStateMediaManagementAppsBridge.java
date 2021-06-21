/*
 * Copyright (C) 2021 The Android Open Source Project
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
import android.app.AppOpsManager;
import android.content.Context;

import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

import java.util.List;

/**
 * Retrieves information from {@link AppOpsManager} and {@link android.content.pm.PackageManager}
 * regarding {@link AppOpsManager#OP_MANAGE_MEDIA} and
 * {@link Manifest.permission#MANAGE_MEDIA}.
 */
public class AppStateMediaManagementAppsBridge extends AppStateAppOpsBridge {

    private final AppOpsManager mAppOpsManager;

    public AppStateMediaManagementAppsBridge(Context context, ApplicationsState appState,
            Callback callback) {
        super(context, appState, callback,
                AppOpsManager.strOpToOp(AppOpsManager.OPSTR_MANAGE_MEDIA),
                new String[]{Manifest.permission.MANAGE_MEDIA});

        mAppOpsManager = context.getSystemService(AppOpsManager.class);
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        app.extraInfo = createPermissionState(pkg, uid);
    }

    @Override
    protected void loadAllExtraInfo() {
        super.loadAllExtraInfo();
        final List<AppEntry> allApps = mAppSession.getAllApps();
        final int appCount = allApps.size();
        for (int i = 0; i < appCount; i++) {
            final AppEntry appEntry = allApps.get(i);
            if (appEntry.extraInfo instanceof PermissionState) {
                updateExtraInfo(appEntry, appEntry.info.packageName, appEntry.info.uid);
            }
        }
    }

    /**
     * Returns information regarding {@link Manifest.permission#MANAGE_MEDIA} for the given
     * package and uid.
     */
    public PermissionState createPermissionState(String packageName, int uid) {
        final PermissionState permissionState = getPermissionInfo(packageName, uid);
        permissionState.appOpMode = mAppOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_MANAGE_MEDIA, uid, packageName);
        return permissionState;
    }

    /**
     * Used by {@link com.android.settings.applications.manageapplications.AppFilterRegistry} to
     * determine which apps get to appear on the Special App Access list.
     */
    public static final AppFilter FILTER_MEDIA_MANAGEMENT_APPS = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            return info.extraInfo != null;
        }
    };
}
