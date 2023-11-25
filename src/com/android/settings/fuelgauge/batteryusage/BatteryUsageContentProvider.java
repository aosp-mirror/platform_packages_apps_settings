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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.batteryusage.db.AppUsageEventDao;
import com.android.settings.fuelgauge.batteryusage.db.AppUsageEventEntity;
import com.android.settings.fuelgauge.batteryusage.db.BatteryEventDao;
import com.android.settings.fuelgauge.batteryusage.db.BatteryEventEntity;
import com.android.settings.fuelgauge.batteryusage.db.BatteryState;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDao;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDatabase;
import com.android.settings.fuelgauge.batteryusage.db.BatteryUsageSlotDao;
import com.android.settings.fuelgauge.batteryusage.db.BatteryUsageSlotEntity;
import com.android.settingslib.fuelgauge.BatteryUtils;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** {@link ContentProvider} class to fetch battery usage data. */
public class BatteryUsageContentProvider extends ContentProvider {
    private static final String TAG = "BatteryUsageContentProvider";

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public static final Duration QUERY_DURATION_HOURS = Duration.ofDays(6);

    /** Codes */
    private static final int BATTERY_STATE_CODE = 1;

    private static final int APP_USAGE_LATEST_TIMESTAMP_CODE = 2;
    private static final int APP_USAGE_EVENT_CODE = 3;
    private static final int BATTERY_EVENT_CODE = 4;
    private static final int LAST_FULL_CHARGE_TIMESTAMP_CODE = 5;
    private static final int BATTERY_STATE_LATEST_TIMESTAMP_CODE = 6;
    private static final int BATTERY_USAGE_SLOT_CODE = 7;

    private static final List<Integer> ALL_BATTERY_EVENT_TYPES =
            Arrays.stream(BatteryEventType.values()).map(type -> type.getNumber()).toList();
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(
                DatabaseUtils.AUTHORITY,
                /* path= */ DatabaseUtils.BATTERY_STATE_TABLE,
                /* code= */ BATTERY_STATE_CODE);
        sUriMatcher.addURI(
                DatabaseUtils.AUTHORITY,
                /* path= */ DatabaseUtils.APP_USAGE_LATEST_TIMESTAMP_PATH,
                /* code= */ APP_USAGE_LATEST_TIMESTAMP_CODE);
        sUriMatcher.addURI(
                DatabaseUtils.AUTHORITY,
                /* path= */ DatabaseUtils.APP_USAGE_EVENT_TABLE,
                /* code= */ APP_USAGE_EVENT_CODE);
        sUriMatcher.addURI(
                DatabaseUtils.AUTHORITY,
                /* path= */ DatabaseUtils.BATTERY_EVENT_TABLE,
                /* code= */ BATTERY_EVENT_CODE);
        sUriMatcher.addURI(
                DatabaseUtils.AUTHORITY,
                /* path= */ DatabaseUtils.LAST_FULL_CHARGE_TIMESTAMP_PATH,
                /* code= */ LAST_FULL_CHARGE_TIMESTAMP_CODE);
        sUriMatcher.addURI(
                DatabaseUtils.AUTHORITY,
                /* path= */ DatabaseUtils.BATTERY_STATE_LATEST_TIMESTAMP_PATH,
                /* code= */ BATTERY_STATE_LATEST_TIMESTAMP_CODE);
        sUriMatcher.addURI(
                DatabaseUtils.AUTHORITY,
                /* path= */ DatabaseUtils.BATTERY_USAGE_SLOT_TABLE,
                /* code= */ BATTERY_USAGE_SLOT_CODE);
    }

    private Clock mClock;
    private BatteryStateDao mBatteryStateDao;
    private AppUsageEventDao mAppUsageEventDao;
    private BatteryEventDao mBatteryEventDao;
    private BatteryUsageSlotDao mBatteryUsageSlotDao;

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public void setClock(Clock clock) {
        this.mClock = clock;
    }

    @Override
    public boolean onCreate() {
        if (BatteryUtils.isWorkProfile(getContext())) {
            Log.w(TAG, "do not create provider for work profile");
            return false;
        }
        mClock = Clock.systemUTC();
        final BatteryStateDatabase database = BatteryStateDatabase.getInstance(getContext());
        mBatteryStateDao = database.batteryStateDao();
        mAppUsageEventDao = database.appUsageEventDao();
        mBatteryEventDao = database.batteryEventDao();
        mBatteryUsageSlotDao = database.batteryUsageSlotDao();
        Log.w(TAG, "create content provider from " + getCallingPackage());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(
            @NonNull Uri uri,
            @Nullable String[] strings,
            @Nullable String s,
            @Nullable String[] strings1,
            @Nullable String s1) {
        switch (sUriMatcher.match(uri)) {
            case BATTERY_STATE_CODE:
                return getBatteryStates(uri);
            case APP_USAGE_EVENT_CODE:
                return getAppUsageEvents(uri);
            case APP_USAGE_LATEST_TIMESTAMP_CODE:
                return getAppUsageLatestTimestamp(uri);
            case BATTERY_EVENT_CODE:
                return getBatteryEvents(uri);
            case LAST_FULL_CHARGE_TIMESTAMP_CODE:
                return getLastFullChargeTimestamp(uri);
            case BATTERY_STATE_LATEST_TIMESTAMP_CODE:
                return getBatteryStateLatestTimestamp(uri);
            case BATTERY_USAGE_SLOT_CODE:
                return getBatteryUsageSlots(uri);
            default:
                throw new IllegalArgumentException("unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        try {
            switch (sUriMatcher.match(uri)) {
                case BATTERY_STATE_CODE:
                    mBatteryStateDao.insert(BatteryState.create(contentValues));
                    break;
                case APP_USAGE_EVENT_CODE:
                    mAppUsageEventDao.insert(AppUsageEventEntity.create(contentValues));
                    break;
                case BATTERY_EVENT_CODE:
                    mBatteryEventDao.insert(BatteryEventEntity.create(contentValues));
                    break;
                case BATTERY_USAGE_SLOT_CODE:
                    mBatteryUsageSlotDao.insert(BatteryUsageSlotEntity.create(contentValues));
                    break;
                default:
                    throw new IllegalArgumentException("unknown URI: " + uri);
            }
        } catch (RuntimeException e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            Log.e(TAG, "insert() from:" + uri + " error:", e);
            return null;
        }
        return uri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        throw new UnsupportedOperationException("unsupported!");
    }

    @Override
    public int update(
            @NonNull Uri uri,
            @Nullable ContentValues contentValues,
            @Nullable String s,
            @Nullable String[] strings) {
        throw new UnsupportedOperationException("unsupported!");
    }

    private Cursor getLastFullChargeTimestamp(Uri uri) {
        final long timestamp = mClock.millis();
        Cursor cursor = null;
        try {
            cursor = mBatteryEventDao.getLastFullChargeTimestamp();
        } catch (RuntimeException e) {
            Log.e(TAG, "query() from:" + uri + " error:", e);
        }
        Log.d(
                TAG,
                String.format(
                        "getLastFullChargeTimestamp() in %d/ms", mClock.millis() - timestamp));
        return cursor;
    }

    private Cursor getBatteryStateLatestTimestamp(Uri uri) {
        final long queryTimestamp = getQueryTimestamp(uri);
        final long timestamp = mClock.millis();
        Cursor cursor = null;
        try {
            cursor = mBatteryStateDao.getLatestTimestampBefore(queryTimestamp);
        } catch (RuntimeException e) {
            Log.e(TAG, "query() from:" + uri + " error:", e);
        }
        Log.d(
                TAG,
                String.format(
                        "getBatteryStateLatestTimestamp() no later than %d in %d/ms",
                        queryTimestamp, mClock.millis() - timestamp));
        return cursor;
    }

    private Cursor getBatteryStates(Uri uri) {
        final long queryTimestamp = getQueryTimestamp(uri);
        final long timestamp = mClock.millis();
        Cursor cursor = null;
        try {
            cursor = mBatteryStateDao.getBatteryStatesAfter(queryTimestamp);
        } catch (RuntimeException e) {
            Log.e(TAG, "query() from:" + uri + " error:", e);
        }
        Log.d(
                TAG,
                String.format(
                        "getBatteryStates() after %d in %d/ms",
                        queryTimestamp, mClock.millis() - timestamp));
        return cursor;
    }

    private Cursor getAppUsageEvents(Uri uri) {
        final List<Long> queryUserIds = getQueryUserIds(uri);
        if (queryUserIds == null || queryUserIds.isEmpty()) {
            return null;
        }
        final long queryTimestamp = getQueryTimestamp(uri);
        final long timestamp = mClock.millis();
        Cursor cursor = null;
        try {
            cursor = mAppUsageEventDao.getAllForUsersAfter(queryUserIds, queryTimestamp);
        } catch (RuntimeException e) {
            Log.e(TAG, "query() from:" + uri + " error:", e);
        }
        Log.w(TAG, "getAppUsageEvents() in " + (mClock.millis() - timestamp) + "/ms");
        return cursor;
    }

    private Cursor getAppUsageLatestTimestamp(Uri uri) {
        final long queryUserId = getQueryUserId(uri);
        if (queryUserId == DatabaseUtils.INVALID_USER_ID) {
            return null;
        }
        final long timestamp = mClock.millis();
        Cursor cursor = null;
        try {
            cursor = mAppUsageEventDao.getLatestTimestampOfUser(queryUserId);
        } catch (RuntimeException e) {
            Log.e(TAG, "query() from:" + uri + " error:", e);
        }
        Log.d(
                TAG,
                String.format(
                        "getAppUsageLatestTimestamp() for user %d in %d/ms",
                        queryUserId, (mClock.millis() - timestamp)));
        return cursor;
    }

    private Cursor getBatteryEvents(Uri uri) {
        List<Integer> queryBatteryEventTypes = getQueryBatteryEventTypes(uri);
        if (queryBatteryEventTypes == null || queryBatteryEventTypes.isEmpty()) {
            queryBatteryEventTypes = ALL_BATTERY_EVENT_TYPES;
        }
        final long queryTimestamp = getQueryTimestamp(uri);
        final long timestamp = mClock.millis();
        Cursor cursor = null;
        try {
            cursor = mBatteryEventDao.getAllAfter(queryTimestamp, queryBatteryEventTypes);
        } catch (RuntimeException e) {
            Log.e(TAG, "query() from:" + uri + " error:", e);
        }
        Log.w(TAG, "getBatteryEvents() in " + (mClock.millis() - timestamp) + "/ms");
        return cursor;
    }

    private Cursor getBatteryUsageSlots(Uri uri) {
        final long queryTimestamp = getQueryTimestamp(uri);
        final long timestamp = mClock.millis();
        Cursor cursor = null;
        try {
            cursor = mBatteryUsageSlotDao.getAllAfter(queryTimestamp);
        } catch (RuntimeException e) {
            Log.e(TAG, "query() from:" + uri + " error:", e);
        }
        Log.w(TAG, "getBatteryUsageSlots() in " + (mClock.millis() - timestamp) + "/ms");
        return cursor;
    }

    private List<Integer> getQueryBatteryEventTypes(Uri uri) {
        Log.d(TAG, "getQueryBatteryEventTypes from uri: " + uri);
        final String batteryEventTypesParameter =
                uri.getQueryParameter(DatabaseUtils.QUERY_BATTERY_EVENT_TYPE);
        if (TextUtils.isEmpty(batteryEventTypesParameter)) {
            return null;
        }
        try {
            List<Integer> batteryEventTypes = new ArrayList<>();
            for (String typeString : batteryEventTypesParameter.split(",")) {
                batteryEventTypes.add(Integer.parseInt(typeString.trim()));
            }
            return batteryEventTypes;
        } catch (NumberFormatException e) {
            Log.e(TAG, "invalid query value: " + batteryEventTypesParameter, e);
            return null;
        }
    }

    // If URI contains query parameter QUERY_KEY_USERID, use the value directly.
    // Otherwise, return null.
    private List<Long> getQueryUserIds(Uri uri) {
        Log.d(TAG, "getQueryUserIds from uri: " + uri);
        final String userIdsParameter = uri.getQueryParameter(DatabaseUtils.QUERY_KEY_USERID);
        if (TextUtils.isEmpty(userIdsParameter)) {
            return null;
        }
        try {
            List<Long> userIds = new ArrayList<>();
            for (String idString : userIdsParameter.split(",")) {
                userIds.add(Long.parseLong(idString.trim()));
            }
            return userIds;
        } catch (NumberFormatException e) {
            Log.e(TAG, "invalid query value: " + userIdsParameter, e);
            return null;
        }
    }

    // If URI contains query parameter QUERY_KEY_USERID, use the value directly.
    // Otherwise, return INVALID_USER_ID.
    private long getQueryUserId(Uri uri) {
        Log.d(TAG, "getQueryUserId from uri: " + uri);
        return getQueryValueFromUri(
                uri, DatabaseUtils.QUERY_KEY_USERID, DatabaseUtils.INVALID_USER_ID);
    }

    // If URI contains query parameter QUERY_KEY_TIMESTAMP, use the value directly.
    // Otherwise, load the data for QUERY_DURATION_HOURS by default.
    private long getQueryTimestamp(Uri uri) {
        Log.d(TAG, "getQueryTimestamp from uri: " + uri);
        final long defaultTimestamp = mClock.millis() - QUERY_DURATION_HOURS.toMillis();
        return getQueryValueFromUri(uri, DatabaseUtils.QUERY_KEY_TIMESTAMP, defaultTimestamp);
    }

    private long getQueryValueFromUri(Uri uri, String key, long defaultValue) {
        final String value = uri.getQueryParameter(key);
        if (TextUtils.isEmpty(value)) {
            Log.w(TAG, "empty query value");
            return defaultValue;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            Log.e(TAG, "invalid query value: " + value, e);
            return defaultValue;
        }
    }
}
