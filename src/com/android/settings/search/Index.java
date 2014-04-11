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
import android.database.sqlite.SQLiteDatabase;
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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_RANK;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_TITLE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SUMMARY_ON;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SUMMARY_OFF;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ENTRIES;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_KEYWORDS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SCREEN_TITLE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_CLASS_NAME;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_TARGET_PACKAGE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_TARGET_CLASS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_KEY;

import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RANK;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_CLASS_NAME;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS;

import static com.android.settings.search.IndexDatabaseHelper.Tables;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns;

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

    private static final String[] MATCH_COLUMNS = {
            IndexColumns.DATA_TITLE,
            IndexColumns.DATA_TITLE_NORMALIZED,
            IndexColumns.DATA_SUMMARY_ON,
            IndexColumns.DATA_SUMMARY_ON_NORMALIZED,
            IndexColumns.DATA_SUMMARY_OFF,
            IndexColumns.DATA_SUMMARY_OFF_NORMALIZED,
            IndexColumns.DATA_ENTRIES,
            IndexColumns.DATA_KEYWORDS
    };

    private static final String EMPTY = "";
    private static final String NON_BREAKING_HYPHEN = "\u2011";
    private static final String HYPHEN = "-";

    private static final String FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER =
            "SEARCH_INDEX_DATA_PROVIDER";

    private static final String NODE_NAME_PREFERENCE_SCREEN = "PreferenceScreen";
    private static final String NODE_NAME_CHECK_BOX_PREFERENCE = "CheckBoxPreference";
    private static final String NODE_NAME_LIST_PREFERENCE = "ListPreference";

    private static final List<String> EMPTY_LIST = Collections.<String>emptyList();

    private static Index sInstance;
    private final AtomicBoolean mIsAvailable = new AtomicBoolean(false);
    private final UpdateData mDataToProcess = new UpdateData();
    private Context mContext;

    /**
     * A private class to describe the update data for the Index database
     */
    private class UpdateData {
        public List<SearchIndexableData> dataToUpdate;
        public List<String> dataToDelete;
        public boolean forceUpdate = false;

        public UpdateData() {
            dataToUpdate = new ArrayList<SearchIndexableData>();
            dataToDelete = new ArrayList<String>();
        }

        public void clear() {
            dataToUpdate.clear();
            dataToDelete.clear();
            forceUpdate = false;
        }
    }

    /**
     * A basic singleton
     */
    public static Index getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Index(context);
        } else {
            sInstance.setContext(context);
        }
        return sInstance;
    }

    public Index(Context context) {
        mContext = context;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public boolean isAvailable() {
        return mIsAvailable.get();
    }

    public Cursor search(String query) {
        final String sql = buildSQL(query);
        Log.d(LOG_TAG, "Query: " + sql);
        return getReadableDatabase().rawQuery(sql, null);
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

    public void deleteIndexableData(String[] array) {
        synchronized (mDataToProcess) {
            final int count = array.length;
            for (int n = 0; n < count; n++) {
                mDataToProcess.dataToDelete.add(array[n]);
            }
        }
    }

    public boolean update() {
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
        }

        return updateInternal();
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
            return ((packInfo.applicationInfo.flags & ApplicationInfo.FLAG_PRIVILEGED) != 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public boolean updateFromRemoteProvider(String packageName, String authority) {
        if (!addIndexablesFromRemoteProvider(packageName, authority)) {
            return false;
        }
        return updateInternal();
    }

    public boolean updateFromClassNameResource(String className, boolean includeInSearchResults) {
        if (className == null) {
            throw new IllegalArgumentException("class name cannot be null!");
        }
        final SearchIndexableResource res = SearchIndexableResources.getResourceByName(className);
        if (res == null ) {
            Log.e(LOG_TAG, "Cannot find SearchIndexableResources for class name: " + className);
            return false;
        }
        res.enabled = includeInSearchResults;
        addIndexableData(res);
        mDataToProcess.forceUpdate = true;
        boolean result = updateInternal();
        res.enabled = false;
        return result;
    }

    public boolean updateFromSearchIndexableData(SearchIndexableData data) {
        addIndexableData(data);
        mDataToProcess.forceUpdate = true;
        return updateInternal();
    }

    private SQLiteDatabase getReadableDatabase() {
        return IndexDatabaseHelper.getInstance(mContext).getReadableDatabase();
    }

    private SQLiteDatabase getWritableDatabase() {
        return IndexDatabaseHelper.getInstance(mContext).getWritableDatabase();
    }

    private boolean addIndexablesFromRemoteProvider(String packageName, String authority) {
        final Context packageContext;
        try {
            packageContext = mContext.createPackageContext(packageName, 0);

            final Uri uriForResources = buildUriForXmlResources(authority);
            addIndexablesForXmlResourceUri(packageContext, packageName, uriForResources,
                    SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS);

            final Uri uriForRawData = buildUriForRawData(authority);
            addIndexablesForRawDataUri(packageContext, packageName, uriForRawData,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(LOG_TAG, "Could not create context for " + packageName + ": "
                    + Log.getStackTraceString(e));
            return false;
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

    private boolean updateInternal() {
        synchronized (mDataToProcess) {
            final UpdateIndexTask task = new UpdateIndexTask();
            task.execute(mDataToProcess);
            try {
                final boolean result = task.get();
                mDataToProcess.clear();
                return result;
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Cannot update index: " + e.getMessage());
                return false;
            } catch (ExecutionException e) {
                Log.e(LOG_TAG, "Cannot update index: " + e.getMessage());
                return false;
            }
        }
    }

    private void addIndexablesForXmlResourceUri(Context packageContext, String packageName,
            Uri uri, String[] projection) {

        final ContentResolver resolver = packageContext.getContentResolver();
        final Cursor cursor = resolver.query(uri, projection,
                null, null, null);

        if (cursor == null) {
            Log.w(LOG_TAG, "Cannot add index data for Uri: " + uri.toString());
            return;
        }

        try {
            final int count = cursor.getCount();
            if (count > 0) {
                while (cursor.moveToNext()) {
                    final int rank = cursor.getInt(COLUMN_INDEX_XML_RES_RANK);
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
            Uri uri, String[] projection) {

        final ContentResolver resolver = packageContext.getContentResolver();
        final Cursor cursor = resolver.query(uri, projection,
                null, null, null);

        if (cursor == null) {
            Log.w(LOG_TAG, "Cannot add index data for Uri: " + uri.toString());
            return;
        }

        try {
            final int count = cursor.getCount();
            if (count > 0) {
                while (cursor.moveToNext()) {
                    final int rank = cursor.getInt(COLUMN_INDEX_RAW_RANK);
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

                    addIndexableData(data);
                }
            }
        } finally {
            cursor.close();
        }
    }

    private String buildSQL(String query) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildSQLForColumn(query, MATCH_COLUMNS));
        sb.append(" ORDER BY ");
        sb.append(IndexColumns.DATA_RANK);
        return sb.toString();
    }

    private String buildSQLForColumn(String query, String[] columnNames) {
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
        sb.append(buildWhereStringForColumns(query, columnNames));

        return sb.toString();
    }

    private String buildWhereStringForColumns(String query, String[] columnNames) {
        final StringBuilder sb = new StringBuilder(Tables.TABLE_PREFS_INDEX);
        sb.append(" MATCH ");
        DatabaseUtils.appendEscapedSQLString(sb, buildMatchStringForColumns(query, columnNames));
        sb.append(" AND ");
        sb.append(IndexColumns.LOCALE);
        sb.append(" = ");
        DatabaseUtils.appendEscapedSQLString(sb, Locale.getDefault().toString());
        sb.append(" AND ");
        sb.append(IndexColumns.ENABLED);
        sb.append(" = 1");
        return sb.toString();
    }

    private String buildMatchStringForColumns(String query, String[] columnNames) {
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
                                             SearchIndexableData data) {
        if (data instanceof SearchIndexableResource) {
            indexOneResource(database, localeStr, (SearchIndexableResource) data);
        } else if (data instanceof SearchIndexableRaw) {
            indexOneRaw(database, localeStr, (SearchIndexableRaw) data);
        }
    }

    private void indexOneResource(SQLiteDatabase database, String localeStr,
                                  SearchIndexableResource sir) {

        if (sir == null) {
            Log.e(LOG_TAG, "Cannot index a null resource!");
            return;
        }

        // Will be non null only for a Local provider
        final Indexable.SearchIndexProvider provider =
                TextUtils.isEmpty(sir.className) ? null : getSearchIndexProvider(sir.className);

        if (sir.xmlResId > SearchIndexableResources.NO_DATA_RES_ID) {
            List<String> doNotIndexKeys = EMPTY_LIST;
            if (provider != null) {
                doNotIndexKeys = provider.getNonIndexableKeys(sir.context);
            }
            indexFromResource(sir.context, database, localeStr,
                    sir.xmlResId, sir.className, sir.iconResId, sir.rank,
                    sir.intentAction, sir.intentTargetPackage, sir.intentTargetClass,
                    doNotIndexKeys);
        } else if (!TextUtils.isEmpty(sir.className)) {
            indexFromLocalProvider(mContext, database, localeStr, provider, sir.className,
                    sir.iconResId, sir.rank, sir.enabled);
        }
    }

    private void indexFromResource(Context context, SQLiteDatabase database, String localeStr,
           int xmlResId, String fragmentName, int iconResId, int rank,
           String intentAction, String intentTargetPackage, String intentTargetClass,
           List<String> doNotIndexKeys) {

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
            if (!doNotIndexKeys.contains(key)) {
                title = getDataTitle(context, attrs);
                summary = getDataSummary(context, attrs);
                keywords = getDataKeywords(context, attrs);

                updateOneRowWithFilteredData(database, localeStr, title, summary, null, null,
                        fragmentName, screenTitle, iconResId, rank,
                        keywords, intentAction, intentTargetPackage, intentTargetClass, true, key);
            }

            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                nodeName = parser.getName();

                key = getDataKey(context, attrs);
                if (doNotIndexKeys.contains(key)) {
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
                            true, key);
                } else {
                    final String summaryOn = getDataSummaryOn(context, attrs);
                    final String summaryOff = getDataSummaryOff(context, attrs);

                    updateOneRowWithFilteredData(database, localeStr, title, summaryOn, summaryOff,
                            null, fragmentName, screenTitle, iconResId, rank,
                            keywords, intentAction, intentTargetPackage, intentTargetClass,
                            true, key);
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
                raw.key);
    }

    private Indexable.SearchIndexProvider getSearchIndexProvider(String className) {
        try {
            final Class<?> clazz = Class.forName(className);
            if (Indexable.class.isAssignableFrom(clazz)) {
                final Field f = clazz.getField(FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER);
                return (Indexable.SearchIndexProvider) f.get(null);
            }
        } catch (ClassNotFoundException e) {
            Log.e(LOG_TAG, "Cannot find class: " + className, e);
        } catch (NoSuchFieldException e) {
            Log.e(LOG_TAG, "Cannot find field '" + FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'", e);
        } catch (IllegalAccessException e) {
            Log.e(LOG_TAG,
                    "Illegal access to field '" + FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER + "'", e);
        }
        return null;
    }

    private void indexFromLocalProvider(Context context, SQLiteDatabase database, String localeStr,
                Indexable.SearchIndexProvider provider, String className, int iconResId, int rank,
                boolean enabled) {

        if (provider == null) {
            Log.w(LOG_TAG, "Cannot find provider: " + className);
            return;
        }

        final List<String> doNotIndexKeys = provider.getNonIndexableKeys(context);
        final List<SearchIndexableRaw> rawList = provider.getRawDataToIndex(context, enabled);

        if (rawList != null) {
            final int rawSize = rawList.size();
            for (int i = 0; i < rawSize; i++) {
                SearchIndexableRaw raw = rawList.get(i);

                // Should be the same locale as the one we are processing
                if (!raw.locale.toString().equalsIgnoreCase(localeStr)) {
                    continue;
                }

                if (doNotIndexKeys.contains(raw.key)) {
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
                        raw.key);
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

                indexFromResource(context, database, localeStr,
                        item.xmlResId, item.className, item.iconResId, item.rank,
                        item.intentAction, item.intentTargetPackage,
                        item.intentTargetClass, doNotIndexKeys);
            }
        }
    }

    private void updateOneRowWithFilteredData(SQLiteDatabase database, String locale,
            String title, String summaryOn, String summaryOff, String entries,
            String className,
            String screenTitle, int iconResId, int rank, String keywords,
            String intentAction, String intentTargetPackage, String intentTargetClass,
            boolean enabled, String key) {

        String updatedTitle;
        if (title != null) {
            updatedTitle = title.replaceAll(NON_BREAKING_HYPHEN, HYPHEN);
        }
        else {
            updatedTitle = EMPTY;
        }

        String updatedSummaryOn;
        if (summaryOn != null) {
            updatedSummaryOn = summaryOn.replaceAll(NON_BREAKING_HYPHEN, HYPHEN);
        } else {
            updatedSummaryOn = EMPTY;
        }

        String updatedSummaryOff;
        if (summaryOff != null) {
            updatedSummaryOff = summaryOff.replaceAll(NON_BREAKING_HYPHEN, HYPHEN);
        } else {
            updatedSummaryOff = EMPTY;
        }

        String normalizedTitle = updatedTitle.replaceAll(HYPHEN, EMPTY);
        String normalizedSummaryOn = updatedSummaryOn.replaceAll(HYPHEN, EMPTY);
        String normalizedSummaryOff = updatedSummaryOff.replaceAll(HYPHEN, EMPTY);

        updateOneRow(database, locale,
                updatedTitle, normalizedTitle, updatedSummaryOn, normalizedSummaryOn,
                updatedSummaryOff, normalizedSummaryOff, entries,
                className, screenTitle, iconResId,
                rank, keywords, intentAction, intentTargetPackage, intentTargetClass, enabled, key);
    }

    private void updateOneRow(SQLiteDatabase database, String locale,
            String updatedTitle, String normalizedTitle,
            String updatedSummaryOn, String normalizedSummaryOn,
            String updatedSummaryOff, String normalizedSummaryOff, String entries,
            String className, String screenTitle, int iconResId, int rank, String keywords,
            String intentAction, String intentTargetPackage, String intentTargetClass,
            boolean enabled, String key) {

        if (TextUtils.isEmpty(updatedTitle)) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(IndexColumns.DOCID, updatedTitle.hashCode());
        values.put(IndexColumns.LOCALE, locale);
        values.put(IndexColumns.DATA_RANK, rank);
        values.put(IndexColumns.DATA_TITLE, updatedTitle);
        values.put(IndexColumns.DATA_TITLE_NORMALIZED, normalizedTitle);
        values.put(IndexColumns.DATA_SUMMARY_ON, updatedSummaryOn);
        values.put(IndexColumns.DATA_SUMMARY_ON_NORMALIZED, normalizedSummaryOn);
        values.put(IndexColumns.DATA_SUMMARY_OFF, updatedSummaryOff);
        values.put(IndexColumns.DATA_SUMMARY_OFF_NORMALIZED, normalizedSummaryOff);
        values.put(IndexColumns.DATA_ENTRIES, entries);
        values.put(IndexColumns.DATA_KEYWORDS, keywords);
        values.put(IndexColumns.CLASS_NAME, className);
        values.put(IndexColumns.SCREEN_TITLE, screenTitle);
        values.put(IndexColumns.INTENT_ACTION, intentAction);
        values.put(IndexColumns.INTENT_TARGET_PACKAGE, intentTargetPackage);
        values.put(IndexColumns.INTENT_TARGET_CLASS, intentTargetClass);
        values.put(IndexColumns.ICON, iconResId);
        values.put(IndexColumns.ENABLED, enabled);
        values.put(IndexColumns.DATA_KEY_REF, key);

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
            result.append(" ");
        }
        return result.toString();
    }

    private int getResId(Context context, AttributeSet set, int[] attrs, int resId) {
        final TypedArray sa = context.obtainStyledAttributes(set, attrs);
        final TypedValue tv = sa.peekValue(resId);

        if (tv != null && tv.type == TypedValue.TYPE_STRING) {
            return tv.resourceId;
        } else {
            return 0;
        }
   }

    /**
     * A private class for updating the Index database
     */
    private class UpdateIndexTask extends AsyncTask<UpdateData, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mIsAvailable.set(false);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            mIsAvailable.set(true);
        }

        @Override
        protected Boolean doInBackground(UpdateData... params) {
            boolean result = false;

            final List<SearchIndexableData> dataToUpdate = params[0].dataToUpdate;
            final List<String> dataToDelete = params[0].dataToDelete;
            final boolean forceUpdate = params[0].forceUpdate;
            final SQLiteDatabase database = getWritableDatabase();
            final String localeStr = Locale.getDefault().toString();

            try {
                database.beginTransaction();
                if (dataToUpdate.size() > 0) {
                    processDataToUpdate(database, localeStr, dataToUpdate, forceUpdate);
                }
                if (dataToDelete.size() > 0) {
                    processDataToDelete(database, localeStr, dataToDelete);
                }
                database.setTransactionSuccessful();
                result = true;
            } finally {
                database.endTransaction();
            }
            return result;
        }

        private boolean processDataToUpdate(SQLiteDatabase database, String localeStr,
                List<SearchIndexableData> dataToUpdate, boolean forceUpdate) {

            if (!forceUpdate && isLocaleAlreadyIndexed(database, localeStr)) {
                Log.d(LOG_TAG, "Locale '" + localeStr + "' is already indexed");
                return true;
            }

            boolean result = false;
            final long current = System.currentTimeMillis();

            final int count = dataToUpdate.size();
            for (int n = 0; n < count; n++) {
                final SearchIndexableData data = dataToUpdate.get(n);
                indexOneSearchIndexableData(database, localeStr, data);
            }

            final long now = System.currentTimeMillis();
            Log.d(LOG_TAG, "Indexing locale '" + localeStr + "' took " +
                    (now - current) + " millis");
            return result;
        }

        private boolean processDataToDelete(SQLiteDatabase database, String localeStr,
                                            List<String> dataToDelete) {

            boolean result = false;
            final long current = System.currentTimeMillis();

            final int count = dataToDelete.size();
            for (int n = 0; n < count; n++) {
                final String data = dataToDelete.get(n);
                delete(database, data);
            }

            final long now = System.currentTimeMillis();
            Log.d(LOG_TAG, "Deleting data for locale '" + localeStr + "' took " +
                    (now - current) + " millis");
            return result;
        }

        private int delete(SQLiteDatabase database, String title) {
            final String whereClause = IndexColumns.DATA_TITLE + "=?";
            final String[] whereArgs = new String[] { title };

            return database.delete(Tables.TABLE_PREFS_INDEX, whereClause, whereArgs);
        }

        private boolean isLocaleAlreadyIndexed(SQLiteDatabase database, String locale) {
            Cursor cursor = null;
            boolean result = false;
            final StringBuilder sb = new StringBuilder(IndexColumns.LOCALE);
            sb.append(" = ");
            DatabaseUtils.appendEscapedSQLString(sb, locale);
            try {
                // We care only for 1 row
                cursor = database.query(Tables.TABLE_PREFS_INDEX, null,
                        sb.toString(), null, null, null, null, "1");
                final int count = cursor.getCount();
                result = (count >= 1);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return result;
        }
    }
}
