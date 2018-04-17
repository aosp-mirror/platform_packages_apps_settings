/*
 * Copyright (C) 2017 The Android Open Source Project
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
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.settings.dashboard.conditional.ConditionManager;
import com.android.settings.dashboard.conditional.FocusRecyclerView;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.suggestions.SuggestionControllerMixin;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
public class DashboardSummaryTest {

    @Mock
    private DashboardAdapter mAdapter;
    @Mock
    private DashboardFeatureProvider mDashboardFeatureProvider;
    @Mock
    private FocusRecyclerView mDashboard;
    @Mock
    private LinearLayoutManager mLayoutManager;
    @Mock
    private ConditionManager mConditionManager;
    @Mock
    private SummaryLoader mSummaryLoader;
    @Mock
    private SuggestionControllerMixin mSuggestionControllerMixin;

    private Context mContext;
    private DashboardSummary mSummary;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mContext = RuntimeEnvironment.application;
        mSummary = spy(new DashboardSummary());
        ReflectionHelpers.setField(mSummary, "mAdapter", mAdapter);
        ReflectionHelpers.setField(mSummary, "mDashboardFeatureProvider",
                mDashboardFeatureProvider);
        ReflectionHelpers.setField(mSummary, "mDashboard", mDashboard);
        ReflectionHelpers.setField(mSummary, "mLayoutManager", mLayoutManager);
        ReflectionHelpers.setField(mSummary, "mConditionManager", mConditionManager);
        ReflectionHelpers.setField(mSummary, "mSummaryLoader", mSummaryLoader);
    }

    @Test
    public void onAttach_suggestionDisabled_shouldNotStartSuggestionControllerMixin() {
        when(mFeatureFactory.suggestionsFeatureProvider.isSuggestionEnabled(any(Context.class)))
                .thenReturn(false);

        mSummary.onAttach(mContext);
        final SuggestionControllerMixin mixin = ReflectionHelpers
                .getField(mSummary, "mSuggestionControllerMixin");
        assertThat(mixin).isNull();
    }

    @Test
    public void onAttach_suggestionEnabled_shouldStartSuggestionControllerMixin() {
        when(mFeatureFactory.suggestionsFeatureProvider.isSuggestionEnabled(any(Context.class)))
                .thenReturn(true);

        mSummary.onAttach(mContext);
        final SuggestionControllerMixin mixin = ReflectionHelpers
                .getField(mSummary, "mSuggestionControllerMixin");
        assertThat(mixin).isNotNull();
    }

    @Test
    public void updateCategory_shouldGetCategoryFromFeatureProvider() {
        ReflectionHelpers.setField(mSummary, "mSuggestionControllerMixin",
                mSuggestionControllerMixin);

        when(mSuggestionControllerMixin.isSuggestionLoaded()).thenReturn(true);
        doReturn(mock(Activity.class)).when(mSummary).getActivity();
        mSummary.onAttach(mContext);
        mSummary.updateCategory();

        verify(mSummaryLoader).updateSummaryToCache(nullable(DashboardCategory.class));
        verify(mDashboardFeatureProvider).getTilesForCategory(CategoryKey.CATEGORY_HOMEPAGE);
        verify(mAdapter).setCategory(any());
    }

    @Test
    public void updateCategory_shouldGetCategoryFromFeatureProvider_evenIfSuggestionDisabled() {
        when(mFeatureFactory.suggestionsFeatureProvider.isSuggestionEnabled(any(Context.class)))
                .thenReturn(false);

        doReturn(mock(Activity.class)).when(mSummary).getActivity();
        mSummary.onAttach(mContext);
        mSummary.updateCategory();

        verify(mSummaryLoader).updateSummaryToCache(nullable(DashboardCategory.class));
        verify(mDashboardFeatureProvider).getTilesForCategory(CategoryKey.CATEGORY_HOMEPAGE);
        verify(mAdapter).setCategory(any());
    }

    @Test
    public void onConditionChanged_PositionAtTop_ScrollToTop() {
        when(mLayoutManager.findFirstCompletelyVisibleItemPosition()).thenReturn(1);
        mSummary.onConditionsChanged();
        mSummary.onConditionsChanged();
        verify(mDashboard).scrollToPosition(0);
    }

    @Test
    public void onConditionChanged_PositionNotTop_RemainPosition() {
        when(mLayoutManager.findFirstCompletelyVisibleItemPosition()).thenReturn(2);
        mSummary.onConditionsChanged();
        mSummary.onConditionsChanged();
        verify(mDashboard, never()).scrollToPosition(0);
    }

    @Test
    public void onConditionChanged_firstCall_shouldIgnore() {
        mSummary.onConditionsChanged();
        verify(mAdapter, never()).setConditions(any());
    }

    @Test
    public void onConditionChanged_secondCall_shouldSetConditionsOnAdapter() {
        mSummary.onConditionsChanged();
        mSummary.onConditionsChanged();
        verify(mAdapter).setConditions(any());
    }

    @Test
    public void onCategoryChanged_noRebuildOnFirstCall() {
        doReturn(mock(Activity.class)).when(mSummary).getActivity();
        doNothing().when(mSummary).rebuildUI();
        mSummary.onCategoriesChanged();
        verify(mSummary, never()).rebuildUI();
    }

    @Test
    public void onCategoryChanged_rebuildOnSecondCall() {
        doReturn(mock(Activity.class)).when(mSummary).getActivity();
        doNothing().when(mSummary).rebuildUI();
        mSummary.onCategoriesChanged();
        mSummary.onCategoriesChanged();
        verify(mSummary).rebuildUI();
    }
}