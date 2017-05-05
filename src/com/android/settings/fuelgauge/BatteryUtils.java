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

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.BatteryStats;
import android.os.SystemClock;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.android.internal.os.BatterySipper;
import com.android.settings.overlay.FeatureFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Utils for battery operation
 */
public class BatteryUtils {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({StatusType.FOREGROUND,
            StatusType.BACKGROUND,
            StatusType.ALL
    })
    public @interface StatusType {
        int FOREGROUND = 0;
        int BACKGROUND = 1;
        int ALL = 2;
    }

    private static final String TAG = "BatteryUtils";

    private static final int MIN_POWER_THRESHOLD_MILLI_AMP = 5;
    private static final int SECONDS_IN_HOUR = 60 * 60;
    private static BatteryUtils sInstance;

    private PackageManager mPackageManager;
    @VisibleForTesting
    PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    public static BatteryUtils getInstance(Context context) {
        if (sInstance == null || sInstance.isDataCorrupted()) {
            sInstance = new BatteryUtils(context);
        }
        return sInstance;
    }

    @VisibleForTesting
    BatteryUtils(Context context) {
        mPackageManager = context.getPackageManager();
        mPowerUsageFeatureProvider = FeatureFactory.getFactory(
                context).getPowerUsageFeatureProvider(context);
    }

    public long getProcessTimeMs(@StatusType int type, @Nullable BatteryStats.Uid uid,
            int which) {
        if (uid == null) {
            return 0;
        }

        switch (type) {
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

    private long getProcessBackgroundTimeMs(BatteryStats.Uid uid, int which) {
        final long rawRealTimeUs = convertMsToUs(SystemClock.elapsedRealtime());
        final long timeUs = uid.getProcessStateTime(
                BatteryStats.Uid.PROCESS_STATE_BACKGROUND, rawRealTimeUs, which);

        Log.v(TAG, "package: " + mPackageManager.getNameForUid(uid.getUid()));
        Log.v(TAG, "background time(us): " + timeUs);
        return convertUsToMs(timeUs);
    }

    private long getProcessForegroundTimeMs(BatteryStats.Uid uid, int which) {
        final long rawRealTimeUs = convertMsToUs(SystemClock.elapsedRealtime());
        final int foregroundTypes[] = {BatteryStats.Uid.PROCESS_STATE_TOP};
        Log.v(TAG, "package: " + mPackageManager.getNameForUid(uid.getUid()));

        long timeUs = 0;
        for (int type : foregroundTypes) {
            final long localTime = uid.getProcessStateTime(type, rawRealTimeUs, which);
            Log.v(TAG, "type: " + type + " time(us): " + localTime);
            timeUs += localTime;
        }
        Log.v(TAG, "foreground time(us): " + timeUs);

        return convertUsToMs(timeUs);
    }

    /**
     * Remove the {@link BatterySipper} that we should hide.
     *
     * @param sippers sipper list that need to check and remove
     * @return the total power of the hidden items of {@link BatterySipper}
     */
    public double removeHiddenBatterySippers(List<BatterySipper> sippers) {
        double totalPowerMah = 0;
        for (int i = sippers.size() - 1; i >= 0; i--) {
            final BatterySipper sipper = sippers.get(i);
            if (shouldHideSipper(sipper)) {
                sippers.remove(i);
                if (sipper.drainType != BatterySipper.DrainType.OVERCOUNTED) {
                    // Don't add it if it is overcounted
                    totalPowerMah += sipper.totalPowerMah;
                }
            }
        }

        return totalPowerMah;
    }

    /**
     * Check whether we should hide the battery sipper.
     */
    public boolean shouldHideSipper(BatterySipper sipper) {
        final BatterySipper.DrainType drainType = sipper.drainType;

        return drainType == BatterySipper.DrainType.IDLE
                || drainType == BatterySipper.DrainType.CELL
                || drainType == BatterySipper.DrainType.WIFI
                || drainType == BatterySipper.DrainType.SCREEN
                || drainType == BatterySipper.DrainType.BLUETOOTH
                || drainType == BatterySipper.DrainType.UNACCOUNTED
                || drainType == BatterySipper.DrainType.OVERCOUNTED
                || (sipper.totalPowerMah * SECONDS_IN_HOUR) < MIN_POWER_THRESHOLD_MILLI_AMP
                || mPowerUsageFeatureProvider.isTypeService(sipper)
                || mPowerUsageFeatureProvider.isTypeSystem(sipper);
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

    private long convertUsToMs(long timeUs) {
        return timeUs / 1000;
    }

    private long convertMsToUs(long timeMs) {
        return timeMs * 1000;
    }

    private boolean isDataCorrupted() {
        return mPackageManager == null;
    }

}

