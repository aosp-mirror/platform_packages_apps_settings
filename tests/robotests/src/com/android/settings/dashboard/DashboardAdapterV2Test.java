/*
 * Copyright (C) 2018 The Android Open Source Project
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.service.settings.suggestions.Suggestion;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.suggestions.SuggestionAdapterV2;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.drawer.Tile;

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
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class,
        })
public class DashboardAdapterV2Test {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SettingsActivity mContext;
    @Mock
    private View mView;
    @Mock
    private Condition mCondition;
    @Mock
    private Resources mResources;
    private FakeFeatureFactory mFactory;
    private DashboardAdapterV2 mDashboardAdapter;
    private List<Condition> mConditionList;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFactory = FakeFeatureFactory.setupForTest();
        when(mFactory.dashboardFeatureProvider.shouldTintIcon()).thenReturn(true);

        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getQuantityString(any(int.class), any(int.class), any()))
                .thenReturn("");

        mConditionList = new ArrayList<>();
        mConditionList.add(mCondition);
        when(mCondition.shouldShow()).thenReturn(true);
        mDashboardAdapter = new DashboardAdapterV2(mContext, null /* savedInstanceState */,
                mConditionList, null /* suggestionControllerMixin */, null /* lifecycle */);
        when(mView.getTag()).thenReturn(mCondition);
    }

    @Test
    public void testSuggestionDismissed_notOnlySuggestion_updateSuggestionOnly() {
        final DashboardAdapterV2 adapter =
                spy(new DashboardAdapterV2(mContext, null /* savedInstanceState */,
                        null /* conditions */, null /* suggestionControllerMixin */, null /*
                        lifecycle */));
        final List<Suggestion> suggestions = makeSuggestionsV2("pkg1", "pkg2", "pkg3");
        adapter.setSuggestions(suggestions);

        final RecyclerView data = mock(RecyclerView.class);
        when(data.getResources()).thenReturn(mResources);
        when(data.getContext()).thenReturn(mContext);
        when(mResources.getDisplayMetrics()).thenReturn(mock(DisplayMetrics.class));
        final View itemView = mock(View.class);
        when(itemView.findViewById(R.id.suggestion_list)).thenReturn(data);
        when(itemView.findViewById(android.R.id.summary)).thenReturn(mock(TextView.class));
        final DashboardAdapterV2.SuggestionContainerHolder holder =
                new DashboardAdapterV2.SuggestionContainerHolder(itemView);

        adapter.onBindSuggestion(holder, 0);

        final DashboardDataV2 dashboardData = adapter.mDashboardData;
        reset(adapter); // clear interactions tracking

        final Suggestion suggestionToRemove = suggestions.get(1);
        adapter.onSuggestionClosed(suggestionToRemove);

        assertThat(adapter.mDashboardData).isEqualTo(dashboardData);
        assertThat(suggestions.size()).isEqualTo(2);
        assertThat(suggestions.contains(suggestionToRemove)).isFalse();
        verify(adapter, never()).notifyDashboardDataChanged(any());
    }

    @Test
    public void testSuggestionDismissed_moreThanTwoSuggestions_shouldNotCrash() {
        final RecyclerView data = new RecyclerView(RuntimeEnvironment.application);
        final View itemView = mock(View.class);
        when(itemView.findViewById(R.id.suggestion_list)).thenReturn(data);
        when(itemView.findViewById(android.R.id.summary)).thenReturn(mock(TextView.class));
        final DashboardAdapterV2.SuggestionContainerHolder holder =
                new DashboardAdapterV2.SuggestionContainerHolder(itemView);
        final List<Suggestion> suggestions = makeSuggestionsV2("pkg1", "pkg2", "pkg3", "pkg4");
        final DashboardAdapterV2 adapter = spy(new DashboardAdapterV2(mContext,
                null /*savedInstance */, null /* conditions */,
                null /* suggestionControllerMixin */,
                null /* lifecycle */));
        adapter.setSuggestions(suggestions);
        adapter.onBindSuggestion(holder, 0);

        adapter.onSuggestionClosed(suggestions.get(1));

        // verify operations that access the lists will not cause ConcurrentModificationException
        assertThat(holder.data.getAdapter().getItemCount()).isEqualTo(3);
        adapter.setSuggestions(suggestions);
        // should not crash
    }

    @Test
    public void testSuggestionDismissed_onlySuggestion_updateDashboardData() {
        DashboardAdapterV2 adapter =
                spy(new DashboardAdapterV2(mContext, null /* savedInstanceState */,
                        null /* conditions */, null /* suggestionControllerMixin */, null /*
                        lifecycle */));
        final List<Suggestion> suggestions = makeSuggestionsV2("pkg1");
        adapter.setSuggestions(suggestions);
        final DashboardDataV2 dashboardData = adapter.mDashboardData;
        reset(adapter); // clear interactions tracking

        adapter.onSuggestionClosed(suggestions.get(0));

        assertThat(adapter.mDashboardData).isNotEqualTo(dashboardData);
        verify(adapter).notifyDashboardDataChanged(any());
    }

    @Test
    public void testBindSuggestion_shouldSetSuggestionAdapterAndNoCrash() {
        mDashboardAdapter = new DashboardAdapterV2(mContext, null /* savedInstanceState */,
                null /* conditions */, null /* suggestionControllerMixin */, null /* lifecycle */);
        final List<Suggestion> suggestions = makeSuggestionsV2("pkg1");

        mDashboardAdapter.setSuggestions(suggestions);

        final RecyclerView data = mock(RecyclerView.class);
        when(data.getResources()).thenReturn(mResources);
        when(data.getContext()).thenReturn(mContext);
        when(mResources.getDisplayMetrics()).thenReturn(mock(DisplayMetrics.class));
        final View itemView = mock(View.class);
        when(itemView.findViewById(R.id.suggestion_list)).thenReturn(data);
        when(itemView.findViewById(android.R.id.summary)).thenReturn(mock(TextView.class));
        final DashboardAdapterV2.SuggestionContainerHolder holder =
                new DashboardAdapterV2.SuggestionContainerHolder(itemView);

        mDashboardAdapter.onBindSuggestion(holder, 0);

        verify(data).setAdapter(any(SuggestionAdapterV2.class));
        // should not crash
    }

    @Test
    public void testBindSuggestion_shouldSetSummary() {
        mDashboardAdapter = new DashboardAdapterV2(mContext, null /* savedInstanceState */,
                null /* conditions */, null /* suggestionControllerMixin */, null /* lifecycle */);
        final List<Suggestion> suggestions = makeSuggestionsV2("pkg1");

        mDashboardAdapter.setSuggestions(suggestions);

        final RecyclerView data = mock(RecyclerView.class);
        when(data.getResources()).thenReturn(mResources);
        when(data.getContext()).thenReturn(mContext);
        when(mResources.getDisplayMetrics()).thenReturn(mock(DisplayMetrics.class));
        final View itemView = mock(View.class);
        when(itemView.findViewById(R.id.suggestion_list)).thenReturn(data);
        final TextView summary = mock(TextView.class);
        when(itemView.findViewById(android.R.id.summary)).thenReturn(summary);
        final DashboardAdapterV2.SuggestionContainerHolder holder =
                new DashboardAdapterV2.SuggestionContainerHolder(itemView);

        mDashboardAdapter.onBindSuggestion(holder, 0);

        verify(summary).setText("1");

        suggestions.addAll(makeSuggestionsV2("pkg2", "pkg3", "pkg4"));
        mDashboardAdapter.setSuggestions(suggestions);

        mDashboardAdapter.onBindSuggestion(holder, 0);

        verify(summary).setText("4");
    }

    @Test
    public void onBindTile_internalTile_shouldNotUseGenericBackgroundIcon() {
        final Context context = RuntimeEnvironment.application;
        final View view = LayoutInflater.from(context).inflate(R.layout.dashboard_tile, null);
        final DashboardAdapterV2.DashboardItemHolder holder =
                new DashboardAdapterV2.DashboardItemHolder(view);
        final Tile tile = new Tile();
        tile.icon = Icon.createWithResource(context, R.drawable.ic_settings);
        final DashboardAdapterV2.IconCache iconCache = mock(DashboardAdapterV2.IconCache.class);
        when(iconCache.getIcon(tile.icon)).thenReturn(context.getDrawable(R.drawable.ic_settings));

        mDashboardAdapter = new DashboardAdapterV2(context, null /* savedInstanceState */,
                null /* conditions */, null /* suggestionControllerMixin */, null /* lifecycle */);
        ReflectionHelpers.setField(mDashboardAdapter, "mCache", iconCache);
        mDashboardAdapter.onBindTile(holder, tile);

        verify(iconCache, never()).updateIcon(any(Icon.class), any(Drawable.class));
    }

    @Test
    public void onBindTile_externalTile_shouldNotUseGenericBackgroundIcon() {
        final Context context = RuntimeEnvironment.application;
        final View view = LayoutInflater.from(context).inflate(R.layout.dashboard_tile, null);
        final DashboardAdapterV2.DashboardItemHolder holder =
                new DashboardAdapterV2.DashboardItemHolder(view);
        final Tile tile = new Tile();
        tile.icon = mock(Icon.class);
        when(tile.icon.getResPackage()).thenReturn("another.package");

        final DashboardAdapterV2.IconCache iconCache = mock(DashboardAdapterV2.IconCache.class);
        when(iconCache.getIcon(tile.icon)).thenReturn(context.getDrawable(R.drawable.ic_settings));

        mDashboardAdapter = new DashboardAdapterV2(context, null /* savedInstanceState */,
                null /* conditions */, null /* suggestionControllerMixin */, null /* lifecycle */);
        ReflectionHelpers.setField(mDashboardAdapter, "mCache", iconCache);
        mDashboardAdapter.onBindTile(holder, tile);

        verify(iconCache).updateIcon(eq(tile.icon), any(RoundedHomepageIcon.class));
    }

    private List<Suggestion> makeSuggestionsV2(String... pkgNames) {
        final List<Suggestion> suggestions = new ArrayList<>();
        for (String pkgName : pkgNames) {
            final Suggestion suggestion = new Suggestion.Builder(pkgName)
                    .setPendingIntent(mock(PendingIntent.class))
                    .build();
            suggestions.add(suggestion);
        }
        return suggestions;
    }

    private void setupSuggestions(List<Suggestion> suggestions) {
        final Context context = RuntimeEnvironment.application;
        mDashboardAdapter.setSuggestions(suggestions);
    }
}
