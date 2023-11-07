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
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.os.Build;
import android.os.IDeviceIdleController;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.fuelgauge.PowerAllowlistBackend;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** An implementation to backup and restore battery configurations. */
public final class BatteryBackupHelper implements BackupHelper {
    /** An inditifier for {@link BackupHelper}. */
    public static final String TAG = "BatteryBackupHelper";

    // Definition for the device build information.
    public static final String KEY_BUILD_BRAND = "device_build_brand";
    public static final String KEY_BUILD_PRODUCT = "device_build_product";
    public static final String KEY_BUILD_MANUFACTURER = "device_build_manufacture";
    public static final String KEY_BUILD_FINGERPRINT = "device_build_fingerprint";
    // Customized fields for device extra information.
    public static final String KEY_BUILD_METADATA_1 = "device_build_metadata_1";
    public static final String KEY_BUILD_METADATA_2 = "device_build_metadata_2";

    private static final String DEVICE_IDLE_SERVICE = "deviceidle";
    private static final String BATTERY_OPTIMIZE_BACKUP_FILE_NAME =
            "battery_optimize_backup_historical_logs";
    private static final int DEVICE_BUILD_INFO_SIZE = 6;

    static final String DELIMITER = ",";
    static final String DELIMITER_MODE = ":";
    static final String KEY_OPTIMIZATION_LIST = "optimization_mode_list";

    @VisibleForTesting ArraySet<ApplicationInfo> mTestApplicationInfoList = null;

    @VisibleForTesting PowerAllowlistBackend mPowerAllowlistBackend;
    @VisibleForTesting IDeviceIdleController mIDeviceIdleController;
    @VisibleForTesting IPackageManager mIPackageManager;
    @VisibleForTesting BatteryOptimizeUtils mBatteryOptimizeUtils;

    private byte[] mOptimizationModeBytes;
    private boolean mVerifyMigrateConfiguration = false;

    private final Context mContext;
    // Device information map from the restoreEntity() method.
    private final ArrayMap<String, String> mDeviceBuildInfoMap =
            new ArrayMap<>(DEVICE_BUILD_INFO_SIZE);

    public BatteryBackupHelper(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void performBackup(
            ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {
        if (!isOwner() || data == null) {
            Log.w(TAG, "ignore performBackup() for non-owner or empty data");
            return;
        }
        final List<String> allowlistedApps = getFullPowerList();
        if (allowlistedApps == null) {
            return;
        }

        writeBackupData(data, KEY_BUILD_BRAND, Build.BRAND);
        writeBackupData(data, KEY_BUILD_PRODUCT, Build.PRODUCT);
        writeBackupData(data, KEY_BUILD_MANUFACTURER, Build.MANUFACTURER);
        writeBackupData(data, KEY_BUILD_FINGERPRINT, Build.FINGERPRINT);
        // Add customized device build metadata fields.
        final PowerUsageFeatureProvider provider =
                FeatureFactory.getFeatureFactory().getPowerUsageFeatureProvider();
        writeBackupData(data, KEY_BUILD_METADATA_1, provider.getBuildMetadata1(mContext));
        writeBackupData(data, KEY_BUILD_METADATA_2, provider.getBuildMetadata2(mContext));

        backupOptimizationMode(data, allowlistedApps);
    }

    @Override
    public void restoreEntity(BackupDataInputStream data) {
        // Ensure we only verify the migrate configuration one time.
        if (!mVerifyMigrateConfiguration) {
            mVerifyMigrateConfiguration = true;
            BatterySettingsMigrateChecker.verifySaverConfiguration(mContext);
        }
        if (!isOwner() || data == null || data.size() == 0) {
            Log.w(TAG, "ignore restoreEntity() for non-owner or empty data");
            return;
        }
        final String dataKey = data.getKey();
        switch (dataKey) {
            case KEY_BUILD_BRAND:
            case KEY_BUILD_PRODUCT:
            case KEY_BUILD_MANUFACTURER:
            case KEY_BUILD_FINGERPRINT:
            case KEY_BUILD_METADATA_1:
            case KEY_BUILD_METADATA_2:
                restoreBackupData(dataKey, data);
                break;
            case KEY_OPTIMIZATION_LIST:
                // Hold the optimization mode data until all conditions are matched.
                mOptimizationModeBytes = getBackupData(dataKey, data);
                break;
        }
        performRestoreIfNeeded();
    }

    @Override
    public void writeNewStateDescription(ParcelFileDescriptor newState) {}

    private List<String> getFullPowerList() {
        final long timestamp = System.currentTimeMillis();
        String[] allowlistedApps;
        try {
            allowlistedApps = getIDeviceIdleController().getFullPowerWhitelist();
        } catch (RemoteException e) {
            Log.e(TAG, "backupFullPowerList() failed", e);
            return null;
        }
        // Ignores unexpected empty result case.
        if (allowlistedApps == null || allowlistedApps.length == 0) {
            Log.w(TAG, "no data found in the getFullPowerList()");
            return new ArrayList<>();
        }
        Log.d(
                TAG,
                String.format(
                        "getFullPowerList() size=%d in %d/ms",
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
        final SharedPreferences sharedPreferences = getSharedPreferences(mContext);
        // Converts application into the AppUsageState.
        for (ApplicationInfo info : applications) {
            final int mode = BatteryOptimizeUtils.getMode(appOps, info.uid, info.packageName);
            @BatteryOptimizeUtils.OptimizationMode
            final int optimizationMode =
                    BatteryOptimizeUtils.getAppOptimizationMode(
                            mode, allowlistedApps.contains(info.packageName));
            // Ignores default optimized/unknown state or system/default apps.
            if (optimizationMode == BatteryOptimizeUtils.MODE_OPTIMIZED
                    || optimizationMode == BatteryOptimizeUtils.MODE_UNKNOWN
                    || isSystemOrDefaultApp(info.packageName, info.uid)) {
                continue;
            }
            final String packageOptimizeMode = info.packageName + DELIMITER_MODE + optimizationMode;
            builder.append(packageOptimizeMode + DELIMITER);
            Log.d(TAG, "backupOptimizationMode: " + packageOptimizeMode);
            BatteryOptimizeLogUtils.writeLog(
                    sharedPreferences,
                    Action.BACKUP,
                    info.packageName,
                    /* actionDescription */ "mode: " + optimizationMode);
            backupCount++;
        }

        writeBackupData(data, KEY_OPTIMIZATION_LIST, builder.toString());
        Log.d(
                TAG,
                String.format(
                        "backup getInstalledApplications():%d count=%d in %d/ms",
                        applications.size(),
                        backupCount,
                        (System.currentTimeMillis() - timestamp)));
    }

    @VisibleForTesting
    int restoreOptimizationMode(byte[] dataBytes) {
        final long timestamp = System.currentTimeMillis();
        final String dataContent = new String(dataBytes, StandardCharsets.UTF_8);
        if (dataContent == null || dataContent.isEmpty()) {
            Log.w(TAG, "no data found in the restoreOptimizationMode()");
            return 0;
        }
        final String[] appConfigurations = dataContent.split(BatteryBackupHelper.DELIMITER);
        if (appConfigurations == null || appConfigurations.length == 0) {
            Log.w(TAG, "no data found from the split() processing");
            return 0;
        }
        int restoreCount = 0;
        for (int index = 0; index < appConfigurations.length; index++) {
            final String[] results =
                    appConfigurations[index].split(BatteryBackupHelper.DELIMITER_MODE);
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
                Log.e(TAG, "failed to parse the optimization mode: " + appConfigurations[index], e);
                continue;
            }
            restoreOptimizationMode(packageName, optimizationMode);
            restoreCount++;
        }
        Log.d(
                TAG,
                String.format(
                        "restoreOptimizationMode() count=%d in %d/ms",
                        restoreCount, (System.currentTimeMillis() - timestamp)));
        return restoreCount;
    }

    private void performRestoreIfNeeded() {
        if (mOptimizationModeBytes == null || mOptimizationModeBytes.length == 0) {
            return;
        }
        final PowerUsageFeatureProvider provider =
                FeatureFactory.getFeatureFactory().getPowerUsageFeatureProvider();
        if (!provider.isValidToRestoreOptimizationMode(mDeviceBuildInfoMap)) {
            return;
        }
        // Start to restore the app optimization mode data.
        final int restoreCount = restoreOptimizationMode(mOptimizationModeBytes);
        if (restoreCount > 0) {
            BatterySettingsMigrateChecker.verifyBatteryOptimizeModes(mContext);
        }
        mOptimizationModeBytes = null; // clear data
    }

    /** Dump the app optimization mode backup history data. */
    public static void dumpHistoricalData(Context context, PrintWriter writer) {
        BatteryOptimizeLogUtils.printBatteryOptimizeHistoricalLog(
                getSharedPreferences(context), writer);
    }

    static boolean isOwner() {
        return UserHandle.myUserId() == UserHandle.USER_SYSTEM;
    }

    static BatteryOptimizeUtils newBatteryOptimizeUtils(
            Context context, String packageName, BatteryOptimizeUtils testOptimizeUtils) {
        final int uid = BatteryUtils.getInstance(context).getPackageUid(packageName);
        if (uid == BatteryUtils.UID_NULL) {
            return null;
        }
        final BatteryOptimizeUtils batteryOptimizeUtils =
                testOptimizeUtils != null
                        ? testOptimizeUtils /*testing only*/
                        : new BatteryOptimizeUtils(context, uid, packageName);
        return batteryOptimizeUtils;
    }

    @VisibleForTesting
    static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(
                BATTERY_OPTIMIZE_BACKUP_FILE_NAME, Context.MODE_PRIVATE);
    }

    private void restoreOptimizationMode(
            String packageName, @BatteryOptimizeUtils.OptimizationMode int mode) {
        final BatteryOptimizeUtils batteryOptimizeUtils =
                newBatteryOptimizeUtils(mContext, packageName, mBatteryOptimizeUtils);
        if (batteryOptimizeUtils == null) {
            return;
        }
        batteryOptimizeUtils.setAppUsageState(
                mode, BatteryOptimizeHistoricalLogEntry.Action.RESTORE);
        Log.d(TAG, String.format("restore:%s mode=%d", packageName, mode));
    }

    // Provides an opportunity to inject mock IDeviceIdleController for testing.
    private IDeviceIdleController getIDeviceIdleController() {
        if (mIDeviceIdleController != null) {
            return mIDeviceIdleController;
        }
        mIDeviceIdleController =
                IDeviceIdleController.Stub.asInterface(
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
        return BatteryOptimizeUtils.isSystemOrDefaultApp(
                mContext, getPowerAllowlistBackend(), packageName, uid);
    }

    private ArraySet<ApplicationInfo> getInstalledApplications() {
        if (mTestApplicationInfoList != null) {
            return mTestApplicationInfoList;
        }
        return BatteryOptimizeUtils.getInstalledApplications(mContext, getIPackageManager());
    }

    private void restoreBackupData(String dataKey, BackupDataInputStream data) {
        final byte[] dataBytes = getBackupData(dataKey, data);
        if (dataBytes == null || dataBytes.length == 0) {
            return;
        }
        final String dataContent = new String(dataBytes, StandardCharsets.UTF_8);
        mDeviceBuildInfoMap.put(dataKey, dataContent);
        Log.d(TAG, String.format("restore:%s:%s", dataKey, dataContent));
    }

    private static byte[] getBackupData(String dataKey, BackupDataInputStream data) {
        final int dataSize = data.size();
        final byte[] dataBytes = new byte[dataSize];
        try {
            data.read(dataBytes, 0 /*offset*/, dataSize);
        } catch (IOException e) {
            Log.e(TAG, "failed to getBackupData() " + dataKey, e);
            return null;
        }
        return dataBytes;
    }

    private static void writeBackupData(BackupDataOutput data, String dataKey, String dataContent) {
        if (dataContent == null || dataContent.isEmpty()) {
            return;
        }
        final byte[] dataContentBytes = dataContent.getBytes();
        try {
            data.writeEntityHeader(dataKey, dataContentBytes.length);
            data.writeEntityData(dataContentBytes, dataContentBytes.length);
        } catch (IOException e) {
            Log.e(TAG, "writeBackupData() is failed for " + dataKey, e);
        }
        Log.d(TAG, String.format("backup:%s:%s", dataKey, dataContent));
    }
}
