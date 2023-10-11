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
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Load app usage events data in the background. */
public final class AppUsageDataLoader {
    private static final String TAG = "AppUsageDataLoader";

    // For testing only.
    @VisibleForTesting
    static Supplier<Map<Long, UsageEvents>> sFakeAppUsageEventsSupplier;
    @VisibleForTesting
    static Supplier<List<AppUsageEvent>> sFakeUsageEventsListSupplier;

    private AppUsageDataLoader() {}

    static void enqueueWork(final Context context) {
        AsyncTask.execute(() -> {
            Log.d(TAG, "loadAppUsageDataSafely() in the AsyncTask");
            loadAppUsageDataSafely(context.getApplicationContext());
        });
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
        Log.d(TAG, String.format("loadAppUsageData() size=%d in %d/ms", appUsageEventList.size(),
                elapsedTime));
        // Uploads the AppUsageEvent data into database.
        DatabaseUtils.sendAppUsageEventData(context, appUsageEventList);
    }

    private static void loadAppUsageDataSafely(final Context context) {
        try {
            loadAppUsageData(context);
        } catch (RuntimeException e) {
            Log.e(TAG, "loadAppUsageData:" + e);
        }
    }
}
