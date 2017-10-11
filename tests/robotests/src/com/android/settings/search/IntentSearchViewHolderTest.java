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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search2.AppSearchResult;
import com.android.settings.search2.IntentPayload;
import com.android.settings.search2.IntentSearchViewHolder;
import com.android.settings.search2.SearchFragment;
import com.android.settings.search2.SearchResult;
import com.android.settings.search2.SearchResult.Builder;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class IntentSearchViewHolderTest {

    private static final String TITLE = "title";
    private static final String SUMMARY = "summary";
    private static final int USER_ID = 10;
    private static final String BADGED_LABEL = "work title";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SearchFragment mFragment;
    @Mock
    private PackageManager mPackageManager;
    private FakeFeatureFactory mFeatureFactory;
    private IntentSearchViewHolder mHolder;
    private Drawable mIcon;
    private Drawable mBadgedIcon;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);

        final Context context = RuntimeEnvironment.application;
        View view = LayoutInflater.from(context).inflate(R.layout.search_intent_item, null);
        mHolder = new IntentSearchViewHolder(view);

        mIcon = context.getDrawable(R.drawable.ic_search_history);
        mBadgedIcon = context.getDrawable(R.drawable.ic_add);
        when(mFragment.getActivity().getPackageManager()).thenReturn(mPackageManager);
    }

    @Test
    public void testConstructor_membersNotNull() {
        assertThat(mHolder.titleView).isNotNull();
        assertThat(mHolder.summaryView).isNotNull();
        assertThat(mHolder.iconView).isNotNull();
        assertThat(mHolder.breadcrumbView).isNotNull();
    }

    @Test
    public void testBindViewElements_allUpdated() {
        SearchResult result = getSearchResult(TITLE, SUMMARY, mIcon);
        mHolder.onBind(mFragment, result);
        mHolder.itemView.performClick();

        assertThat(mHolder.titleView.getText()).isEqualTo(TITLE);
        assertThat(mHolder.summaryView.getText()).isEqualTo(SUMMARY);
        assertThat(mHolder.iconView.getDrawable()).isEqualTo(mIcon);
        assertThat(mHolder.summaryView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mHolder.breadcrumbView.getVisibility()).isEqualTo(View.GONE);

        verify(mFragment).onSearchResultClicked();
        verify(mFragment).startActivity(any(Intent.class));
        verify(mFeatureFactory.metricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.ACTION_CLICK_SETTINGS_SEARCH_RESULT),
                eq(((IntentPayload)result.payload).intent.getComponent().flattenToString()),
                any(Pair.class));
    }

    @Test
    public void testBindViewIcon_nullIcon_imageDrawableIsNull() {
        final SearchResult result = getSearchResult(TITLE, SUMMARY, null);
        mHolder.onBind(mFragment, result);

        assertThat(mHolder.iconView.getDrawable()).isNull();
    }

    @Test
    public void testBindViewElements_emptySummary_hideSummaryView() {
        final SearchResult result = new Builder()
                .addTitle(TITLE)
                .addRank(1)
                .addPayload(new IntentPayload(null))
                .addIcon(mIcon)
                .build();

        mHolder.onBind(mFragment, result);
        assertThat(mHolder.summaryView.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testBindViewElements_withBreadcrumb_shouldFormatBreadcrumb() {
        final List<String> breadcrumbs = new ArrayList<>();
        breadcrumbs.add("a");
        breadcrumbs.add("b");
        breadcrumbs.add("c");
        final SearchResult result = new Builder()
                .addTitle(TITLE)
                .addRank(1)
                .addPayload(new IntentPayload(null))
                .addBreadcrumbs(breadcrumbs)
                .addIcon(mIcon)
                .build();

        mHolder.onBind(mFragment, result);
        assertThat(mHolder.breadcrumbView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mHolder.breadcrumbView.getText()).isEqualTo("a > b > c");
    }

    @Test
    public void testBindElements_placeholderSummary_visibilityIsGone() {
        String nonBreakingSpace = mContext.getString(R.string.summary_placeholder);
        SearchResult result = new Builder()
                .addTitle(TITLE)
                .addSummary(nonBreakingSpace)
                .addPayload(new IntentPayload(null))
                .build();

        mHolder.onBind(mFragment, result);

        assertThat(mHolder.summaryView.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testBindElements_dynamicSummary_visibilityIsGone() {
        String dynamicSummary = "%s";
        SearchResult result = new Builder()
                .addTitle(TITLE)
                .addSummary(dynamicSummary)
                .addPayload(new IntentPayload(null))
                .build();

        mHolder.onBind(mFragment, result);

        assertThat(mHolder.summaryView.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    @Ignore
    public void testBindViewElements_appSearchResult() {
        when(mPackageManager.getUserBadgedLabel(any(CharSequence.class),
                eq(new UserHandle(USER_ID)))).thenReturn(BADGED_LABEL);

        SearchResult result = getAppSearchResult(
                TITLE, SUMMARY, mIcon, getApplicationInfo(USER_ID, TITLE, mIcon));
        mHolder.onBind(mFragment, result);
        mHolder.itemView.performClick();

        assertThat(mHolder.titleView.getText()).isEqualTo(TITLE);
        assertThat(mHolder.summaryView.getText()).isEqualTo(SUMMARY);
        assertThat(mHolder.summaryView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mHolder.breadcrumbView.getVisibility()).isEqualTo(View.GONE);
        assertThat(mHolder.titleView.getContentDescription()).isEqualTo(BADGED_LABEL);

        verify(mFragment).onSearchResultClicked();
        verify(mFragment.getActivity()).startActivityAsUser(
                any(Intent.class), eq(new UserHandle(USER_ID)));
        verify(mFeatureFactory.metricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.ACTION_CLICK_SETTINGS_SEARCH_RESULT),
                eq(((IntentPayload)result.payload).intent.getComponent().flattenToString()),
                any(Pair.class));
    }

    private SearchResult getSearchResult(String title, String summary, Drawable icon) {
        Builder builder = new Builder();
        builder.addTitle(title)
                .addSummary(summary)
                .addRank(1)
                .addPayload(new IntentPayload(
                        new Intent().setComponent(new ComponentName("pkg", "class"))))
                .addBreadcrumbs(new ArrayList<>())
                .addIcon(icon);

        return builder.build();
    }

    private SearchResult getAppSearchResult(
            String title, String summary, Drawable icon, ApplicationInfo applicationInfo) {
        AppSearchResult.Builder builder = new AppSearchResult.Builder();
        builder.addTitle(title)
                .addSummary(summary)
                .addRank(1)
                .addPayload(new IntentPayload(
                        new Intent().setComponent(new ComponentName("pkg", "class"))))
                .addBreadcrumbs(new ArrayList<>())
                .addIcon(icon);
        builder.setAppInfo(applicationInfo);
        return builder.build();
    }

    private ApplicationInfo getApplicationInfo(int userId, CharSequence appLabel, Drawable icon) {
        ApplicationInfo applicationInfo = spy(new ApplicationInfo());
        applicationInfo.uid = UserHandle.getUid(userId, 12345);
        doReturn(icon).when(applicationInfo).loadIcon(any(PackageManager.class));
        doReturn(appLabel).when(applicationInfo).loadLabel(any(PackageManager.class));
        return applicationInfo;
    }
}
