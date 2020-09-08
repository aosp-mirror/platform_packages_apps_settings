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

package com.android.settings.search;

import static android.provider.SearchIndexablesContract.COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.database.Cursor;
import android.text.TextUtils;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.FakeIndexProvider;
import com.android.settings.wifi.WifiSettings;
import com.android.settingslib.search.SearchIndexableData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SearchIndexableResourcesTest {

    private SearchFeatureProviderImpl mSearchProvider;
    private FakeFeatureFactory mFakeFeatureFactory;

    @Before
    public void setUp() {
        mSearchProvider = new SearchFeatureProviderImpl();
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mFakeFeatureFactory.searchFeatureProvider = mSearchProvider;
    }

    @After
    public void cleanUp() {
        mFakeFeatureFactory.searchFeatureProvider = mock(SearchFeatureProvider.class);
    }

    @Test
    public void testAddIndex() {
        // Confirms that String.class isn't contained in SearchIndexableResources.
        assertThat(mSearchProvider.getSearchIndexableResources().getProviderValues())
                .doesNotContain(String.class);
        final int beforeCount =
                mSearchProvider.getSearchIndexableResources().getProviderValues().size();
        final SearchIndexableData testBundle = new SearchIndexableData(null, null);
        mSearchProvider.getSearchIndexableResources().addIndex(testBundle);

        assertThat(mSearchProvider.getSearchIndexableResources().getProviderValues())
                .contains(testBundle);
        final int afterCount =
                mSearchProvider.getSearchIndexableResources().getProviderValues().size();
        assertThat(afterCount).isEqualTo(beforeCount + 1);
    }

    @Test
    public void testIndexHasWifiSettings() {
        boolean hasWifi = false;
        for (SearchIndexableData bundle :
                mSearchProvider.getSearchIndexableResources().getProviderValues()) {
            if (bundle.getTargetClass().getName().equals(WifiSettings.class.getName())) {
                hasWifi = true;
                break;
            }
        }
        assertThat(hasWifi).isTrue();
    }

    @Test
    public void testNonIndexableKeys_GetsKeyFromProvider() {
        mSearchProvider.getSearchIndexableResources().getProviderValues().clear();
        mSearchProvider.getSearchIndexableResources().addIndex(
                new SearchIndexableData(null, FakeIndexProvider.SEARCH_INDEX_DATA_PROVIDER));

        SettingsSearchIndexablesProvider provider = spy(new SettingsSearchIndexablesProvider());

        when(provider.getContext()).thenReturn(RuntimeEnvironment.application);

        Cursor cursor = provider.queryNonIndexableKeys(null);
        boolean hasTestKey = false;
        while (cursor.moveToNext()) {
            String key = cursor.getString(COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE);
            if (TextUtils.equals(key, FakeIndexProvider.KEY)) {
                hasTestKey = true;
                break;
            }
        }

        assertThat(hasTestKey).isTrue();
    }

    @Test
    public void testAllClassNamesHaveProviders() {
        for (SearchIndexableData data :
                mSearchProvider.getSearchIndexableResources().getProviderValues()) {
            if (DatabaseIndexingUtils.getSearchIndexProvider(data.getTargetClass()) == null) {
                fail(data.getTargetClass().getName() + "is not an index provider");
            }
        }
    }
}
