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

import static android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE;
import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;

import static com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper.AnomalyColumns.ANOMALY_STATE;
import static com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper.AnomalyColumns.ANOMALY_TYPE;
import static com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper.AnomalyColumns.PACKAGE_NAME;
import static com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper.AnomalyColumns.TIME_STAMP_MS;
import static com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper.AnomalyColumns.UID;
import static com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper.Tables.TABLE_ACTION;
import static com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper.Tables.TABLE_ANOMALY;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.SparseLongArray;

import androidx.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper.ActionColumns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Database manager for battery data. Now it only contains anomaly data stored in {@link AppInfo}.
 *
 * <p>This manager may be accessed by multi-threads. All the database related methods are
 * synchronized so each operation won't be interfered by other threads.
 */
public class BatteryDatabaseManager {
    private static BatteryDatabaseManager sSingleton;

    private AnomalyDatabaseHelper mDatabaseHelper;

    private BatteryDatabaseManager(Context context) {
        mDatabaseHelper = AnomalyDatabaseHelper.getInstance(context);
    }

    public static synchronized BatteryDatabaseManager getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new BatteryDatabaseManager(context);
        }
        return sSingleton;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static void setUpForTest(BatteryDatabaseManager batteryDatabaseManager) {
        sSingleton = batteryDatabaseManager;
    }

    /**
     * Insert an anomaly log to database.
     *
     * @param uid the uid of the app
     * @param packageName the package name of the app
     * @param type the type of the anomaly
     * @param anomalyState the state of the anomaly
     * @param timestampMs the time when it is happened
     * @return {@code true} if insert operation succeed
     */
    public synchronized boolean insertAnomaly(
            int uid, String packageName, int type, int anomalyState, long timestampMs) {
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UID, uid);
        values.put(PACKAGE_NAME, packageName);
        values.put(ANOMALY_TYPE, type);
        values.put(ANOMALY_STATE, anomalyState);
        values.put(TIME_STAMP_MS, timestampMs);

        return db.insertWithOnConflict(TABLE_ANOMALY, null, values, CONFLICT_IGNORE) != -1;
    }

    /**
     * Query all the anomalies that happened after {@code timestampMsAfter} and with {@code state}.
     */
    public synchronized List<AppInfo> queryAllAnomalies(long timestampMsAfter, int state) {
        final List<AppInfo> appInfos = new ArrayList<>();
        final SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        final String[] projection = {PACKAGE_NAME, ANOMALY_TYPE, UID};
        final String orderBy = AnomalyDatabaseHelper.AnomalyColumns.TIME_STAMP_MS + " DESC";
        final Map<Integer, AppInfo.Builder> mAppInfoBuilders = new ArrayMap<>();
        final String selection = TIME_STAMP_MS + " > ? AND " + ANOMALY_STATE + " = ? ";
        final String[] selectionArgs =
                new String[] {String.valueOf(timestampMsAfter), String.valueOf(state)};

        try (Cursor cursor =
                db.query(
                        TABLE_ANOMALY,
                        projection,
                        selection,
                        selectionArgs,
                        null /* groupBy */,
                        null /* having */,
                        orderBy)) {
            while (cursor.moveToNext()) {
                final int uid = cursor.getInt(cursor.getColumnIndex(UID));
                if (!mAppInfoBuilders.containsKey(uid)) {
                    final AppInfo.Builder builder =
                            new AppInfo.Builder()
                                    .setUid(uid)
                                    .setPackageName(
                                            cursor.getString(cursor.getColumnIndex(PACKAGE_NAME)));
                    mAppInfoBuilders.put(uid, builder);
                }
                mAppInfoBuilders
                        .get(uid)
                        .addAnomalyType(cursor.getInt(cursor.getColumnIndex(ANOMALY_TYPE)));
            }
        }

        for (Integer uid : mAppInfoBuilders.keySet()) {
            appInfos.add(mAppInfoBuilders.get(uid).build());
        }

        return appInfos;
    }

    public synchronized void deleteAllAnomaliesBeforeTimeStamp(long timestampMs) {
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        db.delete(
                TABLE_ANOMALY, TIME_STAMP_MS + " < ?", new String[] {String.valueOf(timestampMs)});
    }

    /**
     * Update the type of anomalies to {@code state}
     *
     * @param appInfos represents the anomalies
     * @param state which state to update to
     */
    public synchronized void updateAnomalies(List<AppInfo> appInfos, int state) {
        if (!appInfos.isEmpty()) {
            final int size = appInfos.size();
            final String[] whereArgs = new String[size];
            for (int i = 0; i < size; i++) {
                whereArgs[i] = appInfos.get(i).packageName;
            }

            final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
            final ContentValues values = new ContentValues();
            values.put(ANOMALY_STATE, state);
            db.update(
                    TABLE_ANOMALY,
                    values,
                    PACKAGE_NAME
                            + " IN ("
                            + TextUtils.join(",", Collections.nCopies(appInfos.size(), "?"))
                            + ")",
                    whereArgs);
        }
    }

    /**
     * Query latest timestamps when an app has been performed action {@code type}
     *
     * @param type of action been performed
     * @return {@link SparseLongArray} where key is uid and value is timestamp
     */
    public synchronized SparseLongArray queryActionTime(
            @AnomalyDatabaseHelper.ActionType int type) {
        final SparseLongArray timeStamps = new SparseLongArray();
        final SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        final String[] projection = {ActionColumns.UID, ActionColumns.TIME_STAMP_MS};
        final String selection = ActionColumns.ACTION_TYPE + " = ? ";
        final String[] selectionArgs = new String[] {String.valueOf(type)};

        try (Cursor cursor =
                db.query(
                        TABLE_ACTION,
                        projection,
                        selection,
                        selectionArgs,
                        null /* groupBy */,
                        null /* having */,
                        null /* orderBy */)) {
            final int uidIndex = cursor.getColumnIndex(ActionColumns.UID);
            final int timestampIndex = cursor.getColumnIndex(ActionColumns.TIME_STAMP_MS);

            while (cursor.moveToNext()) {
                final int uid = cursor.getInt(uidIndex);
                final long timeStamp = cursor.getLong(timestampIndex);
                timeStamps.append(uid, timeStamp);
            }
        }

        return timeStamps;
    }

    /** Insert an action, or update it if already existed */
    public synchronized boolean insertAction(
            @AnomalyDatabaseHelper.ActionType int type,
            int uid,
            String packageName,
            long timestampMs) {
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final ContentValues values = new ContentValues();
        values.put(ActionColumns.UID, uid);
        values.put(ActionColumns.PACKAGE_NAME, packageName);
        values.put(ActionColumns.ACTION_TYPE, type);
        values.put(ActionColumns.TIME_STAMP_MS, timestampMs);

        return db.insertWithOnConflict(TABLE_ACTION, null, values, CONFLICT_REPLACE) != -1;
    }

    /** Remove an action */
    public synchronized boolean deleteAction(
            @AnomalyDatabaseHelper.ActionType int type, int uid, String packageName) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final String where =
                ActionColumns.ACTION_TYPE
                        + " = ? AND "
                        + ActionColumns.UID
                        + " = ? AND "
                        + ActionColumns.PACKAGE_NAME
                        + " = ? ";
        final String[] whereArgs =
                new String[] {
                    String.valueOf(type), String.valueOf(uid), String.valueOf(packageName)
                };

        return db.delete(TABLE_ACTION, where, whereArgs) != 0;
    }
}
