/*
 * Copyright (C) 2020 The Android Open Source Project
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

import java.util.List;

/**
 * Retrieves information from {@link AppOpsManager} and {@link android.content.pm.PackageManager}
 * regarding {@link AppOpsManager#OP_MANAGE_EXTERNAL_STORAGE} and
 * {@link Manifest.permission#MANAGE_EXTERNAL_STORAGE}.
 */
public class AppStateManageExternalStorageBridge extends AppStateAppOpsBridge {
    private static final String APP_OP_STR = AppOpsManager.OPSTR_MANAGE_EXTERNAL_STORAGE;
    private static final String[] PERMISSIONS = {
            Manifest.permission.MANAGE_EXTERNAL_STORAGE
    };

    private final AppOpsManager mAppOpsManager;

    public AppStateManageExternalStorageBridge(Context context, ApplicationsState appState,
            Callback callback) {
        super(context, appState, callback, AppOpsManager.strOpToOp(APP_OP_STR), PERMISSIONS);
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
    }

    @Override
    protected void updateExtraInfo(ApplicationsState.AppEntry app, String pkg, int uid) {
        app.extraInfo = getManageExternalStoragePermState(pkg, uid);
    }

    @Override
    protected void loadAllExtraInfo() {
        super.loadAllExtraInfo();
        List<ApplicationsState.AppEntry> apps = mAppSession.getAllApps();
        for (ApplicationsState.AppEntry app : apps) {
            if (app.extraInfo instanceof PermissionState) {
                ((PermissionState) app.extraInfo).appOpMode =  mAppOpsManager.unsafeCheckOpNoThrow(
                        APP_OP_STR, app.info.uid, app.info.packageName);
            }
        }
    }

    @Override
    public PermissionState getPermissionInfo(String pkg, int uid) {
        PermissionState ps = super.getPermissionInfo(pkg, uid);
        ps.appOpMode = mAppOpsManager.unsafeCheckOpNoThrow(APP_OP_STR, uid, pkg);
        return ps;
    }

    /**
     * Returns the MANAGE_EXTERNAL_STORAGE {@link AppStateAppOpsBridge.PermissionState} object
     * associated with the given package and UID.
     */
    public PermissionState getManageExternalStoragePermState(String pkg, int uid) {
        return getPermissionInfo(pkg, uid);
    }

    /**
     * Used by {@link com.android.settings.applications.manageapplications.AppFilterRegistry} to
     * determine which apps get to appear on the Special App Access list.
     */
    public static final ApplicationsState.AppFilter FILTER_MANAGE_EXTERNAL_STORAGE =
            new ApplicationsState.AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationsState.AppEntry info) {
            // If extraInfo != null, it means that the app has declared
            // Manifest.permission.MANAGE_EXTERNAL_STORAGE and therefore it should appear on our
            // list
            return info.extraInfo != null;
        }
    };
}
