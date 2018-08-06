/*
 * Copyright (C) 2017 The Android Open Source Project
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
 *
 */

package com.android.settings.search;


import static com.android.settings.search.DatabaseResultLoader.COLUMN_INDEX_ID;
import static com.android.settings.search.DatabaseResultLoader
        .COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE;
import static com.android.settings.search.DatabaseResultLoader.COLUMN_INDEX_KEY;
import static com.android.settings.search.DatabaseResultLoader.SELECT_COLUMNS;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.CLASS_NAME;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_ENTRIES;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_KEYWORDS;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_KEY_REF;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns
        .DATA_SUMMARY_ON_NORMALIZED;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_TITLE;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_TITLE_NORMALIZED;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DOCID;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.ENABLED;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.ICON;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.INTENT_ACTION;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.INTENT_TARGET_CLASS;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.INTENT_TARGET_PACKAGE;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.LOCALE;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.PAYLOAD;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.PAYLOAD_TYPE;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.SCREEN_TITLE;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.USER_ID;
import static com.android.settings.search.IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.provider.SearchIndexablesContract;
import android.provider.SearchIndexablesContract.SiteMapColumns;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.indexing.IndexData;
import com.android.settings.search.indexing.IndexDataConverter;
import com.android.settings.search.indexing.PreIndexData;
import com.android.settings.search.indexing.PreIndexDataCollector;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Consumes the SearchIndexableProvider content providers.
 * Updates the Resource, Raw Data and non-indexable data for Search.
 *
 * TODO(b/33577327) this class needs to be refactored by moving most of its methods into controllers
 */
public class DatabaseIndexingManager {

    private static final String LOG_TAG = "DatabaseIndexingManager";

    private PreIndexDataCollector mCollector;
    private IndexDataConverter mConverter;

    private Context mContext;

    public DatabaseIndexingManager(Context context) {
        mContext = context;
    }

    /**
     * Accumulate all data and non-indexable keys from each of the content-providers.
     * Only the first indexing for the default language gets static search results - subsequent
     * calls will only gather non-indexable keys.
     */
    public void performIndexing() {
        final long startTime = System.currentTimeMillis();
        final Intent intent = new Intent(SearchIndexablesContract.PROVIDER_INTERFACE);
        final List<ResolveInfo> providers =
                mContext.getPackageManager().queryIntentContentProviders(intent, 0);

        final String localeStr = Locale.getDefault().toString();
        final String fingerprint = Build.FINGERPRINT;
        final String providerVersionedNames =
                IndexDatabaseHelper.buildProviderVersionedNames(providers);

        final boolean isFullIndex = isFullIndex(mContext, localeStr, fingerprint,
                providerVersionedNames);

        if (isFullIndex) {
            rebuildDatabase();
        }

        PreIndexData indexData = getIndexDataFromProviders(providers, isFullIndex);

        final long updateDatabaseStartTime = System.currentTimeMillis();
        updateDatabase(indexData, isFullIndex);
        if (SettingsSearchIndexablesProvider.DEBUG) {
            final long updateDatabaseTime = System.currentTimeMillis() - updateDatabaseStartTime;
            Log.d(LOG_TAG, "performIndexing updateDatabase took time: " + updateDatabaseTime);
        }

        //TODO(63922686): Setting indexed should be a single method, not 3 separate setters.
        IndexDatabaseHelper.setLocaleIndexed(mContext, localeStr);
        IndexDatabaseHelper.setBuildIndexed(mContext, fingerprint);
        IndexDatabaseHelper.setProvidersIndexed(mContext, providerVersionedNames);

        if (SettingsSearchIndexablesProvider.DEBUG) {
            final long indexingTime = System.currentTimeMillis() - startTime;
            Log.d(LOG_TAG, "performIndexing took time: " + indexingTime
                    + "ms. Full index? " + isFullIndex);
        }
    }

    @VisibleForTesting
    PreIndexData getIndexDataFromProviders(List<ResolveInfo> providers, boolean isFullIndex) {
        if (mCollector == null) {
            mCollector = new PreIndexDataCollector(mContext);
        }
        return mCollector.collectIndexableData(providers, isFullIndex);
    }

    /**
     * Checks if the indexed data is obsolete, when either:
     * - Device language has changed
     * - Device has taken an OTA.
     * In both cases, the device requires a full index.
     *
     * @param locale      is the default for the device
     * @param fingerprint id for the current build.
     * @return true if a full index should be preformed.
     */
    @VisibleForTesting
    boolean isFullIndex(Context context, String locale, String fingerprint,
            String providerVersionedNames) {
        final boolean isLocaleIndexed = IndexDatabaseHelper.isLocaleAlreadyIndexed(context, locale);
        final boolean isBuildIndexed = IndexDatabaseHelper.isBuildIndexed(context, fingerprint);
        final boolean areProvidersIndexed = IndexDatabaseHelper
                .areProvidersIndexed(context, providerVersionedNames);

        return !(isLocaleIndexed && isBuildIndexed && areProvidersIndexed);
    }

    /**
     * Drop the currently stored database, and clear the flags which mark the database as indexed.
     */
    private void rebuildDatabase() {
        // Drop the database when the locale or build has changed. This eliminates rows which are
        // dynamically inserted in the old language, or deprecated settings.
        final SQLiteDatabase db = getWritableDatabase();
        IndexDatabaseHelper.getInstance(mContext).reconstruct(db);
    }

    /**
     * Adds new data to the database and verifies the correctness of the ENABLED column.
     * First, the data to be updated and all non-indexable keys are copied locally.
     * Then all new data to be added is inserted.
     * Then search results are verified to have the correct value of enabled.
     * Finally, we record that the locale has been indexed.
     *
     * @param needsReindexing true the database needs to be rebuilt.
     */
    @VisibleForTesting
    void updateDatabase(PreIndexData preIndexData, boolean needsReindexing) {
        final Map<String, Set<String>> nonIndexableKeys = preIndexData.nonIndexableKeys;

        final SQLiteDatabase database = getWritableDatabase();
        if (database == null) {
            Log.w(LOG_TAG, "Cannot indexDatabase Index as I cannot get a writable database");
            return;
        }

        try {
            database.beginTransaction();

            // Convert all Pre-index data to Index data.
            List<IndexData> indexData = getIndexData(preIndexData);
            insertIndexData(database, indexData);

            // Only check for non-indexable key updates after initial index.
            // Enabled state with non-indexable keys is checked when items are first inserted.
            if (!needsReindexing) {
                updateDataInDatabase(database, nonIndexableKeys);
            }

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    @VisibleForTesting
    List<IndexData> getIndexData(PreIndexData data) {
        if (mConverter == null) {
            mConverter = new IndexDataConverter(mContext);
        }
        return mConverter.convertPreIndexDataToIndexData(data);
    }

    /**
     * Inserts all of the entries in {@param indexData} into the {@param database}
     * as Search Data and as part of the Information Hierarchy.
     */
    @VisibleForTesting
    void insertIndexData(SQLiteDatabase database, List<IndexData> indexData) {
        ContentValues values;

        for (IndexData dataRow : indexData) {
            if (TextUtils.isEmpty(dataRow.normalizedTitle)) {
                continue;
            }

            values = new ContentValues();
            values.put(IndexDatabaseHelper.IndexColumns.DOCID, dataRow.getDocId());
            values.put(LOCALE, dataRow.locale);
            values.put(DATA_TITLE, dataRow.updatedTitle);
            values.put(DATA_TITLE_NORMALIZED, dataRow.normalizedTitle);
            values.put(DATA_SUMMARY_ON, dataRow.updatedSummaryOn);
            values.put(DATA_SUMMARY_ON_NORMALIZED, dataRow.normalizedSummaryOn);
            values.put(DATA_ENTRIES, dataRow.entries);
            values.put(DATA_KEYWORDS, dataRow.spaceDelimitedKeywords);
            values.put(CLASS_NAME, dataRow.className);
            values.put(SCREEN_TITLE, dataRow.screenTitle);
            values.put(INTENT_ACTION, dataRow.intentAction);
            values.put(INTENT_TARGET_PACKAGE, dataRow.intentTargetPackage);
            values.put(INTENT_TARGET_CLASS, dataRow.intentTargetClass);
            values.put(ICON, dataRow.iconResId);
            values.put(ENABLED, dataRow.enabled);
            values.put(DATA_KEY_REF, dataRow.key);
            values.put(USER_ID, dataRow.userId);
            values.put(PAYLOAD_TYPE, dataRow.payloadType);
            values.put(PAYLOAD, dataRow.payload);

            database.replaceOrThrow(TABLE_PREFS_INDEX, null, values);

            if (!TextUtils.isEmpty(dataRow.className)
                    && !TextUtils.isEmpty(dataRow.childClassName)) {
                final ContentValues siteMapPair = new ContentValues();
                siteMapPair.put(SiteMapColumns.PARENT_CLASS, dataRow.className);
                siteMapPair.put(SiteMapColumns.PARENT_TITLE, dataRow.screenTitle);
                siteMapPair.put(SiteMapColumns.CHILD_CLASS, dataRow.childClassName);
                siteMapPair.put(SiteMapColumns.CHILD_TITLE, dataRow.updatedTitle);

                database.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_SITE_MAP,
                        null /* nullColumnHack */, siteMapPair);
            }
        }
    }

    /**
     * Upholds the validity of enabled data for the user.
     * All rows which are enabled but are now flagged with non-indexable keys will become disabled.
     * All rows which are disabled but no longer a non-indexable key will become enabled.
     *
     * @param database         The database to validate.
     * @param nonIndexableKeys A map between package name and the set of non-indexable keys for it.
     */
    @VisibleForTesting
    void updateDataInDatabase(SQLiteDatabase database,
            Map<String, Set<String>> nonIndexableKeys) {
        final String whereEnabled = ENABLED + " = 1";
        final String whereDisabled = ENABLED + " = 0";

        final Cursor enabledResults = database.query(TABLE_PREFS_INDEX, SELECT_COLUMNS,
                whereEnabled, null, null, null, null);

        final ContentValues enabledToDisabledValue = new ContentValues();
        enabledToDisabledValue.put(ENABLED, 0);

        String packageName;
        // TODO Refactor: Move these two loops into one method.
        while (enabledResults.moveToNext()) {
            // Package name is the key for remote providers.
            // If package name is null, the provider is Settings.
            packageName = enabledResults.getString(COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE);
            if (packageName == null) {
                packageName = mContext.getPackageName();
            }

            final String key = enabledResults.getString(COLUMN_INDEX_KEY);
            final Set<String> packageKeys = nonIndexableKeys.get(packageName);

            // The indexed item is set to Enabled but is now non-indexable
            if (packageKeys != null && packageKeys.contains(key)) {
                final String whereClause = DOCID + " = " + enabledResults.getInt(COLUMN_INDEX_ID);
                database.update(TABLE_PREFS_INDEX, enabledToDisabledValue, whereClause, null);
            }
        }
        enabledResults.close();

        final Cursor disabledResults = database.query(TABLE_PREFS_INDEX, SELECT_COLUMNS,
                whereDisabled, null, null, null, null);

        final ContentValues disabledToEnabledValue = new ContentValues();
        disabledToEnabledValue.put(ENABLED, 1);

        while (disabledResults.moveToNext()) {
            // Package name is the key for remote providers.
            // If package name is null, the provider is Settings.
            packageName = disabledResults.getString(COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE);
            if (packageName == null) {
                packageName = mContext.getPackageName();
            }

            final String key = disabledResults.getString(COLUMN_INDEX_KEY);
            final Set<String> packageKeys = nonIndexableKeys.get(packageName);

            // The indexed item is set to Disabled but is no longer non-indexable.
            // We do not enable keys when packageKeys is null because it means the keys came
            // from an unrecognized package and therefore should not be surfaced as results.
            if (packageKeys != null && !packageKeys.contains(key)) {
                String whereClause = DOCID + " = " + disabledResults.getInt(COLUMN_INDEX_ID);
                database.update(TABLE_PREFS_INDEX, disabledToEnabledValue, whereClause, null);
            }
        }
        disabledResults.close();
    }

    private SQLiteDatabase getWritableDatabase() {
        try {
            return IndexDatabaseHelper.getInstance(mContext).getWritableDatabase();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Cannot open writable database", e);
            return null;
        }
    }
}