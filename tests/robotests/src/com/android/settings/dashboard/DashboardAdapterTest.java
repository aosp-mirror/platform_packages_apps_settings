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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
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
import android.os.Bundle;
import android.service.settings.suggestions.Suggestion;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.suggestions.SuggestionAdapter;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.drawer.TileUtils;
import com.android.settingslib.utils.IconCache;

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
@Config(shadows = SettingsShadowResources.SettingsShadowTheme.class)
public class DashboardAdapterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SettingsActivity mContext;
    @Mock
    private View mView;
    @Mock
    private Condition mCondition;
    @Mock
    private Resources mResources;
    @Mock
    private WindowManager mWindowManager;
    private FakeFeatureFactory mFactory;
    private DashboardAdapter mDashboardAdapter;
    private List<Condition> mConditionList;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFactory = FakeFeatureFactory.setupForTest();
        when(mFactory.dashboardFeatureProvider.shouldTintIcon()).thenReturn(true);

        when(mContext.getSystemService(Context.WINDOW_SERVICE)).thenReturn(mWindowManager);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getQuantityString(any(int.class), any(int.class), any())).thenReturn("");

        mConditionList = new ArrayList<>();
        mConditionList.add(mCondition);
        when(mCondition.shouldShow()).thenReturn(true);
        mDashboardAdapter = new DashboardAdapter(mContext, null /* savedInstanceState */,
                mConditionList, null /* suggestionControllerMixin */, null /* lifecycle */);
        when(mView.getTag()).thenReturn(mCondition);
    }

    @Test
    public void testSuggestionDismissed_notOnlySuggestion_updateSuggestionOnly() {
        final DashboardAdapter adapter =
                spy(new DashboardAdapter(mContext, null /* savedInstanceState */,
                        null /* conditions */, null /* suggestionControllerMixin */,
                        null /* lifecycle */));
        final List<Suggestion> suggestions = makeSuggestionsV2("pkg1", "pkg2", "pkg3");
        adapter.setSuggestions(suggestions);

        final RecyclerView data = mock(RecyclerView.class);
        when(data.getResources()).thenReturn(mResources);
        when(data.getContext()).thenReturn(mContext);
        when(mResources.getDisplayMetrics()).thenReturn(mock(DisplayMetrics.class));
        final View itemView = mock(View.class);
        when(itemView.findViewById(R.id.suggestion_list)).thenReturn(data);
        when(itemView.findViewById(android.R.id.summary)).thenReturn(mock(TextView.class));
        when(itemView.findViewById(android.R.id.title)).thenReturn(mock(TextView.class));
        final DashboardAdapter.SuggestionContainerHolder holder =
                new DashboardAdapter.SuggestionContainerHolder(itemView);

        adapter.onBindSuggestion(holder, 0);

        final DashboardData dashboardData = adapter.mDashboardData;
        reset(adapter); // clear interactions tracking

        final Suggestion suggestionToRemove = suggestions.get(1);
        adapter.onSuggestionClosed(suggestionToRemove);

        assertThat(suggestions.size()).isEqualTo(2);
        assertThat(suggestions.contains(suggestionToRemove)).isFalse();
        verify(adapter).notifyDashboardDataChanged(any());
    }

    @Test
    public void testSuggestionDismissed_onlySuggestion_updateDashboardData() {
        DashboardAdapter adapter =
                spy(new DashboardAdapter(mContext, null /* savedInstanceState */,
                        null /* conditions */, null /* suggestionControllerMixin */,
                        null /* lifecycle */));
        final List<Suggestion> suggestions = makeSuggestionsV2("pkg1");
        adapter.setSuggestions(suggestions);
        final DashboardData dashboardData = adapter.mDashboardData;
        reset(adapter); // clear interactions tracking

        adapter.onSuggestionClosed(suggestions.get(0));

        assertThat(adapter.mDashboardData).isNotEqualTo(dashboardData);
        verify(adapter).notifyDashboardDataChanged(any());
    }

    @Test
    public void testBindSuggestion_shouldSetSuggestionAdapterAndNoCrash() {
        mDashboardAdapter = new DashboardAdapter(mContext, null /* savedInstanceState */,
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
        when(itemView.findViewById(android.R.id.title)).thenReturn(mock(TextView.class));
        final DashboardAdapter.SuggestionContainerHolder holder =
                new DashboardAdapter.SuggestionContainerHolder(itemView);

        mDashboardAdapter.onBindSuggestion(holder, 0);

        verify(data).setAdapter(any(SuggestionAdapter.class));
        // should not crash
    }

    @Test
    public void onBindTile_internalTile_shouldNotUseGenericBackgroundIcon() {
        final Context context = RuntimeEnvironment.application;
        final View view = LayoutInflater.from(context).inflate(R.layout.dashboard_tile, null);
        final DashboardAdapter.DashboardItemHolder holder =
                new DashboardAdapter.DashboardItemHolder(view);
        final Tile tile = new Tile();
        tile.icon = Icon.createWithResource(context, R.drawable.ic_settings);
        final IconCache iconCache = mock(IconCache.class);
        when(iconCache.getIcon(tile.icon)).thenReturn(context.getDrawable(R.drawable.ic_settings));

        mDashboardAdapter = new DashboardAdapter(context, null /* savedInstanceState */,
                null /* conditions */, null /* suggestionControllerMixin */, null /* lifecycle */);
        ReflectionHelpers.setField(mDashboardAdapter, "mCache", iconCache);
        mDashboardAdapter.onBindTile(holder, tile);

        verify(iconCache, never()).updateIcon(any(Icon.class), any(Drawable.class));
    }

    @Test
    public void onBindTile_externalTile_shouldUpdateIcon() {
        final Context context = spy(RuntimeEnvironment.application);
        final View view = LayoutInflater.from(context).inflate(R.layout.dashboard_tile, null);
        final DashboardAdapter.DashboardItemHolder holder =
                new DashboardAdapter.DashboardItemHolder(view);
        final Tile tile = new Tile();
        tile.icon = Icon.createWithResource(context, R.drawable.ic_settings);
        when(tile.icon.getResPackage()).thenReturn("another.package");

        final IconCache iconCache = new IconCache(context);

        mDashboardAdapter = new DashboardAdapter(context, null /* savedInstanceState */,
                null /* conditions */, null /* suggestionControllerMixin */, null /* lifecycle */);
        ReflectionHelpers.setField(mDashboardAdapter, "mCache", iconCache);

        doReturn("another.package").when(context).getPackageName();
        mDashboardAdapter.onBindTile(holder, tile);

        assertThat(iconCache.getIcon(tile.icon)).isInstanceOf(RoundedHomepageIcon.class);
    }

    @Test
    public void onBindTile_externalTileWithBackgroundColorHint_shouldUpdateIcon() {
        final Context context = spy(RuntimeEnvironment.application);
        final View view = LayoutInflater.from(context).inflate(R.layout.dashboard_tile, null);
        final DashboardAdapter.DashboardItemHolder holder =
                new DashboardAdapter.DashboardItemHolder(view);
        final Tile tile = new Tile();
        tile.metaData = new Bundle();
        tile.metaData.putInt(TileUtils.META_DATA_PREFERENCE_ICON_BACKGROUND_HINT,
                R.color.memory_critical);
        tile.icon = Icon.createWithResource(context, R.drawable.ic_settings);
        final IconCache iconCache = new IconCache(context);
        mDashboardAdapter = new DashboardAdapter(context, null /* savedInstanceState */,
                null /* conditions */, null /* suggestionControllerMixin */, null /* lifecycle */);
        ReflectionHelpers.setField(mDashboardAdapter, "mCache", iconCache);

        doReturn("another.package").when(context).getPackageName();
        mDashboardAdapter.onBindTile(holder, tile);

        final RoundedHomepageIcon homepageIcon = (RoundedHomepageIcon) iconCache.getIcon(tile.icon);
        assertThat(homepageIcon.mBackgroundColor)
                .isEqualTo(RuntimeEnvironment.application.getColor(R.color.memory_critical));
    }

    @Test
    public void onBindTile_externalTile_usingRoundedHomepageIcon_shouldNotUpdateIcon() {
        final Context context = RuntimeEnvironment.application;
        final View view = LayoutInflater.from(context).inflate(R.layout.dashboard_tile, null);
        final DashboardAdapter.DashboardItemHolder holder =
                new DashboardAdapter.DashboardItemHolder(view);
        final Tile tile = new Tile();
        tile.icon = mock(Icon.class);
        when(tile.icon.getResPackage()).thenReturn("another.package");

        final IconCache iconCache = mock(IconCache.class);
        when(iconCache.getIcon(tile.icon)).thenReturn(mock(RoundedHomepageIcon.class));

        mDashboardAdapter = new DashboardAdapter(context, null /* savedInstanceState */,
                null /* conditions */, null /* suggestionControllerMixin */, null /* lifecycle */);
        ReflectionHelpers.setField(mDashboardAdapter, "mCache", iconCache);

        mDashboardAdapter.onBindTile(holder, tile);

        verify(iconCache, never()).updateIcon(eq(tile.icon), any(RoundedHomepageIcon.class));
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
}
