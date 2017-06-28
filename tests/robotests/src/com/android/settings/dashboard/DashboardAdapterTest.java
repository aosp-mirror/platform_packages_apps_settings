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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Icon;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.suggestions.SuggestionAdapter;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
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
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private SettingsActivity mContext;
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
        when(mFactory.dashboardFeatureProvider.shouldTintIcon()).thenReturn(true);
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

    @Test
    public void testBindViewHolder_inflateRemoteView() {
        List<Tile> packages = makeSuggestions("pkg1");
        RemoteViews remoteViews = mock(RemoteViews.class);
        TextView textView = new TextView(RuntimeEnvironment.application);
        doReturn(textView).when(remoteViews).apply(any(Context.class), any(ViewGroup.class));
        packages.get(0).remoteViews = remoteViews;
        mDashboardAdapter.setCategoriesAndSuggestions(Collections.emptyList(), packages);
        mSuggestionHolder = mDashboardAdapter.onCreateViewHolder(
                new FrameLayout(RuntimeEnvironment.application),
                R.layout.suggestion_tile_card);

        mDashboardAdapter.onBindViewHolder(mSuggestionHolder, 1);
        assertThat(textView.getParent()).isSameAs(mSuggestionHolder.itemView);
        mSuggestionHolder.itemView.performClick();

        verify(mContext).startSuggestion(any(Intent.class));
    }

    @Test
    public void testBindViewHolder_primaryViewHandlesClick() {
        Context context =
                new ContextThemeWrapper(RuntimeEnvironment.application, R.style.Theme_Settings);

        List<Tile> packages = makeSuggestions("pkg1");
        RemoteViews remoteViews = mock(RemoteViews.class);
        FrameLayout layout = new FrameLayout(context);
        Button primary = new Button(context);
        primary.setId(android.R.id.primary);
        layout.addView(primary);
        doReturn(layout).when(remoteViews).apply(any(Context.class), any(ViewGroup.class));
        packages.get(0).remoteViews = remoteViews;
        mDashboardAdapter.setCategoriesAndSuggestions(Collections.emptyList(), packages);
        mSuggestionHolder = mDashboardAdapter.onCreateViewHolder(
                new FrameLayout(context),
                R.layout.suggestion_tile_card);

        mDashboardAdapter.onBindViewHolder(mSuggestionHolder, 1);

        mSuggestionHolder.itemView.performClick();
        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
        verify(mContext, never()).startSuggestion(any(Intent.class));

        primary.performClick();

        verify(mContext).startSuggestion(any(Intent.class));
    }

    @Test
    public void testBindViewHolder_viewsClearedOnRebind() {
        Context context =
                new ContextThemeWrapper(RuntimeEnvironment.application, R.style.Theme_Settings);

        List<Tile> packages = makeSuggestions("pkg1");
        RemoteViews remoteViews = mock(RemoteViews.class);
        FrameLayout layout = new FrameLayout(context);
        Button primary = new Button(context);
        primary.setId(android.R.id.primary);
        layout.addView(primary);
        doReturn(layout).when(remoteViews).apply(any(Context.class), any(ViewGroup.class));
        packages.get(0).remoteViews = remoteViews;
        mDashboardAdapter.setCategoriesAndSuggestions(Collections.emptyList(), packages);
        mSuggestionHolder = mDashboardAdapter.onCreateViewHolder(
                new FrameLayout(context),
                R.layout.suggestion_tile_card);

        mDashboardAdapter.onBindViewHolder(mSuggestionHolder, 1);
        mDashboardAdapter.onBindViewHolder(mSuggestionHolder, 1);

        ViewGroup itemView = (ViewGroup) mSuggestionHolder.itemView;
        assertThat(itemView.getChildCount()).isEqualTo(1);
    }

    @Test
    public void testSuggestionDismissed_notOnlySuggestion_doNothing() {
        final DashboardAdapter adapter = spy(new DashboardAdapter(mContext, null, null));
        adapter.setCategoriesAndSuggestions(
                new ArrayList<>(), makeSuggestions("pkg1", "pkg2", "pkg3"));
        final DashboardData dashboardData = adapter.mDashboardData;
        reset(adapter); // clear interactions tracking

        adapter.onSuggestionDismissed();

        assertThat(adapter.mDashboardData).isEqualTo(dashboardData);
        verify(adapter, never()).notifyDashboardDataChanged(any());
    }

    @Test
    public void testSuggestionDismissed_onlySuggestion_updateDashboardData() {
        DashboardAdapter adapter = spy(new DashboardAdapter(mContext, null, null));
        adapter.setCategoriesAndSuggestions(new ArrayList<>(), makeSuggestions("pkg1"));
        final DashboardData dashboardData = adapter.mDashboardData;
        reset(adapter); // clear interactions tracking

        adapter.onSuggestionDismissed();

        assertThat(adapter.mDashboardData).isNotEqualTo(dashboardData);
        verify(adapter).notifyDashboardDataChanged(any());
    }

    @Test
    public void testSetCategoriesAndSuggestions_iconTinted() {
        TypedArray mockTypedArray = mock(TypedArray.class);
        doReturn(mockTypedArray).when(mContext).obtainStyledAttributes(any(int[].class));
        doReturn(0x89000000).when(mockTypedArray).getColor(anyInt(), anyInt());

        List<Tile> packages = makeSuggestions("pkg1");
        Icon mockIcon = mock(Icon.class);
        packages.get(0).isIconTintable = true;
        packages.get(0).icon = mockIcon;

        mDashboardAdapter.setCategoriesAndSuggestions(Collections.emptyList(), packages);

        verify(mockIcon).setTint(eq(0x89000000));
    }

    @Test
    public void testSetCategories_iconTinted() {
        TypedArray mockTypedArray = mock(TypedArray.class);
        doReturn(mockTypedArray).when(mContext).obtainStyledAttributes(any(int[].class));
        doReturn(0x89000000).when(mockTypedArray).getColor(anyInt(), anyInt());

        final List<DashboardCategory> categories = new ArrayList<>();
        final DashboardCategory category = mock(DashboardCategory.class);
        final List<Tile> tiles = new ArrayList<>();
        final Icon mockIcon = mock(Icon.class);
        final Tile tile = new Tile();
        tile.isIconTintable = true;
        tile.icon = mockIcon;
        tiles.add(tile);
        category.tiles = tiles;
        categories.add(category);

        mDashboardAdapter.setCategory(categories);

        verify(mockIcon).setTint(eq(0x89000000));
    }

    @Test
    public void testBindConditionAndSuggestion_shouldSetSuggestionAdapterAndNoCrash() {
        when(mFactory.dashboardFeatureProvider.combineSuggestionAndCondition()).thenReturn(true);
        mDashboardAdapter = new DashboardAdapter(mContext, null, null);
        final List<Tile> suggestions = makeSuggestions("pkg1");
        final List<DashboardCategory> categories = new ArrayList<>();
        final DashboardCategory category = mock(DashboardCategory.class);
        final List<Tile> tiles = new ArrayList<>();
        tiles.add(mock(Tile.class));
        category.tiles = tiles;
        categories.add(category);
        mDashboardAdapter.setCategoriesAndSuggestions(categories, suggestions);

        final RecyclerView data = mock(RecyclerView.class);
        when(data.getResources()).thenReturn(mResources);
        when(data.getContext()).thenReturn(mContext);
        when(mResources.getDisplayMetrics()).thenReturn(mock(DisplayMetrics.class));
        final View itemView = mock(View.class);
        when(itemView.findViewById(R.id.data)).thenReturn(data);
        final DashboardAdapter.SuggestionAndConditionContainerHolder holder =
                new DashboardAdapter.SuggestionAndConditionContainerHolder(itemView);

        mDashboardAdapter.onBindConditionAndSuggestion(
                holder, DashboardAdapter.SUGGESTION_CONDITION_HEADER_POSITION + 1);

        verify(data).setAdapter(any(SuggestionAdapter.class));
        // should not crash
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
}
