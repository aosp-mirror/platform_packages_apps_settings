/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.slices;

import static junit.framework.Assert.fail;

import android.content.Context;
import android.os.Bundle;
import android.platform.test.annotations.Presubmit;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.core.PreferenceXmlParserUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexableData;
import com.android.settingslib.search.SearchIndexableResources;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class SliceDataContractTest {

    private static final String TAG = "SliceDataContractTest";
    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    @Presubmit
    public void preferenceWithControllerMustHaveNonEmptyTitle()
            throws IOException, XmlPullParserException {
        final Set<String> nullTitleFragments = new HashSet<>();

        final SearchIndexableResources resources =
                FeatureFactory.getFactory(mContext).getSearchFeatureProvider()
                        .getSearchIndexableResources();

        for (SearchIndexableData SearchIndexableData : resources.getProviderValues()) {
            verifyPreferenceTitle(nullTitleFragments, SearchIndexableData);
        }

        if (!nullTitleFragments.isEmpty()) {
            final StringBuilder error = new StringBuilder(
                    "All preferences with a controller must have a non-empty title by default, "
                            + "found empty title in the following fragments\n");
            for (String c : nullTitleFragments) {
                error.append(c).append("\n");
            }
            fail(error.toString());
        }
    }

    private void verifyPreferenceTitle(Set<String> nullTitleFragments,
            SearchIndexableData searchIndexableData)
            throws IOException, XmlPullParserException {

        final String className = searchIndexableData.getTargetClass().getName();
        final Indexable.SearchIndexProvider provider =
                searchIndexableData.getSearchIndexProvider();

        final List<SearchIndexableResource> resourcesToIndex =
                provider.getXmlResourcesToIndex(mContext, true);

        if (resourcesToIndex == null) {
            Log.d(TAG, className + "is not providing SearchIndexableResource, skipping");
            return;
        }

        for (SearchIndexableResource sir : resourcesToIndex) {
            final List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(mContext,
                    sir.xmlResId,
                    PreferenceXmlParserUtils.MetadataFlag.FLAG_INCLUDE_PREF_SCREEN
                            | PreferenceXmlParserUtils.MetadataFlag.FLAG_NEED_PREF_TITLE
                            | PreferenceXmlParserUtils.MetadataFlag.FLAG_NEED_PREF_CONTROLLER);

            for (Bundle bundle : metadata) {
                final String controller = bundle.getString(
                        PreferenceXmlParserUtils.METADATA_CONTROLLER);
                if (TextUtils.isEmpty(controller)) {
                    continue;
                }
                final String title = bundle.getString(PreferenceXmlParserUtils.METADATA_TITLE);
                if (TextUtils.isEmpty(title)) {
                    nullTitleFragments.add(className);
                }
            }
        }
    }

}