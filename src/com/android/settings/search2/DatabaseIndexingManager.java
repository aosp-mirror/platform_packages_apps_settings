/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.search2;

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
import android.database.sqlite.SQLiteFullException;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.SearchIndexableData;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesContract;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import com.android.settings.search.IndexDatabaseHelper;
import com.android.settings.search.Indexable;
import com.android.settings.search.Ranking;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.search.SearchIndexableResources;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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

/**
 * Consumes the SearchIndexableProvider content providers.
 * Updates the Resource, Raw Data and non-indexable data for Search.
 */
public class DatabaseIndexingManager {
    private static final String LOG_TAG = "DatabaseIndexingManager";

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

    // If you change the order of columns here, you SHOULD change the COLUMN_INDEX_XXX values
    private static final String[] SELECT_COLUMNS = new String[] {
            IndexDatabaseHelper.IndexColumns.DATA_RANK,               // 0
            IndexDatabaseHelper.IndexColumns.DATA_TITLE,              // 1
            IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON,         // 2
            IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF,        // 3
            IndexDatabaseHelper.IndexColumns.DATA_ENTRIES,            // 4
            IndexDatabaseHelper.IndexColumns.DATA_KEYWORDS,           // 5
            IndexDatabaseHelper.IndexColumns.CLASS_NAME,              // 6
            IndexDatabaseHelper.IndexColumns.SCREEN_TITLE,            // 7
            IndexDatabaseHelper.IndexColumns.ICON,                    // 8
            IndexDatabaseHelper.IndexColumns.INTENT_ACTION,           // 9
            IndexDatabaseHelper.IndexColumns.INTENT_TARGET_PACKAGE,   // 10
            IndexDatabaseHelper.IndexColumns.INTENT_TARGET_CLASS,     // 11
            IndexDatabaseHelper.IndexColumns.ENABLED,                 // 12
            IndexDatabaseHelper.IndexColumns.DATA_KEY_REF             // 13
    };

    private static final String[] MATCH_COLUMNS_PRIMARY = {
            IndexDatabaseHelper.IndexColumns.DATA_TITLE,
            IndexDatabaseHelper.IndexColumns.DATA_TITLE_NORMALIZED,
            IndexDatabaseHelper.IndexColumns.DATA_KEYWORDS
    };

    private static final String[] MATCH_COLUMNS_SECONDARY = {
            IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON,
            IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON_NORMALIZED,
            IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF,
            IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF_NORMALIZED,
            IndexDatabaseHelper.IndexColumns.DATA_ENTRIES
    };


    private static final String NODE_NAME_PREFERENCE_SCREEN = "PreferenceScreen";
    private static final String NODE_NAME_CHECK_BOX_PREFERENCE = "CheckBoxPreference";
    private static final String NODE_NAME_LIST_PREFERENCE = "ListPreference";

    private static final List<String> EMPTY_LIST = Collections.<String>emptyList();

    private final String mBaseAuthority;

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

        public UpdateData(DatabaseIndexingManager.UpdateData other) {
            dataToUpdate = new ArrayList<SearchIndexableData>(other.dataToUpdate);
            dataToDelete = new ArrayList<SearchIndexableData>(other.dataToDelete);
            nonIndexableKeys = new HashMap<String, List<String>>(other.nonIndexableKeys);
            forceUpdate = other.forceUpdate;
            fullIndex = other.fullIndex;
        }

        public DatabaseIndexingManager.UpdateData copy() {
            return new DatabaseIndexingManager.UpdateData(this);
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
    private final DatabaseIndexingManager.UpdateData mDataToProcess =
            new DatabaseIndexingManager.UpdateData();
    private Context mContext;

    public DatabaseIndexingManager(Context context, String baseAuthority) {
        mContext = context;
        mBaseAuthority = baseAuthority;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public boolean isAvailable() {
        return mIsAvailable.get();
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
                    if (!DatabaseIndexingUtils.isWellKnownProvider(info, mContext)) {
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

    public void updateFromSearchIndexableData(final SearchIndexableData data) {
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
            final DatabaseIndexingManager.UpdateIndexTask task =
                    new DatabaseIndexingManager.UpdateIndexTask();
            DatabaseIndexingManager.UpdateData copy = mDataToProcess.copy();
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

                indexFromProvider(mContext, database, localeStr, provider, sir.className,
                        sir.iconResId, sir.rank, sir.enabled, nonIndexableKeys);
            }
        }
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

            final String screenTitle = XMLParserUtil.getDataTitle(context, attrs);

            String key = XMLParserUtil.getDataKey(context, attrs);

            String title;
            String summary;
            String keywords;

            // Insert rows for the main PreferenceScreen node. Rewrite the data for removing
            // hyphens.
            if (!nonIndexableKeys.contains(key)) {
                title = XMLParserUtil.getDataTitle(context, attrs);
                summary = XMLParserUtil.getDataSummary(context, attrs);
                keywords = XMLParserUtil.getDataKeywords(context, attrs);

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

                key = XMLParserUtil.getDataKey(context, attrs);
                if (nonIndexableKeys.contains(key)) {
                    continue;
                }

                title = XMLParserUtil.getDataTitle(context, attrs);
                keywords = XMLParserUtil.getDataKeywords(context, attrs);

                if (!nodeName.equals(NODE_NAME_CHECK_BOX_PREFERENCE)) {
                    summary = XMLParserUtil.getDataSummary(context, attrs);

                    String entries = null;

                    if (nodeName.endsWith(NODE_NAME_LIST_PREFERENCE)) {
                        entries = XMLParserUtil.getDataEntries(context, attrs);
                    }

                    // Insert rows for the child nodes of PreferenceScreen
                    updateOneRowWithFilteredData(database, localeStr, title, summary, null, entries,
                            fragmentName, screenTitle, iconResId, rank,
                            keywords, intentAction, intentTargetPackage, intentTargetClass,
                            true, key, -1 /* default user id */);
                } else {
                    String summaryOn = XMLParserUtil.getDataSummaryOn(context, attrs);
                    String summaryOff = XMLParserUtil.getDataSummaryOff(context, attrs);

                    if (TextUtils.isEmpty(summaryOn) && TextUtils.isEmpty(summaryOff)) {
                        summaryOn = XMLParserUtil.getDataSummary(context, attrs);
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

        final String updatedTitle = XMLParserUtil.normalizeHyphen(title);
        final String updatedSummaryOn = XMLParserUtil.normalizeHyphen(summaryOn);
        final String updatedSummaryOff = XMLParserUtil.normalizeHyphen(summaryOff);

        final String normalizedTitle = XMLParserUtil.normalizeString(updatedTitle);
        final String normalizedSummaryOn = XMLParserUtil.normalizeString(updatedSummaryOn);
        final String normalizedSummaryOff = XMLParserUtil.normalizeString(updatedSummaryOff);

        final String spaceDelimitedKeywords = XMLParserUtil.normalizeKeywords(keywords);

        updateOneRow(database, locale,
                updatedTitle, normalizedTitle, updatedSummaryOn, normalizedSummaryOn,
                updatedSummaryOff, normalizedSummaryOff, entries, className, screenTitle, iconResId,
                rank, spaceDelimitedKeywords, intentAction, intentTargetPackage, intentTargetClass,
                enabled, key, userId);
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
        values.put(IndexDatabaseHelper.IndexColumns.DOCID, docId);
        values.put(IndexDatabaseHelper.IndexColumns.LOCALE, locale);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_RANK, rank);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_TITLE, updatedTitle);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_TITLE_NORMALIZED, normalizedTitle);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON, updatedSummaryOn);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON_NORMALIZED,
                normalizedSummaryOn);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF, updatedSummaryOff);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF_NORMALIZED,
                normalizedSummaryOff);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_ENTRIES, entries);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEYWORDS, spaceDelimitedKeywords);
        values.put(IndexDatabaseHelper.IndexColumns.CLASS_NAME, className);
        values.put(IndexDatabaseHelper.IndexColumns.SCREEN_TITLE, screenTitle);
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_ACTION, intentAction);
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_PACKAGE, intentTargetPackage);
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_CLASS, intentTargetClass);
        values.put(IndexDatabaseHelper.IndexColumns.ICON, iconResId);
        values.put(IndexDatabaseHelper.IndexColumns.ENABLED, enabled);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEY_REF, key);
        values.put(IndexDatabaseHelper.IndexColumns.USER_ID, userId);

        database.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX, null, values);
    }

    /**
     * A private class for updating the Index database
     */
    private class UpdateIndexTask extends AsyncTask<DatabaseIndexingManager.UpdateData, Integer,
            Void> {

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
        protected Void doInBackground(DatabaseIndexingManager.UpdateData... params) {
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
                    delete(database, IndexDatabaseHelper.IndexColumns.CLASS_NAME, data.className);
                } else  {
                    if (data instanceof SearchIndexableRaw) {
                        final SearchIndexableRaw raw = (SearchIndexableRaw) data;
                        if (!TextUtils.isEmpty(raw.title)) {
                            delete(database, IndexDatabaseHelper.IndexColumns.DATA_TITLE,
                                    raw.title);
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

            return database.delete(IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX, whereClause,
                    whereArgs);
        }
    }
}
