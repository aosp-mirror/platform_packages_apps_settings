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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.BatteryUsageStats;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDatabase;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A utility class to operate battery usage database. */
public final class DatabaseUtils {
    private static final String TAG = "DatabaseUtils";
    /** Key for query parameter timestamp used in BATTERY_CONTENT_URI **/
    private static final String QUERY_KEY_TIMESTAMP = "timestamp";
    /** Clear memory threshold for device booting phase. **/
    private static final long CLEAR_MEMORY_THRESHOLD_MS = Duration.ofMinutes(5).toMillis();
    private static final long CLEAR_MEMORY_DELAYED_MS = Duration.ofSeconds(2).toMillis();

    @VisibleForTesting
    static final int DATA_RETENTION_INTERVAL_DAY = 9;

    /** An authority name of the battery content provider. */
    public static final String AUTHORITY = "com.android.settings.battery.usage.provider";
    /** A table name for battery usage history. */
    public static final String BATTERY_STATE_TABLE = "BatteryState";
    /** A class name for battery usage data provider. */
    public static final String SETTINGS_PACKAGE_PATH = "com.android.settings";

    /** A content URI to access battery usage states data. */
    public static final Uri BATTERY_CONTENT_URI =
            new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(AUTHORITY)
                    .appendPath(BATTERY_STATE_TABLE)
                    .build();

    private DatabaseUtils() {
    }

    /** Returns true if current user is a work profile user. */
    public static boolean isWorkProfile(Context context) {
        final UserManager userManager = context.getSystemService(UserManager.class);
        return userManager.isManagedProfile() && !userManager.isSystemUser();
    }

    /** Long: for timestamp and String: for BatteryHistEntry.getKey() */
    public static Map<Long, Map<String, BatteryHistEntry>> getHistoryMapSinceLastFullCharge(
            Context context, Calendar calendar) {
        final long startTime = System.currentTimeMillis();
        final long sixDaysAgoTimestamp = getTimestampSixDaysAgo(calendar);
        Log.d(TAG, "sixDayAgoTimestamp: " + sixDaysAgoTimestamp);
        // Builds the content uri everytime to avoid cache.
        final Uri batteryStateUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(AUTHORITY)
                        .appendPath(BATTERY_STATE_TABLE)
                        .appendQueryParameter(
                                QUERY_KEY_TIMESTAMP, Long.toString(sixDaysAgoTimestamp))
                        .build();

        final Map<Long, Map<String, BatteryHistEntry>> resultMap =
                loadHistoryMapFromContentProvider(context, batteryStateUri);
        if (resultMap == null || resultMap.isEmpty()) {
            Log.d(TAG, "getHistoryMapSinceLastFullCharge() returns empty or null");
        } else {
            Log.d(TAG, String.format("getHistoryMapSinceLastFullCharge() size=%d in %d/ms",
                    resultMap.size(), (System.currentTimeMillis() - startTime)));
        }
        return resultMap;
    }

    /** Clears all data in the battery usage database. */
    public static void clearAll(Context context) {
        AsyncTask.execute(() -> {
            try {
                BatteryStateDatabase
                        .getInstance(context.getApplicationContext())
                        .batteryStateDao()
                        .clearAll();
            } catch (RuntimeException e) {
                Log.e(TAG, "clearAll() failed", e);
            }
        });
    }

    /** Clears all out-of-date data in the battery usage database. */
    public static void clearExpiredDataIfNeeded(Context context) {
        AsyncTask.execute(() -> {
            try {
                BatteryStateDatabase
                        .getInstance(context.getApplicationContext())
                        .batteryStateDao()
                        .clearAllBefore(Clock.systemUTC().millis()
                                - Duration.ofDays(DATA_RETENTION_INTERVAL_DAY).toMillis());
            } catch (RuntimeException e) {
                Log.e(TAG, "clearAllBefore() failed", e);
            }
        });
    }

    static List<ContentValues> sendBatteryEntryData(
            final Context context,
            final List<BatteryEntry> batteryEntryList,
            final BatteryUsageStats batteryUsageStats,
            final boolean isFullChargeStart) {
        final long startTime = System.currentTimeMillis();
        final Intent intent = BatteryUtils.getBatteryIntent(context);
        if (intent == null) {
            Log.e(TAG, "sendBatteryEntryData(): cannot fetch battery intent");
            clearMemory();
            return null;
        }
        final int batteryLevel = BatteryUtils.getBatteryLevel(intent);
        final int batteryStatus = intent.getIntExtra(
                BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        final int batteryHealth = intent.getIntExtra(
                BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
        // We should use the same timestamp for each data snapshot.
        final long snapshotTimestamp = Clock.systemUTC().millis();
        final long snapshotBootTimestamp = SystemClock.elapsedRealtime();

        // Creates the ContentValues list to insert them into provider.
        final List<ContentValues> valuesList = new ArrayList<>();
        if (batteryEntryList != null) {
            batteryEntryList.stream()
                    .filter(entry -> {
                        final long foregroundMs = entry.getTimeInForegroundMs();
                        final long backgroundMs = entry.getTimeInBackgroundMs();
                        if (entry.getConsumedPower() == 0
                                && (foregroundMs != 0 || backgroundMs != 0)) {
                            Log.w(TAG, String.format(
                                    "no consumed power but has running time for %s time=%d|%d",
                                    entry.getLabel(), foregroundMs, backgroundMs));
                        }
                        return entry.getConsumedPower() != 0
                                || foregroundMs != 0
                                || backgroundMs != 0;
                    })
                    .forEach(entry -> valuesList.add(
                            ConvertUtils.convertToContentValues(
                                    entry,
                                    batteryUsageStats,
                                    batteryLevel,
                                    batteryStatus,
                                    batteryHealth,
                                    snapshotBootTimestamp,
                                    snapshotTimestamp,
                                    isFullChargeStart)));
        }

        int size = 1;
        final ContentResolver resolver = context.getContentResolver();
        // Inserts all ContentValues into battery provider.
        if (!valuesList.isEmpty()) {
            final ContentValues[] valuesArray = new ContentValues[valuesList.size()];
            valuesList.toArray(valuesArray);
            try {
                size = resolver.bulkInsert(BATTERY_CONTENT_URI, valuesArray);
                Log.d(TAG, "insert() data into database with isFullChargeStart:"
                        + isFullChargeStart);
            } catch (Exception e) {
                Log.e(TAG, "bulkInsert() data into database error:\n" + e);
            }
        } else {
            // Inserts one fake data into battery provider.
            final ContentValues contentValues =
                    ConvertUtils.convertToContentValues(
                            /*entry=*/ null,
                            /*batteryUsageStats=*/ null,
                            batteryLevel,
                            batteryStatus,
                            batteryHealth,
                            snapshotBootTimestamp,
                            snapshotTimestamp,
                            isFullChargeStart);
            try {
                resolver.insert(BATTERY_CONTENT_URI, contentValues);
                Log.d(TAG, "insert() data into database with isFullChargeStart:"
                        + isFullChargeStart);

            } catch (Exception e) {
                Log.e(TAG, "insert() data into database error:\n" + e);
            }
            valuesList.add(contentValues);
        }
        resolver.notifyChange(BATTERY_CONTENT_URI, /*observer=*/ null);
        Log.d(TAG, String.format("sendBatteryEntryData() size=%d in %d/ms",
                size, (System.currentTimeMillis() - startTime)));
        clearMemory();
        return valuesList;
    }

    private static Map<Long, Map<String, BatteryHistEntry>> loadHistoryMapFromContentProvider(
            Context context, Uri batteryStateUri) {
        final boolean isWorkProfileUser = isWorkProfile(context);
        Log.d(TAG, "loadHistoryMapFromContentProvider() isWorkProfileUser:" + isWorkProfileUser);
        if (isWorkProfileUser) {
            try {
                context = context.createPackageContextAsUser(
                        /*packageName=*/ context.getPackageName(),
                        /*flags=*/ 0,
                        /*user=*/ UserHandle.OWNER);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "context.createPackageContextAsUser() fail:" + e);
                return null;
            }
        }
        final Map<Long, Map<String, BatteryHistEntry>> resultMap = new HashMap();
        try (Cursor cursor =
                     context.getContentResolver().query(batteryStateUri, null, null, null)) {
            if (cursor == null || cursor.getCount() == 0) {
                return resultMap;
            }
            // Loads and recovers all BatteryHistEntry data from cursor.
            while (cursor.moveToNext()) {
                final BatteryHistEntry entry = new BatteryHistEntry(cursor);
                final long timestamp = entry.mTimestamp;
                final String key = entry.getKey();
                Map batteryHistEntryMap = resultMap.get(timestamp);
                // Creates new one if there is no corresponding map.
                if (batteryHistEntryMap == null) {
                    batteryHistEntryMap = new HashMap<>();
                    resultMap.put(timestamp, batteryHistEntryMap);
                }
                batteryHistEntryMap.put(key, entry);
            }
            try {
                cursor.close();
            } catch (Exception e) {
                Log.e(TAG, "cursor.close() failed", e);
            }
        }
        return resultMap;
    }

    private static void clearMemory() {
        if (SystemClock.uptimeMillis() > CLEAR_MEMORY_THRESHOLD_MS) {
            return;
        }
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.postDelayed(() -> {
            System.gc();
            System.runFinalization();
            System.gc();
            Log.w(TAG, "invoke clearMemory()");
        }, CLEAR_MEMORY_DELAYED_MS);
    }

    /** Returns the timestamp for 00:00 6 days before the calendar date. */
    private static long getTimestampSixDaysAgo(Calendar calendar) {
        Calendar startCalendar =
                calendar == null ? Calendar.getInstance() : (Calendar) calendar.clone();
        startCalendar.add(Calendar.DAY_OF_YEAR, -6);
        startCalendar.set(Calendar.HOUR_OF_DAY, 0);
        startCalendar.set(Calendar.MINUTE, 0);
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);
        return startCalendar.getTimeInMillis();
    }

}
