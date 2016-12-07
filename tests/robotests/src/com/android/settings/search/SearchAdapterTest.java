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

package com.android.settings.search;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search2.DatabaseResultLoader;
import com.android.settings.search2.IntentPayload;
import com.android.settings.search2.ResultPayload;
import com.android.settings.search2.SearchFragment;
import com.android.settings.search2.SearchResult;
import com.android.settings.search2.SearchResult.Builder;
import com.android.settings.search2.SearchResultsAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SearchAdapterTest {

    @Mock
    private SearchFragment mFragment;
    private SearchResultsAdapter mAdapter;
    private Context mContext;
    private String mLoaderClassName;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = Robolectric.buildActivity(Activity.class).get();
        mAdapter = new SearchResultsAdapter(mFragment);
        mLoaderClassName = DatabaseResultLoader.class.getName();
    }

    private ArrayList<SearchResult> getIntentSampleResults() {
        ArrayList<SearchResult> sampleResults = new ArrayList<>();
        ArrayList<String> breadcrumbs = new ArrayList<>();
        final Drawable icon = mContext.getDrawable(R.drawable.ic_search_history);
        final ResultPayload payload = new IntentPayload(null);
        final SearchResult.Builder builder = new Builder();
        builder.addTitle("title")
                .addSummary("summary")
                .addRank(1)
                .addBreadcrumbs(breadcrumbs)
                .addIcon(icon)
                .addPayload(payload);
        sampleResults.add(builder.build());

        builder.addRank(2);
        sampleResults.add(builder.build());

        builder.addRank(3);
        sampleResults.add(builder.build());
        return sampleResults;
    }


    @Test
    public void testNoResultsAdded_EmptyListReturned() {
        List<SearchResult> updatedResults = mAdapter.getSearchResults();
        assertThat(updatedResults).isEmpty();
    }


    @Test
    public void testSingleSourceMerge_ExactCopyReturned() {
        ArrayList<SearchResult> intentResults = getIntentSampleResults();
        mAdapter.mergeResults(intentResults, mLoaderClassName);

        List<SearchResult> updatedResults = mAdapter.getSearchResults();
        assertThat(updatedResults).containsAllIn(intentResults);
    }

    @Test
    public void testDuplicateSourceMerge_ExactCopyReturned() {
        ArrayList<SearchResult> intentResults = getIntentSampleResults();
        mAdapter.mergeResults(intentResults, mLoaderClassName);
        mAdapter.mergeResults(intentResults, mLoaderClassName);

        List<SearchResult> updatedResults = mAdapter.getSearchResults();
        assertThat(updatedResults).containsAllIn(intentResults);
    }
}