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

import android.annotation.IntDef;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.BatteryStats;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
    private static BatteryUtils sInstance;

    private PackageManager mPackageManager;

    public static BatteryUtils getInstance(Context context) {
        if (sInstance == null || sInstance.isDataCorrupted()) {
            sInstance = new BatteryUtils(context);
        }
        return sInstance;
    }

    private BatteryUtils(Context context) {
        mPackageManager = context.getPackageManager();
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
        final int foregroundTypes[] = {BatteryStats.Uid.PROCESS_STATE_TOP,
                BatteryStats.Uid.PROCESS_STATE_FOREGROUND_SERVICE,
                BatteryStats.Uid.PROCESS_STATE_TOP_SLEEPING,
                BatteryStats.Uid.PROCESS_STATE_FOREGROUND};
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

