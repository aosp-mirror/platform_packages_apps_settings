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
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.compat.CompatChanges;
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
 * to the semantics of {@link Manifest.permission#SCHEDULE_EXACT_ALARM}.
 * Also provides app filters that can use the info.
 */
public class AppStateAlarmsAndRemindersBridge extends AppStateBaseBridge {
    private static final String PERMISSION = Manifest.permission.SCHEDULE_EXACT_ALARM;
    private static final String TAG = "AlarmsAndRemindersBridge";

    @VisibleForTesting
    AlarmManager mAlarmManager;
    @VisibleForTesting
    String[] mRequesterPackages;

    public AppStateAlarmsAndRemindersBridge(Context context, ApplicationsState appState,
            Callback callback) {
        super(appState, callback);

        mAlarmManager = context.getSystemService(AlarmManager.class);
        final IPackageManager iPm = AppGlobals.getPackageManager();
        try {
            mRequesterPackages = iPm.getAppOpPermissionPackages(PERMISSION);
        } catch (RemoteException re) {
            Log.e(TAG, "Cannot reach package manager", re);
            mRequesterPackages = EmptyArray.STRING;
        }
    }

    private boolean isChangeEnabled(String packageName, int userId) {
        return CompatChanges.isChangeEnabled(AlarmManager.REQUIRE_EXACT_ALARM_PERMISSION,
                packageName, UserHandle.of(userId));
    }

    /**
     * Returns information regarding {@link Manifest.permission#SCHEDULE_EXACT_ALARM} for the given
     * package and uid.
     */
    public AlarmsAndRemindersState createPermissionState(String packageName, int uid) {
        final int userId = UserHandle.getUserId(uid);

        final boolean permissionRequested = ArrayUtils.contains(mRequesterPackages, packageName)
                && isChangeEnabled(packageName, userId);
        final boolean permissionGranted = mAlarmManager.hasScheduleExactAlarm(packageName, userId);
        return new AlarmsAndRemindersState(permissionRequested, permissionGranted);
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
            if (info.extraInfo instanceof AlarmsAndRemindersState) {
                final AlarmsAndRemindersState state = (AlarmsAndRemindersState) info.extraInfo;
                return state.shouldBeVisible();
            }
            return false;
        }
    };

    /**
     * Class to denote the state of an app regarding
     * {@link Manifest.permission#SCHEDULE_EXACT_ALARM}.
     */
    public static class AlarmsAndRemindersState {
        private boolean mPermissionRequested;
        private boolean mPermissionGranted;

        AlarmsAndRemindersState(boolean permissionRequested, boolean permissionGranted) {
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
