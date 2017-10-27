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
 */

package com.android.settings;

import static junit.framework.Assert.fail;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.provider.SearchIndexableResource;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableResources;
import com.android.settings.search.XmlParserUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class UniquePreferenceTest {

    private static final String TAG = "UniquePreferenceTest";
    private static final List<String> SUPPORTED_PREF_TYPES = Arrays.asList(
            "Preference", "PreferenceCategory", "PreferenceScreen");
    private static final List<String> WHITELISTED_DUPLICATE_KEYS = Arrays.asList(
            "owner_info_settings",          // Lock screen message in security - multiple xml files
                                            // contain this because security page is constructed by
                                            // combining small xml chunks. Eventually the page
                                            // should be formed as one single xml and this entry
                                            // should be removed.

            "dashboard_tile_placeholder"    // This is the placeholder pref for injecting dynamic
                                            // tiles.
            );

    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    /**
     * All preferences should have their unique key. It's especially important for many parts of
     * Settings to work properly: we assume pref keys are unique in displaying, search ranking,\
     * search result suppression, and many other areas.
     * <p/>
     * So in this test we are checking preferences participating in search.
     * <p/>
     * Note: Preference is not limited to just <Preference/> object. Everything in preference xml
     * should have a key.
     */
    @Test
    public void allPreferencesShouldHaveUniqueKey()
            throws IOException, XmlPullParserException, Resources.NotFoundException {
        final Set<String> uniqueKeys = new HashSet<>();
        final Set<String> nullKeyClasses = new HashSet<>();
        final Set<String> duplicatedKeys = new HashSet<>();
        for (SearchIndexableResource sir : SearchIndexableResources.values()) {
            verifyPreferenceIdInXml(uniqueKeys, duplicatedKeys, nullKeyClasses, sir);
        }

        if (!nullKeyClasses.isEmpty()) {
            final StringBuilder nullKeyErrors = new StringBuilder()
                    .append("Each preference must have a key, ")
                    .append("the following classes have pref without keys:\n");
            for (String c : nullKeyClasses) {
                nullKeyErrors.append(c).append("\n");
            }
            fail(nullKeyErrors.toString());
        }

        if (!duplicatedKeys.isEmpty()) {
            final StringBuilder dupeKeysError = new StringBuilder(
                    "The following keys are not unique\n");
            for (String c : duplicatedKeys) {
                dupeKeysError.append(c).append("\n");
            }
            fail(dupeKeysError.toString());
        }
    }

    private void verifyPreferenceIdInXml(Set<String> uniqueKeys, Set<String> duplicatedKeys,
            Set<String> nullKeyClasses, SearchIndexableResource page)
            throws IOException, XmlPullParserException, Resources.NotFoundException {
        final Class<?> clazz = DatabaseIndexingUtils.getIndexableClass(page.className);

        final Indexable.SearchIndexProvider provider =
                DatabaseIndexingUtils.getSearchIndexProvider(clazz);
        final List<SearchIndexableResource> resourcesToIndex =
                provider.getXmlResourcesToIndex(mContext, true);
        if (resourcesToIndex == null) {
            Log.d(TAG, page.className + "is not providing SearchIndexableResource, skipping");
            return;
        }

        for (SearchIndexableResource sir : resourcesToIndex) {
            if (sir.xmlResId <= 0) {
                Log.d(TAG, page.className + " doesn't have a valid xml to index.");
                continue;
            }
            final XmlResourceParser parser = mContext.getResources().getXml(sir.xmlResId);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }
            final int outerDepth = parser.getDepth();

            do {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
                final String nodeName = parser.getName();
                if (!SUPPORTED_PREF_TYPES.contains(nodeName) && !nodeName.endsWith("Preference")) {
                    continue;
                }
                final AttributeSet attrs = Xml.asAttributeSet(parser);
                final String key = XmlParserUtils.getDataKey(mContext, attrs);
                if (TextUtils.isEmpty(key)) {
                    Log.e(TAG, "Every preference must have an key; found null key"
                            + " in " + page.className
                            + " at " + parser.getPositionDescription());
                    nullKeyClasses.add(page.className);
                    continue;
                }
                if (uniqueKeys.contains(key) && !WHITELISTED_DUPLICATE_KEYS.contains(key)) {
                    Log.e(TAG, "Every preference key must unique; found " + nodeName
                            + " in " + page.className
                            + " at " + parser.getPositionDescription());
                    duplicatedKeys.add(key);
                }
                uniqueKeys.add(key);
            } while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth));
        }
    }
}
