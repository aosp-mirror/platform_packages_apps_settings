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
import android.app.compat.CompatChanges;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.PowerExemptionManager;
import android.os.UserHandle;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

import java.util.List;

/**
 * Connects app op info to the ApplicationsState. Extends {@link AppStateAppOpsBridge} to tailor
 * to the semantics of {@link Manifest.permission#SCHEDULE_EXACT_ALARM}.
 * Also provides app filters that can use the info.
 */
public class AppStateAlarmsAndRemindersBridge extends AppStateBaseBridge {
    private static final String SEA_PERMISSION = Manifest.permission.SCHEDULE_EXACT_ALARM;
    private static final String UEA_PERMISSION = Manifest.permission.USE_EXACT_ALARM;
    private static final String TAG = "AlarmsAndRemindersBridge";

    @VisibleForTesting
    AlarmManager mAlarmManager;
    @VisibleForTesting
    PowerExemptionManager mPowerExemptionManager;
    @VisibleForTesting
    PackageManager mPackageManager;

    public AppStateAlarmsAndRemindersBridge(Context context, ApplicationsState appState,
            Callback callback) {
        super(appState, callback);

        mPowerExemptionManager = context.getSystemService(PowerExemptionManager.class);
        mAlarmManager = context.getSystemService(AlarmManager.class);
        mPackageManager = context.getPackageManager();
    }

    private boolean isChangeEnabled(String packageName, int userId) {
        return CompatChanges.isChangeEnabled(AlarmManager.REQUIRE_EXACT_ALARM_PERMISSION,
                packageName, UserHandle.of(userId));
    }

    private boolean isUeaChangeEnabled(String packageName, int userId) {
        return CompatChanges.isChangeEnabled(AlarmManager.ENABLE_USE_EXACT_ALARM, packageName,
                UserHandle.of(userId));
    }

    private String[] getRequestedPermissions(String packageName, int userId) {
        try {
            final PackageInfo info = mPackageManager.getPackageInfoAsUser(packageName,
                    PackageManager.GET_PERMISSIONS, userId);
            return info.requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not find package " + packageName, e);
        }
        return null;
    }

    /**
     * Returns information regarding {@link Manifest.permission#SCHEDULE_EXACT_ALARM} for the given
     * package and uid.
     */
    public AlarmsAndRemindersState createPermissionState(String packageName, int uid) {
        final int userId = UserHandle.getUserId(uid);

        final String[] requestedPermissions = getRequestedPermissions(packageName, userId);

        final boolean seaRequested = ArrayUtils.contains(requestedPermissions, SEA_PERMISSION)
                && isChangeEnabled(packageName, userId);
        final boolean ueaRequested = ArrayUtils.contains(requestedPermissions, UEA_PERMISSION)
                && isUeaChangeEnabled(packageName, userId);

        final boolean seaGranted = mAlarmManager.hasScheduleExactAlarm(packageName, userId);
        final boolean allowListed = mPowerExemptionManager.isAllowListed(packageName, true);

        return new AlarmsAndRemindersState(seaRequested, ueaRequested, seaGranted, allowListed);
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
     * Class to denote the state of an app regarding "Alarms and Reminders" permission.
     * This permission state is a combination of {@link Manifest.permission#SCHEDULE_EXACT_ALARM},
     * {@link Manifest.permission#USE_EXACT_ALARM} and the power allowlist state.
     */
    public static class AlarmsAndRemindersState {
        private boolean mSeaPermissionRequested;
        private boolean mUeaPermissionRequested;
        private boolean mSeaPermissionGranted;
        private boolean mAllowListed;

        AlarmsAndRemindersState(boolean seaPermissionRequested, boolean ueaPermissionRequested,
                boolean seaPermissionGranted, boolean allowListed) {
            mSeaPermissionRequested = seaPermissionRequested;
            mUeaPermissionRequested = ueaPermissionRequested;
            mSeaPermissionGranted = seaPermissionGranted;
            mAllowListed = allowListed;
        }

        /** Should the app associated with this state appear on the Settings screen */
        public boolean shouldBeVisible() {
            return mSeaPermissionRequested && !mUeaPermissionRequested && !mAllowListed;
        }

        /** Is the permission granted to the app associated with this state */
        public boolean isAllowed() {
            return mSeaPermissionGranted || mUeaPermissionRequested || mAllowListed;
        }
    }
}
