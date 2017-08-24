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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.search.SearchResult.Builder;
import com.android.settings.search.ranking.SearchResultsRankerCallback;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SearchResultsAdapterTest {

    @Mock
    private SearchFragment mFragment;
    @Mock
    private SearchFeatureProvider mSearchFeatureProvider;
    @Mock
    private Context mMockContext;
    @Captor
    private ArgumentCaptor<Integer> mSearchResultsCountCaptor =
            ArgumentCaptor.forClass(Integer.class);
    private SearchResultsAdapter mAdapter;
    private Context mContext;
    private String mLoaderClassName;

    private String[] TITLES = {"alpha", "bravo", "charlie", "appAlpha", "appBravo", "appCharlie"};

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = Robolectric.buildActivity(Activity.class).get();
        mLoaderClassName = DatabaseResultLoader.class.getName();
        when(mFragment.getContext()).thenReturn(mMockContext);
        when(mMockContext.getApplicationContext()).thenReturn(mContext);
        when(mSearchFeatureProvider.smartSearchRankingTimeoutMs(any(Context.class)))
                .thenReturn(300L);
        mAdapter = new SearchResultsAdapter(mFragment, mSearchFeatureProvider);
    }

    @Test
    public void testNoResultsAdded_emptyListReturned() {
        List<SearchResult> updatedResults = mAdapter.getSearchResults();
        assertThat(updatedResults).isEmpty();
    }

    @Test
    public void testSingleSourceMerge_exactCopyReturned() {
        Set<SearchResult> intentResults = getIntentSampleResults();
        mAdapter.initializeSearch("");
        mAdapter.addSearchResults(intentResults, mLoaderClassName);
        mAdapter.notifyResultsLoaded();

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
    public void testCreateViewHolder_returnsIntentSwitchResult() {
        // TODO (b/62807132) test for InlineResult
        ViewGroup group = new FrameLayout(mContext);
        SearchViewHolder view = mAdapter.onCreateViewHolder(group,
                ResultPayload.PayloadType.INLINE_SWITCH);
        assertThat(view).isInstanceOf(IntentSearchViewHolder.class);
    }

    @Test
    public void testEndToEndSearch_properResultsMerged_correctOrder() {
        mAdapter.initializeSearch("");
        mAdapter.addSearchResults(new HashSet<>(getDummyAppResults()),
                InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(new HashSet<>(getDummyDbResults()),
                DatabaseResultLoader.class.getName());
        mAdapter.notifyResultsLoaded();

        List<SearchResult> results = mAdapter.getSearchResults();
        assertThat(results.get(0).title).isEqualTo(TITLES[0]); // alpha
        assertThat(results.get(1).title).isEqualTo(TITLES[3]); // appAlpha
        assertThat(results.get(2).title).isEqualTo(TITLES[4]); // appBravo
        assertThat(results.get(3).title).isEqualTo(TITLES[1]); // bravo
        assertThat(results.get(4).title).isEqualTo(TITLES[5]); // appCharlie
        assertThat(results.get(5).title).isEqualTo(TITLES[2]); // charlie
        verify(mFragment).onSearchResultsDisplayed(mSearchResultsCountCaptor.capture());
        assertThat(mSearchResultsCountCaptor.getValue()).isEqualTo(6);
    }

    @Test
    public void testEndToEndSearch_addResults_resultsAddedInOrder() {
        List<SearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        mAdapter.initializeSearch("");
        // Add two individual items
        mAdapter.addSearchResults(new HashSet<>(appResults.subList(0, 1)),
                InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(new HashSet<>(dbResults.subList(0, 1)),
                DatabaseResultLoader.class.getName());
        mAdapter.notifyResultsLoaded();
        // Add super-set of items
        mAdapter.initializeSearch("");
        mAdapter.addSearchResults(
                new HashSet<>(appResults), InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(
                new HashSet<>(dbResults), DatabaseResultLoader.class.getName());
        mAdapter.notifyResultsLoaded();

        List<SearchResult> results = mAdapter.getSearchResults();
        assertThat(results.get(0).title).isEqualTo(TITLES[0]); // alpha
        assertThat(results.get(1).title).isEqualTo(TITLES[3]); // appAlpha
        assertThat(results.get(2).title).isEqualTo(TITLES[4]); // appBravo
        assertThat(results.get(3).title).isEqualTo(TITLES[1]); // bravo
        assertThat(results.get(4).title).isEqualTo(TITLES[5]); // appCharlie
        assertThat(results.get(5).title).isEqualTo(TITLES[2]); // charlie
        verify(mFragment, times(2)).onSearchResultsDisplayed(mSearchResultsCountCaptor.capture());
        assertThat(mSearchResultsCountCaptor.getAllValues().toArray())
                .isEqualTo(new Integer[] {2, 6});
    }

    @Test
    public void testEndToEndSearch_removeResults_resultsAdded() {
        List<SearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        // Add list of items
        mAdapter.initializeSearch("");
        mAdapter.addSearchResults(new HashSet<>(appResults),
                InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(new HashSet<>(dbResults),
                DatabaseResultLoader.class.getName());
        mAdapter.notifyResultsLoaded();
        // Add subset of items
        mAdapter.initializeSearch("");
        mAdapter.addSearchResults(new HashSet<>(appResults.subList(0, 1)),
                InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(new HashSet<>(dbResults.subList(0, 1)),
                DatabaseResultLoader.class.getName());
        mAdapter.notifyResultsLoaded();

        List<SearchResult> results = mAdapter.getSearchResults();
        assertThat(results.get(0).title).isEqualTo(TITLES[0]);
        assertThat(results.get(1).title).isEqualTo(TITLES[3]);
        verify(mFragment, times(2)).onSearchResultsDisplayed(mSearchResultsCountCaptor.capture());
        assertThat(mSearchResultsCountCaptor.getAllValues().toArray())
                .isEqualTo(new Integer[] {6, 2});
    }
    @Test
    public void testEndToEndSearch_smartSearchRankingEnabledAndSucceededAfterResultsLoaded() {
        when(mSearchFeatureProvider.isSmartSearchRankingEnabled(any())).thenReturn(true);

        List<SearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        mAdapter.initializeSearch("");
        mAdapter.addSearchResults(
                new HashSet<>(appResults), InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(
                new HashSet<>(dbResults), DatabaseResultLoader.class.getName());
        mAdapter.notifyResultsLoaded();
        mAdapter.onRankingScoresAvailable(getDummyRankingScores());

        List<SearchResult> results = mAdapter.getSearchResults();
        assertThat(results.get(0).title).isEqualTo(TITLES[2]); // charlie
        assertThat(results.get(1).title).isEqualTo(TITLES[0]); // alpha
        assertThat(results.get(2).title).isEqualTo(TITLES[1]); // bravo
        assertThat(results.get(3).title).isEqualTo(TITLES[3]); // appAlpha
        assertThat(results.get(4).title).isEqualTo(TITLES[4]); // appBravo
        assertThat(results.get(5).title).isEqualTo(TITLES[5]); // appCharlie
        verify(mFragment).onSearchResultsDisplayed(mSearchResultsCountCaptor.capture());
        assertThat(mSearchResultsCountCaptor.getValue()).isEqualTo(6);
        assertThat(mAdapter.getAsyncRankingState()).isEqualTo(SearchResultsAdapter.SUCCEEDED);
    }

    @Test
    public void testEndToEndSearch_smartSearchRankingEnabledAndSucceededBeforeResultsLoaded() {
        when(mSearchFeatureProvider.isSmartSearchRankingEnabled(any())).thenReturn(true);

        List<SearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        mAdapter.initializeSearch("");
        mAdapter.onRankingScoresAvailable(getDummyRankingScores());
        mAdapter.addSearchResults(
                new HashSet<>(appResults), InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(
                new HashSet<>(dbResults), DatabaseResultLoader.class.getName());
        mAdapter.notifyResultsLoaded();

        List<SearchResult> results = mAdapter.getSearchResults();
        assertThat(results.get(0).title).isEqualTo(TITLES[2]); // charlie
        assertThat(results.get(1).title).isEqualTo(TITLES[0]); // alpha
        assertThat(results.get(2).title).isEqualTo(TITLES[1]); // bravo
        assertThat(results.get(3).title).isEqualTo(TITLES[3]); // appAlpha
        assertThat(results.get(4).title).isEqualTo(TITLES[4]); // appBravo
        assertThat(results.get(5).title).isEqualTo(TITLES[5]); // appCharlie
        verify(mFragment).onSearchResultsDisplayed(mSearchResultsCountCaptor.capture());
        assertThat(mSearchResultsCountCaptor.getValue()).isEqualTo(6);
        assertThat(mAdapter.getAsyncRankingState()).isEqualTo(SearchResultsAdapter.SUCCEEDED);
    }

    @Test
    public void testEndToEndSearch_smartSearchRankingEnabledAndFailedAfterResultsLoaded() {
        when(mSearchFeatureProvider.isSmartSearchRankingEnabled(any())).thenReturn(true);

        List<SearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        mAdapter.initializeSearch("");
        mAdapter.addSearchResults(
                new HashSet<>(appResults), InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(
                new HashSet<>(dbResults), DatabaseResultLoader.class.getName());
        mAdapter.notifyResultsLoaded();
        mAdapter.onRankingFailed();

        List<SearchResult> results = mAdapter.getSearchResults();
        assertThat(results.get(0).title).isEqualTo(TITLES[0]); // alpha
        assertThat(results.get(1).title).isEqualTo(TITLES[3]); // appAlpha
        assertThat(results.get(2).title).isEqualTo(TITLES[4]); // appBravo
        assertThat(results.get(3).title).isEqualTo(TITLES[1]); // bravo
        assertThat(results.get(4).title).isEqualTo(TITLES[5]); // appCharlie
        assertThat(results.get(5).title).isEqualTo(TITLES[2]); // charlie
        verify(mFragment).onSearchResultsDisplayed(mSearchResultsCountCaptor.capture());
        assertThat(mSearchResultsCountCaptor.getValue()).isEqualTo(6);
        assertThat(mAdapter.getAsyncRankingState()).isEqualTo(SearchResultsAdapter.FAILED);
    }

    @Test
    public void testEndToEndSearch_smartSearchRankingEnabledAndFailedBeforeResultsLoaded() {
        when(mSearchFeatureProvider.isSmartSearchRankingEnabled(any())).thenReturn(true);

        List<SearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        mAdapter.initializeSearch("");
        mAdapter.onRankingFailed();
        mAdapter.addSearchResults(
                new HashSet<>(appResults), InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(
                new HashSet<>(dbResults), DatabaseResultLoader.class.getName());
        mAdapter.notifyResultsLoaded();

        List<SearchResult> results = mAdapter.getSearchResults();
        assertThat(results.get(0).title).isEqualTo(TITLES[0]); // alpha
        assertThat(results.get(1).title).isEqualTo(TITLES[3]); // appAlpha
        assertThat(results.get(2).title).isEqualTo(TITLES[4]); // appBravo
        assertThat(results.get(3).title).isEqualTo(TITLES[1]); // bravo
        assertThat(results.get(4).title).isEqualTo(TITLES[5]); // appCharlie
        assertThat(results.get(5).title).isEqualTo(TITLES[2]); // charlie
        verify(mFragment).onSearchResultsDisplayed(mSearchResultsCountCaptor.capture());
        assertThat(mSearchResultsCountCaptor.getValue()).isEqualTo(6);
        assertThat(mAdapter.getAsyncRankingState()).isEqualTo(SearchResultsAdapter.FAILED);
    }

    @Test
    public void testEndToEndSearch_smartSearchRankingEnabledAndTimedoutAfterResultsLoaded() {
        when(mSearchFeatureProvider.isSmartSearchRankingEnabled(any())).thenReturn(true);

        List<SearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        mAdapter.initializeSearch("");
        mAdapter.addSearchResults(
                new HashSet<>(appResults), InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(
                new HashSet<>(dbResults), DatabaseResultLoader.class.getName());
        mAdapter.notifyResultsLoaded();

        waitUntilRankingTimesOut();

        List<SearchResult> results = mAdapter.getSearchResults();
        assertThat(results.get(0).title).isEqualTo(TITLES[0]); // alpha
        assertThat(results.get(1).title).isEqualTo(TITLES[3]); // appAlpha
        assertThat(results.get(2).title).isEqualTo(TITLES[4]); // appBravo
        assertThat(results.get(3).title).isEqualTo(TITLES[1]); // bravo
        assertThat(results.get(4).title).isEqualTo(TITLES[5]); // appCharlie
        assertThat(results.get(5).title).isEqualTo(TITLES[2]); // charlie
        verify(mFragment).onSearchResultsDisplayed(mSearchResultsCountCaptor.capture());
        assertThat(mSearchResultsCountCaptor.getValue()).isEqualTo(6);
        assertThat(mAdapter.getAsyncRankingState()).isEqualTo(SearchResultsAdapter.TIMED_OUT);
    }

    @Test
    public void testEndToEndSearch_smartSearchRankingEnabledAndTimedoutBeforeResultsLoaded() {
        when(mSearchFeatureProvider.isSmartSearchRankingEnabled(any())).thenReturn(true);

        List<SearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        mAdapter.initializeSearch("");

        waitUntilRankingTimesOut();

        mAdapter.addSearchResults(
                new HashSet<>(appResults), InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(
                new HashSet<>(dbResults), DatabaseResultLoader.class.getName());
        mAdapter.notifyResultsLoaded();

        List<SearchResult> results = mAdapter.getSearchResults();
        assertThat(results.get(0).title).isEqualTo(TITLES[0]); // alpha
        assertThat(results.get(1).title).isEqualTo(TITLES[3]); // appAlpha
        assertThat(results.get(2).title).isEqualTo(TITLES[4]); // appBravo
        assertThat(results.get(3).title).isEqualTo(TITLES[1]); // bravo
        assertThat(results.get(4).title).isEqualTo(TITLES[5]); // appCharlie
        assertThat(results.get(5).title).isEqualTo(TITLES[2]); // charlie
        verify(mFragment).onSearchResultsDisplayed(mSearchResultsCountCaptor.capture());
        assertThat(mSearchResultsCountCaptor.getValue()).isEqualTo(6);
        assertThat(mAdapter.getAsyncRankingState()).isEqualTo(SearchResultsAdapter.TIMED_OUT);
    }

    @Test
    public void testDoSmartRanking_shouldRankAppResultsAfterDbResults() {
        when(mSearchFeatureProvider.isSmartSearchRankingEnabled(any())).thenReturn(true);

        List<SearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        mAdapter.initializeSearch("");
        mAdapter.addSearchResults(
                new HashSet<>(appResults), InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(
                new HashSet<>(dbResults), DatabaseResultLoader.class.getName());
        mAdapter.notifyResultsLoaded();
        mAdapter.onRankingScoresAvailable(getDummyRankingScores());
        List<SearchResult> results = mAdapter.doAsyncRanking();
        assertThat(results.get(0).title).isEqualTo(TITLES[2]); // charlie
        assertThat(results.get(1).title).isEqualTo(TITLES[0]); // alpha
        assertThat(results.get(2).title).isEqualTo(TITLES[1]); // bravo
        assertThat(results.get(3).title).isEqualTo(TITLES[3]); // appAlpha
        assertThat(results.get(4).title).isEqualTo(TITLES[4]); // appBravo
        assertThat(results.get(5).title).isEqualTo(TITLES[5]); // appCharlie
    }

    @Test
    public void testDoSmartRanking_shouldRankResultsWithMissingScoresAfterScoredResults() {
        when(mSearchFeatureProvider.isSmartSearchRankingEnabled(any())).thenReturn(true);

        List<SearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        mAdapter.initializeSearch("");
        mAdapter.addSearchResults(
                new HashSet<>(appResults), InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(
                new HashSet<>(dbResults), DatabaseResultLoader.class.getName());
        mAdapter.notifyResultsLoaded();
        List<Pair<String, Float>> rankingScores = getDummyRankingScores();
        rankingScores.remove(1); // no ranking score for alpha
        mAdapter.onRankingScoresAvailable(rankingScores);
        List<SearchResult> results = mAdapter.doAsyncRanking();
        assertThat(results.get(0).title).isEqualTo(TITLES[2]); // charlie
        assertThat(results.get(1).title).isEqualTo(TITLES[1]); // bravo
        assertThat(results.get(2).title).isEqualTo(TITLES[0]); // alpha
        assertThat(results.get(3).title).isEqualTo(TITLES[3]); // appAlpha
        assertThat(results.get(4).title).isEqualTo(TITLES[4]); // appBravo
        assertThat(results.get(5).title).isEqualTo(TITLES[5]); // appCharlie
    }

    @Test
    public void testGetUnsortedLoadedResults () {
        List<SearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        mAdapter.initializeSearch("");
        mAdapter.addSearchResults(
                new HashSet<>(appResults), InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(
                new HashSet<>(dbResults), DatabaseResultLoader.class.getName());
        Set<CharSequence> expectedDbTitles = new HashSet<>(
                Arrays.asList("alpha", "bravo", "charlie"));
        Set<CharSequence> expectedAppTitles = new HashSet<>(
                Arrays.asList("appAlpha", "appBravo", "appCharlie"));
        Set<CharSequence> actualDbTitles = new HashSet<>();
        Set<CharSequence> actualAppTitles = new HashSet<>();
        for (SearchResult result : mAdapter.getUnsortedLoadedResults(SearchResultsAdapter
                .DB_RESULTS_LOADER_KEY)) {
            actualDbTitles.add(result.title);
        }
        for (SearchResult result : mAdapter.getUnsortedLoadedResults(SearchResultsAdapter
                .APP_RESULTS_LOADER_KEY)) {
            actualAppTitles.add(result.title);
        }
        assertThat(actualDbTitles).isEqualTo(expectedDbTitles);
        assertThat(actualAppTitles).isEqualTo(expectedAppTitles);
    }

    @Test
    public void testGetSortedLoadedResults() {
        List<SearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        mAdapter.initializeSearch("");
        mAdapter.addSearchResults(
                new HashSet<>(appResults), InstalledAppResultLoader.class.getName());
        mAdapter.addSearchResults(
                new HashSet<>(dbResults), DatabaseResultLoader.class.getName());
        List<? extends SearchResult> actualDbResults =
                mAdapter.getSortedLoadedResults(SearchResultsAdapter.DB_RESULTS_LOADER_KEY);
        List<? extends SearchResult> actualAppResults =
                mAdapter.getSortedLoadedResults(SearchResultsAdapter.APP_RESULTS_LOADER_KEY);
        assertThat(actualDbResults.get(0).title).isEqualTo(TITLES[0]); // charlie
        assertThat(actualDbResults.get(1).title).isEqualTo(TITLES[1]); // bravo
        assertThat(actualDbResults.get(2).title).isEqualTo(TITLES[2]); // alpha
        assertThat(actualAppResults.get(0).title).isEqualTo(TITLES[3]); // appAlpha
        assertThat(actualAppResults.get(1).title).isEqualTo(TITLES[4]); // appBravo
        assertThat(actualAppResults.get(2).title).isEqualTo(TITLES[5]); // appCharlie
    }

    @Test
    public void testInitializeSearch_shouldNotRunSmartRankingIfDisabled() {
        when(mSearchFeatureProvider.isSmartSearchRankingEnabled(any())).thenReturn(false);
        mAdapter.initializeSearch("");
        mAdapter.notifyResultsLoaded();
        verify(mSearchFeatureProvider, never()).querySearchResults(
                any(Context.class), anyString(), any(SearchResultsRankerCallback.class));
        assertThat(mAdapter.getAsyncRankingState()).isEqualTo(SearchResultsAdapter.DISABLED);
    }

    @Test
    public void testInitialSearch_shouldRunSmartRankingIfEnabled() {
        when(mSearchFeatureProvider.isSmartSearchRankingEnabled(any())).thenReturn(true);
        mAdapter.initializeSearch("");
        mAdapter.notifyResultsLoaded();
        verify(mSearchFeatureProvider, times(1)).querySearchResults(
                any(Context.class), anyString(), any(SearchResultsRankerCallback.class));
        assertThat(mAdapter.getAsyncRankingState())
                .isEqualTo(SearchResultsAdapter.PENDING_RESULTS);
    }

    @Test
    public void testGetRankingScoreByStableId() {
        when(mSearchFeatureProvider.isSmartSearchRankingEnabled(any())).thenReturn(true);

        List<SearchResult> appResults = getDummyAppResults();
        List<SearchResult> dbResults = getDummyDbResults();
        mAdapter.initializeSearch("");
        mAdapter.onRankingScoresAvailable(getDummyRankingScores());
        assertThat(mAdapter.getRankingScoreByStableId(dbResults.get(0).stableId))
                .isWithin(1e-10f).of(0.8f);
        assertThat(mAdapter.getRankingScoreByStableId(dbResults.get(1).stableId))
                .isWithin(1e-10f).of(0.2f);
        assertThat(mAdapter.getRankingScoreByStableId(dbResults.get(2).stableId))
                .isWithin(1e-10f).of(0.9f);
        assertThat(mAdapter.getRankingScoreByStableId(appResults.get(0).stableId))
                .isEqualTo(-Float.MAX_VALUE);
        assertThat(mAdapter.getRankingScoreByStableId(appResults.get(1).stableId))
                .isEqualTo(-Float.MAX_VALUE);
        assertThat(mAdapter.getRankingScoreByStableId(appResults.get(2).stableId))
                .isEqualTo(-Float.MAX_VALUE);
    }

    private void waitUntilRankingTimesOut() {
        while (mAdapter.getHandler().hasMessages(mAdapter.MSG_RANKING_TIMED_OUT)) {
            try {
                ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
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

    private List<SearchResult> getDummyAppResults() {
        List<SearchResult> results = new ArrayList<>();
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

    private Set<SearchResult> getIntentSampleResults() {
        Set<SearchResult> sampleResults = new HashSet<>();
        ArrayList<String> breadcrumbs = new ArrayList<>();
        final Drawable icon = mContext.getDrawable(R.drawable.ic_search_24dp);
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

    private List<Pair<String, Float>> getDummyRankingScores() {
        List<SearchResult> results = getDummyDbResults();
        List<Pair<String, Float>> scores = new ArrayList<>();
        scores.add(new Pair<>(Long.toString(results.get(2).stableId), 0.9f)); // charlie
        scores.add(new Pair<>(Long.toString(results.get(0).stableId), 0.8f)); // alpha
        scores.add(new Pair<>(Long.toString(results.get(1).stableId), 0.2f)); // bravo
        return scores;
    }
}
