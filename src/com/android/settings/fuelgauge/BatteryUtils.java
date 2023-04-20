/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.BatteryConsumer;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.BatteryStatsManager;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.os.UidBatteryConsumer;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.android.internal.util.ArrayUtils;
import com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper;
import com.android.settings.fuelgauge.batterytip.AnomalyInfo;
import com.android.settings.fuelgauge.batterytip.BatteryDatabaseManager;
import com.android.settings.fuelgauge.batterytip.StatsManagerConfig;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.fuelgauge.Estimate;
import com.android.settingslib.fuelgauge.EstimateKt;
import com.android.settingslib.fuelgauge.PowerAllowlistBackend;
import com.android.settingslib.utils.PowerUtil;
import com.android.settingslib.utils.ThreadUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Utils for battery operation
 */
public class BatteryUtils {
    public static final int UID_NULL = -1;
    public static final int SDK_NULL = -1;
    /** Special UID value for data usage by removed apps. */
    public static final int UID_REMOVED_APPS = -4;
    /** Special UID value for data usage by tethering. */
    public static final int UID_TETHERING = -5;
    /** Special UID for aggregated other users. */
    public static final long UID_OTHER_USERS = Long.MIN_VALUE;

    /** Flag to check if the dock defender mode has been temporarily bypassed */
    public static final String SETTINGS_GLOBAL_DOCK_DEFENDER_BYPASS = "dock_defender_bypass";

    public static final String BYPASS_DOCK_DEFENDER_ACTION = "battery.dock.defender.bypass";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({StatusType.SCREEN_USAGE,
            StatusType.FOREGROUND,
            StatusType.BACKGROUND,
            StatusType.ALL
    })
    public @interface StatusType {
        int SCREEN_USAGE = 0;
        int FOREGROUND = 1;
        int BACKGROUND = 2;
        int ALL = 3;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DockDefenderMode.FUTURE_BYPASS,
            DockDefenderMode.ACTIVE,
            DockDefenderMode.TEMPORARILY_BYPASSED,
            DockDefenderMode.DISABLED})
    public @interface DockDefenderMode {
        int FUTURE_BYPASS = 0;
        int ACTIVE = 1;
        int TEMPORARILY_BYPASSED = 2;
        int DISABLED = 3;
    }

    private static final String TAG = "BatteryUtils";

    private static BatteryUtils sInstance;
    private PackageManager mPackageManager;

    private AppOpsManager mAppOpsManager;
    private Context mContext;
    @VisibleForTesting
    PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    public static BatteryUtils getInstance(Context context) {
        if (sInstance == null || sInstance.isDataCorrupted()) {
            sInstance = new BatteryUtils(context.getApplicationContext());
        }
        return sInstance;
    }

    @VisibleForTesting
    public BatteryUtils(Context context) {
        mContext = context;
        mPackageManager = context.getPackageManager();
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        mPowerUsageFeatureProvider = FeatureFactory.getFactory(context)
                .getPowerUsageFeatureProvider(context);
    }

    public long getProcessTimeMs(@StatusType int type, @Nullable BatteryStats.Uid uid,
            int which) {
        if (uid == null) {
            return 0;
        }

        switch (type) {
            case StatusType.SCREEN_USAGE:
                return getScreenUsageTimeMs(uid, which);
            case StatusType.FOREGROUND:
                return getProcessForegroundTimeMs(uid, which);
            case StatusType.BACKGROUND:
                return getProcessBackgroundTimeMs(uid, which);
            case StatusType.ALL:
                return getProcessForegroundTimeMs(uid, which)
                        + getProcessBackgroundTimeMs(uid, which);
        }
        return 0;
    }

    private long getScreenUsageTimeMs(BatteryStats.Uid uid, int which, long rawRealTimeUs) {
        final int foregroundTypes[] = {BatteryStats.Uid.PROCESS_STATE_TOP};
        Log.v(TAG, "package: " + mPackageManager.getNameForUid(uid.getUid()));

        long timeUs = 0;
        for (int type : foregroundTypes) {
            final long localTime = uid.getProcessStateTime(type, rawRealTimeUs, which);
            Log.v(TAG, "type: " + type + " time(us): " + localTime);
            timeUs += localTime;
        }
        Log.v(TAG, "foreground time(us): " + timeUs);

        // Return the min value of STATE_TOP time and foreground activity time, since both of these
        // time have some errors
        return PowerUtil.convertUsToMs(
                Math.min(timeUs, getForegroundActivityTotalTimeUs(uid, rawRealTimeUs)));
    }

    private long getScreenUsageTimeMs(BatteryStats.Uid uid, int which) {
        final long rawRealTimeUs = PowerUtil.convertMsToUs(SystemClock.elapsedRealtime());
        return getScreenUsageTimeMs(uid, which, rawRealTimeUs);
    }

    private long getProcessBackgroundTimeMs(BatteryStats.Uid uid, int which) {
        final long rawRealTimeUs = PowerUtil.convertMsToUs(SystemClock.elapsedRealtime());
        final long timeUs = uid.getProcessStateTime(
                BatteryStats.Uid.PROCESS_STATE_BACKGROUND, rawRealTimeUs, which);

        Log.v(TAG, "package: " + mPackageManager.getNameForUid(uid.getUid()));
        Log.v(TAG, "background time(us): " + timeUs);
        return PowerUtil.convertUsToMs(timeUs);
    }

    private long getProcessForegroundTimeMs(BatteryStats.Uid uid, int which) {
        final long rawRealTimeUs = PowerUtil.convertMsToUs(SystemClock.elapsedRealtime());
        return getScreenUsageTimeMs(uid, which, rawRealTimeUs)
                + PowerUtil.convertUsToMs(getForegroundServiceTotalTimeUs(uid, rawRealTimeUs));
    }

    /**
     * Returns true if the specified battery consumer should be excluded from the summary
     * battery consumption list.
     */
    public boolean shouldHideUidBatteryConsumer(UidBatteryConsumer consumer) {
        return shouldHideUidBatteryConsumer(consumer,
                mPackageManager.getPackagesForUid(consumer.getUid()));
    }

    /**
     * Returns true if the specified battery consumer should be excluded from the summary
     * battery consumption list.
     */
    public boolean shouldHideUidBatteryConsumer(UidBatteryConsumer consumer, String[] packages) {
        return mPowerUsageFeatureProvider.isTypeSystem(consumer.getUid(), packages)
                || shouldHideUidBatteryConsumerUnconditionally(consumer, packages);
    }

    /**
     * Returns true if the specified battery consumer should be excluded from
     * battery consumption lists, either short or full.
     */
    public boolean shouldHideUidBatteryConsumerUnconditionally(UidBatteryConsumer consumer,
            String[] packages) {
        final int uid = consumer.getUid();
        return uid == UID_TETHERING
                ? false
                : uid < 0 || isHiddenSystemModule(packages);
    }

    /**
     * Returns true if the specified device power component should be excluded from the summary
     * battery consumption list.
     */
    public boolean shouldHideDevicePowerComponent(BatteryConsumer consumer,
            @BatteryConsumer.PowerComponent int powerComponentId) {
        switch (powerComponentId) {
            case BatteryConsumer.POWER_COMPONENT_IDLE:
            case BatteryConsumer.POWER_COMPONENT_MOBILE_RADIO:
            case BatteryConsumer.POWER_COMPONENT_SCREEN:
            case BatteryConsumer.POWER_COMPONENT_BLUETOOTH:
            case BatteryConsumer.POWER_COMPONENT_WIFI:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns true if one the specified packages belongs to a hidden system module.
     */
    public boolean isHiddenSystemModule(String[] packages) {
        if (packages != null) {
            for (int i = 0, length = packages.length; i < length; i++) {
                if (AppUtils.isHiddenSystemModule(mContext, packages[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Calculate the power usage percentage for an app
     *
     * @param powerUsageMah   power used by the app
     * @param totalPowerMah   total power used in the system
     * @param dischargeAmount The discharge amount calculated by {@link BatteryStats}
     * @return A percentage value scaled by {@paramref dischargeAmount}
     * @see BatteryStats#getDischargeAmount(int)
     */
    public double calculateBatteryPercent(double powerUsageMah, double totalPowerMah,
            int dischargeAmount) {
        if (totalPowerMah == 0) {
            return 0;
        }

        return (powerUsageMah / totalPowerMah) * dischargeAmount;
    }

    /**
     * Find the package name for a {@link android.os.BatteryStats.Uid}
     *
     * @param uid id to get the package name
     * @return the package name. If there are multiple packages related to
     * given id, return the first one. Or return null if there are no known
     * packages with the given id
     * @see PackageManager#getPackagesForUid(int)
     */
    public String getPackageName(int uid) {
        final String[] packageNames = mPackageManager.getPackagesForUid(uid);

        return ArrayUtils.isEmpty(packageNames) ? null : packageNames[0];
    }

    /**
     * Find the targetSdkVersion for package with name {@code packageName}
     *
     * @return the targetSdkVersion, or {@link #SDK_NULL} if {@code packageName} doesn't exist
     */
    public int getTargetSdkVersion(final String packageName) {
        try {
            ApplicationInfo info = mPackageManager.getApplicationInfo(packageName,
                    PackageManager.GET_META_DATA);

            return info.targetSdkVersion;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot find package: " + packageName, e);
        }

        return SDK_NULL;
    }

    /**
     * Check whether background restriction is enabled
     */
    public boolean isBackgroundRestrictionEnabled(final int targetSdkVersion, final int uid,
            final String packageName) {
        if (targetSdkVersion >= Build.VERSION_CODES.O) {
            return true;
        }
        final int mode = mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_IN_BACKGROUND, uid, packageName);
        return mode == AppOpsManager.MODE_IGNORED || mode == AppOpsManager.MODE_ERRORED;
    }

    /**
     * Calculate the time since last full charge, including the device off time
     *
     * @param batteryUsageStats  class that contains the data
     * @param currentTimeMs      current wall time
     * @return time in millis
     */
    public long calculateLastFullChargeTime(BatteryUsageStats batteryUsageStats,
            long currentTimeMs) {
        return currentTimeMs - batteryUsageStats.getStatsStartTimestamp();
    }

    public static void logRuntime(String tag, String message, long startTime) {
        Log.d(tag, message + ": " + (System.currentTimeMillis() - startTime) + "ms");
    }

    /**
     * Return {@code true} if battery is overheated and charging.
     */
    public static boolean isBatteryDefenderOn(BatteryInfo batteryInfo) {
        return batteryInfo.isOverheated && !batteryInfo.discharging;
    }

    /**
     * Find package uid from package name
     *
     * @param packageName used to find the uid
     * @return uid for packageName, or {@link #UID_NULL} if exception happens or
     * {@code packageName} is null
     */
    public int getPackageUid(String packageName) {
        try {
            return packageName == null ? UID_NULL : mPackageManager.getPackageUid(packageName,
                    PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return UID_NULL;
        }
    }

    public void setForceAppStandby(int uid, String packageName,
            int mode) {
        final boolean isPreOApp = isPreOApp(packageName);
        if (isPreOApp) {
            // Control whether app could run in the background if it is pre O app
            mAppOpsManager.setMode(AppOpsManager.OP_RUN_IN_BACKGROUND, uid, packageName, mode);
        }
        // Control whether app could run jobs in the background
        mAppOpsManager.setMode(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, uid, packageName, mode);

        ThreadUtils.postOnBackgroundThread(() -> {
            final BatteryDatabaseManager batteryDatabaseManager = BatteryDatabaseManager
                    .getInstance(mContext);
            if (mode == AppOpsManager.MODE_IGNORED) {
                batteryDatabaseManager.insertAction(AnomalyDatabaseHelper.ActionType.RESTRICTION,
                        uid, packageName, System.currentTimeMillis());
            } else if (mode == AppOpsManager.MODE_ALLOWED) {
                batteryDatabaseManager.deleteAction(AnomalyDatabaseHelper.ActionType.RESTRICTION,
                        uid, packageName);
            }
        });
    }

    public boolean isForceAppStandbyEnabled(int uid, String packageName) {
        return mAppOpsManager.checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, uid,
                packageName) == AppOpsManager.MODE_IGNORED;
    }

    public boolean clearForceAppStandby(String packageName) {
        final int uid = getPackageUid(packageName);
        if (uid != UID_NULL && isForceAppStandbyEnabled(uid, packageName)) {
            setForceAppStandby(uid, packageName, AppOpsManager.MODE_ALLOWED);
            return true;
        } else {
            return false;
        }
    }

    @WorkerThread
    public BatteryInfo getBatteryInfo(final String tag) {
        final BatteryStatsManager systemService = mContext.getSystemService(
                BatteryStatsManager.class);
        BatteryUsageStats batteryUsageStats;
        try {
            batteryUsageStats = systemService.getBatteryUsageStats(
                    new BatteryUsageStatsQuery.Builder().includeBatteryHistory().build());
        } catch (RuntimeException e) {
            Log.e(TAG, "getBatteryInfo() error from getBatteryUsageStats()", e);
            // Use default BatteryUsageStats.
            batteryUsageStats = new BatteryUsageStats.Builder(new String[0]).build();
        }

        final long startTime = System.currentTimeMillis();

        // Stuff we always need to get BatteryInfo
        final Intent batteryBroadcast = mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        final long elapsedRealtimeUs = PowerUtil.convertMsToUs(
                SystemClock.elapsedRealtime());

        BatteryInfo batteryInfo;
        Estimate estimate = getEnhancedEstimate();

        // couldn't get estimate from cache or provider, use fallback
        if (estimate == null) {
            estimate = new Estimate(
                    batteryUsageStats.getBatteryTimeRemainingMs(),
                    false /* isBasedOnUsage */,
                    EstimateKt.AVERAGE_TIME_TO_DISCHARGE_UNKNOWN);
        }

        BatteryUtils.logRuntime(tag, "BatteryInfoLoader post query", startTime);
        batteryInfo = BatteryInfo.getBatteryInfo(mContext, batteryBroadcast,
                batteryUsageStats, estimate, elapsedRealtimeUs, false /* shortString */);
        BatteryUtils.logRuntime(tag, "BatteryInfoLoader.loadInBackground", startTime);

        try {
            batteryUsageStats.close();
        } catch (Exception e) {
            Log.e(TAG, "BatteryUsageStats.close() failed", e);
        }
        return batteryInfo;
    }

    @VisibleForTesting
    Estimate getEnhancedEstimate() {
        Estimate estimate = null;
        // Get enhanced prediction if available
        if (Duration.between(Estimate.getLastCacheUpdateTime(mContext), Instant.now())
                .compareTo(Duration.ofSeconds(10)) < 0) {
            estimate = Estimate.getCachedEstimateIfAvailable(mContext);
        } else if (mPowerUsageFeatureProvider != null &&
                mPowerUsageFeatureProvider.isEnhancedBatteryPredictionEnabled(mContext)) {
            estimate = mPowerUsageFeatureProvider.getEnhancedBatteryPrediction(mContext);
            if (estimate != null) {
                Estimate.storeCachedEstimate(mContext, estimate);
            }
        }
        return estimate;
    }

    private boolean isDataCorrupted() {
        return mPackageManager == null || mAppOpsManager == null;
    }

    @VisibleForTesting
    long getForegroundActivityTotalTimeUs(BatteryStats.Uid uid, long rawRealtimeUs) {
        final BatteryStats.Timer timer = uid.getForegroundActivityTimer();
        if (timer != null) {
            return timer.getTotalTimeLocked(rawRealtimeUs, BatteryStats.STATS_SINCE_CHARGED);
        }

        return 0;
    }

    @VisibleForTesting
    long getForegroundServiceTotalTimeUs(BatteryStats.Uid uid, long rawRealtimeUs) {
        final BatteryStats.Timer timer = uid.getForegroundServiceTimer();
        if (timer != null) {
            return timer.getTotalTimeLocked(rawRealtimeUs, BatteryStats.STATS_SINCE_CHARGED);
        }

        return 0;
    }

    public boolean isPreOApp(final String packageName) {
        try {
            ApplicationInfo info = mPackageManager.getApplicationInfo(packageName,
                    PackageManager.GET_META_DATA);

            return info.targetSdkVersion < Build.VERSION_CODES.O;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot find package: " + packageName, e);
        }

        return false;
    }

    public boolean isPreOApp(final String[] packageNames) {
        if (ArrayUtils.isEmpty(packageNames)) {
            return false;
        }

        for (String packageName : packageNames) {
            if (isPreOApp(packageName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return {@code true} if we should hide anomaly app represented by {@code uid}
     */
    public boolean shouldHideAnomaly(PowerAllowlistBackend powerAllowlistBackend, int uid,
            AnomalyInfo anomalyInfo) {
        final String[] packageNames = mPackageManager.getPackagesForUid(uid);
        if (ArrayUtils.isEmpty(packageNames)) {
            // Don't show it if app has been uninstalled
            return true;
        }

        return isSystemUid(uid) || powerAllowlistBackend.isAllowlisted(packageNames)
                || (isSystemApp(mPackageManager, packageNames) && !hasLauncherEntry(packageNames))
                || (isExcessiveBackgroundAnomaly(anomalyInfo) && !isPreOApp(packageNames));
    }

    private boolean isExcessiveBackgroundAnomaly(AnomalyInfo anomalyInfo) {
        return anomalyInfo.anomalyType
                == StatsManagerConfig.AnomalyType.EXCESSIVE_BACKGROUND_SERVICE;
    }

    private boolean isSystemUid(int uid) {
        final int appUid = UserHandle.getAppId(uid);
        return appUid >= Process.ROOT_UID && appUid < Process.FIRST_APPLICATION_UID;
    }

    private boolean isSystemApp(PackageManager packageManager, String[] packageNames) {
        for (String packageName : packageNames) {
            try {
                final ApplicationInfo info = packageManager.getApplicationInfo(packageName,
                        0 /* flags */);
                if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    return true;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Package not found: " + packageName, e);
            }
        }

        return false;
    }

    private boolean hasLauncherEntry(String[] packageNames) {
        final Intent launchIntent = new Intent(Intent.ACTION_MAIN, null);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        // If we do not specify MATCH_DIRECT_BOOT_AWARE or
        // MATCH_DIRECT_BOOT_UNAWARE, system will derive and update the flags
        // according to the user's lock state. When the user is locked,
        // components
        // with ComponentInfo#directBootAware == false will be filtered. We should
        // explicitly include both direct boot aware and unaware components here.
        final List<ResolveInfo> resolveInfos = mPackageManager.queryIntentActivities(launchIntent,
                PackageManager.MATCH_DISABLED_COMPONENTS
                        | PackageManager.MATCH_DIRECT_BOOT_AWARE
                        | PackageManager.MATCH_DIRECT_BOOT_UNAWARE
                        | PackageManager.MATCH_SYSTEM_ONLY);
        for (int i = 0, size = resolveInfos.size(); i < size; i++) {
            final ResolveInfo resolveInfo = resolveInfos.get(i);
            if (ArrayUtils.contains(packageNames, resolveInfo.activityInfo.packageName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return version number of an app represented by {@code packageName}, and return -1 if not
     * found.
     */
    public long getAppLongVersionCode(String packageName) {
        try {
            final PackageInfo packageInfo = mPackageManager.getPackageInfo(packageName,
                    0 /* flags */);
            return packageInfo.getLongVersionCode();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot find package: " + packageName, e);
        }

        return -1L;
    }

    /** Gets the current dock defender mode */
    public static int getCurrentDockDefenderMode(Context context, BatteryInfo batteryInfo) {
        if (batteryInfo.pluggedStatus == BatteryManager.BATTERY_PLUGGED_DOCK) {
            if (Settings.Global.getInt(context.getContentResolver(),
                    SETTINGS_GLOBAL_DOCK_DEFENDER_BYPASS, 0) == 1) {
                return DockDefenderMode.TEMPORARILY_BYPASSED;
            } else if (batteryInfo.isOverheated && FeatureFactory.getFactory(context)
                    .getPowerUsageFeatureProvider(context)
                    .isExtraDefend()) {
                return DockDefenderMode.ACTIVE;
            } else if (!batteryInfo.isOverheated) {
                return DockDefenderMode.FUTURE_BYPASS;
            }
        }
        return DockDefenderMode.DISABLED;
    }
}
