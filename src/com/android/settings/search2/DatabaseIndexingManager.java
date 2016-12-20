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
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import com.android.settings.core.PreferenceController;
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
            dataToUpdate = new ArrayList<>();
            dataToDelete = new ArrayList<>();
            nonIndexableKeys = new HashMap<>();
        }

        public UpdateData(DatabaseIndexingManager.UpdateData other) {
            dataToUpdate = new ArrayList<>(other.dataToUpdate);
            dataToDelete = new ArrayList<>(other.dataToDelete);
            nonIndexableKeys = new HashMap<>(other.nonIndexableKeys);
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

    public void indexOneSearchIndexableData(SQLiteDatabase database, String localeStr,
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
                .setEnabled(raw.enabled)
                .setKey(raw.key)
                .setUserId(raw.userId);

        updateOneRowWithFilteredData(database, builder, raw.title, raw.summaryOn, raw.summaryOff,
                raw.keywords);
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

            final String screenTitle = XmlParserUtils.getDataTitle(context, attrs);

            String key = XmlParserUtils.getDataKey(context, attrs);

            String title;
            String summary;
            String keywords;
            ResultPayload payload;

            ArrayMap<String, PreferenceController> controllerUriMap = null;

            if (fragmentName != null) {
                controllerUriMap = (ArrayMap) DatabaseIndexingUtils
                        .getPreferenceControllerUriMap(fragmentName, context);
            }

            // Insert rows for the main PreferenceScreen node. Rewrite the data for removing
            // hyphens.
            if (!nonIndexableKeys.contains(key)) {
                title = XmlParserUtils.getDataTitle(context, attrs);
                summary = XmlParserUtils.getDataSummary(context, attrs);
                keywords = XmlParserUtils.getDataKeywords(context, attrs);

                DatabaseRow.Builder builder = new DatabaseRow.Builder();
                builder.setLocale(localeStr)
                        .setEntries(null)
                        .setClassName(fragmentName)
                        .setScreenTitle(screenTitle)
                        .setIconResId(iconResId)
                        .setRank(rank)
                        .setIntentAction(intentAction)
                        .setIntentTargetPackage(intentTargetPackage)
                        .setIntentTargetClass(intentTargetClass)
                        .setEnabled(true)
                        .setKey(key)
                        .setUserId(-1 /* default user id */);

                updateOneRowWithFilteredData(database, builder, title, summary,
                        null /* summary off */, keywords);
            }

            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                nodeName = parser.getName();

                key = XmlParserUtils.getDataKey(context, attrs);
                if (nonIndexableKeys.contains(key)) {
                    continue;
                }

                title = XmlParserUtils.getDataTitle(context, attrs);
                keywords = XmlParserUtils.getDataKeywords(context, attrs);

                DatabaseRow.Builder builder = new DatabaseRow.Builder();
                builder.setLocale(localeStr)
                        .setClassName(fragmentName)
                        .setScreenTitle(screenTitle)
                        .setIconResId(iconResId)
                        .setRank(rank)
                        .setIntentAction(intentAction)
                        .setIntentTargetPackage(intentTargetPackage)
                        .setIntentTargetClass(intentTargetClass)
                        .setEnabled(true)
                        .setKey(key)
                        .setUserId(-1 /* default user id */);

                if (!nodeName.equals(NODE_NAME_CHECK_BOX_PREFERENCE)) {
                    summary = XmlParserUtils.getDataSummary(context, attrs);

                    String entries = null;

                    if (nodeName.endsWith(NODE_NAME_LIST_PREFERENCE)) {
                        entries = XmlParserUtils.getDataEntries(context, attrs);
                    }

                    payload = DatabaseIndexingUtils.getPayloadFromUriMap(controllerUriMap, key);

                    builder.setEntries(entries)
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

                DatabaseRow.Builder builder = new DatabaseRow.Builder();
                builder.setLocale(localeStr)
                        .setEntries(raw.entries)
                        .setClassName(className)
                        .setScreenTitle(raw.screenTitle)
                        .setIconResId(iconResId)
                        .setRank(rank)
                        .setIntentAction(raw.intentAction)
                        .setIntentTargetPackage(raw.intentTargetPackage)
                        .setIntentTargetClass(raw.intentTargetClass)
                        .setEnabled(raw.enabled)
                        .setKey(raw.key)
                        .setUserId(raw.userId);

                updateOneRowWithFilteredData(database, builder, raw.title, raw.summaryOn,
                        raw.summaryOff, raw.keywords);
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

    private void updateOneRowWithFilteredData(SQLiteDatabase database, DatabaseRow.Builder builder,
            String title, String summaryOn, String summaryOff,String keywords) {

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

        updateOneRow(database, builder.build());
    }

    private void updateOneRow(SQLiteDatabase database, DatabaseRow row) {

        if (TextUtils.isEmpty(row.updatedTitle)) {
            return;
        }

        // The DocID should contains more than the title string itself (you may have two settings
        // with the same title). So we need to use a combination of the title and the screenTitle.
        StringBuilder sb = new StringBuilder(row.updatedTitle);
        sb.append(row.screenTitle);
        int docId = sb.toString().hashCode();

        ContentValues values = new ContentValues();
        values.put(IndexDatabaseHelper.IndexColumns.DOCID, docId);
        values.put(IndexDatabaseHelper.IndexColumns.LOCALE, row.locale);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_RANK, row.rank);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_TITLE, row.updatedTitle);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_TITLE_NORMALIZED, row.normalizedTitle);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON, row.updatedSummaryOn);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON_NORMALIZED,
                row.normalizedSummaryOn);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF, row.updatedSummaryOff);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF_NORMALIZED,
                row.normalizedSummaryOff);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_ENTRIES, row.entries);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEYWORDS, row.spaceDelimitedKeywords);
        values.put(IndexDatabaseHelper.IndexColumns.CLASS_NAME, row.className);
        values.put(IndexDatabaseHelper.IndexColumns.SCREEN_TITLE, row.screenTitle);
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_ACTION, row.intentAction);
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_PACKAGE, row.intentTargetPackage);
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_CLASS, row.intentTargetClass);
        values.put(IndexDatabaseHelper.IndexColumns.ICON, row.iconResId);
        values.put(IndexDatabaseHelper.IndexColumns.ENABLED, row.enabled);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEY_REF, row.key);
        values.put(IndexDatabaseHelper.IndexColumns.USER_ID, row.userId);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD_TYPE, row.payloadType);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD, row.payload);

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
            @ResultPayload.PayloadType private int mPayloadType;
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

                if(mPayload != null) {
                    setPayloadType(mPayload.getType());
                }
                return this;
            }

            /**
             * Payload type is added when a Payload is added to the Builder in {setPayload}
             * @param payloadType PayloadType
             * @return The Builder
             */
            private Builder setPayloadType(@ResultPayload.PayloadType int payloadType) {
                mPayloadType = payloadType;
                return this;
            }

            public DatabaseRow build() {
                return new DatabaseRow(this);
            }
        }
    }
}
