/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.content.SharedPreferences;
import android.util.Base64;

import com.android.settings.fuelgauge.BatteryUsageHistoricalLog;
import com.android.settings.fuelgauge.BatteryUsageHistoricalLogEntry;
import com.android.settings.fuelgauge.BatteryUsageHistoricalLogEntry.Action;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batteryusage.ConvertUtils;

import com.google.common.annotations.VisibleForTesting;

import java.io.PrintWriter;
import java.util.List;

/** Writes and reads a historical log of battery usage periodic job events. */
public final class BatteryUsageLogUtils {
    private static final String TAG = "BatteryUsageLogUtils";
    private static final String BATTERY_USAGE_FILE_NAME = "battery_usage_historical_logs";
    private static final String LOGS_KEY = "battery_usage_logs_key";

    // 24 hours x 4 events every hour x 3 days
    static final int MAX_ENTRIES = 288;

    private BatteryUsageLogUtils() {}

    /** Write the log into the {@link SharedPreferences}. */
    public static void writeLog(Context context, Action action, String actionDescription) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        final BatteryUsageHistoricalLogEntry newLogEntry =
                BatteryUsageHistoricalLogEntry.newBuilder()
                        .setTimestamp(System.currentTimeMillis())
                        .setAction(action)
                        .setActionDescription(actionDescription)
                        .build();

        final BatteryUsageHistoricalLog existingLog =
                parseLogFromString(sharedPreferences.getString(LOGS_KEY, ""));
        final BatteryUsageHistoricalLog.Builder newLogBuilder = existingLog.toBuilder();
        // Prune old entries to limit the max logging data count.
        if (existingLog.getLogEntryCount() >= MAX_ENTRIES) {
            newLogBuilder.removeLogEntry(0);
        }
        newLogBuilder.addLogEntry(newLogEntry);

        final String loggingContent =
                Base64.encodeToString(newLogBuilder.build().toByteArray(), Base64.DEFAULT);
        sharedPreferences.edit().putString(LOGS_KEY, loggingContent).apply();
    }

    /** Prints the historical log that has previously been stored by this utility. */
    public static void printHistoricalLog(Context context, PrintWriter writer) {
        final BatteryUsageHistoricalLog existingLog =
                parseLogFromString(getSharedPreferences(context).getString(LOGS_KEY, ""));
        final List<BatteryUsageHistoricalLogEntry> logEntryList = existingLog.getLogEntryList();
        if (logEntryList.isEmpty()) {
            writer.println("\tnothing to dump");
        } else {
            logEntryList.forEach(entry -> writer.println(toString(entry)));
        }
    }

    @VisibleForTesting
    static SharedPreferences getSharedPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(BATTERY_USAGE_FILE_NAME, Context.MODE_PRIVATE);
    }

    private static BatteryUsageHistoricalLog parseLogFromString(String storedLogs) {
        return BatteryUtils.parseProtoFromString(
                storedLogs, BatteryUsageHistoricalLog.getDefaultInstance());
    }

    private static String toString(BatteryUsageHistoricalLogEntry entry) {
        final StringBuilder builder =
                new StringBuilder("\t")
                        .append(ConvertUtils.utcToLocalTimeForLogging(entry.getTimestamp()))
                        .append(" " + entry.getAction());
        final String description = entry.getActionDescription();
        if (description != null && !description.isEmpty()) {
            builder.append(" " + description);
        }
        return builder.toString();
    }
}
