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

import android.app.usage.UsageEvents;
import android.content.Context;
import android.os.AsyncTask;
import android.os.BatteryUsageStats;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.BatteryUsageHistoricalLogEntry.Action;
import com.android.settings.fuelgauge.batteryusage.bugreport.BatteryUsageLogUtils;
import com.android.settings.overlay.FeatureFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Load battery usage data in the background. */
public final class BatteryUsageDataLoader {
    private static final String TAG = "BatteryUsageDataLoader";

    // For testing only.
    @VisibleForTesting static Supplier<List<BatteryEntry>> sFakeBatteryEntryListSupplier;
    @VisibleForTesting static Supplier<Map<Long, UsageEvents>> sFakeAppUsageEventsSupplier;
    @VisibleForTesting static Supplier<List<AppUsageEvent>> sFakeUsageEventsListSupplier;

    private BatteryUsageDataLoader() {}

    static void enqueueWork(final Context context, final boolean isFullChargeStart) {
        AsyncTask.execute(
                () -> {
                    Log.d(TAG, "loadUsageDataSafely() in the AsyncTask");
                    loadUsageDataSafely(context.getApplicationContext(), isFullChargeStart);
                });
    }

    @VisibleForTesting
    static void loadBatteryStatsData(final Context context, final boolean isFullChargeStart) {
        BatteryUsageLogUtils.writeLog(context, Action.FETCH_USAGE_DATA, "");
        final long currentTime = System.currentTimeMillis();
        final BatteryUsageStats batteryUsageStats = DataProcessor.getBatteryUsageStats(context);
        final List<BatteryEntry> batteryEntryList =
                sFakeBatteryEntryListSupplier != null
                        ? sFakeBatteryEntryListSupplier.get()
                        : DataProcessor.generateBatteryEntryListFromBatteryUsageStats(
                                context, batteryUsageStats);
        if (batteryEntryList == null || batteryEntryList.isEmpty()) {
            Log.w(TAG, "getBatteryEntryList() returns null or empty content");
        }
        final long elapsedTime = System.currentTimeMillis() - currentTime;
        Log.d(TAG, String.format("getBatteryUsageStats() in %d/ms", elapsedTime));
        if (isFullChargeStart) {
            DatabaseUtils.recordDateTime(context, DatabaseUtils.KEY_LAST_LOAD_FULL_CHARGE_TIME);
            DatabaseUtils.sendBatteryEventData(
                    context,
                    ConvertUtils.convertToBatteryEvent(
                            currentTime, BatteryEventType.FULL_CHARGED, 100));
            DatabaseUtils.removeDismissedPowerAnomalyKeys(context);
        }

        // Uploads the BatteryEntry data into database.
        DatabaseUtils.sendBatteryEntryData(
                context, currentTime, batteryEntryList, batteryUsageStats, isFullChargeStart);
        DataProcessor.closeBatteryUsageStats(batteryUsageStats);
    }

    @VisibleForTesting
    static void loadAppUsageData(final Context context) {
        final long start = System.currentTimeMillis();
        final Map<Long, UsageEvents> appUsageEvents =
                sFakeAppUsageEventsSupplier != null
                        ? sFakeAppUsageEventsSupplier.get()
                        : DataProcessor.getAppUsageEvents(context);
        if (appUsageEvents == null) {
            Log.w(TAG, "loadAppUsageData() returns null");
            return;
        }
        final List<AppUsageEvent> appUsageEventList =
                sFakeUsageEventsListSupplier != null
                        ? sFakeUsageEventsListSupplier.get()
                        : DataProcessor.generateAppUsageEventListFromUsageEvents(
                                context, appUsageEvents);
        if (appUsageEventList == null || appUsageEventList.isEmpty()) {
            Log.w(TAG, "loadAppUsageData() returns null or empty content");
            return;
        }
        final long elapsedTime = System.currentTimeMillis() - start;
        Log.d(
                TAG,
                String.format(
                        "loadAppUsageData() size=%d in %d/ms",
                        appUsageEventList.size(), elapsedTime));
        // Uploads the AppUsageEvent data into database.
        DatabaseUtils.sendAppUsageEventData(context, appUsageEventList);
    }

    private static void preprocessBatteryUsageSlots(final Context context) {
        final long start = System.currentTimeMillis();
        final Handler handler = new Handler(Looper.getMainLooper());
        final BatteryLevelData batteryLevelData =
                DataProcessManager.getBatteryLevelData(
                        context,
                        handler,
                        /* isFromPeriodJob= */ true,
                        batteryDiffDataMap -> {
                            DatabaseUtils.sendBatteryUsageSlotData(
                                    context,
                                    ConvertUtils.convertToBatteryUsageSlotList(batteryDiffDataMap));
                            if (batteryDiffDataMap.values().stream()
                                    .anyMatch(
                                            data ->
                                                    data != null
                                                            && (!data.getSystemDiffEntryList()
                                                                            .isEmpty()
                                                                    || !data.getAppDiffEntryList()
                                                                            .isEmpty()))) {
                                FeatureFactory.getFeatureFactory()
                                        .getPowerUsageFeatureProvider()
                                        .detectSettingsAnomaly(
                                                context,
                                                /* displayDrain= */ 0,
                                                DetectRequestSourceType.TYPE_DATA_LOADER);
                            }
                        });
        if (batteryLevelData == null) {
            Log.d(TAG, "preprocessBatteryUsageSlots() no new battery usage data.");
            return;
        }

        DatabaseUtils.sendBatteryEventData(
                context, ConvertUtils.convertToBatteryEventList(batteryLevelData));
        Log.d(
                TAG,
                String.format(
                        "preprocessBatteryUsageSlots() batteryLevelData=%s in %d/ms",
                        batteryLevelData, System.currentTimeMillis() - start));
    }

    private static void loadUsageDataSafely(
            final Context context, final boolean isFullChargeStart) {
        try {
            final long start = System.currentTimeMillis();
            loadBatteryStatsData(context, isFullChargeStart);
            if (!isFullChargeStart) {
                // No app usage data or battery diff data at this time.
                loadAppUsageData(context);
                preprocessBatteryUsageSlots(context);
            }
            Log.d(
                    TAG,
                    String.format(
                            "loadUsageDataSafely() in %d/ms", System.currentTimeMillis() - start));
        } catch (RuntimeException e) {
            Log.e(TAG, "loadUsageData:", e);
        }
    }
}
