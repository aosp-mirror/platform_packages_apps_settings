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
import android.os.Bundle;

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
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
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
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        when(mFeatureFactory.searchFeatureProvider
                .getDatabaseSearchLoader(any(Context.class), anyString()))
                .thenReturn(mDatabaseResultLoader);
        when(mFeatureFactory.searchFeatureProvider
                .getInstalledAppSearchLoader(any(Context.class), anyString()))
                .thenReturn(mInstalledAppResultLoader);
    }

    @Test
    public void screenRotate_shouldPersistQuery() {
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
    public void queryTextChange_shouldTriggerLoader() {
        final String testQuery = "test";
        ActivityController<SearchActivity> activityController =
                Robolectric.buildActivity(SearchActivity.class);
        activityController.setup();
        SearchFragment fragment = (SearchFragment) activityController.get().getFragmentManager()
                .findFragmentById(R.id.main_content);

        fragment.onQueryTextChange(testQuery);

        verify(mFeatureFactory.searchFeatureProvider)
                .getDatabaseSearchLoader(any(Context.class), anyString());
        verify(mFeatureFactory.searchFeatureProvider)
                .getInstalledAppSearchLoader(any(Context.class), anyString());
    }
}
