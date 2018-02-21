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

import android.annotation.Nullable;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.provider.SearchIndexableData;
import android.provider.SearchIndexableResource;
import android.support.annotation.DrawableRes;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.core.PreferenceXmlParserUtils;
import com.android.settings.search.ResultPayload;
import com.android.settings.search.SearchIndexableRaw;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to convert {@link PreIndexData} to {@link IndexData}.
 */
public class IndexDataConverter {

    private static final String LOG_TAG = "IndexDataConverter";

    private static final String NODE_NAME_PREFERENCE_SCREEN = "PreferenceScreen";
    private static final String NODE_NAME_CHECK_BOX_PREFERENCE = "CheckBoxPreference";
    private static final String NODE_NAME_LIST_PREFERENCE = "ListPreference";

    private final Context mContext;

    public IndexDataConverter(Context context) {
        mContext = context;
    }

    /**
     * Return the collection of {@param preIndexData} converted into {@link IndexData}.
     *
     * @param preIndexData a collection of {@link SearchIndexableResource},
     *                     {@link SearchIndexableRaw} and non-indexable keys.
     */
    public List<IndexData> convertPreIndexDataToIndexData(PreIndexData preIndexData) {
        final long current = System.currentTimeMillis();
        final List<SearchIndexableData> indexableData = preIndexData.dataToUpdate;
        final Map<String, Set<String>> nonIndexableKeys = preIndexData.nonIndexableKeys;
        final List<IndexData> indexData = new ArrayList<>();

        for (SearchIndexableData data : indexableData) {
            if (data instanceof SearchIndexableRaw) {
                final SearchIndexableRaw rawData = (SearchIndexableRaw) data;
                final Set<String> rawNonIndexableKeys = nonIndexableKeys.get(
                        rawData.intentTargetPackage);
                final IndexData.Builder builder = convertRaw(rawData, rawNonIndexableKeys);

                if (builder != null) {
                    indexData.add(builder.build(mContext));
                }
            } else if (data instanceof SearchIndexableResource) {
                final SearchIndexableResource sir = (SearchIndexableResource) data;
                final Set<String> resourceNonIndexableKeys =
                        getNonIndexableKeysForResource(nonIndexableKeys, sir.packageName);
                final List<IndexData> resourceData = convertResource(sir, resourceNonIndexableKeys);
                indexData.addAll(resourceData);
            }
        }

        final long endConversion = System.currentTimeMillis();
        Log.d(LOG_TAG, "Converting pre-index data to index data took: "
                + (endConversion - current));

        return indexData;
    }

    /**
     * Return the conversion of {@link SearchIndexableRaw} to {@link IndexData}.
     * The fields of {@link SearchIndexableRaw} are a subset of {@link IndexData},
     * and there is some data sanitization in the conversion.
     */
    @Nullable
    private IndexData.Builder convertRaw(SearchIndexableRaw raw, Set<String> nonIndexableKeys) {
        // A row is enabled if it does not show up as an nonIndexableKey
        boolean enabled = !(nonIndexableKeys != null && nonIndexableKeys.contains(raw.key));

        IndexData.Builder builder = new IndexData.Builder();
        builder.setTitle(raw.title)
                .setSummaryOn(raw.summaryOn)
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

        return builder;
    }

    /**
     * Return the conversion of the {@link SearchIndexableResource} to {@link IndexData}.
     * Each of the elements in the xml layout attribute of {@param sir} is a candidate to be
     * converted (including the header element).
     *
     * TODO (b/33577327) simplify this method.
     */
    private List<IndexData> convertResource(SearchIndexableResource sir,
            Set<String> nonIndexableKeys) {
        final Context context = sir.context;
        XmlResourceParser parser = null;

        List<IndexData> resourceIndexData = new ArrayList<>();
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

            final String screenTitle = PreferenceXmlParserUtils.getDataTitle(context, attrs);
            String key = PreferenceXmlParserUtils.getDataKey(context, attrs);

            String title;
            String headerTitle;
            String summary;
            String headerSummary;
            String keywords;
            String headerKeywords;
            String childFragment;
            @DrawableRes int iconResId;
            ResultPayload payload;
            boolean enabled;
            final String fragmentName = sir.className;
            final String intentAction = sir.intentAction;
            final String intentTargetPackage = sir.intentTargetPackage;
            final String intentTargetClass = sir.intentTargetClass;

            Map<String, ResultPayload> controllerUriMap = new HashMap<>();

            if (fragmentName != null) {
                controllerUriMap = DatabaseIndexingUtils
                        .getPayloadKeyMap(fragmentName, context);
            }

            headerTitle = PreferenceXmlParserUtils.getDataTitle(context, attrs);
            headerSummary = PreferenceXmlParserUtils.getDataSummary(context, attrs);
            headerKeywords = PreferenceXmlParserUtils.getDataKeywords(context, attrs);
            enabled = !nonIndexableKeys.contains(key);

            // TODO: Set payload type for header results
            IndexData.Builder headerBuilder = new IndexData.Builder();
            headerBuilder.setTitle(headerTitle)
                    .setSummaryOn(headerSummary)
                    .setKeywords(headerKeywords)
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

                title = PreferenceXmlParserUtils.getDataTitle(context, attrs);
                key = PreferenceXmlParserUtils.getDataKey(context, attrs);
                enabled = !nonIndexableKeys.contains(key);
                keywords = PreferenceXmlParserUtils.getDataKeywords(context, attrs);
                iconResId = PreferenceXmlParserUtils.getDataIcon(context, attrs);

                if (isHeaderUnique && TextUtils.equals(headerTitle, title)) {
                    isHeaderUnique = false;
                }

                builder = new IndexData.Builder();
                builder.setTitle(title)
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
                    summary = PreferenceXmlParserUtils.getDataSummary(context, attrs);

                    String entries = null;

                    if (nodeName.endsWith(NODE_NAME_LIST_PREFERENCE)) {
                        entries = PreferenceXmlParserUtils.getDataEntries(context, attrs);
                    }

                    // TODO (b/62254931) index primitives instead of payload
                    payload = controllerUriMap.get(key);
                    childFragment = PreferenceXmlParserUtils.getDataChildFragment(context, attrs);

                    builder.setSummaryOn(summary)
                            .setEntries(entries)
                            .setChildClassName(childFragment)
                            .setPayload(payload);

                    resourceIndexData.add(builder.build(mContext));
                } else {
                    // TODO (b/33577327) We removed summary off here. We should check if we can
                    // merge this 'else' section with the one above. Put a break point to
                    // investigate.
                    String summaryOn = PreferenceXmlParserUtils.getDataSummaryOn(context, attrs);
                    String summaryOff = PreferenceXmlParserUtils.getDataSummaryOff(context, attrs);

                    if (TextUtils.isEmpty(summaryOn) && TextUtils.isEmpty(summaryOff)) {
                        summaryOn = PreferenceXmlParserUtils.getDataSummary(context, attrs);
                    }

                    builder.setSummaryOn(summaryOn);

                    resourceIndexData.add(builder.build(mContext));
                }
            }

            // The xml header's title does not match the title of one of the child settings.
            if (isHeaderUnique) {
                resourceIndexData.add(headerBuilder.build(mContext));
            }
        } catch (XmlPullParserException e) {
            Log.w(LOG_TAG, "XML Error parsing PreferenceScreen: ", e);
        } catch (IOException e) {
            Log.w(LOG_TAG, "IO Error parsing PreferenceScreen: ", e);
        } catch (Resources.NotFoundException e) {
            Log.w(LOG_TAG, "Resoucre not found error parsing PreferenceScreen: ", e);
        } finally {
            if (parser != null) parser.close();
        }
        return resourceIndexData;
    }

    private Set<String> getNonIndexableKeysForResource(Map<String, Set<String>> nonIndexableKeys,
            String packageName) {
        return nonIndexableKeys.containsKey(packageName)
                ? nonIndexableKeys.get(packageName)
                : new HashSet<>();
    }
}
