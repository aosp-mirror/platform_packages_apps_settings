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
import android.os.BatteryStats;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseLongArray;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.util.ArrayUtils;
import com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper;
import com.android.settings.fuelgauge.batterytip.AnomalyInfo;
import com.android.settings.fuelgauge.batterytip.BatteryDatabaseManager;
import com.android.settings.fuelgauge.batterytip.StatsManagerConfig;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.fuelgauge.Estimate;
import com.android.settingslib.fuelgauge.EstimateKt;
import com.android.settingslib.fuelgauge.PowerWhitelistBackend;
import com.android.settingslib.utils.PowerUtil;
import com.android.settingslib.utils.ThreadUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utils for battery operation
 */
public class BatteryUtils {
    public static final int UID_NULL = -1;
    public static final int SDK_NULL = -1;

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

    private static final String TAG = "BatteryUtils";

    private static final int MIN_POWER_THRESHOLD_MILLI_AMP = 5;

    private static final int SECONDS_IN_HOUR = 60 * 60;
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
    BatteryUtils(Context context) {
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
     * Remove the {@link BatterySipper} that we should hide and smear the screen usage based on
     * foreground activity time.
     *
     * @param sippers sipper list that need to check and remove
     * @return the total power of the hidden items of {@link BatterySipper}
     * for proportional smearing
     */
    public double removeHiddenBatterySippers(List<BatterySipper> sippers) {
        double proportionalSmearPowerMah = 0;
        BatterySipper screenSipper = null;
        for (int i = sippers.size() - 1; i >= 0; i--) {
            final BatterySipper sipper = sippers.get(i);
            if (shouldHideSipper(sipper)) {
                sippers.remove(i);
                if (sipper.drainType != BatterySipper.DrainType.OVERCOUNTED
                        && sipper.drainType != BatterySipper.DrainType.SCREEN
                        && sipper.drainType != BatterySipper.DrainType.UNACCOUNTED
                        && sipper.drainType != BatterySipper.DrainType.BLUETOOTH
                        && sipper.drainType != BatterySipper.DrainType.WIFI
                        && sipper.drainType != BatterySipper.DrainType.IDLE
                        && !isHiddenSystemModule(sipper)) {
                    // Don't add it if it is overcounted, unaccounted, wifi, bluetooth, screen
                    // or hidden system modules
                    proportionalSmearPowerMah += sipper.totalPowerMah;
                }
            }

            if (sipper.drainType == BatterySipper.DrainType.SCREEN) {
                screenSipper = sipper;
            }
        }

        smearScreenBatterySipper(sippers, screenSipper);

        return proportionalSmearPowerMah;
    }

    /**
     * Smear the screen on power usage among {@code sippers}, based on ratio of foreground activity
     * time.
     */
    @VisibleForTesting
    void smearScreenBatterySipper(List<BatterySipper> sippers, BatterySipper screenSipper) {
        long totalActivityTimeMs = 0;
        final SparseLongArray activityTimeArray = new SparseLongArray();
        for (int i = 0, size = sippers.size(); i < size; i++) {
            final BatteryStats.Uid uid = sippers.get(i).uidObj;
            if (uid != null) {
                final long timeMs = getProcessTimeMs(StatusType.SCREEN_USAGE, uid,
                        BatteryStats.STATS_SINCE_CHARGED);
                activityTimeArray.put(uid.getUid(), timeMs);
                totalActivityTimeMs += timeMs;
            }
        }

        if (totalActivityTimeMs >= 10 * DateUtils.MINUTE_IN_MILLIS) {
            if (screenSipper == null) {
                Log.e(TAG, "screen sipper is null even when app screen time is not zero");
                return;
            }

            final double screenPowerMah = screenSipper.totalPowerMah;
            for (int i = 0, size = sippers.size(); i < size; i++) {
                final BatterySipper sipper = sippers.get(i);
                sipper.totalPowerMah += screenPowerMah * activityTimeArray.get(sipper.getUid(), 0)
                        / totalActivityTimeMs;
            }
        }
    }

    /**
     * Check whether we should hide the battery sipper.
     */
    public boolean shouldHideSipper(BatterySipper sipper) {
        final BatterySipper.DrainType drainType = sipper.drainType;

        return drainType == BatterySipper.DrainType.IDLE
                || drainType == BatterySipper.DrainType.CELL
                || drainType == BatterySipper.DrainType.SCREEN
                || drainType == BatterySipper.DrainType.UNACCOUNTED
                || drainType == BatterySipper.DrainType.OVERCOUNTED
                || drainType == BatterySipper.DrainType.BLUETOOTH
                || drainType == BatterySipper.DrainType.WIFI
                || (sipper.totalPowerMah * SECONDS_IN_HOUR) < MIN_POWER_THRESHOLD_MILLI_AMP
                || mPowerUsageFeatureProvider.isTypeService(sipper)
                || mPowerUsageFeatureProvider.isTypeSystem(sipper)
                || isHiddenSystemModule(sipper);
    }

    /**
     * Return {@code true} if one of packages in {@code sipper} is hidden system modules
     */
    public boolean isHiddenSystemModule(BatterySipper sipper) {
        if (sipper.uidObj == null) {
            return false;
        }
        sipper.mPackages = mPackageManager.getPackagesForUid(sipper.getUid());
        if (sipper.mPackages != null) {
            for (int i = 0, length = sipper.mPackages.length; i < length; i++) {
                if (AppUtils.isHiddenSystemModule(mContext, sipper.mPackages[i])) {
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
     * @param hiddenPowerMah  power used by no-actionable app that we want to hide, i.e. Screen,
     *                        Android OS.
     * @param dischargeAmount The discharge amount calculated by {@link BatteryStats}
     * @return A percentage value scaled by {@paramref dischargeAmount}
     * @see BatteryStats#getDischargeAmount(int)
     */
    public double calculateBatteryPercent(double powerUsageMah, double totalPowerMah,
            double hiddenPowerMah, int dischargeAmount) {
        if (totalPowerMah == 0) {
            return 0;
        }

        return (powerUsageMah / (totalPowerMah - hiddenPowerMah)) * dischargeAmount;
    }

    /**
     * Calculate the whole running time in the state {@code statsType}
     *
     * @param batteryStatsHelper utility class that contains the data
     * @param statsType          state that we want to calculate the time for
     * @return the running time in millis
     */
    public long calculateRunningTimeBasedOnStatsType(BatteryStatsHelper batteryStatsHelper,
            int statsType) {
        final long elapsedRealtimeUs = PowerUtil.convertMsToUs(
                SystemClock.elapsedRealtime());
        // Return the battery time (millisecond) on status mStatsType
        return PowerUtil.convertUsToMs(
                batteryStatsHelper.getStats().computeBatteryRealtime(elapsedRealtimeUs, statsType));

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
     * Sort the {@code usageList} based on {@link BatterySipper#totalPowerMah}
     */
    public void sortUsageList(List<BatterySipper> usageList) {
        Collections.sort(usageList, new Comparator<BatterySipper>() {
            @Override
            public int compare(BatterySipper a, BatterySipper b) {
                return Double.compare(b.totalPowerMah, a.totalPowerMah);
            }
        });
    }

    /**
     * Calculate the time since last full charge, including the device off time
     *
     * @param batteryStatsHelper utility class that contains the data
     * @param currentTimeMs      current wall time
     * @return time in millis
     */
    public long calculateLastFullChargeTime(BatteryStatsHelper batteryStatsHelper,
            long currentTimeMs) {
        return currentTimeMs - batteryStatsHelper.getStats().getStartClockTime();

    }

    /**
     * Calculate the screen usage time since last full charge.
     *
     * @param batteryStatsHelper utility class that contains the screen usage data
     * @return time in millis
     */
    public long calculateScreenUsageTime(BatteryStatsHelper batteryStatsHelper) {
        final BatterySipper sipper = findBatterySipperByType(
                batteryStatsHelper.getUsageList(), BatterySipper.DrainType.SCREEN);
        return sipper != null ? sipper.usageTimeMs : 0;
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

    public void initBatteryStatsHelper(BatteryStatsHelper statsHelper, Bundle bundle,
            UserManager userManager) {
        statsHelper.create(bundle);
        statsHelper.clearStats();
        statsHelper.refreshStats(BatteryStats.STATS_SINCE_CHARGED, userManager.getUserProfiles());
    }

    @WorkerThread
    public BatteryInfo getBatteryInfo(final BatteryStatsHelper statsHelper, final String tag) {
        final long startTime = System.currentTimeMillis();

        // Stuff we always need to get BatteryInfo
        final Intent batteryBroadcast = mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        final long elapsedRealtimeUs = PowerUtil.convertMsToUs(
                SystemClock.elapsedRealtime());
        final BatteryStats stats = statsHelper.getStats();
        BatteryInfo batteryInfo;
        Estimate estimate = getEnhancedEstimate();

        // couldn't get estimate from cache or provider, use fallback
        if (estimate == null) {
            estimate = new Estimate(
                    PowerUtil.convertUsToMs(stats.computeBatteryTimeRemaining(elapsedRealtimeUs)),
                    false /* isBasedOnUsage */,
                    EstimateKt.AVERAGE_TIME_TO_DISCHARGE_UNKNOWN);
        }

        BatteryUtils.logRuntime(tag, "BatteryInfoLoader post query", startTime);
        batteryInfo = BatteryInfo.getBatteryInfo(mContext, batteryBroadcast, stats,
                estimate, elapsedRealtimeUs, false /* shortString */);
        BatteryUtils.logRuntime(tag, "BatteryInfoLoader.loadInBackground", startTime);

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

    /**
     * Find the {@link BatterySipper} with the corresponding {@link BatterySipper.DrainType}
     */
    public BatterySipper findBatterySipperByType(List<BatterySipper> usageList,
            BatterySipper.DrainType type) {
        for (int i = 0, size = usageList.size(); i < size; i++) {
            final BatterySipper sipper = usageList.get(i);
            if (sipper.drainType == type) {
                return sipper;
            }
        }
        return null;
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
    public boolean shouldHideAnomaly(PowerWhitelistBackend powerWhitelistBackend, int uid,
            AnomalyInfo anomalyInfo) {
        final String[] packageNames = mPackageManager.getPackagesForUid(uid);
        if (ArrayUtils.isEmpty(packageNames)) {
            // Don't show it if app has been uninstalled
            return true;
        }

        return isSystemUid(uid) || powerWhitelistBackend.isWhitelisted(packageNames)
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
}

