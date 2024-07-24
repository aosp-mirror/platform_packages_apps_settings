/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.IDeviceIdleController;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.datastore.BackupContext;
import com.android.settingslib.datastore.BackupRestoreEntity;
import com.android.settingslib.datastore.BackupRestoreStorageManager;
import com.android.settingslib.datastore.EntityBackupResult;
import com.android.settingslib.datastore.ObservableBackupRestoreStorage;
import com.android.settingslib.datastore.RestoreContext;
import com.android.settingslib.fuelgauge.PowerAllowlistBackend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** An implementation to backup and restore battery configurations. */
public final class BatterySettingsStorage extends ObservableBackupRestoreStorage {
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

    @Nullable private byte[] mOptimizationModeBytes;

    private final Application mApplication;
    // Device information map from restore.
    private final ArrayMap<String, String> mDeviceBuildInfoMap =
            new ArrayMap<>(DEVICE_BUILD_INFO_SIZE);

    /**
     * Returns the {@link BatterySettingsStorage} registered to {@link BackupRestoreStorageManager}.
     */
    public static @NonNull BatterySettingsStorage get(@NonNull Context context) {
        return (BatterySettingsStorage)
                BackupRestoreStorageManager.getInstance(context).getOrThrow(TAG);
    }

    public BatterySettingsStorage(@NonNull Context context) {
        mApplication = (Application) context.getApplicationContext();
    }

    @NonNull
    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public boolean enableBackup(@NonNull BackupContext backupContext) {
        return isOwner();
    }

    @Override
    public boolean enableRestore() {
        return isOwner();
    }

    static boolean isOwner() {
        return UserHandle.myUserId() == UserHandle.USER_SYSTEM;
    }

    @NonNull
    @Override
    public List<BackupRestoreEntity> createBackupRestoreEntities() {
        List<String> allowlistedApps = getFullPowerList();
        if (allowlistedApps == null) {
            return Collections.emptyList();
        }
        PowerUsageFeatureProvider provider =
                FeatureFactory.getFeatureFactory().getPowerUsageFeatureProvider();
        return Arrays.asList(
                new StringEntity(KEY_BUILD_BRAND, Build.BRAND),
                new StringEntity(KEY_BUILD_PRODUCT, Build.PRODUCT),
                new StringEntity(KEY_BUILD_MANUFACTURER, Build.MANUFACTURER),
                new StringEntity(KEY_BUILD_FINGERPRINT, Build.FINGERPRINT),
                new StringEntity(KEY_BUILD_METADATA_1, provider.getBuildMetadata1(mApplication)),
                new StringEntity(KEY_BUILD_METADATA_2, provider.getBuildMetadata2(mApplication)),
                new OptimizationModeEntity(allowlistedApps));
    }

    private @Nullable List<String> getFullPowerList() {
        final long timestamp = System.currentTimeMillis();
        String[] allowlistedApps;
        try {
            IDeviceIdleController deviceIdleController =
                    IDeviceIdleController.Stub.asInterface(
                            ServiceManager.getService(DEVICE_IDLE_SERVICE));
            allowlistedApps = deviceIdleController.getFullPowerWhitelist();
        } catch (RemoteException e) {
            Log.e(TAG, "backupFullPowerList() failed", e);
            return null;
        }
        // Ignores unexpected empty result case.
        if (allowlistedApps == null || allowlistedApps.length == 0) {
            Log.w(TAG, "no data found in the getFullPowerList()");
            return Collections.emptyList();
        }
        Log.d(
                TAG,
                String.format(
                        "getFullPowerList() size=%d in %d/ms",
                        allowlistedApps.length, (System.currentTimeMillis() - timestamp)));
        return Arrays.asList(allowlistedApps);
    }

    @Override
    public void writeNewStateDescription(@NonNull ParcelFileDescriptor newState) {
        BatterySettingsMigrateChecker.verifySaverConfiguration(mApplication);
        performRestoreIfNeeded();
    }

    private void performRestoreIfNeeded() {
        byte[] bytes = mOptimizationModeBytes;
        mOptimizationModeBytes = null; // clear data
        if (bytes == null || bytes.length == 0) {
            return;
        }
        final PowerUsageFeatureProvider provider =
                FeatureFactory.getFeatureFactory().getPowerUsageFeatureProvider();
        if (!provider.isValidToRestoreOptimizationMode(mDeviceBuildInfoMap)) {
            return;
        }
        // Start to restore the app optimization mode data.
        final int restoreCount = restoreOptimizationMode(bytes);
        if (restoreCount > 0) {
            BatterySettingsMigrateChecker.verifyBatteryOptimizeModes(mApplication);
        }
    }

    int restoreOptimizationMode(byte[] dataBytes) {
        final long timestamp = System.currentTimeMillis();
        final String dataContent = new String(dataBytes, UTF_8);
        if (dataContent.isEmpty()) {
            Log.w(TAG, "no data found in the restoreOptimizationMode()");
            return 0;
        }
        final String[] appConfigurations = dataContent.split(BatteryBackupHelper.DELIMITER);
        if (appConfigurations.length == 0) {
            Log.w(TAG, "no data found from the split() processing");
            return 0;
        }
        int restoreCount = 0;
        for (String appConfiguration : appConfigurations) {
            final String[] results = appConfiguration.split(BatteryBackupHelper.DELIMITER_MODE);
            // Example format: com.android.systemui:2 we should have length=2
            if (results.length != 2) {
                Log.w(TAG, "invalid raw data found:" + appConfiguration);
                continue;
            }
            final String packageName = results[0];
            final int uid = BatteryUtils.getInstance(mApplication).getPackageUid(packageName);
            // Ignores system/default apps.
            if (isSystemOrDefaultApp(packageName, uid)) {
                Log.w(TAG, "ignore from isSystemOrDefaultApp():" + packageName);
                continue;
            }
            @BatteryOptimizeUtils.OptimizationMode int optimizationMode;
            try {
                optimizationMode = Integer.parseInt(results[1]);
            } catch (NumberFormatException e) {
                Log.e(TAG, "failed to parse the optimization mode: " + appConfiguration, e);
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

    private void restoreOptimizationMode(
            String packageName, @BatteryOptimizeUtils.OptimizationMode int mode) {
        final BatteryOptimizeUtils batteryOptimizeUtils =
                newBatteryOptimizeUtils(mApplication, packageName);
        if (batteryOptimizeUtils == null) {
            return;
        }
        batteryOptimizeUtils.setAppUsageState(
                mode, BatteryOptimizeHistoricalLogEntry.Action.RESTORE);
        Log.d(TAG, String.format("restore:%s mode=%d", packageName, mode));
    }

    @Nullable
    static BatteryOptimizeUtils newBatteryOptimizeUtils(Context context, String packageName) {
        final int uid = BatteryUtils.getInstance(context).getPackageUid(packageName);
        return uid == BatteryUtils.UID_NULL
                ? null
                : new BatteryOptimizeUtils(context, uid, packageName);
    }

    private boolean isSystemOrDefaultApp(String packageName, int uid) {
        return BatteryOptimizeUtils.isSystemOrDefaultApp(
                mApplication, PowerAllowlistBackend.getInstance(mApplication), packageName, uid);
    }

    private class StringEntity implements BackupRestoreEntity {
        private final String mKey;
        private final String mValue;

        StringEntity(String key, String value) {
            this.mKey = key;
            this.mValue = value;
        }

        @NonNull
        @Override
        public String getKey() {
            return mKey;
        }

        @Override
        public @NonNull EntityBackupResult backup(
                @NonNull BackupContext backupContext, @NonNull OutputStream outputStream)
                throws IOException {
            Log.d(TAG, String.format("backup:%s:%s", mKey, mValue));
            outputStream.write(mValue.getBytes(UTF_8));
            return EntityBackupResult.UPDATE;
        }

        @Override
        public void restore(
                @NonNull RestoreContext restoreContext, @NonNull InputStream inputStream)
                throws IOException {
            String dataContent = new String(inputStream.readAllBytes(), UTF_8);
            mDeviceBuildInfoMap.put(mKey, dataContent);
            Log.d(TAG, String.format("restore:%s:%s", mKey, dataContent));
        }
    }

    private class OptimizationModeEntity implements BackupRestoreEntity {
        private final List<String> mAllowlistedApps;

        private OptimizationModeEntity(List<String> allowlistedApps) {
            this.mAllowlistedApps = allowlistedApps;
        }

        @NonNull
        @Override
        public String getKey() {
            return KEY_OPTIMIZATION_LIST;
        }

        @Override
        public @NonNull EntityBackupResult backup(
                @NonNull BackupContext backupContext, @NonNull OutputStream outputStream)
                throws IOException {
            final long timestamp = System.currentTimeMillis();
            final ArraySet<ApplicationInfo> applications = getInstalledApplications();
            if (applications == null || applications.isEmpty()) {
                Log.w(TAG, "no data found in the getInstalledApplications()");
                return EntityBackupResult.DELETE;
            }
            int backupCount = 0;
            final StringBuilder builder = new StringBuilder();
            final AppOpsManager appOps = mApplication.getSystemService(AppOpsManager.class);
            final SharedPreferences sharedPreferences = getSharedPreferences(mApplication);
            // Converts application into the AppUsageState.
            for (ApplicationInfo info : applications) {
                final int mode = BatteryOptimizeUtils.getMode(appOps, info.uid, info.packageName);
                @BatteryOptimizeUtils.OptimizationMode
                final int optimizationMode =
                        BatteryOptimizeUtils.getAppOptimizationMode(
                                mode, mAllowlistedApps.contains(info.packageName));
                // Ignores default optimized/unknown state or system/default apps.
                if (optimizationMode == BatteryOptimizeUtils.MODE_OPTIMIZED
                        || optimizationMode == BatteryOptimizeUtils.MODE_UNKNOWN
                        || isSystemOrDefaultApp(info.packageName, info.uid)) {
                    continue;
                }
                final String packageOptimizeMode =
                        info.packageName + DELIMITER_MODE + optimizationMode;
                builder.append(packageOptimizeMode).append(DELIMITER);
                Log.d(TAG, "backupOptimizationMode: " + packageOptimizeMode);
                BatteryOptimizeLogUtils.writeLog(
                        sharedPreferences,
                        Action.BACKUP,
                        info.packageName,
                        /* actionDescription */ "mode: " + optimizationMode);
                backupCount++;
            }

            outputStream.write(builder.toString().getBytes(UTF_8));
            Log.d(
                    TAG,
                    String.format(
                            "backup getInstalledApplications():%d count=%d in %d/ms",
                            applications.size(),
                            backupCount,
                            (System.currentTimeMillis() - timestamp)));
            return EntityBackupResult.UPDATE;
        }

        private @Nullable ArraySet<ApplicationInfo> getInstalledApplications() {
            return BatteryOptimizeUtils.getInstalledApplications(
                    mApplication, AppGlobals.getPackageManager());
        }

        static @NonNull SharedPreferences getSharedPreferences(Context context) {
            return context.getSharedPreferences(
                    BATTERY_OPTIMIZE_BACKUP_FILE_NAME, Context.MODE_PRIVATE);
        }

        @Override
        public void restore(
                @NonNull RestoreContext restoreContext, @NonNull InputStream inputStream)
                throws IOException {
            mOptimizationModeBytes = inputStream.readAllBytes();
        }
    }
}
