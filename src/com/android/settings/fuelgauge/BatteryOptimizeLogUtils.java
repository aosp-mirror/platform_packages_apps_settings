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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action;
import com.android.settings.fuelgauge.batteryusage.ConvertUtils;

import java.io.PrintWriter;
import java.util.List;

/** Writes and reads a historical log of battery related state change events. */
public final class BatteryOptimizeLogUtils {
    private static final String TAG = "BatteryOptimizeLogUtils";
    private static final String BATTERY_OPTIMIZE_FILE_NAME = "battery_optimize_historical_logs";
    private static final String LOGS_KEY = "battery_optimize_logs_key";

    @VisibleForTesting static final int MAX_ENTRIES = 40;

    private BatteryOptimizeLogUtils() {}

    /** Writes a log entry for battery optimization mode. */
    static void writeLog(
            Context context, Action action, String packageName, String actionDescription) {
        writeLog(getSharedPreferences(context), action, packageName, actionDescription);
    }

    static void writeLog(
            SharedPreferences sharedPreferences,
            Action action,
            String packageName,
            String actionDescription) {
        writeLog(
                sharedPreferences,
                BatteryOptimizeHistoricalLogEntry.newBuilder()
                        .setPackageName(packageName)
                        .setAction(action)
                        .setActionDescription(actionDescription)
                        .setTimestamp(System.currentTimeMillis())
                        .build());
    }

    private static void writeLog(
            SharedPreferences sharedPreferences, BatteryOptimizeHistoricalLogEntry logEntry) {
        BatteryOptimizeHistoricalLog existingLog =
                parseLogFromString(sharedPreferences.getString(LOGS_KEY, ""));
        BatteryOptimizeHistoricalLog.Builder newLogBuilder = existingLog.toBuilder();
        // Prune old entries to limit the max logging data count.
        if (existingLog.getLogEntryCount() >= MAX_ENTRIES) {
            newLogBuilder.removeLogEntry(0);
        }
        newLogBuilder.addLogEntry(logEntry);

        String loggingContent =
                Base64.encodeToString(newLogBuilder.build().toByteArray(), Base64.DEFAULT);
        sharedPreferences.edit().putString(LOGS_KEY, loggingContent).apply();
    }

    private static BatteryOptimizeHistoricalLog parseLogFromString(String storedLogs) {
        return BatteryUtils.parseProtoFromString(
                storedLogs, BatteryOptimizeHistoricalLog.getDefaultInstance());
    }

    /** Prints the historical log that has previously been stored by this utility. */
    public static void printBatteryOptimizeHistoricalLog(Context context, PrintWriter writer) {
        printBatteryOptimizeHistoricalLog(getSharedPreferences(context), writer);
    }

    /** Prints the historical log that has previously been stored by this utility. */
    public static void printBatteryOptimizeHistoricalLog(
            SharedPreferences sharedPreferences, PrintWriter writer) {
        writer.println("Battery optimize state history:");
        BatteryOptimizeHistoricalLog existingLog =
                parseLogFromString(sharedPreferences.getString(LOGS_KEY, ""));
        List<BatteryOptimizeHistoricalLogEntry> logEntryList = existingLog.getLogEntryList();
        if (logEntryList.isEmpty()) {
            writer.println("\tnothing to dump");
        } else {
            writer.println("0:UNKNOWN 1:RESTRICTED 2:UNRESTRICTED 3:OPTIMIZED");
            logEntryList.forEach(entry -> writer.println(toString(entry)));
        }
    }

    /** Gets the unique key for logging. */
    static String getPackageNameWithUserId(String packageName, int userId) {
        return packageName + ":" + userId;
    }

    private static String toString(BatteryOptimizeHistoricalLogEntry entry) {
        return String.format(
                "%s\t%s\taction:%s\tevent:%s",
                ConvertUtils.utcToLocalTimeForLogging(entry.getTimestamp()),
                entry.getPackageName(),
                entry.getAction(),
                entry.getActionDescription());
    }

    @VisibleForTesting
    static SharedPreferences getSharedPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(BATTERY_OPTIMIZE_FILE_NAME, Context.MODE_PRIVATE);
    }
}
