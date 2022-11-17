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
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.batteryusage.db.BatteryState;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDao;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDatabase;

import java.time.Clock;
import java.time.Duration;

/** {@link ContentProvider} class to fetch battery usage data. */
public class BatteryUsageContentProvider extends ContentProvider {
    private static final String TAG = "BatteryUsageContentProvider";

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public static final Duration QUERY_DURATION_HOURS = Duration.ofDays(6);

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public static final String QUERY_KEY_TIMESTAMP = "timestamp";

    /** Codes */
    private static final int BATTERY_STATE_CODE = 1;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(
                DatabaseUtils.AUTHORITY,
                /*path=*/ DatabaseUtils.BATTERY_STATE_TABLE,
                /*code=*/ BATTERY_STATE_CODE);
    }

    private Clock mClock;
    private BatteryStateDao mBatteryStateDao;

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public void setClock(Clock clock) {
        this.mClock = clock;
    }

    @Override
    public boolean onCreate() {
        if (DatabaseUtils.isWorkProfile(getContext())) {
            Log.w(TAG, "do not create provider for work profile");
            return false;
        }
        mClock = Clock.systemUTC();
        mBatteryStateDao = BatteryStateDatabase.getInstance(getContext()).batteryStateDao();
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
        switch (sUriMatcher.match(uri)) {
            case BATTERY_STATE_CODE:
                try {
                    mBatteryStateDao.insert(BatteryState.create(contentValues));
                    return uri;
                } catch (RuntimeException e) {
                    Log.e(TAG, "insert() from:" + uri + " error:" + e);
                    return null;
                }
            default:
                throw new IllegalArgumentException("unknown URI: " + uri);
        }
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

    private Cursor getBatteryStates(Uri uri) {
        final long defaultTimestamp = mClock.millis() - QUERY_DURATION_HOURS.toMillis();
        final long queryTimestamp = getQueryTimestamp(uri, defaultTimestamp);
        return getBatteryStates(uri, queryTimestamp);
    }

    private Cursor getBatteryStates(Uri uri, long firstTimestamp) {
        final long timestamp = mClock.millis();
        Cursor cursor = null;
        try {
            cursor = mBatteryStateDao.getCursorSinceLastFullCharge(firstTimestamp);
        } catch (RuntimeException e) {
            Log.e(TAG, "query() from:" + uri + " error:" + e);
        }
        AsyncTask.execute(() -> BootBroadcastReceiver.invokeJobRecheck(getContext()));
        Log.w(TAG, "query battery states in " + (mClock.millis() - timestamp) + "/ms");
        return cursor;
    }

    // If URI contains query parameter QUERY_KEY_TIMESTAMP, use the value directly.
    // Otherwise, load the data for QUERY_DURATION_HOURS by default.
    private long getQueryTimestamp(Uri uri, long defaultTimestamp) {
        final String firstTimestampString = uri.getQueryParameter(QUERY_KEY_TIMESTAMP);
        if (TextUtils.isEmpty(firstTimestampString)) {
            Log.w(TAG, "empty query timestamp");
            return defaultTimestamp;
        }

        try {
            return Long.parseLong(firstTimestampString);
        } catch (NumberFormatException e) {
            Log.e(TAG, "invalid query timestamp: " + firstTimestampString, e);
            return defaultTimestamp;
        }
    }
}
