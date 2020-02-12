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

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;

import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

/*
 * Connects app usage info to the ApplicationsState. Wraps around the generic AppStateAppOpsBridge
 * class to tailor to the semantics of PACKAGE_USAGE_STATS. Also provides app filters that can use
 * the info.
 */
public class AppStateUsageBridge extends AppStateAppOpsBridge {

    private static final String TAG = "AppStateUsageBridge";

    private static final String PM_USAGE_STATS = Manifest.permission.PACKAGE_USAGE_STATS;
    private static final String PM_LOADER_STATS = Manifest.permission.LOADER_USAGE_STATS;
    private static final int APP_OPS_USAGE_STATS = AppOpsManager.OP_GET_USAGE_STATS;
    private static final int APP_OPS_LOADER_STATS = AppOpsManager.OP_LOADER_USAGE_STATS;
    private static final int[] APP_OPS_OP_CODES = {
            APP_OPS_USAGE_STATS,
            APP_OPS_LOADER_STATS,
    };
    private static final String[] PM_PERMISSIONS = {
            PM_USAGE_STATS,
            PM_LOADER_STATS,
    };

    public AppStateUsageBridge(Context context, ApplicationsState appState, Callback callback) {
        super(context, appState, callback, APP_OPS_OP_CODES, PM_PERMISSIONS);
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        app.extraInfo = getUsageInfo(pkg, uid);
    }

    public UsageState getUsageInfo(String pkg, int uid) {
        PermissionState permissionState = super.getPermissionInfo(pkg, uid);
        return new UsageState(permissionState);
    }

    public static class UsageState extends AppStateAppOpsBridge.PermissionState {

        public UsageState(PermissionState permissionState) {
            super(permissionState.packageName, permissionState.userHandle);
            this.packageInfo = permissionState.packageInfo;
            this.appOpMode = permissionState.appOpMode;
            this.permissionDeclared = permissionState.permissionDeclared;
            this.staticPermissionGranted = permissionState.staticPermissionGranted;
        }
    }

    public static final AppFilter FILTER_APP_USAGE = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            return info.extraInfo != null;
        }
    };
}
