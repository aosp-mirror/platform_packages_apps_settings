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

import android.content.Context;
import android.content.Loader;
import android.os.Bundle;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
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

import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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

        fragment.mQuery = testQuery;

        activityController.saveInstanceState(bundle).pause().stop().destroy();

        activityController = Robolectric.buildActivity(SearchActivity.class);
        activityController.setup(bundle);

        verify(mFeatureFactory.searchFeatureProvider)
                .getDatabaseSearchLoader(any(Context.class), anyString());
        verify(mFeatureFactory.searchFeatureProvider)
                .getInstalledAppSearchLoader(any(Context.class), anyString());
    }

    @Test
    public void screenRotateEmptyString_ShouldNotCrash() {
        when(mFeatureFactory.searchFeatureProvider
                .getDatabaseSearchLoader(any(Context.class), anyString()))
                .thenReturn(mDatabaseResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getInstalledAppSearchLoader(any(Context.class), anyString()))
                .thenReturn(mInstalledAppResultLoader);
        when(mFeatureFactory.searchFeatureProvider.getSavedQueryLoader(any(Context.class)))
                .thenReturn(mSavedQueryLoader);

        final Bundle bundle = new Bundle();
        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = (SearchFragment) activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content);

        fragment.mQuery = "";

        activityController.saveInstanceState(bundle).pause().stop().destroy();

        activityController = Robolectric.buildActivity(SearchActivity.class);
        activityController.setup(bundle);

        verify(mFeatureFactory.searchFeatureProvider)
                .getDatabaseSearchLoader(any(Context.class), anyString());
        verify(mFeatureFactory.searchFeatureProvider)
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
    public void queryTextChangeToEmpty_shouldTriggerSavedQueryLoader() {
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

        fragment.onQueryTextChange("");
        activityController.get().onBackPressed();
        activityController.pause().stop().destroy();

        verify(mFeatureFactory.searchFeatureProvider, never())
                .getDatabaseSearchLoader(any(Context.class), anyString());
        verify(mFeatureFactory.searchFeatureProvider, never())
                .getInstalledAppSearchLoader(any(Context.class), anyString());
        // Saved query loaded 2 times: fragment start, and query change to empty.
        verify(mFeatureFactory.searchFeatureProvider, times(2))
                .getSavedQueryLoader(any(Context.class));
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

        fragment.onAttach(null);
        verify(mFeatureFactory.searchFeatureProvider).updateIndex(any(Context.class));
    }

    @Test
    public void syncLoaders_MergeWhenAllLoadersDone() {

        when(mFeatureFactory.searchFeatureProvider
                .getDatabaseSearchLoader(any(Context.class), anyString()))
                .thenReturn(new MockDBLoader(RuntimeEnvironment.application));
        when(mFeatureFactory.searchFeatureProvider
                .getInstalledAppSearchLoader(any(Context.class), anyString()))
                .thenReturn(new MockAppLoader(RuntimeEnvironment.application));

        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = (SearchFragment) spy(activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content));

        fragment.onQueryTextChange("non-empty");

        Robolectric.flushForegroundThreadScheduler();

        verify(fragment, times(2)).onLoadFinished(any(Loader.class), any(List.class));
    }
}
