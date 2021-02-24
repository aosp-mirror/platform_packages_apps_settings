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
 * Connects app op info to the ApplicationsState. Extends {@link AppStateAppOpsBridge} to tailor
 * to the semantics of {@link Manifest.permission#SCHEDULE_EXACT_ALARM}.
 * Also provides app filters that can use the info.
 */
public class AppStateAlarmsAndRemindersBridge extends AppStateAppOpsBridge {

    private AppOpsManager mAppOpsManager;

    public AppStateAlarmsAndRemindersBridge(Context context, ApplicationsState appState,
            Callback callback) {
        super(context, appState, callback,
                AppOpsManager.strOpToOp(AppOpsManager.OPSTR_SCHEDULE_EXACT_ALARM),
                new String[]{Manifest.permission.SCHEDULE_EXACT_ALARM});

        mAppOpsManager = context.getSystemService(AppOpsManager.class);
    }

    /**
     * Returns information regarding {@link Manifest.permission#SCHEDULE_EXACT_ALARM} for the given
     * package and uid.
     */
    public PermissionState createPermissionState(String packageName, int uid) {
        final PermissionState permState = getPermissionInfo(packageName, uid);
        permState.appOpMode = mAppOpsManager.unsafeCheckOpRawNoThrow(
                AppOpsManager.OPSTR_SCHEDULE_EXACT_ALARM, uid, packageName);
        return permState;
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

    public static final AppFilter FILTER_CLOCK_APPS = new AppFilter() {

        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            if (info.extraInfo instanceof PermissionState) {
                final PermissionState permissionState = (PermissionState) info.extraInfo;
                return permissionState.permissionDeclared;
            }
            return false;
        }
    };

}
