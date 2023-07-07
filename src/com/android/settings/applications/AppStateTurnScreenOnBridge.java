/*
 * Copyright (C) 2023 The Android Open Source Project
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
 * Connects app op info to the ApplicationsState. Extends {@link AppStateAppOpsBridge} to tailor
 * to the semantics of {@link Manifest.permission#TURN_SCREEN_ON}.
 * Also provides app filters that can use the info.
 */
public class AppStateTurnScreenOnBridge extends AppStateAppOpsBridge {
    private static final String APP_OP_STR = AppOpsManager.OPSTR_TURN_SCREEN_ON;
    private static final String[] PERMISSIONS = {
            Manifest.permission.TURN_SCREEN_ON
    };

    private AppOpsManager mAppOpsManager;

    public AppStateTurnScreenOnBridge(Context context, ApplicationsState appState,
            Callback callback) {
        super(context, appState, callback, AppOpsManager.strOpToOp(APP_OP_STR), PERMISSIONS);
        mAppOpsManager = context.getSystemService(AppOpsManager.class);
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        app.extraInfo = getPermissionInfo(pkg, uid);
    }

    @Override
    protected void loadAllExtraInfo() {
        super.loadAllExtraInfo();
        final List<AppEntry> apps = mAppSession.getAllApps();
        for (AppEntry app : apps) {
            if (app.extraInfo instanceof PermissionState) {
                ((PermissionState) app.extraInfo).appOpMode = mAppOpsManager.unsafeCheckOpNoThrow(
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

    public static final AppFilter FILTER_TURN_SCREEN_ON_APPS = new AppFilter() {

        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            // If extraInfo != null, it means that the app has declared
            // Manifest.permission.TURN_SCREEN_ON and therefore it should appear on our
            // list
            return info.extraInfo != null;
        }
    };
}
