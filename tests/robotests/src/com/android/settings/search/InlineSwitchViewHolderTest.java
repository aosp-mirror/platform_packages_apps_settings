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
import android.util.Pair;
import android.view.LayoutInflater;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search2.InlineSwitchPayload;
import com.android.settings.search2.InlineSwitchViewHolder;
import com.android.settings.search2.SearchFragment;
import com.android.settings.search2.SearchResult;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class InlineSwitchViewHolderTest {

    private static final String TITLE = "title";
    private static final String SUMMARY = "summary";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private SearchFragment mFragment;

    @Mock
    private InlineSwitchPayload mPayload;

    private FakeFeatureFactory mFeatureFactory;
    private InlineSwitchViewHolder mHolder;
    private Drawable mIcon;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context context = RuntimeEnvironment.application;
        mIcon = context.getDrawable(R.drawable.ic_search_history);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);

        mHolder = new InlineSwitchViewHolder(
                LayoutInflater.from(context).inflate(R.layout.search_inline_switch_item, null),
                context);
        ReflectionHelpers.setField(mHolder, "mMetricsFeatureProvider",
                mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    public void testConstructor_MembersNotNull() {
        assertThat(mHolder.titleView).isNotNull();
        assertThat(mHolder.summaryView).isNotNull();
        assertThat(mHolder.iconView).isNotNull();
        assertThat(mHolder.switchView).isNotNull();
    }

    @Test
    public void testBindViewElements_AllUpdated() {
        when(mPayload.getSwitchValue(any(Context.class))).thenReturn(true);
        SearchResult result = getSearchResult();
        mHolder.onBind(mFragment, result);
        // Precondition: switch is on.
        assertThat(mHolder.switchView.isChecked()).isTrue();

        mHolder.switchView.performClick();

        verify(mFeatureFactory.metricsFeatureProvider).action(
                any(Context.class),
                eq(MetricsProto.MetricsEvent.ACTION_CLICK_SETTINGS_SEARCH_INLINE_RESULT),
                any(Pair.class), any(Pair.class), any(Pair.class));
        assertThat(mHolder.titleView.getText()).isEqualTo(TITLE);
        assertThat(mHolder.summaryView.getText()).isEqualTo(SUMMARY);
        assertThat(mHolder.iconView.getDrawable()).isEqualTo(mIcon);
        assertThat(mHolder.switchView.isChecked()).isFalse();
    }

    private SearchResult getSearchResult() {
        SearchResult.Builder builder = new SearchResult.Builder();
        builder.addTitle(TITLE)
                .addSummary(SUMMARY)
                .addRank(1)
                .addPayload(new InlineSwitchPayload("", 0, null))
                .addBreadcrumbs(new ArrayList<>())
                .addIcon(mIcon)
                .addPayload(mPayload);

        return builder.build();
    }
}
