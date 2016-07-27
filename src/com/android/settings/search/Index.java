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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.SearchIndexableData;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesContract;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;

import com.android.settings.R;
import com.android.settings.search.IndexDatabaseHelper.IndexColumns;
import com.android.settings.search.IndexDatabaseHelper.Tables;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

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
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RANK;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RESID;

public class Index {

    private static final String LOG_TAG = "Index";

    // Those indices should match the indices of SELECT_COLUMNS !
    public static final int COLUMN_INDEX_RANK = 0;
    public static final int COLUMN_INDEX_TITLE = 1;
    public static final int COLUMN_INDEX_SUMMARY_ON = 2;
    public static final int COLUMN_INDEX_SUMMARY_OFF = 3;
    public static final int COLUMN_INDEX_ENTRIES = 4;
    public static final int COLUMN_INDEX_KEYWORDS = 5;
    public static final int COLUMN_INDEX_CLASS_NAME = 6;
    public static final int COLUMN_INDEX_SCREEN_TITLE = 7;
    public static final int COLUMN_INDEX_ICON = 8;
    public static final int COLUMN_INDEX_INTENT_ACTION = 9;
    public static final int COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE = 10;
    public static final int COLUMN_INDEX_INTENT_ACTION_TARGET_CLASS = 11;
    public static final int COLUMN_INDEX_ENABLED = 12;
    public static final int COLUMN_INDEX_KEY = 13;
    public static final int COLUMN_INDEX_USER_ID = 14;

    public static final String ENTRIES_SEPARATOR = "|";

    // If you change the order of columns here, you SHOULD change the COLUMN_INDEX_XXX values
    private static final String[] SELECT_COLUMNS = new String[] {
            IndexColumns.DATA_RANK,               // 0
            IndexColumns.DATA_TITLE,              // 1
            IndexColumns.DATA_SUMMARY_ON,         // 2
            IndexColumns.DATA_SUMMARY_OFF,        // 3
            IndexColumns.DATA_ENTRIES,            // 4
            IndexColumns.DATA_KEYWORDS,           // 5
            IndexColumns.CLASS_NAME,              // 6
            IndexColumns.SCREEN_TITLE,            // 7
            IndexColumns.ICON,                    // 8
            IndexColumns.INTENT_ACTION,           // 9
            IndexColumns.INTENT_TARGET_PACKAGE,   // 10
            IndexColumns.INTENT_TARGET_CLASS,     // 11
            IndexColumns.ENABLED,                 // 12
            IndexColumns.DATA_KEY_REF             // 13
    };

    private static final String[] MATCH_COLUMNS_PRIMARY = {
            IndexColumns.DATA_TITLE,
            IndexColumns.DATA_TITLE_NORMALIZED,
            IndexColumns.DATA_KEYWORDS
    };

    private static final String[] MATCH_COLUMNS_SECONDARY = {
            IndexColumns.DATA_SUMMARY_ON,
            IndexColumns.DATA_SUMMARY_ON_NORMALIZED,
            IndexColumns.DATA_SUMMARY_OFF,
            IndexColumns.DATA_SUMMARY_OFF_NORMALIZED,
            IndexColumns.DATA_ENTRIES
    };

    // Max number of saved search queries (who will be used for proposing suggestions)
    private static long MAX_SAVED_SEARCH_QUERY = 64;
    // Max number of proposed suggestions
    private static final int MAX_PROPOSED_SUGGESTIONS = 5;

    private static final String BASE_AUTHORITY = "com.android.settings";

    private static final String EMPTY = "";
    private static final String NON_BREAKING_HYPHEN = "\u2011";
    private static final String LIST_DELIMITERS = "[,]\\s*";
    private static final String HYPHEN = "-";
    private static final String SPACE = " ";

    private static final String FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER =
            "SEARCH_INDEX_DATA_PROVIDER";

    private static final String NODE_NAME_PREFERENCE_SCREEN = "PreferenceScreen";
    private static final String NODE_NAME_CHECK_BOX_PREFERENCE = "CheckBoxPreference";
    private static final String NODE_NAME_LIST_PREFERENCE = "ListPreference";

    private static final List<String> EMPTY_LIST = Collections.<String>emptyList();

    private static Index sInstance;

    private static final Pattern REMOVE_DIACRITICALS_PATTERN
            = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /**
     * A private class to describe the update data for the Index database
     */
    private static class UpdateData {
        public List<SearchIndexableData> dataToUpdate;
        public List<SearchIndexableData> dataToDelete;
        public Map<String, List<String>> nonIndexableKeys;

        public boolean forceUpdate;
        public boolean fullIndex;

        public UpdateData() {
            dataToUpdate = new ArrayList<SearchIndexableData>();
            dataToDelete = new ArrayList<SearchIndexableData>();
            nonIndexableKeys = new HashMap<String, List<String>>();
        }

        public UpdateData(UpdateData other) {
            dataToUpdate = new ArrayList<SearchIndexableData>(other.dataToUpdate);
            dataToDelete = new ArrayList<SearchIndexableData>(other.dataToDelete);
            nonIndexableKeys = new HashMap<String, List<String>>(other.nonIndexableKeys);
            forceUpdate = other.forceUpdate;
            fullIndex = other.fullIndex;
        }

        public UpdateData copy() {
            return new UpdateData(this);
        }

        public void clear() {
            dataToUpdate.clear();
            dataToDelete.clear();
            nonIndexableKeys.clear();
            forceUpdate = false;
            fullIndex = false;
        }
    }

    private final AtomicBoolean mIsAvailable = new AtomicBoolean(false);
    private final UpdateData mDataToProcess = new UpdateData();
    private Context mContext;
    private final String mBaseAuthority;

    /**
     * A basic singleton
     */
    public static Index getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Index(context.getApplicationContext(), BASE_AUTHORITY);
        }
        return sInstance;
    }

    public Index(Context context, String baseAuthority) {
        mContext = context;
        mBaseAuthority = baseAuthority;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public boolean isAvailable() {
        return mIsAvailable.get();
    }

    public Cursor search(String query) {
        final SQLiteDatabase database = getReadableDatabase();
        final Cursor[] cursors = new Cursor[2];

        final String primarySql = buildSearchSQL(query, MATCH_COLUMNS_PRIMARY, true);
        Log.d(LOG_TAG, "Search primary query: " + primarySql);
        cursors[0] = database.rawQuery(primarySql, null);

        // We need to use an EXCEPT operator as negate MATCH queries do not work.
        StringBuilder sql = new StringBuilder(
                buildSearchSQL(query, MATCH_COLUMNS_SECONDARY, false));
        sql.append(" EXCEPT ");
        sql.append(primarySql);

        final String secondarySql = sql.toString();
        Log.d(LOG_TAG, "Search secondary query: " + secondarySql);
        cursors[1] = database.rawQuery(secondarySql, null);

        return new MergeCursor(cursors);
    }

    public Cursor getSuggestions(String query) {
        final String sql = buildSuggestionsSQL(query);
        Log.d(LOG_TAG, "Suggestions query: " + sql);
        return getReadableDatabase().rawQuery(sql, null);
    }

    private String buildSuggestionsSQL(String query) {
        StringBuilder sb = new StringBuilder();

        sb.append("SELECT ");
        sb.append(IndexDatabaseHelper.SavedQueriesColums.QUERY);
        sb.append(" FROM ");
        sb.append(Tables.TABLE_SAVED_QUERIES);

        if (TextUtils.isEmpty(query)) {
            sb.append(" ORDER BY rowId DESC");
        } else {
            sb.append(" WHERE ");
            sb.append(IndexDatabaseHelper.SavedQueriesColums.QUERY);
            sb.append(" LIKE ");
            sb.append("'");
            sb.append(query);
            sb.append("%");
            sb.append("'");
        }

        sb.append(" LIMIT ");
        sb.append(MAX_PROPOSED_SUGGESTIONS);

        return sb.toString();
    }

    public long addSavedQuery(String query){
        final SaveSearchQueryTask task = new SaveSearchQueryTask();
        task.execute(query);
        try {
            return task.get();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Cannot insert saved query: " + query, e);
            return -1 ;
        } catch (ExecutionException e) {
            Log.e(LOG_TAG, "Cannot insert saved query: " + query, e);
            return -1;
        }
    }

    public void update() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final Intent intent = new Intent(SearchIndexablesContract.PROVIDER_INTERFACE);
                List<ResolveInfo> list =
                        mContext.getPackageManager().queryIntentContentProviders(intent, 0);

                final int size = list.size();
                for (int n = 0; n < size; n++) {
                    final ResolveInfo info = list.get(n);
                    if (!isWellKnownProvider(info)) {
                        continue;
                    }
                    final String authority = info.providerInfo.authority;
                    final String packageName = info.providerInfo.packageName;

                    addIndexablesFromRemoteProvider(packageName, authority);
                    addNonIndexablesKeysFromRemoteProvider(packageName, authority);
                }

                mDataToProcess.fullIndex = true;
                updateInternal();
            }
        });
    }

    private boolean addIndexablesFromRemoteProvider(String packageName, String authority) {
        try {
            final int baseRank = Ranking.getBaseRankForAuthority(authority);

            final Context context = mBaseAuthority.equals(authority) ?
                    mContext : mContext.createPackageContext(packageName, 0);

            final Uri uriForResources = buildUriForXmlResources(authority);
            addIndexablesForXmlResourceUri(context, packageName, uriForResources,
                    SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS, baseRank);

            final Uri uriForRawData = buildUriForRawData(authority);
            addIndexablesForRawDataUri(context, packageName, uriForRawData,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS, baseRank);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(LOG_TAG, "Could not create context for " + packageName + ": "
                    + Log.getStackTraceString(e));
            return false;
        }
    }

    private void addNonIndexablesKeysFromRemoteProvider(String packageName,
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

        List<String> result = new ArrayList<String>();
        try {
            final int count = cursor.getCount();
            if (count > 0) {
                while (cursor.moveToNext()) {
                    final String key = cursor.getString(COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE);
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

    public void addIndexableData(SearchIndexableResource[] array) {
        synchronized (mDataToProcess) {
            final int count = array.length;
            for (int n = 0; n < count; n++) {
                mDataToProcess.dataToUpdate.add(array[n]);
            }
        }
    }

    public void deleteIndexableData(SearchIndexableData data) {
        synchronized (mDataToProcess) {
            mDataToProcess.dataToDelete.add(data);
        }
    }

    public void addNonIndexableKeys(String authority, List<String> keys) {
        synchronized (mDataToProcess) {
            mDataToProcess.nonIndexableKeys.put(authority, keys);
        }
    }

    /**
     * Only allow a "well known" SearchIndexablesProvider. The provider should:
     *
     * - have read/write {@link android.Manifest.permission#READ_SEARCH_INDEXABLES}
     * - be from a privileged package
     */
    private boolean isWellKnownProvider(ResolveInfo info) {
        final String authority = info.providerInfo.authority;
        final String packageName = info.providerInfo.applicationInfo.packageName;

        if (TextUtils.isEmpty(authority) || TextUtils.isEmpty(packageName)) {
            return false;
        }

        final String readPermission = info.providerInfo.readPermission;
        final String writePermission = info.providerInfo.writePermission;

        if (TextUtils.isEmpty(readPermission) || TextUtils.isEmpty(writePermission)) {
            return false;
        }

        if (!android.Manifest.permission.READ_SEARCH_INDEXABLES.equals(readPermission) ||
            !android.Manifest.permission.READ_SEARCH_INDEXABLES.equals(writePermission)) {
            return false;
        }

        return isPrivilegedPackage(packageName);
    }

    private boolean isPrivilegedPackage(String packageName) {
        final PackageManager pm = mContext.getPackageManager();
        try {
            PackageInfo packInfo = pm.getPackageInfo(packageName, 0);
            return ((packInfo.applicationInfo.privateFlags
                & ApplicationInfo.PRIVATE_FLAG_PRIVILEGED) != 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void updateFromRemoteProvider(String packageName, String authority) {
        if (addIndexablesFromRemoteProvider(packageName, authority)) {
            updateInternal();
        }
    }

    /**
     * Update the Index for a specific class name resources
     *
     * @param className the class name (typically a fragment name).
     * @param rebuild true means that you want to delete the data from the Index first.
     * @param includeInSearchResults true means that you want the bit "enabled" set so that the
     *                               data will be seen included into the search results
     */
    public void updateFromClassNameResource(String className, final boolean rebuild,
            boolean includeInSearchResults) {
        if (className == null) {
            throw new IllegalArgumentException("class name cannot be null!");
        }
        final SearchIndexableResource res = SearchIndexableResources.getResourceByName(className);
        if (res == null ) {
            Log.e(LOG_TAG, "Cannot find SearchIndexableResources for class name: " + className);
            return;
        }
        res.context = mContext;
        res.enabled = includeInSearchResults;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (rebuild) {
                    deleteIndexableData(res);
                }
                addIndexableData(res);
                mDataToProcess.forceUpdate = true;
                updateInternal();
                res.enabled = false;
            }
        });
    }

    public void updateFromSearchIndexableData(SearchIndexableData data) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                addIndexableData(data);
                mDataToProcess.forceUpdate = true;
                updateInternal();
            }
        });
    }

    private SQLiteDatabase getReadableDatabase() {
        return IndexDatabaseHelper.getInstance(mContext).getReadableDatabase();
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

    private void updateInternal() {
        synchronized (mDataToProcess) {
            final UpdateIndexTask task = new UpdateIndexTask();
            UpdateData copy = mDataToProcess.copy();
            task.execute(copy);
            mDataToProcess.clear();
        }
    }

    private void addIndexablesForXmlResourceUri(Context packageContext, String packageName,
            Uri uri, String[] projection, int baseRank) {

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
                    final int providerRank = cursor.getInt(COLUMN_INDEX_XML_RES_RANK);
                    final int rank = (providerRank > 0) ? baseRank + providerRank : baseRank;

                    final int xmlResId = cursor.getInt(COLUMN_INDEX_XML_RES_RESID);

                    final String className = cursor.getString(COLUMN_INDEX_XML_RES_CLASS_NAME);
                    final int iconResId = cursor.getInt(COLUMN_INDEX_XML_RES_ICON_RESID);

                    final String action = cursor.getString(COLUMN_INDEX_XML_RES_INTENT_ACTION);
                    final String targetPackage = cursor.getString(
                            COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE);
                    final String targetClass = cursor.getString(
                            COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS);

                    SearchIndexableResource sir = new SearchIndexableResource(packageContext);
                    sir.rank = rank;
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
            Uri uri, String[] projection, int baseRank) {

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
                    final int rank = (providerRank > 0) ? baseRank + providerRank : baseRank;

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
                    data.rank = rank;
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

    private String buildSearchSQL(String query, String[] colums, boolean withOrderBy) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildSearchSQLForColumn(query, colums));
        if (withOrderBy) {
            sb.append(" ORDER BY ");
            sb.append(IndexColumns.DATA_RANK);
        }
        return sb.toString();
    }

    private String buildSearchSQLForColumn(String query, String[] columnNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        for (int n = 0; n < SELECT_COLUMNS.length; n++) {
            sb.append(SELECT_COLUMNS[n]);
            if (n < SELECT_COLUMNS.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(" FROM ");
        sb.append(Tables.TABLE_PREFS_INDEX);
        sb.append(" WHERE ");
        sb.append(buildSearchWhereStringForColumns(query, columnNames));

        return sb.toString();
    }

    private String buildSearchWhereStringForColumns(String query, String[] columnNames) {
        final StringBuilder sb = new StringBuilder(Tables.TABLE_PREFS_INDEX);
        sb.append(" MATCH ");
        DatabaseUtils.appendEscapedSQLString(sb,
                buildSearchMatchStringForColumns(query, columnNames));
        sb.append(" AND ");
        sb.append(IndexColumns.LOCALE);
        sb.append(" = ");
        DatabaseUtils.appendEscapedSQLString(sb, Locale.getDefault().toString());
        sb.append(" AND ");
        sb.append(IndexColumns.ENABLED);
        sb.append(" = 1");
        return sb.toString();
    }

    private String buildSearchMatchStringForColumns(String query, String[] columnNames) {
        final String value = query + "*";
        StringBuilder sb = new StringBuilder();
        final int count = columnNames.length;
        for (int n = 0; n < count; n++) {
            sb.append(columnNames[n]);
            sb.append(":");
            sb.append(value);
            if (n < count - 1) {
                sb.append(" OR ");
            }
        }
        return sb.toString();
    }

    private void indexOneSearchIndexableData(SQLiteDatabase database, String localeStr,
            SearchIndexableData data, Map<String, List<String>> nonIndexableKeys) {
        if (data instanceof SearchIndexableResource) {
            indexOneResource(database, localeStr, (SearchIndexableResource) data, nonIndexableKeys);
        } else if (data instanceof SearchIndexableRaw) {
            indexOneRaw(database, localeStr, (SearchIndexableRaw) data);
        }
    }

    private void indexOneRaw(SQLiteDatabase database, String localeStr,
                             SearchIndexableRaw raw) {
        // Should be the same locale as the one we are processing
        if (!raw.locale.toString().equalsIgnoreCase(localeStr)) {
            return;
        }

        updateOneRowWithFilteredData(database, localeStr,
                raw.title,
                raw.summaryOn,
                raw.summaryOff,
                raw.entries,
                raw.className,
                raw.screenTitle,
                raw.iconResId,
                raw.rank,
                raw.keywords,
                raw.intentAction,
                raw.intentTargetPackage,
                raw.intentTargetClass,
                raw.enabled,
                raw.key,
                raw.userId);
    }

    private static boolean isIndexableClass(final Class<?> clazz) {
        return (clazz != null) && Indexable.class.isAssignableFrom(clazz);
    }

    private static Class<?> getIndexableClass(String className) {
        final Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            Log.d(LOG_TAG, "Cannot find class: " + className);
            return null;
        }
        return isIndexableClass(clazz) ? clazz : null;
    }

    private void indexOneResource(SQLiteDatabase database, String localeStr,
            SearchIndexableResource sir, Map<String, List<String>> nonIndexableKeysFromResource) {

        if (sir == null) {
            Log.e(LOG_TAG, "Cannot index a null resource!");
            return;
        }

        final List<String> nonIndexableKeys = new ArrayList<String>();

        if (sir.xmlResId > SearchIndexableResources.NO_DATA_RES_ID) {
            List<String> resNonIndxableKeys = nonIndexableKeysFromResource.get(sir.packageName);
            if (resNonIndxableKeys != null && resNonIndxableKeys.size() > 0) {
                nonIndexableKeys.addAll(resNonIndxableKeys);
            }

            indexFromResource(sir.context, database, localeStr,
                    sir.xmlResId, sir.className, sir.iconResId, sir.rank,
                    sir.intentAction, sir.intentTargetPackage, sir.intentTargetClass,
                    nonIndexableKeys);
        } else {
            if (TextUtils.isEmpty(sir.className)) {
                Log.w(LOG_TAG, "Cannot index an empty Search Provider name!");
                return;
            }

            final Class<?> clazz = getIndexableClass(sir.className);
            if (clazz == null) {
                Log.d(LOG_TAG, "SearchIndexableResource '" + sir.className +
                        "' should implement the " + Indexable.class.getName() + " interface!");
                return;
            }

            // Will be non null only for a Local provider implementing a
            // SEARCH_INDEX_DATA_PROVIDER field
            final Indexable.SearchIndexProvider provider = getSearchIndexProvider(clazz);
            if (provider != null) {
                List<String> providerNonIndexableKeys = provider.getNonIndexableKeys(sir.context);
                if (providerNonIndexableKeys != null && providerNonIndexableKeys.size() > 0) {
                    nonIndexableKeys.addAll(providerNonIndexableKeys);
                }

                indexFromProvider(mContext, database, localeStr, provider, sir.className,
                        sir.iconResId, sir.rank, sir.enabled, nonIndexableKeys);
            }
        }
    }

    private Indexable.SearchIndexProvider getSearchIndexProvider(final Class<?> clazz) {
        try {
            final Field f = clazz.getField(FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER);
            return (Indexable.SearchIndexProvider) f.get(null);
        } catch (NoSuchFieldException e) {
            Log.d(LOG_TAG, "Cannot find field '" + FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'");
        } catch (SecurityException se) {
            Log.d(LOG_TAG,
                    "Security exception for field '" + FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'");
        } catch (IllegalAccessException e) {
            Log.d(LOG_TAG,
                    "Illegal access to field '" + FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'");
        } catch (IllegalArgumentException e) {
            Log.d(LOG_TAG,
                    "Illegal argument when accessing field '" +
                            FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'");
        }
        return null;
    }

    private void indexFromResource(Context context, SQLiteDatabase database, String localeStr,
           int xmlResId, String fragmentName, int iconResId, int rank,
           String intentAction, String intentTargetPackage, String intentTargetClass,
           List<String> nonIndexableKeys) {

        XmlResourceParser parser = null;
        try {
            parser = context.getResources().getXml(xmlResId);

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

            final String screenTitle = getDataTitle(context, attrs);

            String key = getDataKey(context, attrs);

            String title;
            String summary;
            String keywords;

            // Insert rows for the main PreferenceScreen node. Rewrite the data for removing
            // hyphens.
            if (!nonIndexableKeys.contains(key)) {
                title = getDataTitle(context, attrs);
                summary = getDataSummary(context, attrs);
                keywords = getDataKeywords(context, attrs);

                updateOneRowWithFilteredData(database, localeStr, title, summary, null, null,
                        fragmentName, screenTitle, iconResId, rank,
                        keywords, intentAction, intentTargetPackage, intentTargetClass, true,
                        key, -1 /* default user id */);
            }

            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                nodeName = parser.getName();

                key = getDataKey(context, attrs);
                if (nonIndexableKeys.contains(key)) {
                    continue;
                }

                title = getDataTitle(context, attrs);
                keywords = getDataKeywords(context, attrs);

                if (!nodeName.equals(NODE_NAME_CHECK_BOX_PREFERENCE)) {
                    summary = getDataSummary(context, attrs);

                    String entries = null;

                    if (nodeName.endsWith(NODE_NAME_LIST_PREFERENCE)) {
                        entries = getDataEntries(context, attrs);
                    }

                    // Insert rows for the child nodes of PreferenceScreen
                    updateOneRowWithFilteredData(database, localeStr, title, summary, null, entries,
                            fragmentName, screenTitle, iconResId, rank,
                            keywords, intentAction, intentTargetPackage, intentTargetClass,
                            true, key, -1 /* default user id */);
                } else {
                    String summaryOn = getDataSummaryOn(context, attrs);
                    String summaryOff = getDataSummaryOff(context, attrs);

                    if (TextUtils.isEmpty(summaryOn) && TextUtils.isEmpty(summaryOff)) {
                        summaryOn = getDataSummary(context, attrs);
                    }

                    updateOneRowWithFilteredData(database, localeStr, title, summaryOn, summaryOff,
                            null, fragmentName, screenTitle, iconResId, rank,
                            keywords, intentAction, intentTargetPackage, intentTargetClass,
                            true, key, -1 /* default user id */);
                }
            }

        } catch (XmlPullParserException e) {
            throw new RuntimeException("Error parsing PreferenceScreen", e);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing PreferenceScreen", e);
        } finally {
            if (parser != null) parser.close();
        }
    }

    private void indexFromProvider(Context context, SQLiteDatabase database, String localeStr,
            Indexable.SearchIndexProvider provider, String className, int iconResId, int rank,
            boolean enabled, List<String> nonIndexableKeys) {

        if (provider == null) {
            Log.w(LOG_TAG, "Cannot find provider: " + className);
            return;
        }

        final List<SearchIndexableRaw> rawList = provider.getRawDataToIndex(context, enabled);

        if (rawList != null) {
            final int rawSize = rawList.size();
            for (int i = 0; i < rawSize; i++) {
                SearchIndexableRaw raw = rawList.get(i);

                // Should be the same locale as the one we are processing
                if (!raw.locale.toString().equalsIgnoreCase(localeStr)) {
                    continue;
                }

                if (nonIndexableKeys.contains(raw.key)) {
                    continue;
                }

                updateOneRowWithFilteredData(database, localeStr,
                        raw.title,
                        raw.summaryOn,
                        raw.summaryOff,
                        raw.entries,
                        className,
                        raw.screenTitle,
                        iconResId,
                        rank,
                        raw.keywords,
                        raw.intentAction,
                        raw.intentTargetPackage,
                        raw.intentTargetClass,
                        raw.enabled,
                        raw.key,
                        raw.userId);
            }
        }

        final List<SearchIndexableResource> resList =
                provider.getXmlResourcesToIndex(context, enabled);
        if (resList != null) {
            final int resSize = resList.size();
            for (int i = 0; i < resSize; i++) {
                SearchIndexableResource item = resList.get(i);

                // Should be the same locale as the one we are processing
                if (!item.locale.toString().equalsIgnoreCase(localeStr)) {
                    continue;
                }

                final int itemIconResId = (item.iconResId == 0) ? iconResId : item.iconResId;
                final int itemRank = (item.rank == 0) ? rank : item.rank;
                String itemClassName = (TextUtils.isEmpty(item.className))
                        ? className : item.className;

                indexFromResource(context, database, localeStr,
                        item.xmlResId, itemClassName, itemIconResId, itemRank,
                        item.intentAction, item.intentTargetPackage,
                        item.intentTargetClass, nonIndexableKeys);
            }
        }
    }

    private void updateOneRowWithFilteredData(SQLiteDatabase database, String locale,
            String title, String summaryOn, String summaryOff, String entries,
            String className,
            String screenTitle, int iconResId, int rank, String keywords,
            String intentAction, String intentTargetPackage, String intentTargetClass,
            boolean enabled, String key, int userId) {

        final String updatedTitle = normalizeHyphen(title);
        final String updatedSummaryOn = normalizeHyphen(summaryOn);
        final String updatedSummaryOff = normalizeHyphen(summaryOff);

        final String normalizedTitle = normalizeString(updatedTitle);
        final String normalizedSummaryOn = normalizeString(updatedSummaryOn);
        final String normalizedSummaryOff = normalizeString(updatedSummaryOff);

        final String spaceDelimitedKeywords = normalizeKeywords(keywords);

        updateOneRow(database, locale,
                updatedTitle, normalizedTitle, updatedSummaryOn, normalizedSummaryOn,
                updatedSummaryOff, normalizedSummaryOff, entries, className, screenTitle, iconResId,
                rank, spaceDelimitedKeywords, intentAction, intentTargetPackage, intentTargetClass,
                enabled, key, userId);
    }

    private static String normalizeHyphen(String input) {
        return (input != null) ? input.replaceAll(NON_BREAKING_HYPHEN, HYPHEN) : EMPTY;
    }

    private static String normalizeString(String input) {
        final String nohyphen = (input != null) ? input.replaceAll(HYPHEN, EMPTY) : EMPTY;
        final String normalized = Normalizer.normalize(nohyphen, Normalizer.Form.NFD);

        return REMOVE_DIACRITICALS_PATTERN.matcher(normalized).replaceAll("").toLowerCase();
    }

    private static String normalizeKeywords(String input) {
        return (input != null) ? input.replaceAll(LIST_DELIMITERS, SPACE) : EMPTY;
    }

    private void updateOneRow(SQLiteDatabase database, String locale, String updatedTitle,
            String normalizedTitle, String updatedSummaryOn, String normalizedSummaryOn,
            String updatedSummaryOff, String normalizedSummaryOff, String entries, String className,
            String screenTitle, int iconResId, int rank, String spaceDelimitedKeywords,
            String intentAction, String intentTargetPackage, String intentTargetClass,
            boolean enabled, String key, int userId) {

        if (TextUtils.isEmpty(updatedTitle)) {
            return;
        }

        // The DocID should contains more than the title string itself (you may have two settings
        // with the same title). So we need to use a combination of the title and the screenTitle.
        StringBuilder sb = new StringBuilder(updatedTitle);
        sb.append(screenTitle);
        int docId = sb.toString().hashCode();

        ContentValues values = new ContentValues();
        values.put(IndexColumns.DOCID, docId);
        values.put(IndexColumns.LOCALE, locale);
        values.put(IndexColumns.DATA_RANK, rank);
        values.put(IndexColumns.DATA_TITLE, updatedTitle);
        values.put(IndexColumns.DATA_TITLE_NORMALIZED, normalizedTitle);
        values.put(IndexColumns.DATA_SUMMARY_ON, updatedSummaryOn);
        values.put(IndexColumns.DATA_SUMMARY_ON_NORMALIZED, normalizedSummaryOn);
        values.put(IndexColumns.DATA_SUMMARY_OFF, updatedSummaryOff);
        values.put(IndexColumns.DATA_SUMMARY_OFF_NORMALIZED, normalizedSummaryOff);
        values.put(IndexColumns.DATA_ENTRIES, entries);
        values.put(IndexColumns.DATA_KEYWORDS, spaceDelimitedKeywords);
        values.put(IndexColumns.CLASS_NAME, className);
        values.put(IndexColumns.SCREEN_TITLE, screenTitle);
        values.put(IndexColumns.INTENT_ACTION, intentAction);
        values.put(IndexColumns.INTENT_TARGET_PACKAGE, intentTargetPackage);
        values.put(IndexColumns.INTENT_TARGET_CLASS, intentTargetClass);
        values.put(IndexColumns.ICON, iconResId);
        values.put(IndexColumns.ENABLED, enabled);
        values.put(IndexColumns.DATA_KEY_REF, key);
        values.put(IndexColumns.USER_ID, userId);

        database.replaceOrThrow(Tables.TABLE_PREFS_INDEX, null, values);
    }

    private String getDataKey(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.internal.R.styleable.Preference,
                com.android.internal.R.styleable.Preference_key);
    }

    private String getDataTitle(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.internal.R.styleable.Preference,
                com.android.internal.R.styleable.Preference_title);
    }

    private String getDataSummary(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.internal.R.styleable.Preference,
                com.android.internal.R.styleable.Preference_summary);
    }

    private String getDataSummaryOn(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.internal.R.styleable.CheckBoxPreference,
                com.android.internal.R.styleable.CheckBoxPreference_summaryOn);
    }

    private String getDataSummaryOff(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.internal.R.styleable.CheckBoxPreference,
                com.android.internal.R.styleable.CheckBoxPreference_summaryOff);
    }

    private String getDataEntries(Context context, AttributeSet attrs) {
        return getDataEntries(context, attrs,
                com.android.internal.R.styleable.ListPreference,
                com.android.internal.R.styleable.ListPreference_entries);
    }

    private String getDataKeywords(Context context, AttributeSet attrs) {
        return getData(context, attrs, R.styleable.Preference, R.styleable.Preference_keywords);
    }

    private String getData(Context context, AttributeSet set, int[] attrs, int resId) {
        final TypedArray sa = context.obtainStyledAttributes(set, attrs);
        final TypedValue tv = sa.peekValue(resId);

        CharSequence data = null;
        if (tv != null && tv.type == TypedValue.TYPE_STRING) {
            if (tv.resourceId != 0) {
                data = context.getText(tv.resourceId);
            } else {
                data = tv.string;
            }
        }
        return (data != null) ? data.toString() : null;
    }

    private String getDataEntries(Context context, AttributeSet set, int[] attrs, int resId) {
        final TypedArray sa = context.obtainStyledAttributes(set, attrs);
        final TypedValue tv = sa.peekValue(resId);

        String[] data = null;
        if (tv != null && tv.type == TypedValue.TYPE_REFERENCE) {
            if (tv.resourceId != 0) {
                data = context.getResources().getStringArray(tv.resourceId);
            }
        }
        final int count = (data == null ) ? 0 : data.length;
        if (count == 0) {
            return null;
        }
        final StringBuilder result = new StringBuilder();
        for (int n = 0; n < count; n++) {
            result.append(data[n]);
            result.append(ENTRIES_SEPARATOR);
        }
        return result.toString();
    }

    /**
     * A private class for updating the Index database
     */
    private class UpdateIndexTask extends AsyncTask<UpdateData, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mIsAvailable.set(false);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mIsAvailable.set(true);
        }

        @Override
        protected Void doInBackground(UpdateData... params) {
            try {
                final List<SearchIndexableData> dataToUpdate = params[0].dataToUpdate;
                final List<SearchIndexableData> dataToDelete = params[0].dataToDelete;
                final Map<String, List<String>> nonIndexableKeys = params[0].nonIndexableKeys;

                final boolean forceUpdate = params[0].forceUpdate;
                final boolean fullIndex = params[0].fullIndex;

                final SQLiteDatabase database = getWritableDatabase();
                if (database == null) {
                    Log.e(LOG_TAG, "Cannot update Index as I cannot get a writable database");
                    return null;
                }
                final String localeStr = Locale.getDefault().toString();

                try {
                    database.beginTransaction();
                    if (dataToDelete.size() > 0) {
                        processDataToDelete(database, localeStr, dataToDelete);
                    }
                    if (dataToUpdate.size() > 0) {
                        processDataToUpdate(database, localeStr, dataToUpdate, nonIndexableKeys,
                                forceUpdate);
                    }
                    database.setTransactionSuccessful();
                } finally {
                    database.endTransaction();
                }
                if (fullIndex) {
                    IndexDatabaseHelper.setLocaleIndexed(mContext, localeStr);
                }
            } catch (SQLiteFullException e) {
                Log.e(LOG_TAG, "Unable to index search, out of space", e);
            }

            return null;
        }

        private boolean processDataToUpdate(SQLiteDatabase database, String localeStr,
                List<SearchIndexableData> dataToUpdate, Map<String, List<String>> nonIndexableKeys,
                boolean forceUpdate) {

            if (!forceUpdate && IndexDatabaseHelper.isLocaleAlreadyIndexed(mContext, localeStr)) {
                Log.d(LOG_TAG, "Locale '" + localeStr + "' is already indexed");
                return true;
            }

            boolean result = false;
            final long current = System.currentTimeMillis();

            final int count = dataToUpdate.size();
            for (int n = 0; n < count; n++) {
                final SearchIndexableData data = dataToUpdate.get(n);
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
            return result;
        }

        private boolean processDataToDelete(SQLiteDatabase database, String localeStr,
                List<SearchIndexableData> dataToDelete) {

            boolean result = false;
            final long current = System.currentTimeMillis();

            final int count = dataToDelete.size();
            for (int n = 0; n < count; n++) {
                final SearchIndexableData data = dataToDelete.get(n);
                if (data == null) {
                    continue;
                }
                if (!TextUtils.isEmpty(data.className)) {
                    delete(database, IndexColumns.CLASS_NAME, data.className);
                } else  {
                    if (data instanceof SearchIndexableRaw) {
                        final SearchIndexableRaw raw = (SearchIndexableRaw) data;
                        if (!TextUtils.isEmpty(raw.title)) {
                            delete(database, IndexColumns.DATA_TITLE, raw.title);
                        }
                    }
                }
            }

            final long now = System.currentTimeMillis();
            Log.d(LOG_TAG, "Deleting data for locale '" + localeStr + "' took " +
                    (now - current) + " millis");
            return result;
        }

        private int delete(SQLiteDatabase database, String columName, String value) {
            final String whereClause = columName + "=?";
            final String[] whereArgs = new String[] { value };

            return database.delete(Tables.TABLE_PREFS_INDEX, whereClause, whereArgs);
        }
    }

    /**
     * A basic AsyncTask for saving a Search query into the database
     */
    private class SaveSearchQueryTask extends AsyncTask<String, Void, Long> {

        @Override
        protected Long doInBackground(String... params) {
            final long now = new Date().getTime();

            final ContentValues values = new ContentValues();
            values.put(IndexDatabaseHelper.SavedQueriesColums.QUERY, params[0]);
            values.put(IndexDatabaseHelper.SavedQueriesColums.TIME_STAMP, now);

            final SQLiteDatabase database = getWritableDatabase();
            if (database == null) {
                Log.e(LOG_TAG, "Cannot save Search queries as I cannot get a writable database");
                return -1L;
            }

            long lastInsertedRowId = -1L;
            try {
                // First, delete all saved queries that are the same
                database.delete(Tables.TABLE_SAVED_QUERIES,
                        IndexDatabaseHelper.SavedQueriesColums.QUERY + " = ?",
                        new String[] { params[0] });

                // Second, insert the saved query
                lastInsertedRowId =
                        database.insertOrThrow(Tables.TABLE_SAVED_QUERIES, null, values);

                // Last, remove "old" saved queries
                final long delta = lastInsertedRowId - MAX_SAVED_SEARCH_QUERY;
                if (delta > 0) {
                    int count = database.delete(Tables.TABLE_SAVED_QUERIES, "rowId <= ?",
                            new String[] { Long.toString(delta) });
                    Log.d(LOG_TAG, "Deleted '" + count + "' saved Search query(ies)");
                }
            } catch (Exception e) {
                Log.d(LOG_TAG, "Cannot update saved Search queries", e);
            }

            return lastInsertedRowId;
        }
    }
}
