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
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search.SearchResult.Builder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SearchResultsAdapterTest {

    @Mock
    private SearchFragment mFragment;
    @Mock
    private SearchFeatureProvider mSearchFeatureProvider;
    @Mock
    private Context mMockContext;
    private SearchResultsAdapter mAdapter;
    private Context mContext;
    private String mLoaderClassName;

    private String[] TITLES = {"alpha", "bravo", "charlie", "appAlpha", "appBravo", "appCharlie"};

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = Robolectric.buildActivity(Activity.class).get();
        mAdapter = new SearchResultsAdapter(mFragment, mSearchFeatureProvider);
        mLoaderClassName = DatabaseResultLoader.class.getName();
        when(mFragment.getContext()).thenReturn(mMockContext);
        when(mMockContext.getApplicationContext()).thenReturn(mContext);
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
        mAdapter.displaySearchResults("");

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
        int count = mAdapter.displaySearchResults("");

        List<SearchResult> results = mAdapter.getSearchResults();
        assertThat(results.get(0).title).isEqualTo(TITLES[0]); // alpha
        assertThat(results.get(1).title).isEqualTo(TITLES[3]); // appAlpha
        assertThat(results.get(2).title).isEqualTo(TITLES[4]); // appBravo
        assertThat(results.get(3).title).isEqualTo(TITLES[1]); // bravo
        assertThat(results.get(4).title).isEqualTo(TITLES[5]); // appCharlie
        assertThat(results.get(5).title).isEqualTo(TITLES[2]); // charlie
        assertThat(count).isEqualTo(6);
    }

    @Test
    public void testEndToEndSearch_addResults_resultsAddedInOrder() {
        List<AppSearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        // Add two individual items
        mAdapter.addSearchResults(appResults.subList(0,1),
                InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(dbResults.subList(0,1), DatabaseResultLoader.class.getName());
        mAdapter.displaySearchResults("");
        // Add super-set of items
        mAdapter.addSearchResults(appResults, InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(dbResults, DatabaseResultLoader.class.getName());
        int count = mAdapter.displaySearchResults("");

        List<SearchResult> results = mAdapter.getSearchResults();
        assertThat(results.get(0).title).isEqualTo(TITLES[0]); // alpha
        assertThat(results.get(1).title).isEqualTo(TITLES[3]); // appAlpha
        assertThat(results.get(2).title).isEqualTo(TITLES[4]); // appBravo
        assertThat(results.get(3).title).isEqualTo(TITLES[1]); // bravo
        assertThat(results.get(4).title).isEqualTo(TITLES[5]); // appCharlie
        assertThat(results.get(5).title).isEqualTo(TITLES[2]); // charlie
        assertThat(count).isEqualTo(6);
    }

    @Test
    public void testDisplayResults_ShouldNotRunSmartRankingIfDisabled() {
        when(mSearchFeatureProvider.isSmartSearchRankingEnabled(any()))
            .thenReturn(false);
        mAdapter.displaySearchResults("");
        verify(mSearchFeatureProvider, never()).rankSearchResults(anyString(), anyList());
    }

    @Test
    public void testDisplayResults_ShouldRunSmartRankingIfEnabled() {
        when(mSearchFeatureProvider.isSmartSearchRankingEnabled(any()))
                .thenReturn(true);
        mAdapter.displaySearchResults("");
        verify(mSearchFeatureProvider, times(1)).rankSearchResults(anyString(), anyList());
    }

    @Test
    public void testEndToEndSearch_removeResults_resultsAdded() {
        List<AppSearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        // Add list of items
        mAdapter.addSearchResults(appResults, InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(dbResults, DatabaseResultLoader.class.getName());
        mAdapter.displaySearchResults("");
        // Add subset of items
        mAdapter.addSearchResults(appResults.subList(0,1),
                InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(dbResults.subList(0,1), DatabaseResultLoader.class.getName());
        int count = mAdapter.displaySearchResults("");

        List<SearchResult> results = mAdapter.getSearchResults();
        assertThat(results.get(0).title).isEqualTo(TITLES[0]);
        assertThat(results.get(1).title).isEqualTo(TITLES[3]);
        assertThat(count).isEqualTo(2);
    }

    private List<SearchResult> getDummyDbResults() {
        List<SearchResult> results = new ArrayList<>();
        ResultPayload payload = new ResultPayload(new Intent());
        SearchResult.Builder builder = new SearchResult.Builder();
        builder.setPayload(payload)
                .setTitle(TITLES[0])
                .setRank(1)
                .setStableId(Objects.hash(TITLES[0], "db"));
        results.add(builder.build());

        builder.setTitle(TITLES[1])
                .setRank(3)
                .setStableId(Objects.hash(TITLES[1], "db"));
        results.add(builder.build());

        builder.setTitle(TITLES[2])
                .setRank(6)
                .setStableId(Objects.hash(TITLES[2], "db"));
        results.add(builder.build());

        return results;
    }

    private List<AppSearchResult> getDummyAppResults() {
        List<AppSearchResult> results = new ArrayList<>();
        ResultPayload payload = new ResultPayload(new Intent());
        AppSearchResult.Builder builder = new AppSearchResult.Builder();
        builder.setPayload(payload)
                .setTitle(TITLES[3])
                .setRank(1)
                .setStableId(Objects.hash(TITLES[3], "app"));
        results.add(builder.build());

        builder.setTitle(TITLES[4])
                .setRank(2)
                .setStableId(Objects.hash(TITLES[4], "app"));
        results.add(builder.build());

        builder.setTitle(TITLES[5])
                .setRank(4)
                .setStableId(Objects.hash(TITLES[5], "app"));
        results.add(builder.build());

        return results;
    }

    private ArrayList<SearchResult> getIntentSampleResults() {
        ArrayList<SearchResult> sampleResults = new ArrayList<>();
        ArrayList<String> breadcrumbs = new ArrayList<>();
        final Drawable icon = mContext.getDrawable(R.drawable.ic_search_history);
        final ResultPayload payload = new ResultPayload(null);
        final SearchResult.Builder builder = new Builder();
        builder.setTitle("title")
                .setSummary("summary")
                .setRank(1)
                .addBreadcrumbs(breadcrumbs)
                .setIcon(icon)
                .setPayload(payload)
                .setStableId(Objects.hash("title", "summary", 1));
        sampleResults.add(builder.build());

        builder.setRank(2)
                .setStableId(Objects.hash("title", "summary", 2));
        sampleResults.add(builder.build());

        builder.setRank(3)
                .setStableId(Objects.hash("title", "summary", 3));
        sampleResults.add(builder.build());
        return sampleResults;
    }
}