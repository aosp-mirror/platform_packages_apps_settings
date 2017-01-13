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

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search2.IntentPayload;
import com.android.settings.search2.IntentSearchViewHolder;
import com.android.settings.search2.SearchFragment;
import com.android.settings.search2.SearchResult;
import com.android.settings.search2.SearchResult.Builder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class IntentSearchViewHolderTest {

    private static final String TITLE = "title";
    private static final String SUMMARY = "summary";

    @Mock
    private SearchFragment mFragment;
    private IntentSearchViewHolder mHolder;
    private Drawable mIcon;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context context = ShadowApplication.getInstance().getApplicationContext();
        View view = LayoutInflater.from(context).inflate(R.layout.search_intent_item, null);
        mHolder = new IntentSearchViewHolder(view);

        mIcon = context.getDrawable(R.drawable.ic_search_history);
    }

    @Test
    public void testConstructor_MembersNotNull() {
        assertThat(mHolder.titleView).isNotNull();
        assertThat(mHolder.summaryView).isNotNull();
        assertThat(mHolder.iconView).isNotNull();
    }

    @Test
    public void testBindViewElements_AllUpdated() {
        SearchResult result = getSearchResult();
        mHolder.onBind(mFragment, result);
        mHolder.itemView.performClick();

        assertThat(mHolder.titleView.getText()).isEqualTo(TITLE);
        assertThat(mHolder.summaryView.getText()).isEqualTo(SUMMARY);
        assertThat(mHolder.iconView.getDrawable()).isEqualTo(mIcon);

        verify(mFragment).onSearchResultClicked();
        verify(mFragment).startActivity(any(Intent.class));
    }

    private SearchResult getSearchResult() {
        Builder builder = new Builder();
        builder.addTitle(TITLE)
                .addSummary(SUMMARY)
                .addRank(1)
                .addPayload(new IntentPayload(null))
                .addBreadcrumbs(new ArrayList<>())
                .addIcon(mIcon);

        return builder.build();
    }
}
