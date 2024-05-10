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

package com.android.settings.fuelgauge.batteryusage.bugreport;

import android.content.Context;
import android.util.Log;

import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batteryusage.BatteryUsageSlot;
import com.android.settings.fuelgauge.batteryusage.ConvertUtils;
import com.android.settings.fuelgauge.batteryusage.DatabaseUtils;
import com.android.settings.fuelgauge.batteryusage.db.AppUsageEventDao;
import com.android.settings.fuelgauge.batteryusage.db.AppUsageEventEntity;
import com.android.settings.fuelgauge.batteryusage.db.BatteryEventDao;
import com.android.settings.fuelgauge.batteryusage.db.BatteryEventEntity;
import com.android.settings.fuelgauge.batteryusage.db.BatteryState;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDao;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDatabase;
import com.android.settings.fuelgauge.batteryusage.db.BatteryUsageSlotDao;
import com.android.settings.fuelgauge.batteryusage.db.BatteryUsageSlotEntity;

import java.io.PrintWriter;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/** A utility class to aggregate and provide required log data. */
public final class LogUtils {
    private static final String TAG = "LogUtils";
    private static final Duration DUMP_TIME_OFFSET = Duration.ofHours(24);
    private static final Duration DUMP_TIME_OFFSET_FOR_ENTRY = Duration.ofHours(4);

    static void dumpBatteryUsageDatabaseHist(Context context, PrintWriter writer) {
        // Dumps periodic job events.
        writer.println("\nBattery PeriodicJob History:");
        BatteryUsageLogUtils.printHistoricalLog(context, writer);
        writer.flush();

        // Dumps phenotype environments.
        DatabaseUtils.dump(context, writer);
        writer.flush();
        final BatteryStateDao dao = BatteryStateDatabase.getInstance(context).batteryStateDao();
        final long timeOffset = Clock.systemUTC().millis() - DUMP_TIME_OFFSET.toMillis();

        // Gets all distinct timestamps.
        final List<Long> timestamps = dao.getDistinctTimestamps(timeOffset);
        final int distinctCount = timestamps.size();
        writer.println("\n\tBattery DatabaseHistory:");
        writer.println("distinct timestamp count:" + distinctCount);
        Log.w(TAG, "distinct timestamp count:" + distinctCount);
        if (distinctCount == 0) {
            return;
        }
        // Dumps all distinct timestamps.
        timestamps.forEach(
                timestamp -> {
                    final String formattedTimestamp =
                            ConvertUtils.utcToLocalTimeForLogging(timestamp);
                    writer.println("\t" + formattedTimestamp);
                    Log.w(TAG, "\t" + formattedTimestamp);
                });
        writer.flush();
    }

    static void dumpBatteryStateDatabaseHist(Context context, PrintWriter writer) {
        final BatteryStateDao dao = BatteryStateDatabase.getInstance(context).batteryStateDao();
        writer.println("\n\tBatteryState DatabaseHistory:");
        final List<BatteryState> stateList =
                dao.getAllAfter(Clock.systemUTC().millis() - DUMP_TIME_OFFSET_FOR_ENTRY.toMillis());
        dumpListItems(writer, stateList, state -> state);
    }

    static void dumpAppUsageDatabaseHist(Context context, PrintWriter writer) {
        final AppUsageEventDao dao = BatteryStateDatabase.getInstance(context).appUsageEventDao();
        writer.println("\n\tApp DatabaseHistory:");
        final List<AppUsageEventEntity> eventList =
                dao.getAllAfter(Clock.systemUTC().millis() - DUMP_TIME_OFFSET_FOR_ENTRY.toMillis());
        dumpListItems(writer, eventList, event -> event);
    }

    static void dumpBatteryUsageSlotDatabaseHist(Context context, PrintWriter writer) {
        final BatteryUsageSlotDao dao =
                BatteryStateDatabase.getInstance(context).batteryUsageSlotDao();
        writer.println("\n\tBattery Usage Slot DatabaseHistory:");
        final List<BatteryUsageSlotEntity> entities =
                dao.getAllAfterForLog(getLastFullChargeTimestamp(context));
        dumpListItems(
                writer,
                entities,
                entity ->
                        BatteryUtils.parseProtoFromString(
                                entity.batteryUsageSlot, BatteryUsageSlot.getDefaultInstance()));
    }

    static void dumpBatteryEventDatabaseHist(Context context, PrintWriter writer) {
        final BatteryEventDao dao = BatteryStateDatabase.getInstance(context).batteryEventDao();
        writer.println("\n\tBattery Event DatabaseHistory:");
        final List<BatteryEventEntity> entities =
                dao.getAllAfterForLog(getLastFullChargeTimestamp(context));
        dumpListItems(writer, entities, entity -> entity);
    }

    private static <T, S> void dumpListItems(
            PrintWriter writer, List<T> itemList, Function<T, S> itemConverter) {
        final AtomicInteger counter = new AtomicInteger(0);
        try {
            itemList.forEach(
                    item -> {
                        writer.println(itemConverter.apply(item));
                        if (counter.incrementAndGet() % 20 == 0) {
                            writer.flush();
                        }
                    });
        } catch (RuntimeException e) {
            Log.e(TAG, "dumpListItems() error: ", e);
        }
        writer.flush();
    }

    private static long getLastFullChargeTimestamp(Context context) {
        final BatteryEventDao dao = BatteryStateDatabase.getInstance(context).batteryEventDao();
        try {
            final Long lastFullChargeTimestamp = dao.getLastFullChargeTimestampForLog();
            return lastFullChargeTimestamp != null ? lastFullChargeTimestamp : 0L;
        } catch (RuntimeException e) {
            Log.e(TAG, "getLastFullChargeTimestamp() error: ", e);
            return 0L;
        }
    }

    private LogUtils() {}
}
