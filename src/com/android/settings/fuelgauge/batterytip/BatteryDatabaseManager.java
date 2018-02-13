/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.fuelgauge.batterytip;

import static com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper.AnomalyColumns
        .ANOMALY_STATE;
import static com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper.AnomalyColumns
        .PACKAGE_NAME;
import static com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper.AnomalyColumns
        .ANOMALY_TYPE;
import static com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper.AnomalyColumns
        .TIME_STAMP_MS;
import static com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper.Tables.TABLE_ANOMALY;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Database manager for battery data. Now it only contains anomaly data stored in {@link AppInfo}.
 */
public class BatteryDatabaseManager {
    private final AnomalyDatabaseHelper mDatabaseHelper;

    public BatteryDatabaseManager(Context context) {
        mDatabaseHelper = AnomalyDatabaseHelper.getInstance(context);
    }

    /**
     * Insert an anomaly log to database.
     *
     * @param packageName the package name of the app
     * @param type        the type of the anomaly
     * @param timestampMs the time when it is happened
     */
    public void insertAnomaly(String packageName, int type, long timestampMs) {
        try (SQLiteDatabase db = mDatabaseHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(PACKAGE_NAME, packageName);
            values.put(ANOMALY_TYPE, type);
            values.put(TIME_STAMP_MS, timestampMs);
            values.put(ANOMALY_STATE, AnomalyDatabaseHelper.State.NEW);
            db.insert(TABLE_ANOMALY, null, values);
        }
    }

    /**
     * Query all the anomalies that happened after {@code timestampMsAfter} and with {@code state}.
     */
    public List<AppInfo> queryAllAnomalies(long timestampMsAfter, int state) {
        final List<AppInfo> appInfos = new ArrayList<>();
        try (SQLiteDatabase db = mDatabaseHelper.getReadableDatabase()) {
            final String[] projection = {PACKAGE_NAME, ANOMALY_TYPE};
            final String orderBy = AnomalyDatabaseHelper.AnomalyColumns.TIME_STAMP_MS + " DESC";

            try (Cursor cursor = db.query(TABLE_ANOMALY, projection,
                    TIME_STAMP_MS + " > ? AND " + ANOMALY_STATE + " = ? ",
                    new String[]{String.valueOf(timestampMsAfter), String.valueOf(state)}, null,
                    null, orderBy)) {
                while (cursor.moveToNext()) {
                    AppInfo appInfo = new AppInfo.Builder()
                            .setPackageName(cursor.getString(cursor.getColumnIndex(PACKAGE_NAME)))
                            .setAnomalyType(cursor.getInt(cursor.getColumnIndex(ANOMALY_TYPE)))
                            .build();
                    appInfos.add(appInfo);
                }
            }
        }

        return appInfos;
    }

    public void deleteAllAnomaliesBeforeTimeStamp(long timestampMs) {
        try (SQLiteDatabase db = mDatabaseHelper.getWritableDatabase()) {
            db.delete(TABLE_ANOMALY, TIME_STAMP_MS + " < ?",
                    new String[]{String.valueOf(timestampMs)});
        }
    }

    /**
     * Update the type of anomalies to {@code state}
     *
     * @param appInfos represents the anomalies
     * @param state    which state to update to
     */
    public void updateAnomalies(List<AppInfo> appInfos, int state) {
        if (!appInfos.isEmpty()) {
            final int size = appInfos.size();
            final String[] whereArgs = new String[size];
            for (int i = 0; i < size; i++) {
                whereArgs[i] = appInfos.get(i).packageName;
            }
            try (SQLiteDatabase db = mDatabaseHelper.getWritableDatabase()) {
                final ContentValues values = new ContentValues();
                values.put(ANOMALY_STATE, state);
                db.update(TABLE_ANOMALY, values, PACKAGE_NAME + " IN (" + TextUtils.join(",",
                        Collections.nCopies(appInfos.size(), "?")) + ")", whereArgs);
            }
        }
    }
}
