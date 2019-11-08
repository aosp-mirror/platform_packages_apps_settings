/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_FRAGMENT;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;

import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.PreferenceXmlParserUtils;
import com.android.settings.core.codeinspection.CodeInspector;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableData;

import org.robolectric.RuntimeEnvironment;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * {@link CodeInspector} to ensure preferences with fragments implement search components correctly.
 */
public class FragmentSearchIndexableCodeInspector extends CodeInspector {

    private final String mErrorNonIndexFragments =
            "The following fragments were used by 'android:fragment=Fragment_Class_Name' in "
                    + "corresponding caller preference Xml. This preference won't be searchable, "
                    + "the fragment should implement SearchIndexable for Settings Search. If it "
                    + "should not be searchable, add the fragment's classname to "
                    + "grandfather_fragment_not_searchable. Fragments:\n";

    private final Context mContext;
    private final List<String> mXmlDeclaredFragments = new ArrayList<>();
    private final List<String> mGrandfatherNotSearchIndesable = new ArrayList<>();

    public FragmentSearchIndexableCodeInspector(List<Class<?>> classes) throws Exception {
        super(classes);
        mContext = RuntimeEnvironment.application;

        initDeclaredFragments();
        initializeGrandfatherList(mGrandfatherNotSearchIndesable,
                "grandfather_fragment_not_searchable");
    }

    @Override
    public void run() {
        for (Class<?> clazz : mClasses) {
            if (!isConcreteSettingsClass(clazz)) {
                // Only care about non-abstract classes.
                continue;
            }
            if (!InstrumentedPreferenceFragment.class.isAssignableFrom(clazz)) {
                // Only care about InstrumentedPreferenceFragment
                continue;
            }

            try {
                clazz.getField("SEARCH_INDEX_DATA_PROVIDER");
                mXmlDeclaredFragments.remove(clazz.getName());
                continue;
            } catch (NoSuchFieldException e) {
            }

            if (SearchIndexable.class.isAssignableFrom(clazz)) {
                mXmlDeclaredFragments.remove(clazz.getName());
                continue;
            }
        }

        mXmlDeclaredFragments.removeAll(mGrandfatherNotSearchIndesable);

        final String missingFragmentError =
                buildErrorMessage(mErrorNonIndexFragments, mXmlDeclaredFragments);

        assertWithMessage(missingFragmentError).that(mXmlDeclaredFragments).isEmpty();
    }

    private String buildErrorMessage(String errorSummary, List<String> errorClasses) {
        final StringBuilder error = new StringBuilder(errorSummary);
        for (String c : errorClasses) {
            error.append(c).append("\n");
        }
        return error.toString();
    }

    private void initDeclaredFragments() throws IOException, XmlPullParserException {
        final List<Integer> xmlResources = getIndexableXml();
        for (int xmlResId : xmlResources) {
            final List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(mContext,
                    xmlResId, PreferenceXmlParserUtils.MetadataFlag.FLAG_NEED_FRAGMENT);
            for (Bundle bundle : metadata) {
                final String fragmentClassName = bundle.getString(METADATA_FRAGMENT);
                if (TextUtils.isEmpty(fragmentClassName)) {
                    continue;
                }
                if (!mXmlDeclaredFragments.contains(fragmentClassName)) {
                    mXmlDeclaredFragments.add(fragmentClassName);
                }
            }
        }
        // We definitely have some fragments in xml, so assert not-empty here as a proxy to
        // make sure the parser didn't fail
        assertThat(mXmlDeclaredFragments).isNotEmpty();
    }

    private List<Integer> getIndexableXml() {
        final List<Integer> xmlResSet = new ArrayList<>();

        final Collection<SearchIndexableData> bundles = FeatureFactory.getFactory(
                mContext).getSearchFeatureProvider().getSearchIndexableResources()
                .getProviderValues();

        for (SearchIndexableData bundle : bundles) {
            Indexable.SearchIndexProvider provider = bundle.getSearchIndexProvider();

            if (provider == null) {
                continue;
            }

            List<SearchIndexableResource> resources = provider.getXmlResourcesToIndex(mContext,
                    true);

            if (resources == null) {
                continue;
            }

            for (SearchIndexableResource resource : resources) {
                // Add '0's anyway. It won't break the test.
                xmlResSet.add(resource.xmlResId);
            }
        }
        return xmlResSet;
    }
}
