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

import static android.provider.SearchIndexablesContract.COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_CLASS_NAME;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ENTRIES;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_TARGET_CLASS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_TARGET_PACKAGE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_KEY;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_KEYWORDS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_RANK;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SCREEN_TITLE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SUMMARY_OFF;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SUMMARY_ON;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_TITLE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_USER_ID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_CLASS_NAME;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RESID;
import static com.android.settings.search.DatabaseResultLoader.COLUMN_INDEX_ID;
import static com.android.settings.search.DatabaseResultLoader
        .COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE;
import static com.android.settings.search.DatabaseResultLoader.COLUMN_INDEX_KEY;
import static com.android.settings.search.DatabaseResultLoader.SELECT_COLUMNS;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.CLASS_NAME;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_ENTRIES;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_KEYWORDS;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_KEY_REF;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_RANK;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns
        .DATA_SUMMARY_OFF_NORMALIZED;
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

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.SearchIndexableData;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesContract;
import android.support.annotation.DrawableRes;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import com.android.settings.SettingsActivity;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Consumes the SearchIndexableProvider content providers.
 * Updates the Resource, Raw Data and non-indexable data for Search.
 *
 * TODO this class needs to be refactored by moving most of its methods into controllers
 */
public class DatabaseIndexingManager {

    private static final String LOG_TAG = "DatabaseIndexingManager";

    private static final String METRICS_ACTION_SETTINGS_ASYNC_INDEX =
            "search_asynchronous_indexing";

    public static final String FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER =
            "SEARCH_INDEX_DATA_PROVIDER";

    private static final String NODE_NAME_PREFERENCE_SCREEN = "PreferenceScreen";
    private static final String NODE_NAME_CHECK_BOX_PREFERENCE = "CheckBoxPreference";
    private static final String NODE_NAME_LIST_PREFERENCE = "ListPreference";

    private static final List<String> EMPTY_LIST = Collections.emptyList();

    private final String mBaseAuthority;

    @VisibleForTesting
    final AtomicBoolean mIsIndexingComplete = new AtomicBoolean(false);

    @VisibleForTesting
    final UpdateData mDataToProcess = new UpdateData();
    private Context mContext;

    public DatabaseIndexingManager(Context context, String baseAuthority) {
        mContext = context;
        mBaseAuthority = baseAuthority;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public boolean isIndexingComplete() {
        return mIsIndexingComplete.get();
    }

    public void indexDatabase(IndexingCallback callback) {
        IndexingTask task = new IndexingTask(callback);
        task.execute();
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

        final boolean isFullIndex = IndexDatabaseHelper.isFullIndex(mContext, localeStr,
                fingerprint, providerVersionedNames);

        if (isFullIndex) {
            rebuildDatabase();
        }

        for (final ResolveInfo info : providers) {
            if (!DatabaseIndexingUtils.isWellKnownProvider(info, mContext)) {
                continue;
            }
            final String authority = info.providerInfo.authority;
            final String packageName = info.providerInfo.packageName;

            if (isFullIndex) {
                addIndexablesFromRemoteProvider(packageName, authority);
            }
            final long nonIndexableStartTime = System.currentTimeMillis();
            addNonIndexablesKeysFromRemoteProvider(packageName, authority);
            if (SettingsSearchIndexablesProvider.DEBUG) {
                final long nonIndextableTime = System.currentTimeMillis() - nonIndexableStartTime;
                Log.d(LOG_TAG, "performIndexing update non-indexable for package " + packageName
                        + " took time: " + nonIndextableTime);
            }
        }
        final long updateDatabaseStartTime = System.currentTimeMillis();
        updateDatabase(isFullIndex, localeStr);
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

    /**
     * Reconstruct the database in the following cases:
     * - Language has changed
     * - Build has changed
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
     * @param localeStr       the default locale for the device.
     */
    @VisibleForTesting
    void updateDatabase(boolean needsReindexing, String localeStr) {
        final UpdateData copy;

        synchronized (mDataToProcess) {
            copy = mDataToProcess.copy();
            mDataToProcess.clear();
        }

        final List<SearchIndexableData> dataToUpdate = copy.dataToUpdate;
        final Map<String, Set<String>> nonIndexableKeys = copy.nonIndexableKeys;

        final SQLiteDatabase database = getWritableDatabase();
        if (database == null) {
            Log.w(LOG_TAG, "Cannot indexDatabase Index as I cannot get a writable database");
            return;
        }

        try {
            database.beginTransaction();

            // Add new data from Providers at initial index time, or inserted later.
            if (dataToUpdate.size() > 0) {
                addDataToDatabase(database, localeStr, dataToUpdate, nonIndexableKeys);
            }

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

    /**
     * Inserts {@link SearchIndexableData} into the database.
     *
     * @param database         where the data will be inserted.
     * @param localeStr        is the locale of the data to be inserted.
     * @param dataToUpdate     is a {@link List} of the data to be inserted.
     * @param nonIndexableKeys is a {@link Map} from Package Name to a {@link Set} of keys which
     *                         identify search results which should not be surfaced.
     */
    @VisibleForTesting
    void addDataToDatabase(SQLiteDatabase database, String localeStr,
            List<SearchIndexableData> dataToUpdate, Map<String, Set<String>> nonIndexableKeys) {
        final long current = System.currentTimeMillis();

        for (SearchIndexableData data : dataToUpdate) {
            try {
                indexOneSearchIndexableData(database, localeStr, data, nonIndexableKeys);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Cannot index: " + (data != null ? data.className : data)
                        + " for locale: " + localeStr, e);
            }
        }

        final long now = System.currentTimeMillis();
        Log.d(LOG_TAG, "Indexing locale '" + localeStr + "' took " +
                (now - current) + " millis");
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

    @VisibleForTesting
    boolean addIndexablesFromRemoteProvider(String packageName, String authority) {
        try {
            final Context context = mBaseAuthority.equals(authority) ?
                    mContext : mContext.createPackageContext(packageName, 0);

            final Uri uriForResources = buildUriForXmlResources(authority);
            addIndexablesForXmlResourceUri(context, packageName, uriForResources,
                    SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS);

            final Uri uriForRawData = buildUriForRawData(authority);
            addIndexablesForRawDataUri(context, packageName, uriForRawData,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(LOG_TAG, "Could not create context for " + packageName + ": "
                    + Log.getStackTraceString(e));
            return false;
        }
    }

    @VisibleForTesting
    void addNonIndexablesKeysFromRemoteProvider(String packageName,
            String authority) {
        final List<String> keys =
                getNonIndexablesKeysFromRemoteProvider(packageName, authority);

        addNonIndexableKeys(packageName, keys);
    }

    private List<String> getNonIndexablesKeysFromRemoteProvider(String packageName,
            String authority) {
        try {
            final Context packageContext = mContext.createPackageContext(packageName, 0);

            final Uri uriForNonIndexableKeys = buildUriForNonIndexableKeys(authority);
            return getNonIndexablesKeys(packageContext, uriForNonIndexableKeys,
                    SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(LOG_TAG, "Could not create context for " + packageName + ": "
                    + Log.getStackTraceString(e));
            return EMPTY_LIST;
        }
    }

    private List<String> getNonIndexablesKeys(Context packageContext, Uri uri,
            String[] projection) {

        final ContentResolver resolver = packageContext.getContentResolver();
        final Cursor cursor = resolver.query(uri, projection, null, null, null);

        if (cursor == null) {
            Log.w(LOG_TAG, "Cannot add index data for Uri: " + uri.toString());
            return EMPTY_LIST;
        }

        final List<String> result = new ArrayList<>();
        try {
            final int count = cursor.getCount();
            if (count > 0) {
                while (cursor.moveToNext()) {
                    final String key = cursor.getString(COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE);

                    if (TextUtils.isEmpty(key) && Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
                        Log.v(LOG_TAG, "Empty non-indexable key from: "
                                + packageContext.getPackageName());
                        continue;
                    }

                    result.add(key);
                }
            }
            return result;
        } finally {
            cursor.close();
        }
    }

    public void addIndexableData(SearchIndexableData data) {
        synchronized (mDataToProcess) {
            mDataToProcess.dataToUpdate.add(data);
        }
    }

    public void addNonIndexableKeys(String authority, List<String> keys) {
        synchronized (mDataToProcess) {
            if (keys != null && !keys.isEmpty()) {
                mDataToProcess.nonIndexableKeys.put(authority, new ArraySet<>(keys));
            }
        }
    }

    /**
     * Update the Index for a specific class name resources
     *
     * @param className              the class name (typically a fragment name).
     * @param includeInSearchResults true means that you want the bit "enabled" set so that the
     *                               data will be seen included into the search results
     */
    public void updateFromClassNameResource(String className, boolean includeInSearchResults) {
        if (className == null) {
            throw new IllegalArgumentException("class name cannot be null!");
        }
        final SearchIndexableResource res = SearchIndexableResources.getResourceByName(className);
        if (res == null) {
            Log.e(LOG_TAG, "Cannot find SearchIndexableResources for class name: " + className);
            return;
        }
        res.context = mContext;
        res.enabled = includeInSearchResults;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                addIndexableData(res);
                updateDatabase(false, Locale.getDefault().toString());
                res.enabled = false;
            }
        });
    }

    private SQLiteDatabase getWritableDatabase() {
        try {
            return IndexDatabaseHelper.getInstance(mContext).getWritableDatabase();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Cannot open writable database", e);
            return null;
        }
    }

    private static Uri buildUriForXmlResources(String authority) {
        return Uri.parse("content://" + authority + "/" +
                SearchIndexablesContract.INDEXABLES_XML_RES_PATH);
    }

    private static Uri buildUriForRawData(String authority) {
        return Uri.parse("content://" + authority + "/" +
                SearchIndexablesContract.INDEXABLES_RAW_PATH);
    }

    private static Uri buildUriForNonIndexableKeys(String authority) {
        return Uri.parse("content://" + authority + "/" +
                SearchIndexablesContract.NON_INDEXABLES_KEYS_PATH);
    }

    private void addIndexablesForXmlResourceUri(Context packageContext, String packageName,
            Uri uri, String[] projection) {

        final ContentResolver resolver = packageContext.getContentResolver();
        final Cursor cursor = resolver.query(uri, projection, null, null, null);

        if (cursor == null) {
            Log.w(LOG_TAG, "Cannot add index data for Uri: " + uri.toString());
            return;
        }

        try {
            final int count = cursor.getCount();
            if (count > 0) {
                while (cursor.moveToNext()) {
                    final int xmlResId = cursor.getInt(COLUMN_INDEX_XML_RES_RESID);

                    final String className = cursor.getString(COLUMN_INDEX_XML_RES_CLASS_NAME);
                    final int iconResId = cursor.getInt(COLUMN_INDEX_XML_RES_ICON_RESID);

                    final String action = cursor.getString(COLUMN_INDEX_XML_RES_INTENT_ACTION);
                    final String targetPackage = cursor.getString(
                            COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE);
                    final String targetClass = cursor.getString(
                            COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS);

                    SearchIndexableResource sir = new SearchIndexableResource(packageContext);
                    sir.xmlResId = xmlResId;
                    sir.className = className;
                    sir.packageName = packageName;
                    sir.iconResId = iconResId;
                    sir.intentAction = action;
                    sir.intentTargetPackage = targetPackage;
                    sir.intentTargetClass = targetClass;

                    addIndexableData(sir);
                }
            }
        } finally {
            cursor.close();
        }
    }

    private void addIndexablesForRawDataUri(Context packageContext, String packageName,
            Uri uri, String[] projection) {

        final ContentResolver resolver = packageContext.getContentResolver();
        final Cursor cursor = resolver.query(uri, projection, null, null, null);

        if (cursor == null) {
            Log.w(LOG_TAG, "Cannot add index data for Uri: " + uri.toString());
            return;
        }

        try {
            final int count = cursor.getCount();
            if (count > 0) {
                while (cursor.moveToNext()) {
                    final int providerRank = cursor.getInt(COLUMN_INDEX_RAW_RANK);
                    // TODO Remove rank
                    final String title = cursor.getString(COLUMN_INDEX_RAW_TITLE);
                    final String summaryOn = cursor.getString(COLUMN_INDEX_RAW_SUMMARY_ON);
                    final String summaryOff = cursor.getString(COLUMN_INDEX_RAW_SUMMARY_OFF);
                    final String entries = cursor.getString(COLUMN_INDEX_RAW_ENTRIES);
                    final String keywords = cursor.getString(COLUMN_INDEX_RAW_KEYWORDS);

                    final String screenTitle = cursor.getString(COLUMN_INDEX_RAW_SCREEN_TITLE);

                    final String className = cursor.getString(COLUMN_INDEX_RAW_CLASS_NAME);
                    final int iconResId = cursor.getInt(COLUMN_INDEX_RAW_ICON_RESID);

                    final String action = cursor.getString(COLUMN_INDEX_RAW_INTENT_ACTION);
                    final String targetPackage = cursor.getString(
                            COLUMN_INDEX_RAW_INTENT_TARGET_PACKAGE);
                    final String targetClass = cursor.getString(
                            COLUMN_INDEX_RAW_INTENT_TARGET_CLASS);

                    final String key = cursor.getString(COLUMN_INDEX_RAW_KEY);
                    final int userId = cursor.getInt(COLUMN_INDEX_RAW_USER_ID);

                    SearchIndexableRaw data = new SearchIndexableRaw(packageContext);
                    data.title = title;
                    data.summaryOn = summaryOn;
                    data.summaryOff = summaryOff;
                    data.entries = entries;
                    data.keywords = keywords;
                    data.screenTitle = screenTitle;
                    data.className = className;
                    data.packageName = packageName;
                    data.iconResId = iconResId;
                    data.intentAction = action;
                    data.intentTargetPackage = targetPackage;
                    data.intentTargetClass = targetClass;
                    data.key = key;
                    data.userId = userId;

                    addIndexableData(data);
                }
            }
        } finally {
            cursor.close();
        }
    }

    public void indexOneSearchIndexableData(SQLiteDatabase database, String localeStr,
            SearchIndexableData data, Map<String, Set<String>> nonIndexableKeys) {
        if (data instanceof SearchIndexableResource) {
            indexOneResource(database, localeStr, (SearchIndexableResource) data, nonIndexableKeys);
        } else if (data instanceof SearchIndexableRaw) {
            indexOneRaw(database, localeStr, (SearchIndexableRaw) data, nonIndexableKeys);
        }
    }

    private void indexOneRaw(SQLiteDatabase database, String localeStr,
            SearchIndexableRaw raw, Map<String, Set<String>> nonIndexableKeysFromResource) {
        // Should be the same locale as the one we are processing
        if (!raw.locale.toString().equalsIgnoreCase(localeStr)) {
            return;
        }

        Set<String> packageKeys = nonIndexableKeysFromResource.get(raw.intentTargetPackage);
        boolean enabled = raw.enabled;

        if (packageKeys != null && packageKeys.contains(raw.key)) {
            enabled = false;
        }

        DatabaseRow.Builder builder = new DatabaseRow.Builder();
        builder.setLocale(localeStr)
                .setEntries(raw.entries)
                .setClassName(raw.className)
                .setScreenTitle(raw.screenTitle)
                .setIconResId(raw.iconResId)
                .setRank(raw.rank)
                .setIntentAction(raw.intentAction)
                .setIntentTargetPackage(raw.intentTargetPackage)
                .setIntentTargetClass(raw.intentTargetClass)
                .setEnabled(enabled)
                .setKey(raw.key)
                .setUserId(raw.userId);

        updateOneRowWithFilteredData(database, builder, raw.title, raw.summaryOn, raw.summaryOff,
                raw.keywords);
    }

    private void indexOneResource(SQLiteDatabase database, String localeStr,
            SearchIndexableResource sir, Map<String, Set<String>> nonIndexableKeysFromResource) {

        if (sir == null) {
            Log.e(LOG_TAG, "Cannot index a null resource!");
            return;
        }

        final List<String> nonIndexableKeys = new ArrayList<String>();

        if (sir.xmlResId > SearchIndexableResources.NO_DATA_RES_ID) {
            Set<String> resNonIndexableKeys = nonIndexableKeysFromResource.get(sir.packageName);
            if (resNonIndexableKeys != null && resNonIndexableKeys.size() > 0) {
                nonIndexableKeys.addAll(resNonIndexableKeys);
            }

            indexFromResource(database, localeStr, sir, nonIndexableKeys);
        } else {
            if (TextUtils.isEmpty(sir.className)) {
                Log.w(LOG_TAG, "Cannot index an empty Search Provider name!");
                return;
            }

            final Class<?> clazz = DatabaseIndexingUtils.getIndexableClass(sir.className);
            if (clazz == null) {
                Log.d(LOG_TAG, "SearchIndexableResource '" + sir.className +
                        "' should implement the " + Indexable.class.getName() + " interface!");
                return;
            }

            // Will be non null only for a Local provider implementing a
            // SEARCH_INDEX_DATA_PROVIDER field
            final Indexable.SearchIndexProvider provider =
                    DatabaseIndexingUtils.getSearchIndexProvider(clazz);
            if (provider != null) {
                List<String> providerNonIndexableKeys = provider.getNonIndexableKeys(sir.context);
                if (providerNonIndexableKeys != null && providerNonIndexableKeys.size() > 0) {
                    nonIndexableKeys.addAll(providerNonIndexableKeys);
                }

                indexFromProvider(database, localeStr, provider, sir, nonIndexableKeys);
            }
        }
    }

    @VisibleForTesting
    void indexFromResource(SQLiteDatabase database, String localeStr,
            SearchIndexableResource sir, List<String> nonIndexableKeys) {
        final Context context = sir.context;
        XmlResourceParser parser = null;
        try {
            parser = context.getResources().getXml(sir.xmlResId);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }

            String nodeName = parser.getName();
            if (!NODE_NAME_PREFERENCE_SCREEN.equals(nodeName)) {
                throw new RuntimeException(
                        "XML document must start with <PreferenceScreen> tag; found"
                                + nodeName + " at " + parser.getPositionDescription());
            }

            final int outerDepth = parser.getDepth();
            final AttributeSet attrs = Xml.asAttributeSet(parser);

            final String screenTitle = XmlParserUtils.getDataTitle(context, attrs);
            String key = XmlParserUtils.getDataKey(context, attrs);

            String title;
            String headerTitle;
            String summary;
            String headerSummary;
            String keywords;
            String headerKeywords;
            String childFragment;
            @DrawableRes
            int iconResId;
            ResultPayload payload;
            boolean enabled;
            final String fragmentName = sir.className;
            final int rank = sir.rank;
            final String intentAction = sir.intentAction;
            final String intentTargetPackage = sir.intentTargetPackage;
            final String intentTargetClass = sir.intentTargetClass;

            Map<String, PreferenceControllerMixin> controllerUriMap = null;

            if (fragmentName != null) {
                controllerUriMap = DatabaseIndexingUtils
                        .getPreferenceControllerUriMap(fragmentName, context);
            }

            // Insert rows for the main PreferenceScreen node. Rewrite the data for removing
            // hyphens.

            headerTitle = XmlParserUtils.getDataTitle(context, attrs);
            headerSummary = XmlParserUtils.getDataSummary(context, attrs);
            headerKeywords = XmlParserUtils.getDataKeywords(context, attrs);
            enabled = !nonIndexableKeys.contains(key);

            // TODO: Set payload type for header results
            DatabaseRow.Builder headerBuilder = new DatabaseRow.Builder();
            headerBuilder.setLocale(localeStr)
                    .setEntries(null)
                    .setClassName(fragmentName)
                    .setScreenTitle(screenTitle)
                    .setRank(rank)
                    .setIntentAction(intentAction)
                    .setIntentTargetPackage(intentTargetPackage)
                    .setIntentTargetClass(intentTargetClass)
                    .setEnabled(enabled)
                    .setKey(key)
                    .setUserId(-1 /* default user id */);

            // Flag for XML headers which a child element's title.
            boolean isHeaderUnique = true;
            DatabaseRow.Builder builder;

            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                nodeName = parser.getName();

                title = XmlParserUtils.getDataTitle(context, attrs);
                key = XmlParserUtils.getDataKey(context, attrs);
                enabled = !nonIndexableKeys.contains(key);
                keywords = XmlParserUtils.getDataKeywords(context, attrs);
                iconResId = XmlParserUtils.getDataIcon(context, attrs);

                if (isHeaderUnique && TextUtils.equals(headerTitle, title)) {
                    isHeaderUnique = false;
                }

                builder = new DatabaseRow.Builder();
                builder.setLocale(localeStr)
                        .setClassName(fragmentName)
                        .setScreenTitle(screenTitle)
                        .setIconResId(iconResId)
                        .setRank(rank)
                        .setIntentAction(intentAction)
                        .setIntentTargetPackage(intentTargetPackage)
                        .setIntentTargetClass(intentTargetClass)
                        .setEnabled(enabled)
                        .setKey(key)
                        .setUserId(-1 /* default user id */);

                if (!nodeName.equals(NODE_NAME_CHECK_BOX_PREFERENCE)) {
                    summary = XmlParserUtils.getDataSummary(context, attrs);

                    String entries = null;

                    if (nodeName.endsWith(NODE_NAME_LIST_PREFERENCE)) {
                        entries = XmlParserUtils.getDataEntries(context, attrs);
                    }

                    // TODO (b/62254931) index primitives instead of payload
                    payload = DatabaseIndexingUtils.getPayloadFromUriMap(controllerUriMap, key);
                    childFragment = XmlParserUtils.getDataChildFragment(context, attrs);

                    builder.setEntries(entries)
                            .setChildClassName(childFragment)
                            .setPayload(payload);

                    // Insert rows for the child nodes of PreferenceScreen
                    updateOneRowWithFilteredData(database, builder, title, summary,
                            null /* summary off */, keywords);
                } else {
                    String summaryOn = XmlParserUtils.getDataSummaryOn(context, attrs);
                    String summaryOff = XmlParserUtils.getDataSummaryOff(context, attrs);

                    if (TextUtils.isEmpty(summaryOn) && TextUtils.isEmpty(summaryOff)) {
                        summaryOn = XmlParserUtils.getDataSummary(context, attrs);
                    }

                    updateOneRowWithFilteredData(database, builder, title, summaryOn, summaryOff,
                            keywords);
                }
            }

            // The xml header's title does not match the title of one of the child settings.
            if (isHeaderUnique) {
                updateOneRowWithFilteredData(database, headerBuilder, headerTitle, headerSummary,
                        null /* summary off */, headerKeywords);
            }
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Error parsing PreferenceScreen", e);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing PreferenceScreen", e);
        } finally {
            if (parser != null) parser.close();
        }
    }

    private void indexFromProvider(SQLiteDatabase database, String localeStr,
            Indexable.SearchIndexProvider provider, SearchIndexableResource sir,
            List<String> nonIndexableKeys) {

        final String className = sir.className;
        final String intentAction = sir.intentAction;
        final String intentTargetPackage = sir.intentTargetPackage;

        if (provider == null) {
            Log.w(LOG_TAG, "Cannot find provider: " + className);
            return;
        }

        final List<SearchIndexableRaw> rawList = provider.getRawDataToIndex(mContext,
                true /* enabled */);

        if (rawList != null) {

            final int rawSize = rawList.size();
            for (int i = 0; i < rawSize; i++) {
                SearchIndexableRaw raw = rawList.get(i);

                // Should be the same locale as the one we are processing
                if (!raw.locale.toString().equalsIgnoreCase(localeStr)) {
                    continue;
                }
                boolean enabled = !nonIndexableKeys.contains(raw.key);

                DatabaseRow.Builder builder = new DatabaseRow.Builder();
                builder.setLocale(localeStr)
                        .setEntries(raw.entries)
                        .setClassName(className)
                        .setScreenTitle(raw.screenTitle)
                        .setIconResId(raw.iconResId)
                        .setIntentAction(raw.intentAction)
                        .setIntentTargetPackage(raw.intentTargetPackage)
                        .setIntentTargetClass(raw.intentTargetClass)
                        .setEnabled(enabled)
                        .setKey(raw.key)
                        .setUserId(raw.userId);

                updateOneRowWithFilteredData(database, builder, raw.title, raw.summaryOn,
                        raw.summaryOff, raw.keywords);
            }
        }

        final List<SearchIndexableResource> resList =
                provider.getXmlResourcesToIndex(mContext, true);
        if (resList != null) {
            final int resSize = resList.size();
            for (int i = 0; i < resSize; i++) {
                SearchIndexableResource item = resList.get(i);

                // Should be the same locale as the one we are processing
                if (!item.locale.toString().equalsIgnoreCase(localeStr)) {
                    continue;
                }

                item.className = TextUtils.isEmpty(item.className)
                        ? className
                        : item.className;
                item.intentAction = TextUtils.isEmpty(item.intentAction)
                        ? intentAction
                        : item.intentAction;
                item.intentTargetPackage = TextUtils.isEmpty(item.intentTargetPackage)
                        ? intentTargetPackage
                        : item.intentTargetPackage;

                indexFromResource(database, localeStr, item, nonIndexableKeys);
            }
        }
    }

    private void updateOneRowWithFilteredData(SQLiteDatabase database, DatabaseRow.Builder builder,
            String title, String summaryOn, String summaryOff, String keywords) {

        final String updatedTitle = DatabaseIndexingUtils.normalizeHyphen(title);
        final String updatedSummaryOn = DatabaseIndexingUtils.normalizeHyphen(summaryOn);
        final String updatedSummaryOff = DatabaseIndexingUtils.normalizeHyphen(summaryOff);

        final String normalizedTitle = DatabaseIndexingUtils.normalizeString(updatedTitle);
        final String normalizedSummaryOn = DatabaseIndexingUtils.normalizeString(updatedSummaryOn);
        final String normalizedSummaryOff = DatabaseIndexingUtils
                .normalizeString(updatedSummaryOff);

        final String spaceDelimitedKeywords = DatabaseIndexingUtils.normalizeKeywords(keywords);

        builder.setUpdatedTitle(updatedTitle)
                .setUpdatedSummaryOn(updatedSummaryOn)
                .setUpdatedSummaryOff(updatedSummaryOff)
                .setNormalizedTitle(normalizedTitle)
                .setNormalizedSummaryOn(normalizedSummaryOn)
                .setNormalizedSummaryOff(normalizedSummaryOff)
                .setSpaceDelimitedKeywords(spaceDelimitedKeywords);

        updateOneRow(database, builder.build(mContext));
    }

    private void updateOneRow(SQLiteDatabase database, DatabaseRow row) {

        if (TextUtils.isEmpty(row.updatedTitle)) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(IndexDatabaseHelper.IndexColumns.DOCID, row.getDocId());
        values.put(LOCALE, row.locale);
        values.put(DATA_RANK, row.rank);
        values.put(DATA_TITLE, row.updatedTitle);
        values.put(DATA_TITLE_NORMALIZED, row.normalizedTitle);
        values.put(DATA_SUMMARY_ON, row.updatedSummaryOn);
        values.put(DATA_SUMMARY_ON_NORMALIZED, row.normalizedSummaryOn);
        values.put(DATA_SUMMARY_OFF, row.updatedSummaryOff);
        values.put(DATA_SUMMARY_OFF_NORMALIZED, row.normalizedSummaryOff);
        values.put(DATA_ENTRIES, row.entries);
        values.put(DATA_KEYWORDS, row.spaceDelimitedKeywords);
        values.put(CLASS_NAME, row.className);
        values.put(SCREEN_TITLE, row.screenTitle);
        values.put(INTENT_ACTION, row.intentAction);
        values.put(INTENT_TARGET_PACKAGE, row.intentTargetPackage);
        values.put(INTENT_TARGET_CLASS, row.intentTargetClass);
        values.put(ICON, row.iconResId);
        values.put(ENABLED, row.enabled);
        values.put(DATA_KEY_REF, row.key);
        values.put(USER_ID, row.userId);
        values.put(PAYLOAD_TYPE, row.payloadType);
        values.put(PAYLOAD, row.payload);

        database.replaceOrThrow(TABLE_PREFS_INDEX, null, values);

        if (!TextUtils.isEmpty(row.className) && !TextUtils.isEmpty(row.childClassName)) {
            ContentValues siteMapPair = new ContentValues();
            final int pairDocId = Objects.hash(row.className, row.childClassName);
            siteMapPair.put(IndexDatabaseHelper.SiteMapColumns.DOCID, pairDocId);
            siteMapPair.put(IndexDatabaseHelper.SiteMapColumns.PARENT_CLASS, row.className);
            siteMapPair.put(IndexDatabaseHelper.SiteMapColumns.PARENT_TITLE, row.screenTitle);
            siteMapPair.put(IndexDatabaseHelper.SiteMapColumns.CHILD_CLASS, row.childClassName);
            siteMapPair.put(IndexDatabaseHelper.SiteMapColumns.CHILD_TITLE, row.updatedTitle);

            database.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_SITE_MAP, null, siteMapPair);
        }
    }

    /**
     * A private class to describe the indexDatabase data for the Index database
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static class UpdateData {
        public List<SearchIndexableData> dataToUpdate;
        public List<SearchIndexableData> dataToDisable;
        public Map<String, Set<String>> nonIndexableKeys;

        public UpdateData() {
            dataToUpdate = new ArrayList<>();
            dataToDisable = new ArrayList<>();
            nonIndexableKeys = new HashMap<>();
        }

        public UpdateData(UpdateData other) {
            dataToUpdate = new ArrayList<>(other.dataToUpdate);
            dataToDisable = new ArrayList<>(other.dataToDisable);
            nonIndexableKeys = new HashMap<>(other.nonIndexableKeys);
        }

        public UpdateData copy() {
            return new UpdateData(this);
        }

        public void clear() {
            dataToUpdate.clear();
            dataToDisable.clear();
            nonIndexableKeys.clear();
        }
    }

    public static class DatabaseRow {
        public final String locale;
        public final String updatedTitle;
        public final String normalizedTitle;
        public final String updatedSummaryOn;
        public final String normalizedSummaryOn;
        public final String updatedSummaryOff;
        public final String normalizedSummaryOff;
        public final String entries;
        public final String className;
        public final String childClassName;
        public final String screenTitle;
        public final int iconResId;
        public final int rank;
        public final String spaceDelimitedKeywords;
        public final String intentAction;
        public final String intentTargetPackage;
        public final String intentTargetClass;
        public final boolean enabled;
        public final String key;
        public final int userId;
        public final int payloadType;
        public final byte[] payload;

        private DatabaseRow(Builder builder) {
            locale = builder.mLocale;
            updatedTitle = builder.mUpdatedTitle;
            normalizedTitle = builder.mNormalizedTitle;
            updatedSummaryOn = builder.mUpdatedSummaryOn;
            normalizedSummaryOn = builder.mNormalizedSummaryOn;
            updatedSummaryOff = builder.mUpdatedSummaryOff;
            normalizedSummaryOff = builder.mNormalizedSummaryOff;
            entries = builder.mEntries;
            className = builder.mClassName;
            childClassName = builder.mChildClassName;
            screenTitle = builder.mScreenTitle;
            iconResId = builder.mIconResId;
            rank = builder.mRank;
            spaceDelimitedKeywords = builder.mSpaceDelimitedKeywords;
            intentAction = builder.mIntentAction;
            intentTargetPackage = builder.mIntentTargetPackage;
            intentTargetClass = builder.mIntentTargetClass;
            enabled = builder.mEnabled;
            key = builder.mKey;
            userId = builder.mUserId;
            payloadType = builder.mPayloadType;
            payload = builder.mPayload != null ? ResultPayloadUtils.marshall(builder.mPayload)
                    : null;
        }

        /**
         * Returns the doc id for this row.
         */
        public int getDocId() {
            // Eventually we want all DocIds to be the data_reference key. For settings values,
            // this will be preference keys, and for non-settings they should be unique.
            return TextUtils.isEmpty(key)
                    ? Objects.hash(updatedTitle, className, screenTitle, intentTargetClass)
                    : key.hashCode();
        }

        public static class Builder {
            private String mLocale;
            private String mUpdatedTitle;
            private String mNormalizedTitle;
            private String mUpdatedSummaryOn;
            private String mNormalizedSummaryOn;
            private String mUpdatedSummaryOff;
            private String mNormalizedSummaryOff;
            private String mEntries;
            private String mClassName;
            private String mChildClassName;
            private String mScreenTitle;
            private int mIconResId;
            private int mRank;
            private String mSpaceDelimitedKeywords;
            private String mIntentAction;
            private String mIntentTargetPackage;
            private String mIntentTargetClass;
            private boolean mEnabled;
            private String mKey;
            private int mUserId;
            @ResultPayload.PayloadType
            private int mPayloadType;
            private ResultPayload mPayload;

            public Builder setLocale(String locale) {
                mLocale = locale;
                return this;
            }

            public Builder setUpdatedTitle(String updatedTitle) {
                mUpdatedTitle = updatedTitle;
                return this;
            }

            public Builder setNormalizedTitle(String normalizedTitle) {
                mNormalizedTitle = normalizedTitle;
                return this;
            }

            public Builder setUpdatedSummaryOn(String updatedSummaryOn) {
                mUpdatedSummaryOn = updatedSummaryOn;
                return this;
            }

            public Builder setNormalizedSummaryOn(String normalizedSummaryOn) {
                mNormalizedSummaryOn = normalizedSummaryOn;
                return this;
            }

            public Builder setUpdatedSummaryOff(String updatedSummaryOff) {
                mUpdatedSummaryOff = updatedSummaryOff;
                return this;
            }

            public Builder setNormalizedSummaryOff(String normalizedSummaryOff) {
                this.mNormalizedSummaryOff = normalizedSummaryOff;
                return this;
            }

            public Builder setEntries(String entries) {
                mEntries = entries;
                return this;
            }

            public Builder setClassName(String className) {
                mClassName = className;
                return this;
            }

            public Builder setChildClassName(String childClassName) {
                mChildClassName = childClassName;
                return this;
            }

            public Builder setScreenTitle(String screenTitle) {
                mScreenTitle = screenTitle;
                return this;
            }

            public Builder setIconResId(int iconResId) {
                mIconResId = iconResId;
                return this;
            }

            public Builder setRank(int rank) {
                mRank = rank;
                return this;
            }

            public Builder setSpaceDelimitedKeywords(String spaceDelimitedKeywords) {
                mSpaceDelimitedKeywords = spaceDelimitedKeywords;
                return this;
            }

            public Builder setIntentAction(String intentAction) {
                mIntentAction = intentAction;
                return this;
            }

            public Builder setIntentTargetPackage(String intentTargetPackage) {
                mIntentTargetPackage = intentTargetPackage;
                return this;
            }

            public Builder setIntentTargetClass(String intentTargetClass) {
                mIntentTargetClass = intentTargetClass;
                return this;
            }

            public Builder setEnabled(boolean enabled) {
                mEnabled = enabled;
                return this;
            }

            public Builder setKey(String key) {
                mKey = key;
                return this;
            }

            public Builder setUserId(int userId) {
                mUserId = userId;
                return this;
            }

            public Builder setPayload(ResultPayload payload) {
                mPayload = payload;

                if (mPayload != null) {
                    setPayloadType(mPayload.getType());
                }
                return this;
            }

            /**
             * Payload type is added when a Payload is added to the Builder in {setPayload}
             *
             * @param payloadType PayloadType
             * @return The Builder
             */
            private Builder setPayloadType(@ResultPayload.PayloadType int payloadType) {
                mPayloadType = payloadType;
                return this;
            }

            /**
             * Adds intent to inline payloads, or creates an Intent Payload as a fallback if the
             * payload is null.
             */
            private void setIntent(Context context) {
                if (mPayload != null) {
                    return;
                }
                final Intent intent = buildIntent(context);
                mPayload = new ResultPayload(intent);
                mPayloadType = ResultPayload.PayloadType.INTENT;
            }

            /**
             * Adds Intent payload to builder.
             */
            private Intent buildIntent(Context context) {
                final Intent intent;

                boolean isEmptyIntentAction = TextUtils.isEmpty(mIntentAction);
                // No intent action is set, or the intent action is for a subsetting.
                if (isEmptyIntentAction
                        || (!isEmptyIntentAction && TextUtils.equals(mIntentTargetPackage,
                        SearchIndexableResources.SUBSETTING_TARGET_PACKAGE))) {
                    // Action is null, we will launch it as a sub-setting
                    intent = DatabaseIndexingUtils.buildSubsettingIntent(context, mClassName, mKey,
                            mScreenTitle);
                } else {
                    intent = new Intent(mIntentAction);
                    final String targetClass = mIntentTargetClass;
                    if (!TextUtils.isEmpty(mIntentTargetPackage)
                            && !TextUtils.isEmpty(targetClass)) {
                        final ComponentName component = new ComponentName(mIntentTargetPackage,
                                targetClass);
                        intent.setComponent(component);
                    }
                    intent.putExtra(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, mKey);
                }
                return intent;
            }

            public DatabaseRow build(Context context) {
                setIntent(context);
                return new DatabaseRow(this);
            }
        }
    }

    public class IndexingTask extends AsyncTask<Void, Void, Void> {

        @VisibleForTesting
        IndexingCallback mCallback;
        private long mIndexStartTime;

        public IndexingTask(IndexingCallback callback) {
            mCallback = callback;
        }

        @Override
        protected void onPreExecute() {
            mIndexStartTime = System.currentTimeMillis();
            mIsIndexingComplete.set(false);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            performIndexing();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            int indexingTime = (int) (System.currentTimeMillis() - mIndexStartTime);
            FeatureFactory.getFactory(mContext).getMetricsFeatureProvider()
                    .histogram(mContext, METRICS_ACTION_SETTINGS_ASYNC_INDEX, indexingTime);

            mIsIndexingComplete.set(true);
            if (mCallback != null) {
                mCallback.onIndexingFinished();
            }
        }
    }
}
