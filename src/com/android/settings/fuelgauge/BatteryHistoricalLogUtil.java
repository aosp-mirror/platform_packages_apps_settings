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

import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action;

import com.google.common.annotations.VisibleForTesting;

import java.io.PrintWriter;
import java.util.List;

/** Writes and reads a historical log of battery related state change events. */
public final class BatteryHistoricalLogUtil {
    private static final String BATTERY_OPTIMIZE_FILE_NAME = "battery_optimize_historical_logs";
    private static final String LOGS_KEY = "battery_optimize_logs_key";
    private static final String TAG = "BatteryHistoricalLogUtil";

    @VisibleForTesting
    static final int MAX_ENTRIES = 40;

    /**
     * Writes a log entry.
     *
     * <p>Keeps up to {@link #MAX_ENTRIES} in the log, once that number is exceeded, it prunes the
     * oldest one.
     */
    static void writeLog(Context context, Action action, String pkg, String actionDescription) {
        writeLog(
                context,
                BatteryOptimizeHistoricalLogEntry.newBuilder()
                        .setPackageName(pkg)
                        .setAction(action)
                        .setActionDescription(actionDescription)
                        .build());
    }

    private static void writeLog(Context context, BatteryOptimizeHistoricalLogEntry logEntry) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);

        BatteryOptimizeHistoricalLog existingLog =
                parseLogFromString(sharedPreferences.getString(LOGS_KEY, ""));
        BatteryOptimizeHistoricalLog.Builder newLogBuilder = existingLog.toBuilder();
        // Prune old entries
        if (existingLog.getLogEntryCount() >= MAX_ENTRIES) {
            newLogBuilder.removeLogEntry(0);
        }
        newLogBuilder.addLogEntry(logEntry);

        sharedPreferences
                .edit()
                .putString(
                        LOGS_KEY,
                        Base64.encodeToString(newLogBuilder.build().toByteArray(), Base64.DEFAULT))
                .apply();
    }

    private static BatteryOptimizeHistoricalLog parseLogFromString(String storedLogs) {
        return BatteryUtils.parseProtoFromString(
                storedLogs, BatteryOptimizeHistoricalLog.getDefaultInstance());
    }

    /**
     * Prints the historical log that has previously been stored by this utility.
     */
    public static void printBatteryOptimizeHistoricalLog(Context context, PrintWriter writer) {
        writer.println("Battery optimize state history:");
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        BatteryOptimizeHistoricalLog existingLog =
                parseLogFromString(sharedPreferences.getString(LOGS_KEY, ""));
        List<BatteryOptimizeHistoricalLogEntry> logEntryList = existingLog.getLogEntryList();
        if (logEntryList.isEmpty()) {
            writer.println("\tNo past logs.");
        } else {
            logEntryList.forEach(entry -> writer.println(toString(entry)));
        }
    }

    /**
     * Gets the unique key for logging, combined with package name, delimiter and user id.
     */
    static String getPackageNameWithUserId(String pkgName, int userId) {
        return pkgName + ":" + userId;
    }

    private static String toString(BatteryOptimizeHistoricalLogEntry entry) {
        return String.format("%s\tAction:%s\tEvent:%s",
                entry.getPackageName(), entry.getAction(), entry.getActionDescription());
    }

    @VisibleForTesting
    static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(BATTERY_OPTIMIZE_FILE_NAME, Context.MODE_PRIVATE);
    }
}
