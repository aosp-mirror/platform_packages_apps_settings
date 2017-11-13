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

package com.android.settings.search;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;
import org.robolectric.util.ReflectionHelpers;

import java.util.Set;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class,
        })
public class SearchFragmentTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private DatabaseResultLoader mDatabaseResultLoader;
    @Mock
    private InstalledAppResultLoader mInstalledAppResultLoader;
    @Mock
    private AccessibilityServiceResultLoader mAccessibilityServiceResultLoader;
    @Mock
    private InputDeviceResultLoader mInputDeviceResultLoader;

    @Mock
    private SavedQueryLoader mSavedQueryLoader;
    @Mock
    private SavedQueryController mSavedQueryController;
    @Mock
    private SearchResultsAdapter mSearchResultsAdapter;
    @Captor
    private ArgumentCaptor<String> mQueryCaptor = ArgumentCaptor.forClass(String.class);

    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFeatureFactory = FakeFeatureFactory.setupForTest(mContext);
    }

    @After
    public void tearDown() {
        DatabaseTestUtils.clearDb(RuntimeEnvironment.application);
    }

    @Test
    public void screenRotate_shouldPersistQuery() {
        when(mFeatureFactory.searchFeatureProvider
                .getDatabaseSearchLoader(any(Context.class), anyString()))
                .thenReturn(mDatabaseResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getInstalledAppSearchLoader(any(Context.class), anyString()))
                .thenReturn(mInstalledAppResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getAccessibilityServiceResultLoader(any(Context.class), anyString()))
                .thenReturn(mAccessibilityServiceResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getInputDeviceResultLoader(any(Context.class), anyString()))
                .thenReturn(mInputDeviceResultLoader);
        when(mFeatureFactory.searchFeatureProvider.getSavedQueryLoader(any(Context.class)))
                .thenReturn(mSavedQueryLoader);

        final Bundle bundle = new Bundle();
        final String testQuery = "test";
        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = (SearchFragment) activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content);

        ReflectionHelpers.setField(fragment, "mShowingSavedQuery", false);
        fragment.mQuery = testQuery;

        activityController.saveInstanceState(bundle).pause().stop().destroy();

        activityController = Robolectric.buildActivity(SearchActivity.class);
        activityController.setup(bundle);

        assertThat(fragment.mQuery).isEqualTo(testQuery);
    }

    @Test
    public void screenRotateEmptyString_ShouldNotCrash() {
        when(mFeatureFactory.searchFeatureProvider.getSavedQueryLoader(any(Context.class)))
                .thenReturn(mSavedQueryLoader);

        final Bundle bundle = new Bundle();
        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = (SearchFragment) activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content);
        when(mFeatureFactory.searchFeatureProvider.isIndexingComplete(any(Context.class)))
                .thenReturn(true);

        fragment.mQuery = "";

        activityController.saveInstanceState(bundle).pause().stop().destroy();

        activityController = Robolectric.buildActivity(SearchActivity.class);
        activityController.setup(bundle);

        verify(mFeatureFactory.searchFeatureProvider, never())
                .getDatabaseSearchLoader(any(Context.class), anyString());
        verify(mFeatureFactory.searchFeatureProvider, never())
                .getInstalledAppSearchLoader(any(Context.class), anyString());
    }

    @Test
    public void queryTextChange_shouldTriggerLoaderAndInitializeSearch() {
        when(mFeatureFactory.searchFeatureProvider
                .getDatabaseSearchLoader(any(Context.class), anyString()))
                .thenReturn(mDatabaseResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getInstalledAppSearchLoader(any(Context.class), anyString()))
                .thenReturn(mInstalledAppResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getAccessibilityServiceResultLoader(any(Context.class), anyString()))
                .thenReturn(mAccessibilityServiceResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getInputDeviceResultLoader(any(Context.class), anyString()))
                .thenReturn(mInputDeviceResultLoader);
        when(mFeatureFactory.searchFeatureProvider.getSavedQueryLoader(any(Context.class)))
                .thenReturn(mSavedQueryLoader);

        final String testQuery = "test";
        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = (SearchFragment) activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content);
        when(mFeatureFactory.searchFeatureProvider.isIndexingComplete(any(Context.class)))
                .thenReturn(true);

        ReflectionHelpers.setField(fragment, "mSearchAdapter", mSearchResultsAdapter);
        fragment.onQueryTextChange(testQuery);
        activityController.get().onBackPressed();

        activityController.pause().stop().destroy();

        verify(mFeatureFactory.metricsFeatureProvider, never()).action(
                any(Context.class),
                eq(MetricsProto.MetricsEvent.ACTION_LEAVE_SEARCH_RESULT_WITHOUT_QUERY));
        verify(mFeatureFactory.metricsFeatureProvider).histogram(
                any(Context.class), eq(SearchFragment.RESULT_CLICK_COUNT), eq(0));
        verify(mFeatureFactory.searchFeatureProvider)
                .getDatabaseSearchLoader(any(Context.class), anyString());
        verify(mFeatureFactory.searchFeatureProvider)
                .getInstalledAppSearchLoader(any(Context.class), anyString());
        verify(mSearchResultsAdapter).initializeSearch(mQueryCaptor.capture());
        assertThat(mQueryCaptor.getValue()).isEqualTo(testQuery);
    }

    @Test
    public void onSearchResultsDisplayed_noResult_shouldShowNoResultView() {
        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = spy((SearchFragment) activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content));
        fragment.onSearchResultsDisplayed(0 /* count */);

        assertThat(fragment.mNoResultsView.getVisibility()).isEqualTo(View.VISIBLE);
        verify(mFeatureFactory.metricsFeatureProvider).visible(
                any(Context.class),
                anyInt(),
                eq(MetricsProto.MetricsEvent.SETTINGS_SEARCH_NO_RESULT));
    }

    @Test
    public void queryTextChangeToEmpty_shouldLoadSavedQueryAndNotInitializeSearch() {
        when(mFeatureFactory.searchFeatureProvider
                .getDatabaseSearchLoader(any(Context.class), anyString()))
                .thenReturn(mDatabaseResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getInstalledAppSearchLoader(any(Context.class), anyString()))
                .thenReturn(mInstalledAppResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getAccessibilityServiceResultLoader(any(Context.class), anyString()))
                .thenReturn(mAccessibilityServiceResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getInputDeviceResultLoader(any(Context.class), anyString()))
                .thenReturn(mInputDeviceResultLoader);
        when(mFeatureFactory.searchFeatureProvider.getSavedQueryLoader(any(Context.class)))
                .thenReturn(mSavedQueryLoader);
        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = spy((SearchFragment) activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content));
        when(mFeatureFactory.searchFeatureProvider.isIndexingComplete(any(Context.class)))
                .thenReturn(true);
        ReflectionHelpers.setField(fragment, "mSavedQueryController", mSavedQueryController);
        ReflectionHelpers.setField(fragment, "mSearchAdapter", mSearchResultsAdapter);
        fragment.mQuery = "123";

        fragment.onQueryTextChange("");

        verify(mFeatureFactory.searchFeatureProvider, never())
                .getDatabaseSearchLoader(any(Context.class), anyString());
        verify(mFeatureFactory.searchFeatureProvider, never())
                .getInstalledAppSearchLoader(any(Context.class), anyString());
        verify(mSavedQueryController).loadSavedQueries();
        verify(mSearchResultsAdapter, never()).initializeSearch(anyString());
    }

    @Test
    public void updateIndex_TriggerOnCreate() {
        when(mFeatureFactory.searchFeatureProvider
                .getDatabaseSearchLoader(any(Context.class), anyString()))
                .thenReturn(mDatabaseResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getInstalledAppSearchLoader(any(Context.class), anyString()))
                .thenReturn(mInstalledAppResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getAccessibilityServiceResultLoader(any(Context.class), anyString()))
                .thenReturn(mAccessibilityServiceResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getInputDeviceResultLoader(any(Context.class), anyString()))
                .thenReturn(mInputDeviceResultLoader);
        when(mFeatureFactory.searchFeatureProvider.getSavedQueryLoader(any(Context.class)))
                .thenReturn(mSavedQueryLoader);

        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = (SearchFragment) activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content);
        when(mFeatureFactory.searchFeatureProvider.isIndexingComplete(any(Context.class)))
                .thenReturn(true);

        fragment.onAttach(null);
        verify(mFeatureFactory.searchFeatureProvider).updateIndexAsync(any(Context.class),
                any(IndexingCallback.class));
    }

    @Test
    public void syncLoaders_MergeWhenAllLoadersDone() {
        when(mFeatureFactory.searchFeatureProvider
                .getDatabaseSearchLoader(any(Context.class), anyString()))
                .thenReturn(new MockDBLoader(RuntimeEnvironment.application));
        when(mFeatureFactory.searchFeatureProvider
                .getInstalledAppSearchLoader(any(Context.class), anyString()))
                .thenReturn(new MockAppLoader(RuntimeEnvironment.application));
        when(mFeatureFactory.searchFeatureProvider.getSavedQueryLoader(any(Context.class)))
                .thenReturn(mSavedQueryLoader);

        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();

        SearchFragment fragment = (SearchFragment) spy(activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content));
        when(mFeatureFactory.searchFeatureProvider.isIndexingComplete(any(Context.class)))
                .thenReturn(true);

        fragment.onQueryTextChange("non-empty");

        Robolectric.flushForegroundThreadScheduler();

        verify(fragment, times(2)).onLoadFinished(any(Loader.class), any(Set.class));
    }

    @Test
    public void whenNoQuery_HideFeedbackIsCalled() {
        when(mFeatureFactory.searchFeatureProvider
                .getDatabaseSearchLoader(any(Context.class), anyString()))
                .thenReturn(new MockDBLoader(RuntimeEnvironment.application));
        when(mFeatureFactory.searchFeatureProvider
                .getInstalledAppSearchLoader(any(Context.class), anyString()))
                .thenReturn(new MockAppLoader(RuntimeEnvironment.application));
        when(mFeatureFactory.searchFeatureProvider.getSavedQueryLoader(any(Context.class)))
                .thenReturn(mSavedQueryLoader);

        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = (SearchFragment) spy(activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content));
        when(mFeatureFactory.searchFeatureProvider.isIndexingComplete(any(Context.class)))
                .thenReturn(true);
        when(fragment.getLoaderManager()).thenReturn(mock(LoaderManager.class));

        fragment.onQueryTextChange("");
        Robolectric.flushForegroundThreadScheduler();

        verify(mFeatureFactory.searchFeatureProvider).hideFeedbackButton();
    }

    @Test
    public void onLoadFinished_ShowsFeedback() {
        when(mFeatureFactory.searchFeatureProvider
                .getDatabaseSearchLoader(any(Context.class), anyString()))
                .thenReturn(new MockDBLoader(RuntimeEnvironment.application));
        when(mFeatureFactory.searchFeatureProvider
                .getInstalledAppSearchLoader(any(Context.class), anyString()))
                .thenReturn(new MockAppLoader(RuntimeEnvironment.application));
        when(mFeatureFactory.searchFeatureProvider
                .getAccessibilityServiceResultLoader(any(Context.class), anyString()))
                .thenReturn(new MockAccessibilityLoader(RuntimeEnvironment.application));
        when(mFeatureFactory.searchFeatureProvider
                .getInputDeviceResultLoader(any(Context.class), anyString()))
                .thenReturn(new MockInputDeviceResultLoader(RuntimeEnvironment.application));
        when(mFeatureFactory.searchFeatureProvider.getSavedQueryLoader(any(Context.class)))
                .thenReturn(mSavedQueryLoader);
        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = (SearchFragment) activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content);
        when(mFeatureFactory.searchFeatureProvider.isIndexingComplete(any(Context.class)))
                .thenReturn(true);

        fragment.onQueryTextChange("non-empty");
        Robolectric.flushForegroundThreadScheduler();

        verify(mFeatureFactory.searchFeatureProvider).showFeedbackButton(any(SearchFragment.class),
                any(View.class));
    }

    @Test
    public void preIndexingFinished_isIndexingFinishedFlag_isFalse() {
        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = (SearchFragment) activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content);

        when(mFeatureFactory.searchFeatureProvider.isIndexingComplete(any(Context.class)))
                .thenReturn(false);
    }

    @Test
    public void onIndexingFinished_notShowingSavedQuery_initLoaders() {
        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = (SearchFragment) spy(activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content));
        final LoaderManager loaderManager = mock(LoaderManager.class);
        when(fragment.getLoaderManager()).thenReturn(loaderManager);
        fragment.mShowingSavedQuery = false;
        fragment.mQuery = null;

        fragment.onIndexingFinished();

        verify(loaderManager).initLoader(eq(SearchFragment.SearchLoaderId.DATABASE),
                eq(null), any(LoaderManager.LoaderCallbacks.class));
        verify(loaderManager).initLoader(eq(SearchFragment.SearchLoaderId.INSTALLED_APPS),
                eq(null), any(LoaderManager.LoaderCallbacks.class));
    }

    @Test
    public void onIndexingFinished_showingSavedQuery_loadsSavedQueries() {
        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = (SearchFragment) spy(activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content));
        fragment.mShowingSavedQuery = true;
        ReflectionHelpers.setField(fragment, "mSavedQueryController", mSavedQueryController);

        fragment.onIndexingFinished();

        verify(fragment.mSavedQueryController).loadSavedQueries();
    }

    @Test
    public void onIndexingFinished_noActivity_shouldNotCrash() {
        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = (SearchFragment) spy(activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content));
        when(mFeatureFactory.searchFeatureProvider.isIndexingComplete(any(Context.class)))
                .thenReturn(true);
        fragment.mQuery = "bright";
        ReflectionHelpers.setField(fragment, "mLoaderManager", null);
        ReflectionHelpers.setField(fragment, "mHost", null);

        fragment.onIndexingFinished();
        // no crash
    }

    @Test
    public void onSearchResultClicked_shouldLogResultMeta() {
        SearchFragment fragment = new SearchFragment();
        ReflectionHelpers.setField(fragment, "mMetricsFeatureProvider",
                mFeatureFactory.metricsFeatureProvider);
        ReflectionHelpers.setField(fragment, "mSearchFeatureProvider",
                mFeatureFactory.searchFeatureProvider);
        ReflectionHelpers.setField(fragment, "mSearchAdapter", mock(SearchResultsAdapter.class));
        fragment.mSavedQueryController = mock(SavedQueryController.class);

        // Should log result name, result count, clicked rank, etc.
        final SearchViewHolder resultViewHolder = mock(SearchViewHolder.class);
        when(resultViewHolder.getClickActionMetricName())
                .thenReturn(MetricsProto.MetricsEvent.ACTION_CLICK_SETTINGS_SEARCH_RESULT);
        ResultPayload payLoad = new ResultPayload(
                (new Intent()).putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, "test_setting"));
        SearchResult searchResult = new SearchResult.Builder()
                .setStableId(payLoad.hashCode())
                .setPayload(payLoad)
                .setTitle("setting_title")
                .build();
        fragment.onSearchResultClicked(resultViewHolder, searchResult);

        verify(mFeatureFactory.metricsFeatureProvider).action(
                nullable(Context.class),
                eq(MetricsProto.MetricsEvent.ACTION_CLICK_SETTINGS_SEARCH_RESULT),
                eq("test_setting"),
                argThat(pairMatches(MetricsProto.MetricsEvent.FIELD_SETTINGS_SEARCH_RESULT_COUNT)),
                argThat(pairMatches(MetricsProto.MetricsEvent.FIELD_SETTINGS_SEARCH_RESULT_RANK)),
                argThat(pairMatches(MetricsProto.MetricsEvent
                                .FIELD_SETTINGS_SEARCH_RESULT_ASYNC_RANKING_STATE)),
                argThat(pairMatches(MetricsProto.MetricsEvent.FIELD_SETTINGS_SEARCH_QUERY_LENGTH)));

        verify(mFeatureFactory.searchFeatureProvider).searchResultClicked(nullable(Context.class),
                nullable(String.class), eq(searchResult));
    }

    @Test
    public void onResume_shouldCallSearchRankingWarmupIfSmartSearchRankingEnabled(){
        when(mFeatureFactory.searchFeatureProvider.isSmartSearchRankingEnabled(any(Context.class)))
                .thenReturn(true);

        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = (SearchFragment) activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content);

        verify(mFeatureFactory.searchFeatureProvider)
                .searchRankingWarmup(any(Context.class));
    }

    @Test
    public void onResume_shouldNotCallSearchRankingWarmupIfSmartSearchRankingDisabled(){
        when(mFeatureFactory.searchFeatureProvider.isSmartSearchRankingEnabled(any(Context.class)))
                .thenReturn(false);

        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = (SearchFragment) activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content);

        verify(mFeatureFactory.searchFeatureProvider, never())
                .searchRankingWarmup(any(Context.class));
    }

    private ArgumentMatcher<Pair<Integer, Object>> pairMatches(int tag) {
        return pair -> pair.first == tag;
    }
}
