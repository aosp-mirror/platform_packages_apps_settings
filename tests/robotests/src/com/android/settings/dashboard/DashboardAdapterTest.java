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
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionAdapter;
import com.android.settings.dashboard.suggestions.SuggestionAdapter;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
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

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class,
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
    @Captor
    private ArgumentCaptor<Pair> mTaggedDataCaptor = ArgumentCaptor.forClass(Pair.class);
    private FakeFeatureFactory mFactory;
    private DashboardAdapter mDashboardAdapter;
    private DashboardAdapter.SuggestionAndConditionHeaderHolder mSuggestionHolder;
    private DashboardData.SuggestionConditionHeaderData mSuggestionHeaderData;
    private List<Condition> mConditionList;

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

        mConditionList = new ArrayList<>();
        mConditionList.add(mCondition);
        when(mCondition.shouldShow()).thenReturn(true);
        mDashboardAdapter = new DashboardAdapter(mContext, null, mConditionList, null, null);
        mSuggestionHeaderData = new DashboardData.SuggestionConditionHeaderData(mConditionList, 1);
        when(mView.getTag()).thenReturn(mCondition);
    }

    @Test
    public void testSuggestionsLogs_NotExpanded() {
        setupSuggestions(makeSuggestions("pkg1", "pkg2", "pkg3"));

        verify(mFactory.metricsFeatureProvider, times(2)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture(),
                mTaggedDataCaptor.capture());
        assertThat(mActionCategoryCaptor.getAllValues()).containsExactly(
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION);
        assertThat(mActionPackageCaptor.getAllValues()).containsExactly("pkg1", "pkg2");
        assertThat(mTaggedDataCaptor.getAllValues()).containsExactly(
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0));
    }

    @Test
    public void testSuggestionsLogs_NotExpandedAndPaused() {
        setupSuggestions(makeSuggestions("pkg1", "pkg2", "pkg3"));

        mDashboardAdapter.onPause();

        verify(mFactory.metricsFeatureProvider, times(4)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture(),
                mTaggedDataCaptor.capture());
        assertThat(mActionCategoryCaptor.getAllValues()).containsExactly(
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION);
        assertThat(mActionPackageCaptor.getAllValues()).containsExactly(
                "pkg1", "pkg2", "pkg1", "pkg2");
        assertThat(mTaggedDataCaptor.getAllValues()).containsExactly(
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0));
    }

    @Test
    public void testSuggestionsLogs_Expanded() {
        setupSuggestions(makeSuggestions("pkg1", "pkg2", "pkg3"));

        mDashboardAdapter.onBindSuggestionConditionHeader(mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();

        verify(mFactory.metricsFeatureProvider, times(3)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture(),
                mTaggedDataCaptor.capture());
        assertThat(mActionCategoryCaptor.getAllValues()).containsExactly(
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION);
        assertThat(mActionPackageCaptor.getAllValues()).containsExactly(
                "pkg1", "pkg2", "pkg3");
        assertThat(mTaggedDataCaptor.getAllValues()).containsExactly(
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0));
    }

    @Test
    public void testSuggestionsLogs_ExpandedAndPaused() {
        setupSuggestions(makeSuggestions("pkg1", "pkg2", "pkg3"));

        mDashboardAdapter.onBindSuggestionConditionHeader(mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();
        mDashboardAdapter.onPause();

        verify(mFactory.metricsFeatureProvider, times(6)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture(),
                mTaggedDataCaptor.capture());
        assertThat(mActionCategoryCaptor.getAllValues()).containsExactly(
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION);
        assertThat(mActionPackageCaptor.getAllValues()).containsExactly(
                "pkg1", "pkg2", "pkg3", "pkg1", "pkg2", "pkg3");
        assertThat(mTaggedDataCaptor.getAllValues()).containsExactly(
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0));
    }

    @Test
    public void testSuggestionsLogs_ExpandedAfterPause() {
        setupSuggestions(makeSuggestions("pkg1", "pkg2", "pkg3"));

        mDashboardAdapter.onPause();
        mDashboardAdapter.onBindSuggestionConditionHeader(mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();

        verify(mFactory.metricsFeatureProvider, times(7)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture(),
                mTaggedDataCaptor.capture());
        assertThat(mActionCategoryCaptor.getAllValues()).containsExactly(
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION);
        assertThat(mActionPackageCaptor.getAllValues()).containsExactly(
                "pkg1", "pkg2", "pkg1", "pkg2", "pkg1", "pkg2", "pkg3");
        assertThat(mTaggedDataCaptor.getAllValues()).containsExactly(
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0));
    }

    @Test
    public void testSuggestionsLogs_ExpandedAfterPauseAndPausedAgain() {
        setupSuggestions(makeSuggestions("pkg1", "pkg2", "pkg3"));

        mDashboardAdapter.onPause();
        mDashboardAdapter.onBindSuggestionConditionHeader(mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();
        mDashboardAdapter.onPause();

        verify(mFactory.metricsFeatureProvider, times(10)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture(),
                mTaggedDataCaptor.capture());
        assertThat(mActionCategoryCaptor.getAllValues()).containsExactly(
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION);
        assertThat(mActionPackageCaptor.getAllValues()).containsExactly(
                "pkg1", "pkg2", "pkg1", "pkg2", "pkg1", "pkg2", "pkg3", "pkg1", "pkg2", "pkg3");
        assertThat(mTaggedDataCaptor.getAllValues()).containsExactly(
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0));
    }

    @Test
    public void testSuggestionsLogs_ExpandedWithLessThanDefaultShown() {
        setupSuggestions(makeSuggestions("pkg1"));

        mDashboardAdapter.onBindSuggestionConditionHeader(mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();

        verify(mFactory.metricsFeatureProvider, times(1)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture(),
                mTaggedDataCaptor.capture());
        assertThat(mActionCategoryCaptor.getAllValues()).containsExactly(
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION);
        assertThat(mActionPackageCaptor.getAllValues()).containsExactly("pkg1");
        assertThat(mTaggedDataCaptor.getAllValues()).containsExactly(
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0));
    }

    @Test
    public void testSuggestionsLogs_ExpandedWithLessThanDefaultShownAndPaused() {
        setupSuggestions(makeSuggestions("pkg1"));

        mDashboardAdapter.onBindSuggestionConditionHeader(mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();
        mDashboardAdapter.onPause();

        verify(mFactory.metricsFeatureProvider, times(2)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture(),
                mTaggedDataCaptor.capture());
        assertThat(mActionCategoryCaptor.getAllValues()).containsExactly(
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION);
        assertThat(mActionPackageCaptor.getAllValues()).containsExactly("pkg1", "pkg1");
        assertThat(mTaggedDataCaptor.getAllValues()).containsExactly(
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0));
    }

    @Test
    public void testSuggestionsLogs_ExpandedWithLessThanDefaultShownAfterPause() {
        setupSuggestions(makeSuggestions("pkg1"));

        mDashboardAdapter.onPause();
        mDashboardAdapter.onBindSuggestionConditionHeader(mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();

        verify(mFactory.metricsFeatureProvider, times(3)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture(),
                mTaggedDataCaptor.capture());
        assertThat(mActionCategoryCaptor.getAllValues()).containsExactly(
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION);
        assertThat(mActionPackageCaptor.getAllValues()).containsExactly("pkg1", "pkg1", "pkg1");
        assertThat(mTaggedDataCaptor.getAllValues()).containsExactly(
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0));
    }

    @Test
    public void testSuggestionsLogs_ExpandedWithLessThanDefaultShownAfterPauseAndPausedAgain() {
        setupSuggestions(makeSuggestions("pkg1"));
        mDashboardAdapter.onPause();
        mDashboardAdapter.onBindSuggestionConditionHeader(mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();
        mDashboardAdapter.onPause();

        verify(mFactory.metricsFeatureProvider, times(4)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture(),
                mTaggedDataCaptor.capture());
        assertThat(mActionCategoryCaptor.getAllValues()).containsExactly(
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION);
        assertThat(mActionPackageCaptor.getAllValues()).containsExactly(
                "pkg1", "pkg1", "pkg1", "pkg1");
        assertThat(mTaggedDataCaptor.getAllValues()).containsExactly(
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0));
    }

    @Test
    public void testSuggestionsLogs_SmartSuggestionEnabled() {
        when(mFactory.suggestionsFeatureProvider
                .isSmartSuggestionEnabled(any(Context.class))).thenReturn(true);
        setupSuggestions(makeSuggestions("pkg1"));

        mDashboardAdapter.onBindSuggestionConditionHeader(mSuggestionHolder, mSuggestionHeaderData);
        mSuggestionHolder.itemView.callOnClick();
        mDashboardAdapter.onPause();

        verify(mFactory.metricsFeatureProvider, times(2)).action(
                any(Context.class), mActionCategoryCaptor.capture(),
                mActionPackageCaptor.capture(),
                mTaggedDataCaptor.capture());
        assertThat(mActionCategoryCaptor.getAllValues()).containsExactly(
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION);
        assertThat(mActionPackageCaptor.getAllValues()).containsExactly("pkg1", "pkg1");
        assertThat(mTaggedDataCaptor.getAllValues()).containsExactly(
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 1),
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 1));
    }

    @Test
    public void testSuggestionsLogs_nullSuggestionsList_shouldNotCrash() {
        setupSuggestions(makeSuggestions("pkg1", "pkg2", "pkg3", "pkg4", "pkg5"));
        mDashboardAdapter.onBindSuggestionConditionHeader(mSuggestionHolder, mSuggestionHeaderData);

        // set suggestions to null
        final DashboardData prevData = mDashboardAdapter.mDashboardData;
        mDashboardAdapter.mDashboardData = new DashboardData.Builder(prevData)
                .setSuggestions(null)
                .build();

        mSuggestionHolder.itemView.callOnClick();
        // no crash
    }

    @Test
    public void testSuggestionDismissed_notOnlySuggestion_updateSuggestionOnly() {
        final DashboardAdapter adapter =
                spy(new DashboardAdapter(mContext, null, null, null, null));
        final List<Tile> suggestions = makeSuggestions("pkg1", "pkg2", "pkg3");
        adapter.setCategoriesAndSuggestions(null /* category */, suggestions);

        final RecyclerView data = mock(RecyclerView.class);
        when(data.getResources()).thenReturn(mResources);
        when(data.getContext()).thenReturn(mContext);
        when(mResources.getDisplayMetrics()).thenReturn(mock(DisplayMetrics.class));
        final View itemView = mock(View.class);
        when(itemView.findViewById(R.id.data)).thenReturn(data);
        final DashboardAdapter.SuggestionAndConditionContainerHolder holder =
                new DashboardAdapter.SuggestionAndConditionContainerHolder(itemView);

        adapter.onBindConditionAndSuggestion(
                holder, DashboardAdapter.SUGGESTION_CONDITION_HEADER_POSITION);

        final DashboardData dashboardData = adapter.mDashboardData;
        reset(adapter); // clear interactions tracking

        final Tile suggestionToRemove = suggestions.get(1);
        adapter.onSuggestionDismissed(suggestionToRemove);

        assertThat(adapter.mDashboardData).isEqualTo(dashboardData);
        assertThat(suggestions.size()).isEqualTo(2);
        assertThat(suggestions.contains(suggestionToRemove)).isFalse();
        verify(adapter, never()).notifyDashboardDataChanged(any());
    }

    @Test
    public void testSuggestionDismissed_moreThanTwoSuggestions_defaultMode_shouldNotCrash() {
        final RecyclerView data = new RecyclerView(RuntimeEnvironment.application);
        final View itemView = mock(View.class);
        when(itemView.findViewById(R.id.data)).thenReturn(data);
        final DashboardAdapter.SuggestionAndConditionContainerHolder holder =
                new DashboardAdapter.SuggestionAndConditionContainerHolder(itemView);
        final List<Tile> suggestions =
                makeSuggestions("pkg1", "pkg2", "pkg3", "pkg4");
        final DashboardAdapter adapter = spy(new DashboardAdapter(mContext, null /*savedInstance */,
                null /* conditions */, null /* suggestionParser */, null /* callback */));
        adapter.setCategoriesAndSuggestions(null /* category */, suggestions);
        adapter.onBindConditionAndSuggestion(
                holder, DashboardAdapter.SUGGESTION_CONDITION_HEADER_POSITION);
        // default mode, only displaying 2 suggestions

        adapter.onSuggestionDismissed(suggestions.get(1));

        // verify operations that access the lists will not cause ConcurrentModificationException
        assertThat(holder.data.getAdapter().getItemCount()).isEqualTo(1);
        adapter.setCategoriesAndSuggestions(null /* category */, suggestions);
        // should not crash
    }

    @Test
    public void testSuggestionDismissed_onlySuggestion_updateDashboardData() {
        DashboardAdapter adapter =
                spy(new DashboardAdapter(mContext, null, null, null, null));
        final List<Tile> suggestions = makeSuggestions("pkg1");
        adapter.setCategoriesAndSuggestions(null /* category */, suggestions);
        final DashboardData dashboardData = adapter.mDashboardData;
        reset(adapter); // clear interactions tracking

        adapter.onSuggestionDismissed(suggestions.get(0));

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

        mDashboardAdapter.setCategoriesAndSuggestions(null /* category */, packages);

        verify(mockIcon).setTint(eq(0x89000000));
    }

    @Test
    public void testSetCategories_iconTinted() {
        TypedArray mockTypedArray = mock(TypedArray.class);
        doReturn(mockTypedArray).when(mContext).obtainStyledAttributes(any(int[].class));
        doReturn(0x89000000).when(mockTypedArray).getColor(anyInt(), anyInt());

        final DashboardCategory category = mock(DashboardCategory.class);
        final List<Tile> tiles = new ArrayList<>();
        final Icon mockIcon = mock(Icon.class);
        final Tile tile = new Tile();
        tile.isIconTintable = true;
        tile.icon = mockIcon;
        tiles.add(tile);
        category.tiles = tiles;

        mDashboardAdapter.setCategory(category);

        verify(mockIcon).setTint(eq(0x89000000));
    }

    @Test
    public void testSetCategoriesAndSuggestions_limitSuggestionSize() {
        List<Tile> packages =
                makeSuggestions("pkg1", "pkg2", "pkg3", "pkg4", "pkg5", "pkg6", "pkg7");
        mDashboardAdapter.setCategoriesAndSuggestions(null /* category */, packages);

        assertThat(mDashboardAdapter.mDashboardData.getSuggestions().size())
                .isEqualTo(DashboardAdapter.MAX_SUGGESTION_TO_SHOW);
    }

    @Test
    public void testBindConditionAndSuggestion_shouldSetSuggestionAdapterAndNoCrash() {
        mDashboardAdapter = new DashboardAdapter(mContext, null, null, null, null);
        final List<Tile> suggestions = makeSuggestions("pkg1");
        final DashboardCategory category = mock(DashboardCategory.class);
        final List<Tile> tiles = new ArrayList<>();
        tiles.add(mock(Tile.class));
        category.tiles = tiles;

        mDashboardAdapter.setCategoriesAndSuggestions(category, suggestions);

        final RecyclerView data = mock(RecyclerView.class);
        when(data.getResources()).thenReturn(mResources);
        when(data.getContext()).thenReturn(mContext);
        when(mResources.getDisplayMetrics()).thenReturn(mock(DisplayMetrics.class));
        final View itemView = mock(View.class);
        when(itemView.findViewById(R.id.data)).thenReturn(data);
        final DashboardAdapter.SuggestionAndConditionContainerHolder holder =
                new DashboardAdapter.SuggestionAndConditionContainerHolder(itemView);

        mDashboardAdapter.onBindConditionAndSuggestion(
                holder, DashboardAdapter.SUGGESTION_CONDITION_HEADER_POSITION);

        verify(data).setAdapter(any(SuggestionAdapter.class));
        // should not crash
    }

    @Test
    public void testBindConditionAndSuggestion_emptySuggestion_shouldSetConditionAdpater() {
        final Bundle savedInstance = new Bundle();
        savedInstance.putInt(DashboardAdapter.STATE_SUGGESTION_CONDITION_MODE,
                DashboardData.HEADER_MODE_FULLY_EXPANDED);
        mDashboardAdapter = new DashboardAdapter(mContext, savedInstance, mConditionList,
                null /* SuggestionParser */, null /* SuggestionDismissController.Callback */);

        final List<Tile> suggestions = new ArrayList<>();
        final DashboardCategory category = mock(DashboardCategory.class);
        final List<Tile> tiles = new ArrayList<>();
        tiles.add(mock(Tile.class));
        category.tiles = tiles;
        mDashboardAdapter.setCategoriesAndSuggestions(category, suggestions);

        final RecyclerView data = mock(RecyclerView.class);
        when(data.getResources()).thenReturn(mResources);
        when(data.getContext()).thenReturn(mContext);
        when(mResources.getDisplayMetrics()).thenReturn(mock(DisplayMetrics.class));
        final View itemView = mock(View.class);
        when(itemView.findViewById(R.id.data)).thenReturn(data);
        final DashboardAdapter.SuggestionAndConditionContainerHolder holder =
                new DashboardAdapter.SuggestionAndConditionContainerHolder(itemView);

        mDashboardAdapter.onBindConditionAndSuggestion(
                holder, DashboardAdapter.SUGGESTION_CONDITION_HEADER_POSITION);

        verify(data).setAdapter(any(ConditionAdapter.class));
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
        mDashboardAdapter.setCategoriesAndSuggestions(null /* category */, suggestions);
        final Context context = RuntimeEnvironment.application;
        mSuggestionHolder = new DashboardAdapter.SuggestionAndConditionHeaderHolder(
                LayoutInflater.from(context).inflate(
                        R.layout.suggestion_condition_header, new RelativeLayout(context), true));
    }
}
