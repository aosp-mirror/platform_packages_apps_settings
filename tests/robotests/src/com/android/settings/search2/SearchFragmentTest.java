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
 */

package com.android.settings.search2;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.view.View;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search.IndexingCallback;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SearchFragmentTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private DatabaseResultLoader mDatabaseResultLoader;
    @Mock
    private InstalledAppResultLoader mInstalledAppResultLoader;

    @Mock
    private SavedQueryLoader mSavedQueryLoader;
    @Mock
    private SavedQueryController mSavedQueryController;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
    }

    @Test
    public void screenRotate_shouldPersistQuery() {
        when(mFeatureFactory.searchFeatureProvider
                .getDatabaseSearchLoader(any(Context.class), anyString()))
                .thenReturn(mDatabaseResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getInstalledAppSearchLoader(any(Context.class), anyString()))
                .thenReturn(mInstalledAppResultLoader);
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
    public void queryTextChange_shouldTriggerLoader() {
        when(mFeatureFactory.searchFeatureProvider
                .getDatabaseSearchLoader(any(Context.class), anyString()))
                .thenReturn(mDatabaseResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getInstalledAppSearchLoader(any(Context.class), anyString()))
                .thenReturn(mInstalledAppResultLoader);
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
    }

    @Test
    public void queryTextChangeToEmpty_shouldLoadSavedQuery() {
        when(mFeatureFactory.searchFeatureProvider
                .getDatabaseSearchLoader(any(Context.class), anyString()))
                .thenReturn(mDatabaseResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getInstalledAppSearchLoader(any(Context.class), anyString()))
                .thenReturn(mInstalledAppResultLoader);
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
        fragment.mQuery = "123";

        fragment.onQueryTextChange("");

        verify(mFeatureFactory.searchFeatureProvider, never())
                .getDatabaseSearchLoader(any(Context.class), anyString());
        verify(mFeatureFactory.searchFeatureProvider, never())
                .getInstalledAppSearchLoader(any(Context.class), anyString());
        verify(mSavedQueryController).loadSavedQueries();
    }

    @Test
    public void updateIndex_TriggerOnCreate() {
        when(mFeatureFactory.searchFeatureProvider
                .getDatabaseSearchLoader(any(Context.class), anyString()))
                .thenReturn(mDatabaseResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getInstalledAppSearchLoader(any(Context.class), anyString()))
                .thenReturn(mInstalledAppResultLoader);
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
        verify(mFeatureFactory.searchFeatureProvider).updateIndex(any(Context.class),
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

        verify(fragment, times(2)).onLoadFinished(any(Loader.class), any(List.class));
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

        verify(loaderManager).initLoader(eq(SearchFragment.LOADER_ID_DATABASE),
                eq(null), any(LoaderManager.LoaderCallbacks.class));
        verify(loaderManager).initLoader(eq(SearchFragment.LOADER_ID_INSTALLED_APPS),
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
}
