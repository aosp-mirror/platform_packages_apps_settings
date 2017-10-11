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
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search2.AppSearchResult;
import com.android.settings.search2.DatabaseResultLoader;
import com.android.settings.search2.InlineSwitchViewHolder;
import com.android.settings.search2.InstalledAppResultLoader;
import com.android.settings.search2.IntentPayload;
import com.android.settings.search2.IntentSearchViewHolder;
import com.android.settings.search2.ResultPayload;
import com.android.settings.search2.SearchFragment;
import com.android.settings.search2.SearchResult;
import com.android.settings.search2.SearchResult.Builder;
import com.android.settings.search2.SearchResultsAdapter;
import com.android.settings.search2.SearchViewHolder;

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
public class SearchResultsAdapterTest {

    @Mock
    private SearchFragment mFragment;
    private SearchResultsAdapter mAdapter;
    private Context mContext;
    private String mLoaderClassName;

    private String[] TITLES = {"alpha", "bravo", "charlie", "appAlpha", "appBravo", "appCharlie"};

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = Robolectric.buildActivity(Activity.class).get();
        mAdapter = new SearchResultsAdapter(mFragment);
        mLoaderClassName = DatabaseResultLoader.class.getName();
    }

    @Test
    public void testNoResultsAdded_emptyListReturned() {
        List<SearchResult> updatedResults = mAdapter.getSearchResults();
        assertThat(updatedResults).isEmpty();
    }

    @Test
    public void testSingleSourceMerge_exactCopyReturned() {
        ArrayList<SearchResult> intentResults = getIntentSampleResults();
        mAdapter.addSearchResults(intentResults, mLoaderClassName);
        mAdapter.displaySearchResults();

        List<SearchResult> updatedResults = mAdapter.getSearchResults();
        assertThat(updatedResults).containsAllIn(intentResults);
    }

    @Test
    public void testCreateViewHolder_returnsIntentResult() {
        ViewGroup group = new FrameLayout(mContext);
        SearchViewHolder view = mAdapter.onCreateViewHolder(group,
                ResultPayload.PayloadType.INTENT);
        assertThat(view).isInstanceOf(IntentSearchViewHolder.class);
    }

    @Test
    public void testCreateViewHolder_returnsInlineSwitchResult() {
        ViewGroup group = new FrameLayout(mContext);
        SearchViewHolder view = mAdapter.onCreateViewHolder(group,
                ResultPayload.PayloadType.INLINE_SWITCH);
        assertThat(view).isInstanceOf(InlineSwitchViewHolder.class);
    }

    @Test
    public void testEndToEndSearch_properResultsMerged_correctOrder() {
        mAdapter.addSearchResults(getDummyAppResults(), InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(getDummyDbResults(), DatabaseResultLoader.class.getName());
        mAdapter.displaySearchResults();

        List<SearchResult> results = mAdapter.getSearchResults();
        List<SearchResult> sortedDummyResults  = getSortedDummyResults();

        assertThat(results).containsExactlyElementsIn(sortedDummyResults).inOrder();
    }

    @Test
    public void testEndToEndSearch_addResults_resultsAddedInOrder() {
        List<AppSearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        // Add two individual items
        mAdapter.addSearchResults(appResults.subList(0,1),
                InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(dbResults.subList(0,1), DatabaseResultLoader.class.getName());
        mAdapter.displaySearchResults();
        // Add super-set of items
        mAdapter.addSearchResults(appResults, InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(dbResults, DatabaseResultLoader.class.getName());
        mAdapter.displaySearchResults();

        List<SearchResult> results = mAdapter.getSearchResults();
        List<SearchResult> sortedDummyResults  = getSortedDummyResults();
        assertThat(results).containsExactlyElementsIn(sortedDummyResults).inOrder();
    }

    @Test
    public void testEndToEndSearch_removeResults_resultsAdded() {
        List<AppSearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        // Add list of items
        mAdapter.addSearchResults(appResults, InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(dbResults, DatabaseResultLoader.class.getName());
        mAdapter.displaySearchResults();
        // Add subset of items
        mAdapter.addSearchResults(appResults.subList(0,1),
                InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(dbResults.subList(0,1), DatabaseResultLoader.class.getName());
        int count = mAdapter.displaySearchResults();

        List<SearchResult> results = mAdapter.getSearchResults();
        assertThat(results.get(0).title).isEqualTo(TITLES[0]);
        assertThat(results.get(1).title).isEqualTo(TITLES[3]);
        assertThat(count).isEqualTo(2);
    }

    private List<SearchResult> getDummyDbResults() {
        List<SearchResult> results = new ArrayList<>();
        IntentPayload payload = new IntentPayload(new Intent());
        SearchResult.Builder builder = new SearchResult.Builder();
        builder.addPayload(payload);

        builder.addTitle(TITLES[0])
                .addRank(1);
        results.add(builder.build());

        builder.addTitle(TITLES[1])
                .addRank(3);
        results.add(builder.build());

        builder.addTitle(TITLES[2])
                .addRank(6);
        results.add(builder.build());

        return results;
    }

    private List<AppSearchResult> getDummyAppResults() {
        List<AppSearchResult> results = new ArrayList<>();
        IntentPayload payload = new IntentPayload(new Intent());
        AppSearchResult.Builder builder = new AppSearchResult.Builder();
        builder.addPayload(payload);

        builder.addTitle(TITLES[3])
                .addRank(1);
        results.add(builder.build());

        builder.addTitle(TITLES[4])
                .addRank(2);
        results.add(builder.build());

        builder.addTitle(TITLES[5])
                .addRank(4);
        results.add(builder.build());

        return results;
    }

    private List<SearchResult> getSortedDummyResults() {
        List<AppSearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        List<SearchResult> sortedResults = new ArrayList<>(appResults.size() + dbResults.size());
        sortedResults.add(dbResults.get(0)); // alpha
        sortedResults.add(appResults.get(0)); // appAlpha
        sortedResults.add(appResults.get(1)); // appBravo
        sortedResults.add(dbResults.get(1)); // bravo
        sortedResults.add(appResults.get(2)); // appCharlie
        sortedResults.add(dbResults.get(2)); // Charlie

        return sortedResults;
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
}