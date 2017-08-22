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

package com.android.settings.search.indexing;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteDatabase;
import android.provider.SearchIndexableData;
import android.provider.SearchIndexableResource;
import android.support.annotation.DrawableRes;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.settings.core.PreferenceControllerMixin;

import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.IndexDatabaseHelper;
import com.android.settings.search.Indexable;
import com.android.settings.search.ResultPayload;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.search.SearchIndexableResources;
import com.android.settings.search.XmlParserUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.CLASS_NAME;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_ENTRIES;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_KEYWORDS;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_KEY_REF;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_RANK;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF_NORMALIZED;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON_NORMALIZED;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_TITLE;
import static com.android.settings.search.IndexDatabaseHelper.IndexColumns.DATA_TITLE_NORMALIZED;
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

/**
 * Helper class to convert {@link PreIndexData} to {@link IndexData}.
 *
 * TODO (b/33577327) This is just copied straight from DatabaseIndexingManager. But it's still ugly.
 * TODO              This is currently a long chain of method calls. It needs to be broken up.
 * TODO              but for the sake of easy code reviews, that will happen later.
 */
public class IndexDataConverter {

    private static final String LOG_TAG = "IndexDataConverter";

    private static final String NODE_NAME_PREFERENCE_SCREEN = "PreferenceScreen";
    private static final String NODE_NAME_CHECK_BOX_PREFERENCE = "CheckBoxPreference";
    private static final String NODE_NAME_LIST_PREFERENCE = "ListPreference";

    private Context mContext;

    private SQLiteDatabase mDb;

    public IndexDataConverter(Context context, SQLiteDatabase database) {
        mContext = context;
        mDb = database;
    }

    /**
     * Inserts {@link SearchIndexableData} into the database.
     *
     * @param localeStr        is the locale of the data to be inserted.
     * @param dataToUpdate     is a {@link List} of the data to be inserted.
     * @param nonIndexableKeys is a {@link Map} from Package Name to a {@link Set} of keys which
     *                         identify search results which should not be surfaced.
     */
    public void addDataToDatabase(String localeStr, List<SearchIndexableData> dataToUpdate,
            Map<String, Set<String>> nonIndexableKeys) {
        final long current = System.currentTimeMillis();

        for (SearchIndexableData data : dataToUpdate) {
            try {
                indexOneSearchIndexableData(localeStr, data, nonIndexableKeys);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Cannot index: " + (data != null ? data.className : data)
                        + " for locale: " + localeStr, e);
            }
        }

        final long now = System.currentTimeMillis();
        Log.d(LOG_TAG, "Indexing locale '" + localeStr + "' took " +
                (now - current) + " millis");
    }

    @VisibleForTesting
    void indexOneSearchIndexableData(String localeStr, SearchIndexableData data,
            Map<String, Set<String>> nonIndexableKeys) {
        if (data instanceof SearchIndexableResource) {
            indexOneResource(localeStr, (SearchIndexableResource) data, nonIndexableKeys);
        } else if (data instanceof SearchIndexableRaw) {
            indexOneRaw(localeStr, (SearchIndexableRaw) data, nonIndexableKeys);
        }
    }

    @VisibleForTesting
    void indexOneRaw(String localeStr, SearchIndexableRaw raw, Map<String,
            Set<String>> nonIndexableKeysFromResource) {
        // Should be the same locale as the one we are processing
        if (!raw.locale.toString().equalsIgnoreCase(localeStr)) {
            return;
        }

        Set<String> packageKeys = nonIndexableKeysFromResource.get(raw.intentTargetPackage);
        boolean enabled = raw.enabled;

        if (packageKeys != null && packageKeys.contains(raw.key)) {
            enabled = false;
        }

        IndexData.Builder builder = new IndexData.Builder();
        builder.setTitle(raw.title)
                .setSummaryOn(raw.summaryOn)
                .setLocale(localeStr)
                .setEntries(raw.entries)
                .setKeywords(raw.keywords)
                .setClassName(raw.className)
                .setScreenTitle(raw.screenTitle)
                .setIconResId(raw.iconResId)
                .setIntentAction(raw.intentAction)
                .setIntentTargetPackage(raw.intentTargetPackage)
                .setIntentTargetClass(raw.intentTargetClass)
                .setEnabled(enabled)
                .setKey(raw.key)
                .setUserId(raw.userId);

        updateOneRow(builder.build(mContext));
    }

    @VisibleForTesting
    void indexOneResource(String localeStr, SearchIndexableResource sir,
            Map<String, Set<String>> nonIndexableKeysFromResource) {

        if (sir == null) {
            Log.e(LOG_TAG, "Cannot index a null resource!");
            return;
        }

        final List<String> nonIndexableKeys = new ArrayList<>();

        if (sir.xmlResId > SearchIndexableResources.NO_DATA_RES_ID) {
            Set<String> resNonIndexableKeys = nonIndexableKeysFromResource.get(sir.packageName);
            if (resNonIndexableKeys != null && resNonIndexableKeys.size() > 0) {
                nonIndexableKeys.addAll(resNonIndexableKeys);
            }

            indexFromResource(localeStr, sir, nonIndexableKeys);
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

                indexFromProvider(localeStr, provider, sir, nonIndexableKeys);
            }
        }
    }

    @VisibleForTesting
    void indexFromResource(String localeStr, SearchIndexableResource sir,
            List<String> nonIndexableKeys) {
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
            IndexData.Builder headerBuilder = new IndexData.Builder();
            headerBuilder.setTitle(headerTitle)
                    .setSummaryOn(headerSummary)
                    .setKeywords(headerKeywords)
                    .setLocale(localeStr)
                    .setClassName(fragmentName)
                    .setScreenTitle(screenTitle)
                    .setIntentAction(intentAction)
                    .setIntentTargetPackage(intentTargetPackage)
                    .setIntentTargetClass(intentTargetClass)
                    .setEnabled(enabled)
                    .setKey(key)
                    .setUserId(-1 /* default user id */);

            // Flag for XML headers which a child element's title.
            boolean isHeaderUnique = true;
            IndexData.Builder builder;

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

                builder = new IndexData.Builder();
                builder.setTitle(title)
                        .setLocale(localeStr)
                        .setKeywords(keywords)
                        .setClassName(fragmentName)
                        .setScreenTitle(screenTitle)
                        .setIconResId(iconResId)
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

                    builder.setSummaryOn(summary)
                            .setEntries(entries)
                            .setChildClassName(childFragment)
                            .setPayload(payload);

                    // Insert rows for the child nodes of PreferenceScreen
                    updateOneRow(builder.build(mContext));
                } else {
                    // TODO (b/33577327) We removed summary off here. We should check if we can
                    // merge this 'else' section with the one above. Put a break point to
                    // investigate.
                    String summaryOn = XmlParserUtils.getDataSummaryOn(context, attrs);
                    String summaryOff = XmlParserUtils.getDataSummaryOff(context, attrs);

                    if (TextUtils.isEmpty(summaryOn) && TextUtils.isEmpty(summaryOff)) {
                        summaryOn = XmlParserUtils.getDataSummary(context, attrs);
                    }

                    builder.setSummaryOn(summaryOn);

                    updateOneRow(builder.build(mContext));
                }
            }

            // The xml header's title does not match the title of one of the child settings.
            if (isHeaderUnique) {
                updateOneRow(headerBuilder.build(mContext));
            }
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Error parsing PreferenceScreen", e);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing PreferenceScreen", e);
        } finally {
            if (parser != null) parser.close();
        }
    }

    @VisibleForTesting
    void indexFromProvider(String localeStr, Indexable.SearchIndexProvider provider,
            SearchIndexableResource sir, List<String> nonIndexableKeys) {

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

                IndexData.Builder builder = new IndexData.Builder();
                builder.setTitle(raw.title)
                        .setSummaryOn(raw.summaryOn)
                        .setLocale(localeStr)
                        .setEntries(raw.entries)
                        .setKeywords(raw.keywords)
                        .setClassName(className)
                        .setScreenTitle(raw.screenTitle)
                        .setIconResId(raw.iconResId)
                        .setIntentAction(raw.intentAction)
                        .setIntentTargetPackage(raw.intentTargetPackage)
                        .setIntentTargetClass(raw.intentTargetClass)
                        .setEnabled(enabled)
                        .setKey(raw.key)
                        .setUserId(raw.userId);

                updateOneRow(builder.build(mContext));
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

                indexFromResource(localeStr, item, nonIndexableKeys);
            }
        }
    }

    private void updateOneRow(IndexData row) {
        if (TextUtils.isEmpty(row.updatedTitle)) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(IndexDatabaseHelper.IndexColumns.DOCID, row.getDocId());
        values.put(LOCALE, row.locale);
        values.put(DATA_TITLE, row.updatedTitle);
        values.put(DATA_TITLE_NORMALIZED, row.normalizedTitle);
        values.put(DATA_SUMMARY_ON, row.updatedSummaryOn);
        values.put(DATA_SUMMARY_ON_NORMALIZED, row.normalizedSummaryOn);
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

        mDb.replaceOrThrow(TABLE_PREFS_INDEX, null, values);

        if (!TextUtils.isEmpty(row.className) && !TextUtils.isEmpty(row.childClassName)) {
            ContentValues siteMapPair = new ContentValues();
            final int pairDocId = Objects.hash(row.className, row.childClassName);
            siteMapPair.put(IndexDatabaseHelper.SiteMapColumns.DOCID, pairDocId);
            siteMapPair.put(IndexDatabaseHelper.SiteMapColumns.PARENT_CLASS, row.className);
            siteMapPair.put(IndexDatabaseHelper.SiteMapColumns.PARENT_TITLE, row.screenTitle);
            siteMapPair.put(IndexDatabaseHelper.SiteMapColumns.CHILD_CLASS, row.childClassName);
            siteMapPair.put(IndexDatabaseHelper.SiteMapColumns.CHILD_TITLE, row.updatedTitle);

            mDb.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_SITE_MAP, null, siteMapPair);
        }
    }
}
