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
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.fuelgauge.BatteryStatus;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A {@link BatteryUsageBroadcastReceiver} for battery usage data requesting. */
public final class BatteryUsageBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "BatteryUsageBroadcastReceiver";

    /** An intent action to request Settings to clear cache data. */
    public static final String ACTION_CLEAR_BATTERY_CACHE_DATA =
            "com.android.settings.battery.action.CLEAR_BATTERY_CACHE_DATA";

    /** An intent action for power is plugging. */
    public static final String ACTION_BATTERY_PLUGGING =
            "com.android.settings.battery.action.ACTION_BATTERY_PLUGGING";

    /** An intent action for power is unplugging. */
    public static final String ACTION_BATTERY_UNPLUGGING =
            "com.android.settings.battery.action.ACTION_BATTERY_UNPLUGGING";

    @VisibleForTesting static long sBroadcastDelayFromBoot = Duration.ofMinutes(40).toMillis();
    @VisibleForTesting static boolean sIsDebugMode = Build.TYPE.equals("userdebug");

    @VisibleForTesting boolean mFetchBatteryUsageData = false;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        final String action = intent.getAction();
        Log.d(TAG, "onReceive:" + action);
        if (com.android.settingslib.fuelgauge.BatteryUtils.isWorkProfile(context)) {
            Log.w(TAG, "do nothing for work profile action=" + action);
            return;
        }
        DatabaseUtils.recordDateTime(context, action);
        final String fullChargeIntentAction =
                FeatureFactory.getFeatureFactory()
                        .getPowerUsageFeatureProvider()
                        .getFullChargeIntentAction();
        switch (action) {
            case Intent.ACTION_BATTERY_LEVEL_CHANGED:
                // Only when fullChargeIntentAction is ACTION_BATTERY_LEVEL_CHANGED,
                // ACTION_BATTERY_LEVEL_CHANGED will be considered as the full charge event and then
                // start usage events fetching.
                if (Intent.ACTION_BATTERY_LEVEL_CHANGED.equals(fullChargeIntentAction)) {
                    Log.d(TAG, "fetch data because of event: ACTION_BATTERY_LEVEL_CHANGED");
                    tryToFetchUsageData(context);
                }
                break;
            case ACTION_BATTERY_PLUGGING:
                sendBatteryEventData(context, BatteryEventType.POWER_CONNECTED);
                break;
            case ACTION_BATTERY_UNPLUGGING:
                sendBatteryEventData(context, BatteryEventType.POWER_DISCONNECTED);
                // Only when fullChargeIntentAction is ACTION_POWER_DISCONNECTED,
                // ACTION_BATTERY_UNPLUGGING will be considered as the full charge event and then
                // start usage events fetching.
                if (Intent.ACTION_POWER_DISCONNECTED.equals(fullChargeIntentAction)) {
                    Log.d(TAG, "fetch data because of event: ACTION_POWER_DISCONNECTED");
                    tryToFetchUsageData(context);
                }
                break;
            case ACTION_CLEAR_BATTERY_CACHE_DATA:
                if (sIsDebugMode) {
                    BatteryDiffEntry.clearCache();
                    BatteryEntry.clearUidCache();
                }
                break;
        }
    }

    private void tryToFetchUsageData(Context context) {
        final Intent batteryIntent = BatteryUtils.getBatteryIntent(context);
        // Returns when battery is not fully charged.
        if (!BatteryStatus.isCharged(batteryIntent)) {
            return;
        }

        final boolean delayHourlyJobWhenBooting =
                FeatureFactory.getFeatureFactory()
                        .getPowerUsageFeatureProvider()
                        .delayHourlyJobWhenBooting();
        final long broadcastDelay = sBroadcastDelayFromBoot - SystemClock.elapsedRealtime();
        // If current boot time is smaller than expected delay, cancel sending the broadcast.
        if (delayHourlyJobWhenBooting && broadcastDelay > 0) {
            Log.d(
                    TAG,
                    "cancel sendBroadcastToFetchUsageData when broadcastDelay is "
                            + broadcastDelay
                            + "ms.");
            return;
        }

        mFetchBatteryUsageData = true;
        BatteryUsageDataLoader.enqueueWork(context, /* isFullChargeStart= */ true);
        BootBroadcastReceiver.invokeJobRecheck(context);
    }

    private void sendBatteryEventData(Context context, BatteryEventType batteryEventType) {
        final long timestamp = System.currentTimeMillis();
        final Intent intent = BatteryUtils.getBatteryIntent(context);
        final int batteryLevel = BatteryStatus.getBatteryLevel(intent);
        mExecutor.execute(
                () ->
                        DatabaseUtils.sendBatteryEventData(
                                context,
                                ConvertUtils.convertToBatteryEvent(
                                        timestamp, batteryEventType, batteryLevel)));
    }
}
