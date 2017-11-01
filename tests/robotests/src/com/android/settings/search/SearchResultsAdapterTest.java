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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = Robolectric.buildActivity(Activity.class).get();
        when(mFragment.getContext()).thenReturn(mMockContext);
        when(mMockContext.getApplicationContext()).thenReturn(mContext);
        when(mSearchFeatureProvider.smartSearchRankingTimeoutMs(any(Context.class)))
                .thenReturn(300L);
        mAdapter = new SearchResultsAdapter(mFragment);
    }

    @Test
    public void testNoResultsAdded_emptyListReturned() {
        List<SearchResult> updatedResults = mAdapter.getSearchResults();
        assertThat(updatedResults).isEmpty();
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
    public void testPostSearchResults_addsDataAndDisplays() {
        List<SearchResult> results = getDummyDbResults();

        mAdapter.postSearchResults(results);

        assertThat(mAdapter.getSearchResults()).containsExactlyElementsIn(results);
        verify(mFragment).onSearchResultsDisplayed(anyInt());
    }

    private List<SearchResult> getDummyDbResults() {
        List<SearchResult> results = new ArrayList<>();
        ResultPayload payload = new ResultPayload(new Intent());
        SearchResult.Builder builder = new SearchResult.Builder();
        builder.setPayload(payload)
                .setTitle("one")
                .setRank(1)
                .setStableId(Objects.hash("one", "db"));
        results.add(builder.build());

        builder.setTitle("two")
                .setRank(3)
                .setStableId(Objects.hash("two", "db"));
        results.add(builder.build());

        builder.setTitle("three")
                .setRank(6)
                .setStableId(Objects.hash("three", "db"));
        results.add(builder.build());

        return results;
    }
}
