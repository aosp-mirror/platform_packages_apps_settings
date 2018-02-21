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
 * limitations under the License
 */

package com.android.settings.slices;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.provider.SearchIndexableResource;
import android.support.annotation.DrawableRes;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.core.PreferenceXmlParserUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Converts {@link DashboardFragment} to {@link SliceData}.
 */
class SliceDataConverter {

    private static final String TAG = "SliceDataConverter";

    private static final String NODE_NAME_PREFERENCE_SCREEN = "PreferenceScreen";

    private Context mContext;

    private List<SliceData> mSliceData;

    public SliceDataConverter(Context context) {
        mContext = context;
        mSliceData = new ArrayList<>();
    }

    /**
     * @return a list of {@link SliceData} to be indexed and later referenced as a Slice.
     *
     * The collection works as follows:
     * - Collects a list of Fragments from
     * {@link FeatureFactory#getSearchFeatureProvider()}.
     * - From each fragment, grab a {@link SearchIndexProvider}.
     * - For each provider, collect XML resource layout and a list of
     * {@link com.android.settings.core.BasePreferenceController}.
     */
    public List<SliceData> getSliceData() {
        if (!mSliceData.isEmpty()) {
            return mSliceData;
        }

        final Collection<Class> indexableClasses = FeatureFactory.getFactory(mContext)
                .getSearchFeatureProvider().getSearchIndexableResources().getProviderValues();

        for (Class clazz : indexableClasses) {
            final String fragmentName = clazz.getName();

            final SearchIndexProvider provider = DatabaseIndexingUtils.getSearchIndexProvider(
                    clazz);

            // CodeInspection test guards against the null check. Keep check in case of bad actors.
            if (provider == null) {
                Log.e(TAG, fragmentName + " dose not implement Search Index Provider");
                continue;
            }

            final List<SliceData> providerSliceData = getSliceDataFromProvider(provider,
                    fragmentName);
            mSliceData.addAll(providerSliceData);
        }

        return mSliceData;
    }

    private List<SliceData> getSliceDataFromProvider(SearchIndexProvider provider,
            String fragmentName) {
        final List<SliceData> sliceData = new ArrayList<>();

        final List<SearchIndexableResource> resList =
                provider.getXmlResourcesToIndex(mContext, true /* enabled */);

        if (resList == null) {
            return sliceData;
        }

        // TODO (b/67996923) get a list of permanent NIKs and skip the invalid keys.

        for (SearchIndexableResource resource : resList) {
            int xmlResId = resource.xmlResId;
            if (xmlResId == 0) {
                Log.e(TAG, fragmentName + " provides invalid XML (0) in search provider.");
                continue;
            }

            List<SliceData> xmlSliceData = getSliceDataFromXML(xmlResId, fragmentName);
            sliceData.addAll(xmlSliceData);
        }

        return sliceData;
    }

    private List<SliceData> getSliceDataFromXML(int xmlResId, String fragmentName) {
        XmlResourceParser parser = null;

        final List<SliceData> xmlSliceData = new ArrayList<>();
        String key;
        String title;
        String summary;
        @DrawableRes int iconResId;
        String controllerClassName;

        try {
            parser = mContext.getResources().getXml(xmlResId);

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
            final String screenTitle = PreferenceXmlParserUtils.getDataTitle(mContext, attrs);

            // TODO (b/67996923) Investigate if we need headers for Slices, since they never
            // correspond to an actual setting.
            SliceData xmlSlice;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }


                // TODO (b/67996923) Non-controller Slices should become intent-only slices.
                // Note that without a controller, dynamic summaries are impossible.
                // TODO (b/67996923) This will not work if preferences have nested intents:
                // <pref ....>
                //      <intent action="blab"/> </pref>
                controllerClassName = PreferenceXmlParserUtils.getController(mContext, attrs);
                if (TextUtils.isEmpty(controllerClassName)) {
                    continue;
                }

                title = PreferenceXmlParserUtils.getDataTitle(mContext, attrs);
                key = PreferenceXmlParserUtils.getDataKey(mContext, attrs);
                iconResId = PreferenceXmlParserUtils.getDataIcon(mContext, attrs);
                summary = PreferenceXmlParserUtils.getDataSummary(mContext, attrs);

                xmlSlice = new SliceData.Builder()
                        .setKey(key)
                        .setTitle(title)
                        .setSummary(summary)
                        .setIcon(iconResId)
                        .setScreenTitle(screenTitle)
                        .setPreferenceControllerClassName(controllerClassName)
                        .setFragmentName(fragmentName)
                        .build();

                xmlSliceData.add(xmlSlice);
            }
        } catch (XmlPullParserException e) {
            Log.w(TAG, "XML Error parsing PreferenceScreen: ", e);
        } catch (IOException e) {
            Log.w(TAG, "IO Error parsing PreferenceScreen: ", e);
        } catch (Resources.NotFoundException e) {
            Log.w(TAG, "Resoucre not found error parsing PreferenceScreen: ", e);
        } finally {
            if (parser != null) parser.close();
        }
        return xmlSliceData;
    }
}