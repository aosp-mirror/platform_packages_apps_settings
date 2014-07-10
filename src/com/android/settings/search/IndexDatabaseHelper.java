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

package com.android.settings.search;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

public class IndexDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "IndexDatabaseHelper";

    private static final String DATABASE_NAME = "search_index.db";
    private static final int DATABASE_VERSION = 115;

    public interface Tables {
        public static final String TABLE_PREFS_INDEX = "prefs_index";
        public static final String TABLE_META_INDEX = "meta_index";
        public static final String TABLE_SAVED_QUERIES = "saved_queries";
    }

    public interface IndexColumns {
        public static final String DOCID = "docid";
        public static final String LOCALE = "locale";
        public static final String DATA_RANK = "data_rank";
        public static final String DATA_TITLE = "data_title";
        public static final String DATA_TITLE_NORMALIZED = "data_title_normalized";
        public static final String DATA_SUMMARY_ON = "data_summary_on";
        public static final String DATA_SUMMARY_ON_NORMALIZED = "data_summary_on_normalized";
        public static final String DATA_SUMMARY_OFF = "data_summary_off";
        public static final String DATA_SUMMARY_OFF_NORMALIZED = "data_summary_off_normalized";
        public static final String DATA_ENTRIES = "data_entries";
        public static final String DATA_KEYWORDS = "data_keywords";
        public static final String CLASS_NAME = "class_name";
        public static final String SCREEN_TITLE = "screen_title";
        public static final String INTENT_ACTION = "intent_action";
        public static final String INTENT_TARGET_PACKAGE = "intent_target_package";
        public static final String INTENT_TARGET_CLASS = "intent_target_class";
        public static final String ICON = "icon";
        public static final String ENABLED = "enabled";
        public static final String DATA_KEY_REF = "data_key_reference";
        public static final String USER_ID = "user_id";
    }

    public interface MetaColumns {
        public static final String BUILD = "build";
    }

    public interface SavedQueriesColums  {
        public static final String QUERY = "query";
        public static final String TIME_STAMP = "timestamp";
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
                    IndexColumns.DATA_SUMMARY_ON +
                    ", " +
                    IndexColumns.DATA_SUMMARY_ON_NORMALIZED +
                    ", " +
                    IndexColumns.DATA_SUMMARY_OFF +
                    ", " +
                    IndexColumns.DATA_SUMMARY_OFF_NORMALIZED +
                    ", " +
                    IndexColumns.DATA_ENTRIES +
                    ", " +
                    IndexColumns.DATA_KEYWORDS +
                    ", " +
                    IndexColumns.SCREEN_TITLE +
                    ", " +
                    IndexColumns.CLASS_NAME +
                    ", " +
                    IndexColumns.ICON +
                    ", " +
                    IndexColumns.INTENT_ACTION +
                    ", " +
                    IndexColumns.INTENT_TARGET_PACKAGE +
                    ", " +
                    IndexColumns.INTENT_TARGET_CLASS +
                    ", " +
                    IndexColumns.ENABLED +
                    ", " +
                    IndexColumns.DATA_KEY_REF +
                    ", " +
                    IndexColumns.USER_ID +
                    ");";

    private static final String CREATE_META_TABLE =
            "CREATE TABLE " + Tables.TABLE_META_INDEX +
                    "(" +
                    MetaColumns.BUILD + " VARCHAR(32) NOT NULL" +
                    ")";

    private static final String CREATE_SAVED_QUERIES_TABLE =
            "CREATE TABLE " + Tables.TABLE_SAVED_QUERIES +
                    "(" +
                    SavedQueriesColums.QUERY + " VARCHAR(64) NOT NULL" +
                    ", " +
                    SavedQueriesColums.TIME_STAMP + " INTEGER" +
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
        db.execSQL(CREATE_SAVED_QUERIES_TABLE);
        db.execSQL(INSERT_BUILD_VERSION);
        Log.i(TAG, "Bootstrapped database");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);

        Log.i(TAG, "Using schema version: " + db.getVersion());

        if (!Build.VERSION.INCREMENTAL.equals(getBuildVersion(db))) {
            Log.w(TAG, "Index needs to be rebuilt as build-version is not the same");
            // We need to drop the tables and recreate them
            reconstruct(db);
        } else {
            Log.i(TAG, "Index is fine");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < DATABASE_VERSION) {
            Log.w(TAG, "Detected schema version '" +  oldVersion + "'. " +
                    "Index needs to be rebuilt for schema version '" + newVersion + "'.");
            // We need to drop the tables and recreate them
            reconstruct(db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Detected schema version '" +  oldVersion + "'. " +
                "Index needs to be rebuilt for schema version '" + newVersion + "'.");
        // We need to drop the tables and recreate them
        reconstruct(db);
    }

    private void reconstruct(SQLiteDatabase db) {
        dropTables(db);
        bootstrapDB(db);
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
        db.execSQL("DROP TABLE IF EXISTS " + Tables.TABLE_SAVED_QUERIES);
    }
}
