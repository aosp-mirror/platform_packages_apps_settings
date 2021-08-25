/*
 * Copyright (C) 2021 The Android Open Source Project
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
 *
 */

package com.android.settings.fuelgauge;

import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.os.IDeviceIdleController;
import android.os.RemoteException;
import android.os.ParcelFileDescriptor;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** An implementation to backup and restore battery configurations. */
public final class BatteryBackupHelper implements BackupHelper {
    /** An inditifier for {@link BackupHelper}. */
    public static final String TAG = "BatteryBackupHelper";
    private static final String DEVICE_IDLE_SERVICE = "deviceidle";
    private static final boolean DEBUG = false;

    // Only the owner can see all apps.
    private static final int RETRIEVE_FLAG_ADMIN =
            PackageManager.MATCH_ANY_USER |
            PackageManager.MATCH_DISABLED_COMPONENTS |
            PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS;
    private static final int RETRIEVE_FLAG =
            PackageManager.MATCH_DISABLED_COMPONENTS |
            PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS;

    static final CharSequence DELIMITER = ",";
    static final CharSequence DELIMITER_MODE = "|";
    static final String KEY_FULL_POWER_LIST = "full_power_list";
    static final String KEY_OPTIMIZATION_LIST = "optimization_mode_list";

    @VisibleForTesting
    IDeviceIdleController mIDeviceIdleController;
    @VisibleForTesting
    IPackageManager mIPackageManager;

    private final Context mContext;

    public BatteryBackupHelper(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
            ParcelFileDescriptor newState) {
        if (!isOwner()) {
            Log.w(TAG, "ignore performBackup() for non-owner");
            return;
        }
        final List<String> allowlistedApps = backupFullPowerList(data);
        if (allowlistedApps != null) {
            backupOptimizationMode(data, allowlistedApps);
        }
    }

    @Override
    public void restoreEntity(BackupDataInputStream data) {
        Log.d(TAG, "restoreEntity()");
    }

    @Override
    public void writeNewStateDescription(ParcelFileDescriptor newState) {
    }

    private List<String> backupFullPowerList(BackupDataOutput data) {
        final long timestamp = System.currentTimeMillis();
        String[] allowlistedApps;
        try {
            allowlistedApps = getIDeviceIdleController().getFullPowerWhitelist();
        } catch (RemoteException e) {
            Log.e(TAG, "backupFullPowerList() failed", e);
            return null;
        }
        // Ignores unexpected emptty result case.
        if (allowlistedApps == null || allowlistedApps.length == 0) {
            Log.w(TAG, "no data found in the getFullPowerList()");
            return new ArrayList<>();
        }

        debugLog("allowlistedApps:" + Arrays.toString(allowlistedApps));
        final String allowedApps = String.join(DELIMITER, allowlistedApps);
        writeBackupData(data, KEY_FULL_POWER_LIST, allowedApps);
        Log.d(TAG, String.format("backup getFullPowerList() size=%d in %d/ms",
                allowlistedApps.length, (System.currentTimeMillis() - timestamp)));
        return Arrays.asList(allowlistedApps);
    }

    @VisibleForTesting
    void backupOptimizationMode(BackupDataOutput data, List<String> allowlistedApps) {
        final long timestamp = System.currentTimeMillis();
        final List<ApplicationInfo> applications = getInstalledApplications();
        if (applications == null || applications.isEmpty()) {
            Log.w(TAG, "no data found in the getInstalledApplications()");
            return;
        }
        final StringBuilder builder = new StringBuilder();
        final AppOpsManager appOps = mContext.getSystemService(AppOpsManager.class);
        // Converts application into the AppUsageState.
        for (ApplicationInfo info : applications) {
            final int mode = appOps.checkOpNoThrow(
                    AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, info.uid, info.packageName);
            @BatteryOptimizeUtils.OptimizationMode
            final int optimizationMode = BatteryOptimizeUtils.getAppOptimizationMode(
                    mode, allowlistedApps.contains(info.packageName));
            // Ignores default optimized or unknown state.
            if (optimizationMode == BatteryOptimizeUtils.MODE_OPTIMIZED
                    || optimizationMode == BatteryOptimizeUtils.MODE_UNKNOWN) {
                continue;
            }
            final String packageOptimizeMode =
                    info.packageName + DELIMITER_MODE + optimizationMode;
            builder.append(packageOptimizeMode + DELIMITER);
            debugLog(packageOptimizeMode);
        }

        writeBackupData(data, KEY_OPTIMIZATION_LIST, builder.toString());
        Log.d(TAG, String.format("backup getInstalledApplications() size=%d in %d/ms",
                applications.size(), (System.currentTimeMillis() - timestamp)));
    }

    // Provides an opportunity to inject mock IDeviceIdleController for testing.
    private IDeviceIdleController getIDeviceIdleController() {
        if (mIDeviceIdleController != null) {
            return mIDeviceIdleController;
        }
        mIDeviceIdleController = IDeviceIdleController.Stub.asInterface(
                ServiceManager.getService(DEVICE_IDLE_SERVICE));
        return mIDeviceIdleController;
    }

    private IPackageManager getIPackageManager() {
        if (mIPackageManager != null) {
            return mIPackageManager;
        }
        mIPackageManager = AppGlobals.getPackageManager();
        return mIPackageManager;
    }

    private List<ApplicationInfo> getInstalledApplications() {
        final List<ApplicationInfo> applications = new ArrayList<>();
        final UserManager um = mContext.getSystemService(UserManager.class);
        for (UserInfo userInfo : um.getProfiles(UserHandle.myUserId())) {
            try {
                @SuppressWarnings("unchecked")
                final ParceledListSlice<ApplicationInfo> infoList =
                        getIPackageManager().getInstalledApplications(
                                userInfo.isAdmin() ? RETRIEVE_FLAG_ADMIN : RETRIEVE_FLAG,
                                userInfo.id);
                if (infoList != null) {
                    applications.addAll(infoList.getList());
                }
            } catch (Exception e) {
                Log.e(TAG, "getInstalledApplications() is failed", e);
                return null;
            }
        }
        // Removes the application which is disabled by the system.
        for (int index = applications.size() - 1; index >= 0; index--) {
            final ApplicationInfo info = applications.get(index);
            if (info.enabledSetting != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    && !info.enabled) {
                applications.remove(index);
            }
        }
        return applications;
    }

    private void debugLog(String debugContent) {
        if (DEBUG) Log.d(TAG, debugContent);
    }

    private static void writeBackupData(
            BackupDataOutput data, String dataKey, String dataContent) {
        final byte[] dataContentBytes = dataContent.getBytes();
        try {
            data.writeEntityHeader(dataKey, dataContentBytes.length);
            data.writeEntityData(dataContentBytes, dataContentBytes.length);
        } catch (IOException e) {
            Log.e(TAG, "writeBackupData() is failed for " + dataKey, e);
        }
    }

    private static boolean isOwner() {
        return UserHandle.myUserId() == UserHandle.USER_OWNER;
    }
}
