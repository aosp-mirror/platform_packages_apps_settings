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

import static android.content.Intent.FLAG_RECEIVER_REPLACE_PENDING;

import static com.android.settings.fuelgauge.batteryusage.ConvertUtils.utcToLocalTimeForLogging;

import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageStatsManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.BatteryUsageStats;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.BatteryUsageHistoricalLogEntry.Action;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batteryusage.bugreport.BatteryUsageLogUtils;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDatabase;
import com.android.settingslib.fuelgauge.BatteryStatus;

import java.io.PrintWriter;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** A utility class to operate battery usage database. */
public final class DatabaseUtils {
    private static final String TAG = "DatabaseUtils";
    private static final String SHARED_PREFS_FILE = "battery_usage_shared_prefs";

    /** Clear memory threshold for device booting phase. */
    private static final long CLEAR_MEMORY_THRESHOLD_MS = Duration.ofMinutes(5).toMillis();

    private static final long CLEAR_MEMORY_DELAYED_MS = Duration.ofSeconds(2).toMillis();
    private static final long INVALID_TIMESTAMP = 0L;

    static final int DATA_RETENTION_INTERVAL_DAY = 9;
    static final String KEY_LAST_LOAD_FULL_CHARGE_TIME = "last_load_full_charge_time";
    static final String KEY_LAST_UPLOAD_FULL_CHARGE_TIME = "last_upload_full_charge_time";
    static final String KEY_LAST_USAGE_SOURCE = "last_usage_source";
    static final String KEY_DISMISSED_POWER_ANOMALY_KEYS = "dismissed_power_anomaly_keys";

    /** An authority name of the battery content provider. */
    public static final String AUTHORITY = "com.android.settings.battery.usage.provider";

    /** A table name for app usage events. */
    public static final String APP_USAGE_EVENT_TABLE = "AppUsageEvent";

    /** A table name for battery events. */
    public static final String BATTERY_EVENT_TABLE = "BatteryEvent";

    /** A table name for battery usage history. */
    public static final String BATTERY_STATE_TABLE = "BatteryState";

    /** A table name for battery usage slot. */
    public static final String BATTERY_USAGE_SLOT_TABLE = "BatteryUsageSlot";

    /** A path name for last full charge time query. */
    public static final String LAST_FULL_CHARGE_TIMESTAMP_PATH = "lastFullChargeTimestamp";

    /** A path name for querying the latest record timestamp in battery state table. */
    public static final String BATTERY_STATE_LATEST_TIMESTAMP_PATH = "batteryStateLatestTimestamp";

    /** A path name for app usage latest timestamp query. */
    public static final String APP_USAGE_LATEST_TIMESTAMP_PATH = "appUsageLatestTimestamp";

    /** Key for query parameter timestamp used in BATTERY_CONTENT_URI */
    public static final String QUERY_KEY_TIMESTAMP = "timestamp";

    /** Key for query parameter userid used in APP_USAGE_EVENT_URI */
    public static final String QUERY_KEY_USERID = "userid";

    /** Key for query parameter battery event type used in BATTERY_EVENT_URI */
    public static final String QUERY_BATTERY_EVENT_TYPE = "batteryEventType";

    public static final long INVALID_USER_ID = Integer.MIN_VALUE;

    /**
     * The buffer hours to query app usage events that may have begun or ended out of the final
     * desired time frame.
     */
    public static final long USAGE_QUERY_BUFFER_HOURS = Duration.ofHours(3).toMillis();

    /** A content URI to access app usage events data. */
    public static final Uri APP_USAGE_EVENT_URI =
            new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(AUTHORITY)
                    .appendPath(APP_USAGE_EVENT_TABLE)
                    .build();

    /** A content URI to access battery events data. */
    public static final Uri BATTERY_EVENT_URI =
            new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(AUTHORITY)
                    .appendPath(BATTERY_EVENT_TABLE)
                    .build();

    /** A content URI to access battery usage states data. */
    public static final Uri BATTERY_CONTENT_URI =
            new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(AUTHORITY)
                    .appendPath(BATTERY_STATE_TABLE)
                    .build();

    /** A content URI to access battery usage slots data. */
    public static final Uri BATTERY_USAGE_SLOT_URI =
            new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(AUTHORITY)
                    .appendPath(BATTERY_USAGE_SLOT_TABLE)
                    .build();

    /** A list of level record event types to access battery usage data. */
    public static final List<BatteryEventType> BATTERY_LEVEL_RECORD_EVENTS =
            List.of(BatteryEventType.FULL_CHARGED, BatteryEventType.EVEN_HOUR);

    // For testing only.
    @VisibleForTesting static Supplier<Cursor> sFakeSupplier;

    private DatabaseUtils() {}

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
                        .appendQueryParameter(QUERY_KEY_USERID, Long.toString(userId))
                        .build();
        final long latestTimestamp =
                loadLongFromContentProvider(
                        context, appUsageLatestTimestampUri, /* defaultValue= */ INVALID_TIMESTAMP);
        final String latestTimestampString = utcToLocalTimeForLogging(latestTimestamp);
        Log.d(
                TAG,
                String.format(
                        "getAppUsageStartTimestampOfUser() userId=%d latestTimestamp=%s in %d/ms",
                        userId, latestTimestampString, (System.currentTimeMillis() - startTime)));
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
        Log.d(TAG, "sixDaysAgoTimestamp: " + utcToLocalTimeForLogging(sixDaysAgoTimestamp));
        final String queryUserIdString =
                userIds.stream()
                        .map(userId -> String.valueOf(userId))
                        .collect(Collectors.joining(","));
        // Builds the content uri everytime to avoid cache.
        final Uri appUsageEventUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(AUTHORITY)
                        .appendPath(APP_USAGE_EVENT_TABLE)
                        .appendQueryParameter(QUERY_KEY_TIMESTAMP, Long.toString(queryTimestamp))
                        .appendQueryParameter(QUERY_KEY_USERID, queryUserIdString)
                        .build();

        final List<AppUsageEvent> appUsageEventList =
                loadListFromContentProvider(
                        context, appUsageEventUri, ConvertUtils::convertToAppUsageEvent);
        Log.d(
                TAG,
                String.format(
                        "getAppUsageEventForUser userId=%s size=%d in %d/ms",
                        queryUserIdString,
                        appUsageEventList.size(),
                        (System.currentTimeMillis() - startTime)));
        return appUsageEventList;
    }

    /** Returns the battery event data since the query timestamp in battery event table. */
    public static List<BatteryEvent> getBatteryEvents(
            Context context,
            final Calendar calendar,
            final long rawStartTimestamp,
            final List<BatteryEventType> queryBatteryEventTypes) {
        final long startTime = System.currentTimeMillis();
        final long sixDaysAgoTimestamp = getTimestampSixDaysAgo(calendar);
        final long queryTimestamp = Math.max(rawStartTimestamp, sixDaysAgoTimestamp);
        Log.d(TAG, "getBatteryEvents for timestamp: " + queryTimestamp);
        final String queryBatteryEventTypesString =
                queryBatteryEventTypes.stream()
                        .map(type -> String.valueOf(type.getNumber()))
                        .collect(Collectors.joining(","));
        // Builds the content uri everytime to avoid cache.
        final Uri batteryEventUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(AUTHORITY)
                        .appendPath(BATTERY_EVENT_TABLE)
                        .appendQueryParameter(QUERY_KEY_TIMESTAMP, Long.toString(queryTimestamp))
                        .appendQueryParameter(
                                QUERY_BATTERY_EVENT_TYPE, queryBatteryEventTypesString)
                        .build();

        final List<BatteryEvent> batteryEventList =
                loadListFromContentProvider(
                        context, batteryEventUri, ConvertUtils::convertToBatteryEvent);
        Log.d(
                TAG,
                String.format(
                        "getBatteryEvents size=%d in %d/ms",
                        batteryEventList.size(), (System.currentTimeMillis() - startTime)));
        return batteryEventList;
    }

    /**
     * Returns the battery usage slot data after {@code rawStartTimestamp} in battery event table.
     */
    public static List<BatteryUsageSlot> getBatteryUsageSlots(
            Context context, final Calendar calendar, final long rawStartTimestamp) {
        final long startTime = System.currentTimeMillis();
        final long sixDaysAgoTimestamp = getTimestampSixDaysAgo(calendar);
        final long queryTimestamp = Math.max(rawStartTimestamp, sixDaysAgoTimestamp);
        Log.d(TAG, "getBatteryUsageSlots for timestamp: " + queryTimestamp);
        // Builds the content uri everytime to avoid cache.
        final Uri batteryUsageSlotUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(AUTHORITY)
                        .appendPath(BATTERY_USAGE_SLOT_TABLE)
                        .appendQueryParameter(QUERY_KEY_TIMESTAMP, Long.toString(queryTimestamp))
                        .build();

        final List<BatteryUsageSlot> batteryUsageSlotList =
                loadListFromContentProvider(
                        context, batteryUsageSlotUri, ConvertUtils::convertToBatteryUsageSlot);
        Log.d(
                TAG,
                String.format(
                        "getBatteryUsageSlots size=%d in %d/ms",
                        batteryUsageSlotList.size(), (System.currentTimeMillis() - startTime)));
        return batteryUsageSlotList;
    }

    /** Returns the last full charge time. */
    public static long getLastFullChargeTime(Context context) {
        final long startTime = System.currentTimeMillis();
        // Builds the content uri everytime to avoid cache.
        final Uri lastFullChargeTimeUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(AUTHORITY)
                        .appendPath(LAST_FULL_CHARGE_TIMESTAMP_PATH)
                        .build();
        final long lastFullChargeTime =
                loadLongFromContentProvider(
                        context, lastFullChargeTimeUri, /* defaultValue= */ INVALID_TIMESTAMP);
        final String lastFullChargeTimeString = utcToLocalTimeForLogging(lastFullChargeTime);
        Log.d(
                TAG,
                String.format(
                        "getLastFullChargeTime() lastFullChargeTime=%s in %d/ms",
                        lastFullChargeTimeString, (System.currentTimeMillis() - startTime)));
        return lastFullChargeTime;
    }

    /** Returns the first battery state timestamp no later than the {@code queryTimestamp}. */
    @VisibleForTesting
    static long getBatteryStateLatestTimestampBeforeQueryTimestamp(
            Context context, final long queryTimestamp) {
        final long startTime = System.currentTimeMillis();
        // Builds the content uri everytime to avoid cache.
        final Uri batteryStateLatestTimestampUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(AUTHORITY)
                        .appendPath(BATTERY_STATE_LATEST_TIMESTAMP_PATH)
                        .appendQueryParameter(QUERY_KEY_TIMESTAMP, Long.toString(queryTimestamp))
                        .build();
        final long batteryStateLatestTimestamp =
                loadLongFromContentProvider(
                        context,
                        batteryStateLatestTimestampUri,
                        /* defaultValue= */ INVALID_TIMESTAMP);
        final String batteryStateLatestTimestampString =
                utcToLocalTimeForLogging(batteryStateLatestTimestamp);
        Log.d(
                TAG,
                String.format(
                        "getBatteryStateLatestTimestamp() batteryStateLatestTimestamp=%s in %d/ms",
                        batteryStateLatestTimestampString,
                        (System.currentTimeMillis() - startTime)));
        return batteryStateLatestTimestamp;
    }

    /** Returns the battery history map after the given timestamp. */
    @VisibleForTesting
    static Map<Long, Map<String, BatteryHistEntry>> getHistoryMapSinceQueryTimestamp(
            Context context, final long queryTimestamp) {
        final long startTime = System.currentTimeMillis();
        // Builds the content uri everytime to avoid cache.
        final Uri batteryStateUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(AUTHORITY)
                        .appendPath(BATTERY_STATE_TABLE)
                        .appendQueryParameter(QUERY_KEY_TIMESTAMP, Long.toString(queryTimestamp))
                        .build();

        final List<BatteryHistEntry> batteryHistEntryList =
                loadListFromContentProvider(
                        context, batteryStateUri, cursor -> new BatteryHistEntry(cursor));
        final Map<Long, Map<String, BatteryHistEntry>> resultMap = new ArrayMap();
        for (final BatteryHistEntry entry : batteryHistEntryList) {
            final long timestamp = entry.mTimestamp;
            final String key = entry.getKey();
            Map batteryHistEntryMap = resultMap.get(timestamp);
            // Creates new one if there is no corresponding map.
            if (batteryHistEntryMap == null) {
                batteryHistEntryMap = new ArrayMap();
                resultMap.put(timestamp, batteryHistEntryMap);
            }
            batteryHistEntryMap.put(key, entry);
        }

        if (resultMap == null || resultMap.isEmpty()) {
            Log.d(TAG, "getBatteryHistoryMap() returns empty or null");
        } else {
            Log.d(
                    TAG,
                    String.format(
                            "getBatteryHistoryMap() size=%d in %d/ms",
                            resultMap.size(), (System.currentTimeMillis() - startTime)));
        }
        return resultMap;
    }

    /**
     * Returns the battery history map since the latest record no later than the given timestamp. If
     * there is no record before the given timestamp or the given timestamp is before last full
     * charge time, returns the history map since last full charge time.
     */
    public static Map<Long, Map<String, BatteryHistEntry>>
            getHistoryMapSinceLatestRecordBeforeQueryTimestamp(
                    Context context,
                    Calendar calendar,
                    final long queryTimestamp,
                    final long lastFullChargeTime) {
        final long sixDaysAgoTimestamp = getTimestampSixDaysAgo(calendar);
        Log.d(TAG, "sixDaysAgoTimestamp: " + utcToLocalTimeForLogging(sixDaysAgoTimestamp));
        final long batteryStateLatestTimestamp =
                queryTimestamp == 0L
                        ? 0L
                        : getBatteryStateLatestTimestampBeforeQueryTimestamp(
                                context, queryTimestamp);
        final long maxTimestamp =
                Math.max(
                        Math.max(sixDaysAgoTimestamp, lastFullChargeTime),
                        batteryStateLatestTimestamp);
        return getHistoryMapSinceQueryTimestamp(context, maxTimestamp);
    }

    /** Returns the history map since last full charge time. */
    public static Map<Long, Map<String, BatteryHistEntry>> getHistoryMapSinceLastFullCharge(
            Context context, Calendar calendar) {
        final long lastFullChargeTime = getLastFullChargeTime(context);
        return getHistoryMapSinceLatestRecordBeforeQueryTimestamp(
                context, calendar, 0, lastFullChargeTime);
    }

    /** Clears all data in the battery usage database. */
    public static void clearAll(Context context) {
        AsyncTask.execute(
                () -> {
                    try {
                        final BatteryStateDatabase database =
                                BatteryStateDatabase.getInstance(context.getApplicationContext());
                        database.appUsageEventDao().clearAll();
                        database.batteryEventDao().clearAll();
                        database.batteryStateDao().clearAll();
                        database.batteryUsageSlotDao().clearAll();
                    } catch (RuntimeException e) {
                        Log.e(TAG, "clearAll() failed", e);
                    }
                });
    }

    /** Clears all out-of-date data in the battery usage database. */
    public static void clearExpiredDataIfNeeded(Context context) {
        AsyncTask.execute(
                () -> {
                    try {
                        final BatteryStateDatabase database =
                                BatteryStateDatabase.getInstance(context.getApplicationContext());
                        final long earliestTimestamp =
                                Clock.systemUTC().millis()
                                        - Duration.ofDays(DATA_RETENTION_INTERVAL_DAY).toMillis();
                        database.appUsageEventDao().clearAllBefore(earliestTimestamp);
                        database.batteryEventDao().clearAllBefore(earliestTimestamp);
                        database.batteryStateDao().clearAllBefore(earliestTimestamp);
                        database.batteryUsageSlotDao().clearAllBefore(earliestTimestamp);
                    } catch (RuntimeException e) {
                        Log.e(TAG, "clearAllBefore() failed", e);
                    }
                });
    }

    /** Clears all data and jobs if current timestamp is out of the range of last recorded job. */
    public static void clearDataAfterTimeChangedIfNeeded(Context context, Intent intent) {
        if ((intent.getFlags() & FLAG_RECEIVER_REPLACE_PENDING) != 0) {
            BatteryUsageLogUtils.writeLog(
                    context,
                    Action.TIME_UPDATED,
                    "Database is not cleared because the time change intent is only"
                            + " for the existing pending receiver.");
            return;
        }
        AsyncTask.execute(
                () -> {
                    try {
                        clearDataAfterTimeChangedIfNeededInternal(context);
                    } catch (RuntimeException e) {
                        Log.e(TAG, "clearDataAfterTimeChangedIfNeeded() failed", e);
                        BatteryUsageLogUtils.writeLog(
                                context,
                                Action.TIME_UPDATED,
                                "clearDataAfterTimeChangedIfNeeded() failed" + e);
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
        if (com.android.settingslib.fuelgauge.BatteryUtils.isWorkProfile(context)) {
            try {
                return context.createPackageContextAsUser(
                        /* packageName= */ context.getPackageName(),
                        /* flags= */ 0,
                        /* user= */ context.getSystemService(UserManager.class)
                                .getProfileParent(context.getUser()));
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "context.createPackageContextAsUser() fail:", e);
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
                .forEach(
                        appUsageEvent ->
                                valuesList.add(
                                        ConvertUtils.convertAppUsageEventToContentValues(
                                                appUsageEvent)));
        int size = 0;
        final ContentResolver resolver = context.getContentResolver();
        // Inserts all ContentValues into battery provider.
        if (!valuesList.isEmpty()) {
            final ContentValues[] valuesArray = new ContentValues[valuesList.size()];
            valuesList.toArray(valuesArray);
            try {
                size = resolver.bulkInsert(APP_USAGE_EVENT_URI, valuesArray);
                resolver.notifyChange(APP_USAGE_EVENT_URI, /* observer= */ null);
                Log.d(TAG, "insert() app usage events data into database");
            } catch (Exception e) {
                Log.e(TAG, "bulkInsert() app usage data into database error:", e);
            }
        }
        Log.d(
                TAG,
                String.format(
                        "sendAppUsageEventData() size=%d in %d/ms",
                        size, (System.currentTimeMillis() - startTime)));
        clearMemory();
        return valuesList;
    }

    static ContentValues sendBatteryEventData(
            final Context context, final BatteryEvent batteryEvent) {
        final long startTime = System.currentTimeMillis();
        ContentValues contentValues = ConvertUtils.convertBatteryEventToContentValues(batteryEvent);
        final ContentResolver resolver = context.getContentResolver();
        try {
            resolver.insert(BATTERY_EVENT_URI, contentValues);
            Log.d(TAG, "insert() battery event data into database: " + batteryEvent.toString());
        } catch (Exception e) {
            Log.e(TAG, "insert() battery event data into database error:", e);
        }
        Log.d(
                TAG,
                String.format(
                        "sendBatteryEventData() in %d/ms",
                        (System.currentTimeMillis() - startTime)));
        clearMemory();
        return contentValues;
    }

    static List<ContentValues> sendBatteryEventData(
            final Context context, final List<BatteryEvent> batteryEventList) {
        final long startTime = System.currentTimeMillis();
        // Creates the ContentValues list to insert them into provider.
        final List<ContentValues> valuesList = new ArrayList<>();
        batteryEventList.stream()
                .forEach(
                        batteryEvent ->
                                valuesList.add(
                                        ConvertUtils.convertBatteryEventToContentValues(
                                                batteryEvent)));
        int size = 0;
        final ContentResolver resolver = context.getContentResolver();
        // Inserts all ContentValues into battery provider.
        if (!valuesList.isEmpty()) {
            final ContentValues[] valuesArray = new ContentValues[valuesList.size()];
            valuesList.toArray(valuesArray);
            try {
                size = resolver.bulkInsert(BATTERY_EVENT_URI, valuesArray);
                resolver.notifyChange(BATTERY_EVENT_URI, /* observer= */ null);
                Log.d(TAG, "insert() battery event data into database");
            } catch (Exception e) {
                Log.e(TAG, "bulkInsert() battery event data into database error:", e);
            }
        }
        Log.d(
                TAG,
                String.format(
                        "sendBatteryEventData() size=%d in %d/ms",
                        size, (System.currentTimeMillis() - startTime)));
        clearMemory();
        return valuesList;
    }

    static List<ContentValues> sendBatteryUsageSlotData(
            final Context context, final List<BatteryUsageSlot> batteryUsageSlotList) {
        final long startTime = System.currentTimeMillis();
        // Creates the ContentValues list to insert them into provider.
        final List<ContentValues> valuesList = new ArrayList<>();
        batteryUsageSlotList.stream()
                .forEach(
                        batteryUsageSlot ->
                                valuesList.add(
                                        ConvertUtils.convertBatteryUsageSlotToContentValues(
                                                batteryUsageSlot)));
        int size = 0;
        final ContentResolver resolver = context.getContentResolver();
        // Inserts all ContentValues into battery provider.
        if (!valuesList.isEmpty()) {
            final ContentValues[] valuesArray = new ContentValues[valuesList.size()];
            valuesList.toArray(valuesArray);
            try {
                size = resolver.bulkInsert(BATTERY_USAGE_SLOT_URI, valuesArray);
                resolver.notifyChange(BATTERY_USAGE_SLOT_URI, /* observer= */ null);
                Log.d(TAG, "insert() battery usage slots data into database");
            } catch (Exception e) {
                Log.e(TAG, "bulkInsert() battery usage slots data into database error:", e);
            }
        }
        Log.d(
                TAG,
                String.format(
                        "sendBatteryUsageSlotData() size=%d in %d/ms",
                        size, (System.currentTimeMillis() - startTime)));
        clearMemory();
        return valuesList;
    }

    static List<ContentValues> sendBatteryEntryData(
            final Context context,
            final long snapshotTimestamp,
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
        final int batteryStatus =
                intent.getIntExtra(
                        BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        final int batteryHealth =
                intent.getIntExtra(
                        BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
        // We should use the same timestamp for each data snapshot.
        final long snapshotBootTimestamp = SystemClock.elapsedRealtime();

        // Creates the ContentValues list to insert them into provider.
        final List<ContentValues> valuesList = new ArrayList<>();
        if (batteryEntryList != null) {
            for (BatteryEntry entry : batteryEntryList) {
                final long foregroundMs = entry.getTimeInForegroundMs();
                final long foregroundServiceMs = entry.getTimeInForegroundServiceMs();
                final long backgroundMs = entry.getTimeInBackgroundMs();
                if (entry.getConsumedPower() == 0
                        && (foregroundMs != 0 || foregroundServiceMs != 0 || backgroundMs != 0)) {
                    Log.w(
                            TAG,
                            String.format(
                                    "no consumed power but has running time for %s"
                                            + " time=%d|%d|%d",
                                    entry.getLabel(),
                                    foregroundMs,
                                    foregroundServiceMs,
                                    backgroundMs));
                }
                if (entry.getConsumedPower() == 0
                        && foregroundMs == 0
                        && foregroundServiceMs == 0
                        && backgroundMs == 0) {
                    continue;
                }
                valuesList.add(
                        ConvertUtils.convertBatteryEntryToContentValues(
                                entry,
                                batteryUsageStats,
                                batteryLevel,
                                batteryStatus,
                                batteryHealth,
                                snapshotBootTimestamp,
                                snapshotTimestamp,
                                isFullChargeStart));
            }
        }

        int size = 1;
        final ContentResolver resolver = context.getContentResolver();
        String errorMessage = "";
        // Inserts all ContentValues into battery provider.
        if (!valuesList.isEmpty()) {
            final ContentValues[] valuesArray = new ContentValues[valuesList.size()];
            valuesList.toArray(valuesArray);
            try {
                size = resolver.bulkInsert(BATTERY_CONTENT_URI, valuesArray);
                Log.d(
                        TAG,
                        "insert() battery states data into database with isFullChargeStart:"
                                + isFullChargeStart);
            } catch (Exception e) {
                Log.e(TAG, "bulkInsert() data into database error:", e);
            }
        } else {
            // Inserts one fake data into battery provider.
            final ContentValues contentValues =
                    ConvertUtils.convertBatteryEntryToContentValues(
                            /* entry= */ null,
                            /* batteryUsageStats= */ null,
                            batteryLevel,
                            batteryStatus,
                            batteryHealth,
                            snapshotBootTimestamp,
                            snapshotTimestamp,
                            isFullChargeStart);
            try {
                resolver.insert(BATTERY_CONTENT_URI, contentValues);
                Log.d(
                        TAG,
                        "insert() data into database with isFullChargeStart:" + isFullChargeStart);

            } catch (Exception e) {
                Log.e(TAG, "insert() data into database error:", e);
            }
            valuesList.add(contentValues);
        }
        resolver.notifyChange(BATTERY_CONTENT_URI, /* observer= */ null);
        BatteryUsageLogUtils.writeLog(
                context, Action.INSERT_USAGE_DATA, "size=" + size + " " + errorMessage);
        Log.d(
                TAG,
                String.format(
                        "sendBatteryEntryData() size=%d in %d/ms",
                        size, (System.currentTimeMillis() - startTime)));
        if (isFullChargeStart) {
            recordDateTime(context, KEY_LAST_UPLOAD_FULL_CHARGE_TIME);
        }
        clearMemory();
        return valuesList;
    }

    /** Dump all required data into {@link PrintWriter}. */
    public static void dump(Context context, PrintWriter writer) {
        writeString(context, writer, "BatteryLevelChanged", Intent.ACTION_BATTERY_LEVEL_CHANGED);
        writeString(
                context,
                writer,
                "BatteryPlugging",
                BatteryUsageBroadcastReceiver.ACTION_BATTERY_PLUGGING);
        writeString(
                context,
                writer,
                "BatteryUnplugging",
                BatteryUsageBroadcastReceiver.ACTION_BATTERY_UNPLUGGING);
        writeString(
                context,
                writer,
                "ClearBatteryCacheData",
                BatteryUsageBroadcastReceiver.ACTION_CLEAR_BATTERY_CACHE_DATA);
        writeString(context, writer, "LastLoadFullChargeTime", KEY_LAST_LOAD_FULL_CHARGE_TIME);
        writeString(context, writer, "LastUploadFullChargeTime", KEY_LAST_UPLOAD_FULL_CHARGE_TIME);
        writeStringSet(
                context, writer, "DismissedPowerAnomalyKeys", KEY_DISMISSED_POWER_ANOMALY_KEYS);
    }

    static SharedPreferences getSharedPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
    }

    static void removeUsageSource(Context context) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null && sharedPreferences.contains(KEY_LAST_USAGE_SOURCE)) {
            sharedPreferences.edit().remove(KEY_LAST_USAGE_SOURCE).apply();
        }
    }

    /**
     * Returns what App Usage Observers will consider the source of usage for an activity.
     *
     * @see UsageStatsManager#getUsageSource()
     */
    static int getUsageSource(Context context, IUsageStatsManager usageStatsManager) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null && sharedPreferences.contains(KEY_LAST_USAGE_SOURCE)) {
            return sharedPreferences.getInt(
                    KEY_LAST_USAGE_SOURCE, ConvertUtils.DEFAULT_USAGE_SOURCE);
        }
        int usageSource = ConvertUtils.DEFAULT_USAGE_SOURCE;

        try {
            usageSource = usageStatsManager.getUsageSource();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to getUsageSource", e);
        }
        if (sharedPreferences != null) {
            sharedPreferences.edit().putInt(KEY_LAST_USAGE_SOURCE, usageSource).apply();
        }
        return usageSource;
    }

    static void removeDismissedPowerAnomalyKeys(Context context) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null
                && sharedPreferences.contains(KEY_DISMISSED_POWER_ANOMALY_KEYS)) {
            sharedPreferences.edit().remove(KEY_DISMISSED_POWER_ANOMALY_KEYS).apply();
        }
    }

    static Set<String> getDismissedPowerAnomalyKeys(Context context) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences != null
                ? sharedPreferences.getStringSet(KEY_DISMISSED_POWER_ANOMALY_KEYS, new ArraySet<>())
                : new ArraySet<>();
    }

    static void setDismissedPowerAnomalyKeys(Context context, String dismissedPowerAnomalyKey) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            final Set<String> dismissedPowerAnomalyKeys = getDismissedPowerAnomalyKeys(context);
            dismissedPowerAnomalyKeys.add(dismissedPowerAnomalyKey);
            sharedPreferences
                    .edit()
                    .putStringSet(KEY_DISMISSED_POWER_ANOMALY_KEYS, dismissedPowerAnomalyKeys)
                    .apply();
        }
    }

    static void recordDateTime(Context context, String preferenceKey) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences != null) {
            final String currentTime = utcToLocalTimeForLogging(System.currentTimeMillis());
            sharedPreferences.edit().putString(preferenceKey, currentTime).apply();
        }
    }

    @VisibleForTesting
    static <T> T loadFromContentProvider(
            Context context, Uri uri, T defaultValue, Function<Cursor, T> cursorReader) {
        // Transfer work profile to user profile. Please see b/297036263.
        context = getParentContext(context);
        if (context == null) {
            return defaultValue;
        }
        try (Cursor cursor =
                sFakeSupplier != null
                        ? sFakeSupplier.get()
                        : context.getContentResolver().query(uri, null, null, null)) {
            return (cursor == null || cursor.getCount() == 0)
                    ? defaultValue
                    : cursorReader.apply(cursor);
        }
    }

    private static void clearDataAfterTimeChangedIfNeededInternal(Context context) {
        final List<BatteryEvent> batteryLevelRecordEvents =
                DatabaseUtils.getBatteryEvents(
                        context,
                        Calendar.getInstance(),
                        getLastFullChargeTime(context),
                        BATTERY_LEVEL_RECORD_EVENTS);
        final long lastRecordTimestamp =
                batteryLevelRecordEvents.isEmpty()
                        ? INVALID_TIMESTAMP
                        : batteryLevelRecordEvents.get(0).getTimestamp();
        final long nextRecordTimestamp =
                TimestampUtils.getNextEvenHourTimestamp(lastRecordTimestamp);
        final long currentTime = System.currentTimeMillis();
        final boolean isOutOfTimeRange =
                lastRecordTimestamp == INVALID_TIMESTAMP
                        || currentTime < lastRecordTimestamp
                        || currentTime > nextRecordTimestamp;
        final String logInfo =
                String.format(
                        Locale.ENGLISH,
                        "clear database = %b, current time = %d, last record time = %d",
                        isOutOfTimeRange,
                        currentTime,
                        lastRecordTimestamp);
        Log.d(TAG, logInfo);
        BatteryUsageLogUtils.writeLog(context, Action.TIME_UPDATED, logInfo);
        if (isOutOfTimeRange) {
            DatabaseUtils.clearAll(context);
            PeriodicJobManager.getInstance(context)
                    .refreshJob(/* fromBoot= */ false);
        }
    }

    private static long loadLongFromContentProvider(
            Context context, Uri uri, final long defaultValue) {
        return loadFromContentProvider(
                context,
                uri,
                defaultValue,
                cursor ->
                        cursor.moveToFirst() ? cursor.getLong(/* columnIndex= */ 0) : defaultValue);
    }

    private static <E> List<E> loadListFromContentProvider(
            Context context, Uri uri, Function<Cursor, E> converter) {
        return loadFromContentProvider(
                context,
                uri,
                new ArrayList<>(),
                cursor -> {
                    final List<E> list = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        list.add(converter.apply(cursor));
                    }
                    return list;
                });
    }

    private static void writeString(
            Context context, PrintWriter writer, String prefix, String key) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences == null) {
            return;
        }
        final String content = sharedPreferences.getString(key, "");
        writer.println(String.format("\t\t%s: %s", prefix, content));
    }

    private static void writeStringSet(
            Context context, PrintWriter writer, String prefix, String key) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences == null) {
            return;
        }
        final Set<String> results = sharedPreferences.getStringSet(key, new ArraySet<>());
        if (results != null) {
            writer.println(String.format("\t\t%s: %s", prefix, results.toString()));
        }
    }

    private static void clearMemory() {
        if (SystemClock.uptimeMillis() > CLEAR_MEMORY_THRESHOLD_MS) {
            return;
        }
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.postDelayed(
                () -> {
                    System.gc();
                    System.runFinalization();
                    System.gc();
                    Log.w(TAG, "invoke clearMemory()");
                },
                CLEAR_MEMORY_DELAYED_MS);
    }
}
