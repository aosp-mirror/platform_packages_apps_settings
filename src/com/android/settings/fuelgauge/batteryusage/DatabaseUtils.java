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
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDatabase;
import com.android.settingslib.fuelgauge.BatteryStatus;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** A utility class to operate battery usage database. */
public final class DatabaseUtils {
    private static final String TAG = "DatabaseUtils";
    /** Clear memory threshold for device booting phase. **/
    private static final long CLEAR_MEMORY_THRESHOLD_MS = Duration.ofMinutes(5).toMillis();
    private static final long CLEAR_MEMORY_DELAYED_MS = Duration.ofSeconds(2).toMillis();

    @VisibleForTesting
    static final int DATA_RETENTION_INTERVAL_DAY = 9;

    /** An authority name of the battery content provider. */
    public static final String AUTHORITY = "com.android.settings.battery.usage.provider";
    /** A table name for battery usage history. */
    public static final String BATTERY_STATE_TABLE = "BatteryState";
    /** A table name for app usage events. */
    public static final String APP_USAGE_EVENT_TABLE = "AppUsageEvent";
    /** A path name for app usage latest timestamp query. */
    public static final String APP_USAGE_LATEST_TIMESTAMP_PATH = "appUsageLatestTimestamp";
    /** A class name for battery usage data provider. */
    public static final String SETTINGS_PACKAGE_PATH = "com.android.settings";
    /** Key for query parameter timestamp used in BATTERY_CONTENT_URI **/
    public static final String QUERY_KEY_TIMESTAMP = "timestamp";
    /** Key for query parameter userid used in APP_USAGE_EVENT_URI **/
    public static final String QUERY_KEY_USERID = "userid";

    public static final long INVALID_USER_ID = Integer.MIN_VALUE;
    /**
     * The buffer hours to query app usage events that may have begun or ended out of the final
     * desired time frame.
     */
    public static final long USAGE_QUERY_BUFFER_HOURS = Duration.ofHours(3).toMillis();

    /** A content URI to access battery usage states data. */
    public static final Uri BATTERY_CONTENT_URI =
            new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(AUTHORITY)
                    .appendPath(BATTERY_STATE_TABLE)
                    .build();
    /** A content URI to access app usage events data. */
    public static final Uri APP_USAGE_EVENT_URI =
            new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(AUTHORITY)
                    .appendPath(APP_USAGE_EVENT_TABLE)
                    .build();

    // For testing only.
    @VisibleForTesting
    static Supplier<Cursor> sFakeBatteryStateSupplier;
    @VisibleForTesting
    static Supplier<Cursor> sFakeAppUsageEventSupplier;
    @VisibleForTesting
    static Supplier<Cursor> sFakeAppUsageLatestTimestampSupplier;

    private DatabaseUtils() {
    }

    /** Returns true if current user is a work profile user. */
    public static boolean isWorkProfile(Context context) {
        final UserManager userManager = context.getSystemService(UserManager.class);
        return userManager.isManagedProfile();
    }

    /** Returns the latest timestamp current user data in app usage event table. */
    public static long getAppUsageStartTimestampOfUser(
            Context context, final long userId, final long earliestTimestamp) {
        final long startTime = System.currentTimeMillis();
        // Builds the content uri everytime to avoid cache.
        final Uri appUsageLatestTimestampUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(AUTHORITY)
                        .appendPath(APP_USAGE_LATEST_TIMESTAMP_PATH)
                        .appendQueryParameter(
                                QUERY_KEY_USERID, Long.toString(userId))
                        .build();
        final long latestTimestamp =
                loadAppUsageLatestTimestampFromContentProvider(context, appUsageLatestTimestampUri);
        Log.d(TAG, String.format(
                "getAppUsageStartTimestampOfUser() userId=%d latestTimestamp=%d in %d/ms",
                userId, latestTimestamp, (System.currentTimeMillis() - startTime)));
        // Use (latestTimestamp + 1) here to avoid loading the events of the latestTimestamp
        // repeatedly.
        return Math.max(latestTimestamp + 1, earliestTimestamp);
    }

    /** Returns the current user data in app usage event table. */
    public static List<AppUsageEvent> getAppUsageEventForUsers(
            Context context,
            final Calendar calendar,
            final List<Integer> userIds,
            final long rawStartTimestamp) {
        final long startTime = System.currentTimeMillis();
        final long sixDaysAgoTimestamp = getTimestampSixDaysAgo(calendar);
        // Query a longer time period and then trim to the original time period in order to make
        // sure the app usage calculation near the boundaries is correct.
        final long queryTimestamp =
                Math.max(rawStartTimestamp, sixDaysAgoTimestamp) - USAGE_QUERY_BUFFER_HOURS;
        Log.d(TAG, "sixDayAgoTimestamp: " + sixDaysAgoTimestamp);
        final String queryUserIdString = userIds.stream()
                .map(userId -> String.valueOf(userId))
                .collect(Collectors.joining(","));
        // Builds the content uri everytime to avoid cache.
        final Uri appUsageEventUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(AUTHORITY)
                        .appendPath(APP_USAGE_EVENT_TABLE)
                        .appendQueryParameter(
                                QUERY_KEY_TIMESTAMP, Long.toString(queryTimestamp))
                        .appendQueryParameter(QUERY_KEY_USERID, queryUserIdString)
                        .build();

        final List<AppUsageEvent> appUsageEventList =
                loadAppUsageEventsFromContentProvider(context, appUsageEventUri);
        Log.d(TAG, String.format("getAppUsageEventForUser userId=%s size=%d in %d/ms",
                queryUserIdString, appUsageEventList.size(),
                (System.currentTimeMillis() - startTime)));
        return appUsageEventList;
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
                final BatteryStateDatabase database = BatteryStateDatabase
                        .getInstance(context.getApplicationContext());
                database.batteryStateDao().clearAll();
                database.appUsageEventDao().clearAll();
            } catch (RuntimeException e) {
                Log.e(TAG, "clearAll() failed", e);
            }
        });
    }

    /** Clears all out-of-date data in the battery usage database. */
    public static void clearExpiredDataIfNeeded(Context context) {
        AsyncTask.execute(() -> {
            try {
                final BatteryStateDatabase database = BatteryStateDatabase
                        .getInstance(context.getApplicationContext());
                final long earliestTimestamp = Clock.systemUTC().millis()
                        - Duration.ofDays(DATA_RETENTION_INTERVAL_DAY).toMillis();
                database.batteryStateDao().clearAllBefore(earliestTimestamp);
                database.appUsageEventDao().clearAllBefore(earliestTimestamp);
            } catch (RuntimeException e) {
                Log.e(TAG, "clearAllBefore() failed", e);
            }
        });
    }

    /** Returns the timestamp for 00:00 6 days before the calendar date. */
    public static long getTimestampSixDaysAgo(Calendar calendar) {
        Calendar startCalendar =
                calendar == null ? Calendar.getInstance() : (Calendar) calendar.clone();
        startCalendar.add(Calendar.DAY_OF_YEAR, -6);
        startCalendar.set(Calendar.HOUR_OF_DAY, 0);
        startCalendar.set(Calendar.MINUTE, 0);
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);
        return startCalendar.getTimeInMillis();
    }

    /** Returns the context with profile parent identity when current user is work profile. */
    public static Context getParentContext(Context context) {
        if (isWorkProfile(context)) {
            try {
                return context.createPackageContextAsUser(
                        /*packageName=*/ context.getPackageName(),
                        /*flags=*/ 0,
                        /*user=*/ context.getSystemService(UserManager.class)
                                .getProfileParent(context.getUser()));
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "context.createPackageContextAsUser() fail:" + e);
                return null;
            }
        }
        return context;
    }

    static List<ContentValues> sendAppUsageEventData(
            final Context context, final List<AppUsageEvent> appUsageEventList) {
        final long startTime = System.currentTimeMillis();
        // Creates the ContentValues list to insert them into provider.
        final List<ContentValues> valuesList = new ArrayList<>();
        appUsageEventList.stream()
                .filter(appUsageEvent -> appUsageEvent.hasUid())
                .forEach(appUsageEvent -> valuesList.add(
                        ConvertUtils.convertAppUsageEventToContentValues(appUsageEvent)));
        int size = 0;
        final ContentResolver resolver = context.getContentResolver();
        // Inserts all ContentValues into battery provider.
        if (!valuesList.isEmpty()) {
            final ContentValues[] valuesArray = new ContentValues[valuesList.size()];
            valuesList.toArray(valuesArray);
            try {
                size = resolver.bulkInsert(APP_USAGE_EVENT_URI, valuesArray);
                resolver.notifyChange(APP_USAGE_EVENT_URI, /*observer=*/ null);
                Log.d(TAG, "insert() app usage events data into database");
            } catch (Exception e) {
                Log.e(TAG, "bulkInsert() app usage data into database error:\n" + e);
            }
        }
        Log.d(TAG, String.format("sendAppUsageEventData() size=%d in %d/ms",
                size, (System.currentTimeMillis() - startTime)));
        clearMemory();
        return valuesList;
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
        final int batteryLevel = BatteryStatus.getBatteryLevel(intent);
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
                                && (foregroundMs != 0
                                || backgroundMs != 0)) {
                            Log.w(TAG, String.format(
                                    "no consumed power but has running time for %s time=%d|%d",
                                    entry.getLabel(), foregroundMs, backgroundMs));
                        }
                        return entry.getConsumedPower() != 0
                                || foregroundMs != 0
                                || backgroundMs != 0;
                    })
                    .forEach(entry -> valuesList.add(
                            ConvertUtils.convertBatteryEntryToContentValues(
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
                Log.d(TAG, "insert() battery states data into database with isFullChargeStart:"
                        + isFullChargeStart);
            } catch (Exception e) {
                Log.e(TAG, "bulkInsert() battery states data into database error:\n" + e);
            }
        } else {
            // Inserts one fake data into battery provider.
            final ContentValues contentValues =
                    ConvertUtils.convertBatteryEntryToContentValues(
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

    private static long loadAppUsageLatestTimestampFromContentProvider(
            Context context, final Uri appUsageLatestTimestampUri) {
        // We have already make sure the context here is with profile parent's user identity. Don't
        // need to check whether current user is work profile.
        try (Cursor cursor = sFakeAppUsageLatestTimestampSupplier != null
                ? sFakeAppUsageLatestTimestampSupplier.get()
                : context.getContentResolver().query(
                        appUsageLatestTimestampUri, null, null, null)) {
            if (cursor == null || cursor.getCount() == 0) {
                return INVALID_USER_ID;
            }
            cursor.moveToFirst();
            // There is only one column returned so use the index 0 directly.
            final long latestTimestamp = cursor.getLong(/*columnIndex=*/ 0);
            try {
                cursor.close();
            } catch (Exception e) {
                Log.e(TAG, "cursor.close() failed", e);
            }
            // If there is no data for this user, 0 will be returned from the database.
            return latestTimestamp == 0 ? INVALID_USER_ID : latestTimestamp;
        }
    }

    private static List<AppUsageEvent> loadAppUsageEventsFromContentProvider(
            Context context, Uri appUsageEventUri) {
        final List<AppUsageEvent> appUsageEventList = new ArrayList<>();
        context = getParentContext(context);
        if (context == null) {
            return appUsageEventList;
        }
        try (Cursor cursor = sFakeAppUsageEventSupplier != null
                ? sFakeAppUsageEventSupplier.get()
                : context.getContentResolver().query(appUsageEventUri, null, null, null)) {
            if (cursor == null || cursor.getCount() == 0) {
                return appUsageEventList;
            }
            // Loads and recovers all AppUsageEvent data from cursor.
            while (cursor.moveToNext()) {
                appUsageEventList.add(ConvertUtils.convertToAppUsageEventFromCursor(cursor));
            }
            try {
                cursor.close();
            } catch (Exception e) {
                Log.e(TAG, "cursor.close() failed", e);
            }
        }
        return appUsageEventList;
    }

    private static Map<Long, Map<String, BatteryHistEntry>> loadHistoryMapFromContentProvider(
            Context context, Uri batteryStateUri) {
        context = DatabaseUtils.getParentContext(context);
        if (context == null) {
            return null;
        }
        final Map<Long, Map<String, BatteryHistEntry>> resultMap = new HashMap();
        try (Cursor cursor = sFakeBatteryStateSupplier != null ? sFakeBatteryStateSupplier.get() :
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
}
