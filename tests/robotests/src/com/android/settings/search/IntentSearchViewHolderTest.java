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
import android.view.LayoutInflater;
import android.view.View;
import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search2.IntentPayload;
import com.android.settings.search2.IntentSearchViewHolder;
import com.android.settings.search2.SearchResult.Builder;
import com.android.settings.search2.SearchResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class IntentSearchViewHolderTest {
    private IntentSearchViewHolder mHolder;
    private static Drawable mIcon;

    private static final String TITLE = "title";
    private static final String SUMMARY = "summary";


    @Before
    public void setUp() {
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
        mHolder.onBind(result);

        assertThat(mHolder.titleView.getText()).isEqualTo(TITLE);
        assertThat(mHolder.summaryView.getText()).isEqualTo(SUMMARY);
        assertThat(mHolder.iconView.getDrawable()).isEqualTo(mIcon);
    }

    private SearchResult getSearchResult() {
        Builder builder = new Builder();
        builder.addTitle(TITLE)
                .addSummary(SUMMARY)
                .addRank(1)
                .addPayload(new IntentPayload(null))
                .addBreadcrumbs(new ArrayList<String>())
                .addIcon(mIcon);

        return builder.build();
    }
}
