/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.indexer;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

public class IndexDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "IndexDatabaseHelper";

    private static final String DATABASE_NAME = "search_index.db";
    private static final int DATABASE_VERSION = 101;

    public interface Tables {
        public static final String TABLE_PREFS_INDEX = "prefs_index";
        public static final String TABLE_META_INDEX = "meta_index";
    }

    public interface IndexColumns {
        public static final String LOCALE = "locale";
        public static final String DATA_RANK = "data_rank";
        public static final String DATA_TITLE = "data_title";
        public static final String DATA_TITLE_NORMALIZED = "data_title_normalized";
        public static final String DATA_SUMMARY = "data_summary";
        public static final String DATA_SUMMARY_NORMALIZED = "data_summary_normalized";
        public static final String DATA_KEYWORDS = "data_keywords";
        public static final String FRAGMENT_NAME = "fragment_name";
        public static final String FRAGMENT_TITLE = "fragment_title";
        public static final String INTENT = "intent";
        public static final String ICON = "icon";
    }

    public interface MetaColumns {
        public static final String BUILD = "build";
    }

    private static final String CREATE_INDEX_TABLE =
            "CREATE VIRTUAL TABLE " + Tables.TABLE_PREFS_INDEX + " USING fts4" +
                    "(" +
                    IndexColumns.LOCALE +
                    ", " +
                    IndexColumns.DATA_RANK +
                    ", " +
                    IndexColumns.DATA_TITLE +
                    ", " +
                    IndexColumns.DATA_TITLE_NORMALIZED +
                    ", " +
                    IndexColumns.DATA_SUMMARY +
                    ", " +
                    IndexColumns.DATA_SUMMARY_NORMALIZED +
                    ", " +
                    IndexColumns.DATA_KEYWORDS +
                    ", " +
                    IndexColumns.FRAGMENT_NAME +
                    ", " +
                    IndexColumns.FRAGMENT_TITLE +
                    ", " +
                    IndexColumns.INTENT +
                    ", " +
                    IndexColumns.ICON +
                    ");";

    private static final String CREATE_META_TABLE =
            "CREATE TABLE " + Tables.TABLE_META_INDEX +
                    "(" +
                    MetaColumns.BUILD + " VARCHAR(32) NOT NULL" +
                    ")";

    private static final String INSERT_BUILD_VERSION =
            "INSERT INTO " + Tables.TABLE_META_INDEX +
                    " VALUES ('" + Build.VERSION.INCREMENTAL + "');";

    private static final String SELECT_BUILD_VERSION =
            "SELECT " + MetaColumns.BUILD + " FROM " + Tables.TABLE_META_INDEX + " LIMIT 1;";

    private static IndexDatabaseHelper sSingleton;

    public static synchronized IndexDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new IndexDatabaseHelper(context);
        }
        return sSingleton;
    }

    public IndexDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        bootstrapDB(db);
    }

    private void bootstrapDB(SQLiteDatabase db) {
        db.execSQL(CREATE_INDEX_TABLE);
        db.execSQL(CREATE_META_TABLE);
        db.execSQL(INSERT_BUILD_VERSION);
        Log.i(TAG, "Bootstrapped database");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > 100) {
            Log.w(TAG, "Detected schema version 100. " +
                    "Index needs to be rebuilt for schema version 101");
            // We need to drop the tables and recreate them
            dropTables(db);
            bootstrapDB(db);
        }
    }

    private String getBuildVersion(SQLiteDatabase db) {
        String version = null;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SELECT_BUILD_VERSION, null);
            if (cursor.moveToFirst()) {
                version = cursor.getString(0);
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Cannot get build version from Index metadata");
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return version;
    }

    private void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.TABLE_META_INDEX);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.TABLE_PREFS_INDEX);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);

        Log.i(TAG, "Using schema version: " + db.getVersion());

        if (!Build.VERSION.INCREMENTAL.equals(getBuildVersion(db))) {
            Log.w(TAG, "Index needs to be rebuilt as build-version is not the same");
            // We need to drop the tables and recreate them
            dropTables(db);
            bootstrapDB(db);
        } else {
            Log.i(TAG, "Index is fine");
        }
    }
}
