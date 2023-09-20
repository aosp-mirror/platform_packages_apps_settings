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

package com.android.settings.widget;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;

import com.google.android.material.appbar.AppBarLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class HighlightablePreferenceGroupAdapterTest {

    private static final String TEST_KEY = "key";

    @Mock
    private View mRoot;
    @Mock
    private PreferenceCategory mPreferenceCatetory;
    @Mock
    private SettingsPreferenceFragment mFragment;

    private Context mContext;
    private HighlightablePreferenceGroupAdapter mAdapter;
    private PreferenceViewHolder mViewHolder;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new Preference(mContext);
        mPreference.setKey(TEST_KEY);
        when(mPreferenceCatetory.getContext()).thenReturn(mContext);
        mAdapter = spy(new HighlightablePreferenceGroupAdapter(mPreferenceCatetory, TEST_KEY,
                false /* highlighted*/));
        when(mAdapter.getItem(anyInt())).thenReturn(mPreference);
        mViewHolder = PreferenceViewHolder.createInstanceForTests(
                View.inflate(mContext, androidx.preference.R.layout.preference, null));
    }

    @Test
    public void requestHighlight_hasKey_notHighlightedBefore_shouldRequest() {
        when(mAdapter.getPreferenceAdapterPosition(anyString())).thenReturn(1);
        mAdapter.requestHighlight(mRoot, mock(RecyclerView.class), mock(AppBarLayout.class));

        verify(mRoot).postDelayed(any(),
                eq(HighlightablePreferenceGroupAdapter.DELAY_COLLAPSE_DURATION_MILLIS));
        verify(mRoot).postDelayed(any(),
                eq(HighlightablePreferenceGroupAdapter.DELAY_HIGHLIGHT_DURATION_MILLIS));
    }

    @Test
    public void requestHighlight_noKey_highlightedBefore_noRecyclerView_shouldNotRequest() {
        ReflectionHelpers.setField(mAdapter, "mHighlightKey", null);
        ReflectionHelpers.setField(mAdapter, "mHighlightRequested", false);
        mAdapter.requestHighlight(mRoot, mock(RecyclerView.class),  mock(AppBarLayout.class));

        ReflectionHelpers.setField(mAdapter, "mHighlightKey", TEST_KEY);
        ReflectionHelpers.setField(mAdapter, "mHighlightRequested", true);
        mAdapter.requestHighlight(mRoot, mock(RecyclerView.class), mock(AppBarLayout.class));

        ReflectionHelpers.setField(mAdapter, "mHighlightKey", TEST_KEY);
        ReflectionHelpers.setField(mAdapter, "mHighlightRequested", false);
        mAdapter.requestHighlight(mRoot, null /* recyclerView */,  mock(AppBarLayout.class));

        verifyNoInteractions(mRoot);
    }

    @Test
    public void adjustInitialExpandedChildCount_invalidInput_shouldNotAdjust() {
        HighlightablePreferenceGroupAdapter.adjustInitialExpandedChildCount(null /* host */);
        HighlightablePreferenceGroupAdapter.adjustInitialExpandedChildCount(mFragment);
        final Bundle args = new Bundle();
        when(mFragment.getArguments()).thenReturn(args);
        HighlightablePreferenceGroupAdapter.adjustInitialExpandedChildCount(mFragment);
        final PreferenceScreen screen = mock(PreferenceScreen.class);
        when(mFragment.getArguments()).thenReturn(null);
        when(mFragment.getPreferenceScreen()).thenReturn(screen);
        HighlightablePreferenceGroupAdapter.adjustInitialExpandedChildCount(mFragment);
        verifyNoInteractions(screen);
    }

    @Test
    public void adjustInitialExpandedChildCount_hasHighlightKey_shouldExpandAllChildren() {
        final Bundle args = new Bundle();
        when(mFragment.getArguments()).thenReturn(args);
        args.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, "testkey");
        final PreferenceScreen screen = mock(PreferenceScreen.class);
        when(mFragment.getPreferenceScreen()).thenReturn(screen);
        HighlightablePreferenceGroupAdapter.adjustInitialExpandedChildCount(mFragment);

        verify(screen).setInitialExpandedChildrenCount(Integer.MAX_VALUE);
    }

    @Test
    public void adjustInitialExpandedChildCount_noKeyOrChildCountOverride_shouldDoNothing() {
        final Bundle args = new Bundle();
        when(mFragment.getArguments()).thenReturn(args);
        when(mFragment.getInitialExpandedChildCount()).thenReturn(-1);
        final PreferenceScreen screen = mock(PreferenceScreen.class);
        when(mFragment.getPreferenceScreen()).thenReturn(screen);
        HighlightablePreferenceGroupAdapter.adjustInitialExpandedChildCount(mFragment);

        verify(mFragment).getInitialExpandedChildCount();
        verifyNoInteractions(screen);
    }

    @Test
    public void adjustInitialExpandedChildCount_hasCountOverride_shouldDoNothing() {
        when(mFragment.getInitialExpandedChildCount()).thenReturn(10);
        final PreferenceScreen screen = mock(PreferenceScreen.class);
        when(mFragment.getPreferenceScreen()).thenReturn(screen);
        HighlightablePreferenceGroupAdapter.adjustInitialExpandedChildCount(mFragment);

        verify(mFragment).getInitialExpandedChildCount();

        verify(screen).setInitialExpandedChildrenCount(10);
    }

    @Test
    public void updateBackground_notHighlightedRow_shouldNotSetHighlightedTag() {
        ReflectionHelpers.setField(mAdapter, "mHighlightPosition", 10);

        mAdapter.updateBackground(mViewHolder, 0);

        assertThat(mViewHolder.itemView.getTag(R.id.preference_highlighted)).isNull();
    }

    /**
     * When background is being updated, we also request the a11y focus on the preference
     */
    @Test
    public void updateBackground_shouldRequestAccessibilityFocus() {
        View viewItem = mock(View.class);
        mViewHolder = PreferenceViewHolder.createInstanceForTests(viewItem);
        ReflectionHelpers.setField(mAdapter, "mHighlightPosition", 10);

        mAdapter.updateBackground(mViewHolder, 10);

        verify(viewItem).requestAccessibilityFocus();
    }

    @Test
    public void updateBackground_highlight_shouldAnimateBackgroundAndSetHighlightedTag() {
        ReflectionHelpers.setField(mAdapter, "mHighlightPosition", 10);
        assertThat(mAdapter.mFadeInAnimated).isFalse();

        mAdapter.updateBackground(mViewHolder, 10);

        assertThat(mAdapter.mFadeInAnimated).isTrue();
        assertThat(mViewHolder.itemView.getBackground()).isInstanceOf(ColorDrawable.class);
        assertThat(mViewHolder.itemView.getTag(R.id.preference_highlighted)).isEqualTo(true);
        verify(mAdapter).requestRemoveHighlightDelayed(mViewHolder);
    }

    @Test
    public void updateBackgroundTwice_highlight_shouldAnimateOnce() {
        ReflectionHelpers.setField(mAdapter, "mHighlightPosition", 10);
        ReflectionHelpers.setField(mViewHolder, "itemView", spy(mViewHolder.itemView));
        assertThat(mAdapter.mFadeInAnimated).isFalse();
        mAdapter.updateBackground(mViewHolder, 10);
        // mFadeInAnimated change from false to true - indicating background change is scheduled
        // through animation.
        assertThat(mAdapter.mFadeInAnimated).isTrue();
        // remove highlight should be requested.
        verify(mAdapter).requestRemoveHighlightDelayed(mViewHolder);

        ReflectionHelpers.setField(mAdapter, "mHighlightPosition", 10);
        mAdapter.updateBackground(mViewHolder, 10);
        // only sets background color once - if it's animation this would be called many times
        verify(mViewHolder.itemView).setBackgroundColor(mAdapter.mHighlightColor);
        // remove highlight should be requested.
        verify(mAdapter, times(2)).requestRemoveHighlightDelayed(mViewHolder);
    }

    @Test
    public void updateBackground_reuseHighlightedRowForNormalRow_shouldResetBackgroundAndTag() {
        ReflectionHelpers.setField(mAdapter, "mHighlightPosition", 10);
        mViewHolder.itemView.setTag(R.id.preference_highlighted, true);

        mAdapter.updateBackground(mViewHolder, 0);

        assertThat(mViewHolder.itemView.getBackground()).isNotInstanceOf(ColorDrawable.class);
        assertThat(mViewHolder.itemView.getTag(R.id.preference_highlighted)).isEqualTo(false);
    }
}
