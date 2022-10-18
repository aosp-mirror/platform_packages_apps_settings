/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

/** A {@link BatteryUsageBroadcastReceiver} for battery usage data requesting. */
public final class BatteryUsageBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "BatteryUsageBroadcastReceiver";
    /** An intent action to request Settings to fetch usage data. */
    public static final String ACTION_FETCH_BATTERY_USAGE_DATA =
            "com.android.settings.battery.action.FETCH_BATTERY_USAGE_DATA";
    /** An intent action to request Settings to clear cache data. */
    public static final String ACTION_CLEAR_BATTERY_CACHE_DATA =
            "com.android.settings.battery.action.CLEAR_BATTERY_CACHE_DATA";

    @VisibleForTesting
    static boolean sIsDebugMode = Build.TYPE.equals("userdebug");

    @VisibleForTesting
    boolean mFetchBatteryUsageData = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        Log.d(TAG, "onReceive:" + intent.getAction());
        switch (intent.getAction()) {
            case ACTION_FETCH_BATTERY_USAGE_DATA:
                mFetchBatteryUsageData = true;
                BatteryUsageDataLoader.enqueueWork(context);
                break;
            case ACTION_CLEAR_BATTERY_CACHE_DATA:
                if (sIsDebugMode) {
                    BatteryDiffEntry.clearCache();
                    BatteryEntry.clearUidCache();
                }
                break;
        }
    }
}
