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
import android.os.Build;
import android.os.IDeviceIdleController;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settingslib.fuelgauge.PowerAllowlistBackend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** An implementation to backup and restore battery configurations. */
public final class BatteryBackupHelper implements BackupHelper {
    /** An inditifier for {@link BackupHelper}. */
    public static final String TAG = "BatteryBackupHelper";
    private static final String DEVICE_IDLE_SERVICE = "deviceidle";
    private static final boolean DEBUG = Build.TYPE.equals("userdebug");

    static final String DELIMITER = ",";
    static final String DELIMITER_MODE = ":";
    static final String KEY_FULL_POWER_LIST = "full_power_list";
    static final String KEY_OPTIMIZATION_LIST = "optimization_mode_list";

    @VisibleForTesting
    ArraySet<ApplicationInfo> mTestApplicationInfoList = null;

    @VisibleForTesting
    PowerAllowlistBackend mPowerAllowlistBackend;
    @VisibleForTesting
    IDeviceIdleController mIDeviceIdleController;
    @VisibleForTesting
    IPackageManager mIPackageManager;
    @VisibleForTesting
    BatteryOptimizeUtils mBatteryOptimizeUtils;

    private final Context mContext;

    public BatteryBackupHelper(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
            ParcelFileDescriptor newState) {
        if (!isOwner() || data == null) {
            Log.w(TAG, "ignore performBackup() for non-owner or empty data");
            return;
        }
        final List<String> allowlistedApps = backupFullPowerList(data);
        if (allowlistedApps != null) {
            backupOptimizationMode(data, allowlistedApps);
        }
    }

    @Override
    public void restoreEntity(BackupDataInputStream data) {
        BatterySettingsMigrateChecker.verifyConfiguration(mContext);
        if (!isOwner() || data == null || data.size() == 0) {
            Log.w(TAG, "ignore restoreEntity() for non-owner or empty data");
            return;
        }
        if (KEY_OPTIMIZATION_LIST.equals(data.getKey())) {
            final int dataSize = data.size();
            final byte[] dataBytes = new byte[dataSize];
            try {
                data.read(dataBytes, 0 /*offset*/, dataSize);
            } catch (IOException e) {
                Log.e(TAG, "failed to load BackupDataInputStream", e);
                return;
            }
            restoreOptimizationMode(dataBytes);
        }
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

        final String allowedApps = String.join(DELIMITER, allowlistedApps);
        writeBackupData(data, KEY_FULL_POWER_LIST, allowedApps);
        Log.d(TAG, String.format("backup getFullPowerList() size=%d in %d/ms",
                allowlistedApps.length, (System.currentTimeMillis() - timestamp)));
        return Arrays.asList(allowlistedApps);
    }

    @VisibleForTesting
    void backupOptimizationMode(BackupDataOutput data, List<String> allowlistedApps) {
        final long timestamp = System.currentTimeMillis();
        final ArraySet<ApplicationInfo> applications = getInstalledApplications();
        if (applications == null || applications.isEmpty()) {
            Log.w(TAG, "no data found in the getInstalledApplications()");
            return;
        }
        int backupCount = 0;
        final StringBuilder builder = new StringBuilder();
        final AppOpsManager appOps = mContext.getSystemService(AppOpsManager.class);
        // Converts application into the AppUsageState.
        for (ApplicationInfo info : applications) {
            final int mode = appOps.checkOpNoThrow(
                    AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, info.uid, info.packageName);
            @BatteryOptimizeUtils.OptimizationMode
            final int optimizationMode = BatteryOptimizeUtils.getAppOptimizationMode(
                    mode, allowlistedApps.contains(info.packageName));
            // Ignores default optimized/unknown state or system/default apps.
            if (optimizationMode == BatteryOptimizeUtils.MODE_OPTIMIZED
                    || optimizationMode == BatteryOptimizeUtils.MODE_UNKNOWN
                    || isSystemOrDefaultApp(info.packageName, info.uid)) {
                continue;
            }
            final String packageOptimizeMode =
                    info.packageName + DELIMITER_MODE + optimizationMode;
            builder.append(packageOptimizeMode + DELIMITER);
            debugLog(packageOptimizeMode);
            backupCount++;
        }

        writeBackupData(data, KEY_OPTIMIZATION_LIST, builder.toString());
        Log.d(TAG, String.format("backup getInstalledApplications():%d count=%d in %d/ms",
                applications.size(), backupCount, (System.currentTimeMillis() - timestamp)));
    }

    @VisibleForTesting
    void restoreOptimizationMode(byte[] dataBytes) {
        final long timestamp = System.currentTimeMillis();
        final String dataContent = new String(dataBytes, StandardCharsets.UTF_8);
        if (dataContent == null || dataContent.isEmpty()) {
            Log.w(TAG, "no data found in the restoreOptimizationMode()");
            return;
        }
        final String[] appConfigurations = dataContent.split(BatteryBackupHelper.DELIMITER);
        if (appConfigurations == null || appConfigurations.length == 0) {
            Log.w(TAG, "no data found from the split() processing");
            return;
        }
        int restoreCount = 0;
        for (int index = 0; index < appConfigurations.length; index++) {
            final String[] results = appConfigurations[index]
                    .split(BatteryBackupHelper.DELIMITER_MODE);
            // Example format: com.android.systemui:2 we should have length=2
            if (results == null || results.length != 2) {
                Log.w(TAG, "invalid raw data found:" + appConfigurations[index]);
                continue;
            }
            final String packageName = results[0];
            final int uid = BatteryUtils.getInstance(mContext).getPackageUid(packageName);
            // Ignores system/default apps.
            if (isSystemOrDefaultApp(packageName, uid)) {
                Log.w(TAG, "ignore from isSystemOrDefaultApp():" + packageName);
                continue;
            }
            @BatteryOptimizeUtils.OptimizationMode
            int optimizationMode = BatteryOptimizeUtils.MODE_UNKNOWN;
            try {
                optimizationMode = Integer.parseInt(results[1]);
            } catch (NumberFormatException e) {
                Log.e(TAG, "failed to parse the optimization mode: "
                        + appConfigurations[index], e);
                continue;
            }
            restoreOptimizationMode(packageName, optimizationMode);
            restoreCount++;
        }
        Log.d(TAG, String.format("restoreOptimizationMode() count=%d in %d/ms",
                restoreCount, (System.currentTimeMillis() - timestamp)));
    }

    private void restoreOptimizationMode(
            String packageName, @BatteryOptimizeUtils.OptimizationMode int mode) {
        final int uid = BatteryUtils.getInstance(mContext).getPackageUid(packageName);
        if (uid == BatteryUtils.UID_NULL) {
            return;
        }
        final BatteryOptimizeUtils batteryOptimizeUtils =
                mBatteryOptimizeUtils != null
                        ? mBatteryOptimizeUtils /*testing only*/
                        : new BatteryOptimizeUtils(mContext, uid, packageName);
        batteryOptimizeUtils.setAppUsageState(
                mode, BatteryOptimizeHistoricalLogEntry.Action.RESTORE);
        Log.d(TAG, String.format("restore:%s mode=%d", packageName, mode));
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

    private PowerAllowlistBackend getPowerAllowlistBackend() {
        if (mPowerAllowlistBackend != null) {
            return mPowerAllowlistBackend;
        }
        mPowerAllowlistBackend = PowerAllowlistBackend.getInstance(mContext);
        return mPowerAllowlistBackend;
    }

    private boolean isSystemOrDefaultApp(String packageName, int uid) {
        final PowerAllowlistBackend powerAllowlistBackend = getPowerAllowlistBackend();
        return powerAllowlistBackend.isSysAllowlisted(packageName)
                || powerAllowlistBackend.isDefaultActiveApp(packageName, uid);
    }

    private ArraySet<ApplicationInfo> getInstalledApplications() {
        if (mTestApplicationInfoList != null) {
            return mTestApplicationInfoList;
        }
        return BatteryOptimizeUtils.getInstalledApplications(mContext, getIPackageManager());
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
