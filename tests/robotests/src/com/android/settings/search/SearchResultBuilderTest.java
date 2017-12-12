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

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.search.SearchResult.Builder;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SearchResultBuilderTest {

    private static final String TITLE = "title";
    private static final String SUMMARY = "summary";

    private Builder mBuilder;
    private ArrayList<String> mBreadcrumbs;
    private int mRank;
    private ResultPayload mResultPayload;
    private Drawable mIcon;

    @Before
    public void setUp() {
        mBuilder = new Builder();
        mBreadcrumbs = new ArrayList<>();
        mRank = 3;
        mResultPayload = new ResultPayload(new Intent());

        final Context context = ShadowApplication.getInstance().getApplicationContext();
        mIcon = context.getDrawable(R.drawable.ic_search_24dp);
    }

    @Test
    public void testAllInfo_BuildSearchResult() {
        mBuilder.setTitle(TITLE)
                .setSummary(SUMMARY)
                .setRank(mRank)
                .addBreadcrumbs(mBreadcrumbs)
                .setIcon(mIcon)
                .setPayload(mResultPayload)
                .setStableId(1);
        SearchResult result = mBuilder.build();

        assertThat(result).isNotNull();
        assertThat(result.title).isEqualTo(TITLE);
        assertThat(result.summary).isEqualTo(SUMMARY);
        assertThat(result.rank).isEqualTo(mRank);
        assertThat(result.breadcrumbs).isEqualTo(mBreadcrumbs);
        assertThat(result.icon).isEqualTo(mIcon);
        assertThat(result.payload).isEqualTo(mResultPayload);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoStableId_BuildSearchResultException() {
        mBuilder.setTitle(TITLE)
                .setSummary(SUMMARY)
                .setRank(mRank)
                .addBreadcrumbs(mBreadcrumbs)
                .setIcon(mIcon)
                .setPayload(mResultPayload);

        mBuilder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void testNoTitle_BuildSearchResultException() {
        mBuilder.setSummary(SUMMARY)
                .setRank(mRank)
                .addBreadcrumbs(mBreadcrumbs)
                .setIcon(mIcon)
                .setPayload(mResultPayload)
                .setStableId(1);

        mBuilder.build();
    }

    @Test
    public void testNoRank_BuildSearchResult_pass() {
        mBuilder.setTitle(TITLE)
                .setSummary(SUMMARY)
                .addBreadcrumbs(mBreadcrumbs)
                .setIcon(mIcon)
                .setPayload(mResultPayload)
                .setStableId(1);

        assertThat(mBuilder.build()).isNotNull();
    }

    @Test
    public void testNoIcon_BuildSearchResult_pass() {
        mBuilder.setTitle(TITLE)
                .setSummary(SUMMARY)
                .setRank(mRank)
                .addBreadcrumbs(mBreadcrumbs)
                .setPayload(mResultPayload)
                .setStableId(1);

        assertThat(mBuilder.build()).isNotNull();
    }

    @Test(expected = IllegalStateException.class)
    public void testNoPayload_BuildSearchResultException() {
        mBuilder.setTitle(TITLE)
                .setSummary(SUMMARY)
                .setRank(mRank)
                .addBreadcrumbs(mBreadcrumbs)
                .setIcon(mIcon)
                .setStableId(1);

        mBuilder.build();
    }
}


