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
package com.android.settings.dashboard;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Icon;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowDynamicIndexableContentMonitor;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class,
                ShadowDynamicIndexableContentMonitor.class
        })
public class DashboardAdapterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private View mView;
    @Mock
    private Condition mCondition;
    @Mock
    private Resources mResources;
    @Captor
    private ArgumentCaptor<Integer> mActionCategoryCaptor = ArgumentCaptor.forClass(Integer.class);
    @Captor
    private ArgumentCaptor<String> mActionPackageCaptor = ArgumentCaptor.forClass(String.class);
    private FakeFeatureFactory mFactory;
    private DashboardAdapter mDashboardAdapter;
    private DashboardAdapter.DashboardItemHolder mSuggestionHolder;
    private DashboardData.SuggestionHeaderData mSuggestionHeaderData;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        when(mFactory.suggestionsFeatureProvider
                .getSuggestionIdentifier(any(Context.class), any(Tile.class)))
                .thenAnswer(invocation -> {
                    final Object[] args = invocation.getArguments();
                    return ((Tile)args[1]).intent.getComponent().getPackageName();
                });

        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getQuantityString(any(int.class), any(int.class), any()))
                .thenReturn("");

        mDashboardAdapter = new DashboardAdapter(mContext, null, null);
        mSuggestionHeaderData = new DashboardData.SuggestionHeaderData(true, 1, 0);
        when(mView.getTag()).thenReturn(mCondition);
    }

    @Test
    public void testOnBindViewHolder_spacer_noSuggestions_noConditions() {
        makeCategory();
        DashboardAdapter.DashboardItemHolder holder = setupSpacer();

        mDashboardAdapter.onBindViewHolder(holder, 0);

        assertThat(holder.itemView.getBackground()).isNull();
    }

    @Test
    public void testOnBindViewHolder_spacer_suggestion_noConditions() {
        setupSuggestions(makeSuggestions("pkg1"));
        makeCategory();
        DashboardAdapter.DashboardItemHolder holder = setupSpacer();

        mDashboardAdapter.onBindViewHolder(holder, 0);

        assertThat(holder.itemView.getBackground()).isNotNull();
        assertThat(holder.itemView.getBackground()).isInstanceOf(ColorDrawable.class);
    }

    @Test
    public void testOnBindViewHolder_spacer_noSuggestion_condition() {
        makeCondition();
        makeCategory();
        DashboardAdapter.DashboardItemHolder holder = setupSpacer();

        mDashboardAdapter.onBindViewHolder(holder, 0);

        assertThat(holder.itemView.getBackground()).isNotNull();
        assertThat(holder.itemView.getBackground()).isInstanceOf(ColorDrawable.class);
    }

    @Test
    public void testOnBindViewHolder_spacer_suggestion_condition() {
        setupSuggestions(makeSuggestions("pkg1"));
        makeCondition();
        makeCategory();
        DashboardAdapter.DashboardItemHolder holder = setupSpacer();

        mDashboardAdapter.onBindViewHolder(holder, 0);

        assertThat(holder.itemView.getBackground()).isNotNull();
        assertThat(holder.itemView.getBackground()).isInstanceOf(ColorDrawable.class);
    }

    @Test
    public void testSetConditions_AfterSetConditions_ExpandedConditionNull() {
        mDashboardAdapter.onExpandClick(mView);
        assertThat(mDashboardAdapter.mDashboardData.getExpandedCondition()).isEqualTo(mCondition);
        mDashboardAdapter.setConditions(null);
        assertThat(mDashboardAdapter.mDashboardData.getExpandedCondition()).isNull();
    }

    @Test
    public void testSuggestionsLogs_NotExpanded() {
        setupSuggestions(makeSuggestions("pkg1", "pkg2", "pkg3"));
        verify(mFactory.metricsFeatureProvider, times(2)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture());
        String[] expectedPackages = new String[]{"pkg1", "pkg2"};
        Integer[] expectedActions = new Integer[]{
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION
        };
        assertThat(mActionCategoryCaptor.getAllValues().toArray()).isEqualTo(expectedActions);
        assertThat(mActionPackageCaptor.getAllValues().toArray()).isEqualTo(expectedPackages);
    }

    @Test
    public void testSuggestionsLogs_NotExpandedAndPaused() {
        setupSuggestions(makeSuggestions("pkg1", "pkg2", "pkg3"));
        mDashboardAdapter.onPause();
        verify(mFactory.metricsFeatureProvider, times(4)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture());
        String[] expectedPackages = new String[]{"pkg1", "pkg2", "pkg1", "pkg2"};
        Integer[] expectedActions = new Integer[]{
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION};
        assertThat(mActionCategoryCaptor.getAllValues().toArray()).isEqualTo(expectedActions);
        assertThat(mActionPackageCaptor.getAllValues().toArray()).isEqualTo(expectedPackages);
    }

    @Test
    public void testSuggestionsLogs_Expanded() {
        setupSuggestions(makeSuggestions("pkg1", "pkg2", "pkg3"));
        mDashboardAdapter.onBindSuggestionHeader(
                mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();
        verify(mFactory.metricsFeatureProvider, times(3)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture());
        String[] expectedPackages = new String[]{"pkg1", "pkg2", "pkg3"};
        Integer[] expectedActions = new Integer[]{
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION
        };
        assertThat(mActionPackageCaptor.getAllValues().toArray()).isEqualTo(expectedPackages);
        assertThat(mActionCategoryCaptor.getAllValues().toArray()).isEqualTo(expectedActions);
    }

    @Test
    public void testSuggestionsLogs_ExpandedAndPaused() {
        setupSuggestions(makeSuggestions("pkg1", "pkg2", "pkg3"));
        mDashboardAdapter.onBindSuggestionHeader(
                mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();
        mDashboardAdapter.onPause();
        verify(mFactory.metricsFeatureProvider, times(6)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture());
        String[] expectedPackages = new String[]{"pkg1", "pkg2", "pkg3", "pkg1", "pkg2", "pkg3"};
        Integer[] expectedActions = new Integer[]{
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION
        };
        assertThat(mActionPackageCaptor.getAllValues().toArray()).isEqualTo(expectedPackages);
        assertThat(mActionCategoryCaptor.getAllValues().toArray()).isEqualTo(expectedActions);
    }

    @Test
    public void testSuggestionsLogs_ExpandedAfterPause() {
        setupSuggestions(makeSuggestions("pkg1", "pkg2", "pkg3"));
        mDashboardAdapter.onPause();
        mDashboardAdapter.onBindSuggestionHeader(
                mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();
        verify(mFactory.metricsFeatureProvider, times(7)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture());
        String[] expectedPackages = new String[]{
                "pkg1", "pkg2", "pkg1", "pkg2", "pkg1", "pkg2", "pkg3"};
        Integer[] expectedActions = new Integer[]{
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION
        };
        assertThat(mActionPackageCaptor.getAllValues().toArray()).isEqualTo(expectedPackages);
        assertThat(mActionCategoryCaptor.getAllValues().toArray()).isEqualTo(expectedActions);
    }

    @Test
    public void testSuggestionsLogs_ExpandedAfterPauseAndPausedAgain() {
        setupSuggestions(makeSuggestions("pkg1", "pkg2", "pkg3"));
        mDashboardAdapter.onPause();
        mDashboardAdapter.onBindSuggestionHeader(
                mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();
        mDashboardAdapter.onPause();
        verify(mFactory.metricsFeatureProvider, times(10)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture());
        String[] expectedPackages = new String[]{
                "pkg1", "pkg2", "pkg1", "pkg2", "pkg1", "pkg2", "pkg3", "pkg1", "pkg2", "pkg3"};
        Integer[] expectedActions = new Integer[]{
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION
        };
        assertThat(mActionPackageCaptor.getAllValues().toArray()).isEqualTo(expectedPackages);
        assertThat(mActionCategoryCaptor.getAllValues().toArray()).isEqualTo(expectedActions);
    }

    @Test
    public void testSuggestionsLogs_ExpandedWithLessThanDefaultShown() {
        setupSuggestions(makeSuggestions("pkg1"));
        mDashboardAdapter.onBindSuggestionHeader(
                mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();
        verify(mFactory.metricsFeatureProvider, times(1)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture());
        String[] expectedPackages = new String[]{"pkg1"};
        Integer[] expectedActions = new Integer[]{
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
        };
        assertThat(mActionPackageCaptor.getAllValues().toArray()).isEqualTo(expectedPackages);
        assertThat(mActionCategoryCaptor.getAllValues().toArray()).isEqualTo(expectedActions);
    }

    @Test
    public void testSuggestionsLogs_ExpandedWithLessThanDefaultShownAndPaused() {
        setupSuggestions(makeSuggestions("pkg1"));
        mDashboardAdapter.onBindSuggestionHeader(
                mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();
        mDashboardAdapter.onPause();
        verify(mFactory.metricsFeatureProvider, times(2)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture());
        String[] expectedPackages = new String[]{"pkg1", "pkg1"};
        Integer[] expectedActions = new Integer[]{
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION
        };
        assertThat(mActionPackageCaptor.getAllValues().toArray()).isEqualTo(expectedPackages);
        assertThat(mActionCategoryCaptor.getAllValues().toArray()).isEqualTo(expectedActions);
    }

    @Test
    public void testSuggestionsLogs_ExpandedWithLessThanDefaultShownAfterPause() {
        setupSuggestions(makeSuggestions("pkg1"));
        mDashboardAdapter.onPause();
        mDashboardAdapter.onBindSuggestionHeader(
                mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();
        verify(mFactory.metricsFeatureProvider, times(3)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture());
        String[] expectedPackages = new String[]{"pkg1", "pkg1", "pkg1"};
        Integer[] expectedActions = new Integer[]{
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION
        };
        assertThat(mActionPackageCaptor.getAllValues().toArray()).isEqualTo(expectedPackages);
        assertThat(mActionCategoryCaptor.getAllValues().toArray()).isEqualTo(expectedActions);
    }

    @Test
    public void testSuggestionsLogs_ExpandedWithLessThanDefaultShownAfterPauseAndPausedAgain() {
        setupSuggestions(makeSuggestions("pkg1"));
        mDashboardAdapter.onPause();
        mDashboardAdapter.onBindSuggestionHeader(
                mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();
        mDashboardAdapter.onPause();
        verify(mFactory.metricsFeatureProvider, times(4)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture());
        String[] expectedPackages = new String[]{"pkg1", "pkg1", "pkg1", "pkg1"};
        Integer[] expectedActions = new Integer[]{
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION
        };
        assertThat(mActionPackageCaptor.getAllValues().toArray()).isEqualTo(expectedPackages);
        assertThat(mActionCategoryCaptor.getAllValues().toArray()).isEqualTo(expectedActions);
    }

    private List<Tile> makeSuggestions(String... pkgNames) {
        final List<Tile> suggestions = new ArrayList<>();
        for (String pkgName : pkgNames) {
            Tile suggestion = new Tile();
            suggestion.intent = new Intent("action");
            suggestion.intent.setComponent(new ComponentName(pkgName, "cls"));
            suggestions.add(suggestion);
        }
        return suggestions;
    }

    private void setupSuggestions(List<Tile> suggestions) {
        mDashboardAdapter.setCategoriesAndSuggestions(new ArrayList<>(), suggestions);
        mSuggestionHolder = mDashboardAdapter.onCreateViewHolder(
                new FrameLayout(RuntimeEnvironment.application),
                mDashboardAdapter.getItemViewType(1));
    }

    private void makeCondition() {
        final List<Condition> conditions = new ArrayList<>();
        Condition condition = mock(Condition.class);
        when(condition.shouldShow()).thenReturn(true);
        conditions.add(condition);
        mDashboardAdapter.setConditions(conditions);
    }

    private void makeCategory() {
        List<DashboardCategory> categories = new ArrayList<>();
        categories.add(new DashboardCategory());
        mDashboardAdapter.setCategory(categories);
    }

    private DashboardAdapter.DashboardItemHolder setupSpacer() {
        Context context = RuntimeEnvironment.application;
        final View view = LayoutInflater.from(context)
                .inflate(R.layout.dashboard_header_spacer, new LinearLayout(context), false);
        return new DashboardAdapter.DashboardItemHolder(view);
    }
}
