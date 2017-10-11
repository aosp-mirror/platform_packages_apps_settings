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
import android.graphics.drawable.Drawable;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search2.IntentPayload;
import com.android.settings.search2.ResultPayload;
import com.android.settings.search2.SearchResult;
import com.android.settings.search2.SearchResult.Builder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SearchResultBuilderTest {

    private Builder mBuilder;
    private String mTitle;
    private String mSummary;
    private ArrayList<String> mBreadcrumbs;
    private int mRank;
    private ResultPayload mResultPayload;
    private Drawable mIcon;

    @Before
    public void setUp() {
        mBuilder = new Builder();
        mTitle = "title";
        mSummary = "summary";
        mBreadcrumbs = new ArrayList<>();
        mRank = 3;
        mResultPayload = new IntentPayload(null);

        final Context context = ShadowApplication.getInstance().getApplicationContext();
        mIcon = context.getDrawable(R.drawable.ic_search_history);
    }

    @Test
    public void testAllInfo_BuildSearchResult() {
        mBuilder.addTitle(mTitle)
                .addSummary(mSummary)
                .addRank(mRank)
                .addBreadcrumbs(mBreadcrumbs)
                .addIcon(mIcon)
                .addPayload(mResultPayload);
        SearchResult result = mBuilder.build();

        assertThat(result).isNotNull();
        assertThat(result.title).isEqualTo(mTitle);
        assertThat(result.summary).isEqualTo(mSummary);
        assertThat(result.rank).isEqualTo(mRank);
        assertThat(result.breadcrumbs).isEqualTo(mBreadcrumbs);
        assertThat(result.icon).isEqualTo(mIcon);
        assertThat(result.payload).isEqualTo(mResultPayload);
    }

    @Test
    public void testNoTitle_BuildSearchResultException() {
        mBuilder.addSummary(mSummary)
                .addRank(mRank)
                .addBreadcrumbs(mBreadcrumbs)
                .addIcon(mIcon)
                .addPayload(mResultPayload);

        SearchResult result = null;
        try {
            result = mBuilder.build();
        } catch (IllegalArgumentException e) {
            // passes.
        }
        assertThat(result).isNull();
    }

    @Test
    public void testNoRank_BuildSearchResult_pass() {
        mBuilder.addTitle(mTitle)
                .addSummary(mSummary)
                .addBreadcrumbs(mBreadcrumbs)
                .addIcon(mIcon)
                .addPayload(mResultPayload);

        assertThat(mBuilder.build()).isNotNull();
    }

    @Test
    public void testNoIcon_BuildSearchResult_pass() {
        mBuilder.addTitle(mTitle)
                .addSummary(mSummary)
                .addRank(mRank)
                .addBreadcrumbs(mBreadcrumbs)
                .addPayload(mResultPayload);

        assertThat(mBuilder.build()).isNotNull();
    }

    @Test
    public void testNoPayload_BuildSearchResultException() {
        mBuilder.addTitle(mTitle)
                .addSummary(mSummary)
                .addRank(mRank)
                .addBreadcrumbs(mBreadcrumbs)
                .addIcon(mIcon);

        SearchResult result = null;
        try {
            result = mBuilder.build();
        } catch (IllegalArgumentException e) {
            // passes.
        }
        assertThat(result).isNull();
    }
}


