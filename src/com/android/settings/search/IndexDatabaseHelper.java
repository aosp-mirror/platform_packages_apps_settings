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
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

public class IndexDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "IndexDatabaseHelper";

    private static final String DATABASE_NAME = "search_index.db";
    private static final int DATABASE_VERSION = 117;

    private static final String INDEX = "index";

    private static final String PREF_KEY_INDEXED_PROVIDERS = "indexed_providers";

    public interface Tables {
        String TABLE_PREFS_INDEX = "prefs_index";
        String TABLE_SITE_MAP = "site_map";
        String TABLE_META_INDEX = "meta_index";
        String TABLE_SAVED_QUERIES = "saved_queries";
    }

    public interface IndexColumns {
        String DOCID = "docid";
        String LOCALE = "locale";
        String DATA_RANK = "data_rank";
        String DATA_TITLE = "data_title";
        String DATA_TITLE_NORMALIZED = "data_title_normalized";
        String DATA_SUMMARY_ON = "data_summary_on";
        String DATA_SUMMARY_ON_NORMALIZED = "data_summary_on_normalized";
        String DATA_SUMMARY_OFF = "data_summary_off";
        String DATA_SUMMARY_OFF_NORMALIZED = "data_summary_off_normalized";
        String DATA_ENTRIES = "data_entries";
        String DATA_KEYWORDS = "data_keywords";
        String CLASS_NAME = "class_name";
        String SCREEN_TITLE = "screen_title";
        String INTENT_ACTION = "intent_action";
        String INTENT_TARGET_PACKAGE = "intent_target_package";
        String INTENT_TARGET_CLASS = "intent_target_class";
        String ICON = "icon";
        String ENABLED = "enabled";
        String DATA_KEY_REF = "data_key_reference";
        String USER_ID = "user_id";
        String PAYLOAD_TYPE = "payload_type";
        String PAYLOAD = "payload";
    }

    public interface MetaColumns {
        String BUILD = "build";
    }

    public interface SavedQueriesColumns {
        String QUERY = "query";
        String TIME_STAMP = "timestamp";
    }

    public interface SiteMapColumns {
        String DOCID = "docid";
        String PARENT_CLASS = "parent_class";
        String CHILD_CLASS = "child_class";
        String PARENT_TITLE = "parent_title";
        String CHILD_TITLE = "child_title";
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
                    ", " +
                    IndexColumns.PAYLOAD_TYPE +
                    ", " +
                    IndexColumns.PAYLOAD +
                    ");";

    private static final String CREATE_META_TABLE =
            "CREATE TABLE " + Tables.TABLE_META_INDEX +
                    "(" +
                    MetaColumns.BUILD + " VARCHAR(32) NOT NULL" +
                    ")";

    private static final String CREATE_SAVED_QUERIES_TABLE =
            "CREATE TABLE " + Tables.TABLE_SAVED_QUERIES +
                    "(" +
                    SavedQueriesColumns.QUERY + " VARCHAR(64) NOT NULL" +
                    ", " +
                    SavedQueriesColumns.TIME_STAMP + " INTEGER" +
                    ")";

    private static final String CREATE_SITE_MAP_TABLE =
            "CREATE VIRTUAL TABLE " + Tables.TABLE_SITE_MAP + " USING fts4" +
                    "(" +
                    SiteMapColumns.PARENT_CLASS +
                    ", " +
                    SiteMapColumns.CHILD_CLASS +
                    ", " +
                    SiteMapColumns.PARENT_TITLE +
                    ", " +
                    SiteMapColumns.CHILD_TITLE +
                    ")";
    private static final String INSERT_BUILD_VERSION =
            "INSERT INTO " + Tables.TABLE_META_INDEX +
                    " VALUES ('" + Build.VERSION.INCREMENTAL + "');";

    private static final String SELECT_BUILD_VERSION =
            "SELECT " + MetaColumns.BUILD + " FROM " + Tables.TABLE_META_INDEX + " LIMIT 1;";

    private static IndexDatabaseHelper sSingleton;

    private final Context mContext;

    public static synchronized IndexDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new IndexDatabaseHelper(context);
        }
        return sSingleton;
    }

    public IndexDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        bootstrapDB(db);
    }

    private void bootstrapDB(SQLiteDatabase db) {
        db.execSQL(CREATE_INDEX_TABLE);
        db.execSQL(CREATE_META_TABLE);
        db.execSQL(CREATE_SAVED_QUERIES_TABLE);
        db.execSQL(CREATE_SITE_MAP_TABLE);
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
            Log.w(TAG, "Detected schema version '" + oldVersion + "'. " +
                    "Index needs to be rebuilt for schema version '" + newVersion + "'.");
            // We need to drop the tables and recreate them
            reconstruct(db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Detected schema version '" + oldVersion + "'. " +
                "Index needs to be rebuilt for schema version '" + newVersion + "'.");
        // We need to drop the tables and recreate them
        reconstruct(db);
    }

    public void reconstruct(SQLiteDatabase db) {
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
        } catch (Exception e) {
            Log.e(TAG, "Cannot get build version from Index metadata");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return version;
    }

    /**
     * Perform a full index on an OTA or when the locale has changed
     *
     * @param locale      is the default for the device
     * @param fingerprint id for the current build.
     * @return true when the locale or build has changed since last index.
     */
    @VisibleForTesting
    static boolean isFullIndex(Context context, String locale, String fingerprint,
            String providerVersionedNames) {
        final boolean isLocaleIndexed = IndexDatabaseHelper.isLocaleAlreadyIndexed(context, locale);
        final boolean isBuildIndexed = IndexDatabaseHelper.isBuildIndexed(context, fingerprint);
        final boolean areProvidersIndexed = IndexDatabaseHelper
                .areProvidersIndexed(context, providerVersionedNames);

        return !(isLocaleIndexed && isBuildIndexed && areProvidersIndexed);
    }

    @VisibleForTesting
    static String buildProviderVersionedNames(List<ResolveInfo> providers) {
        StringBuilder sb = new StringBuilder();
        for (ResolveInfo info : providers) {
            sb.append(info.providerInfo.packageName)
                    .append(':')
                    .append(info.providerInfo.applicationInfo.versionCode)
                    .append(',');
        }
        return sb.toString();
    }

    static void clearCachedIndexed(Context context) {
        context.getSharedPreferences(INDEX, Context.MODE_PRIVATE).edit().clear().commit();
    }

    static void setLocaleIndexed(Context context, String locale) {
        context.getSharedPreferences(INDEX, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(locale, true)
                .apply();
    }

    static void setProvidersIndexed(Context context, String providerVersionedNames) {
        context.getSharedPreferences(INDEX, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_KEY_INDEXED_PROVIDERS, providerVersionedNames)
                .apply();
    }

    static boolean isLocaleAlreadyIndexed(Context context, String locale) {
        return context.getSharedPreferences(INDEX, Context.MODE_PRIVATE).getBoolean(locale, false);
    }

    static boolean areProvidersIndexed(Context context, String providerVersionedNames) {
        final String indexedProviders = context.getSharedPreferences(INDEX, Context.MODE_PRIVATE)
                .getString(PREF_KEY_INDEXED_PROVIDERS, null);
        return TextUtils.equals(indexedProviders, providerVersionedNames);
    }

    static boolean isBuildIndexed(Context context, String buildNo) {
        return context.getSharedPreferences(INDEX, Context.MODE_PRIVATE).getBoolean(buildNo, false);
    }

    static void setBuildIndexed(Context context, String buildNo) {
        context.getSharedPreferences(INDEX, 0).edit().putBoolean(buildNo, true).commit();
    }

    private void dropTables(SQLiteDatabase db) {
        clearCachedIndexed(mContext);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.TABLE_META_INDEX);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.TABLE_PREFS_INDEX);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.TABLE_SAVED_QUERIES);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.TABLE_SITE_MAP);
    }
}
