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

package com.android.settings.dashboard.suggestions;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.suggestions.SuggestionParser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SuggestionDismissControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RecyclerView mRecyclerView;
    @Mock
    private SuggestionParser mSuggestionParser;
    @Mock
    private SuggestionDismissController.Callback mCallback;

    private FakeFeatureFactory mFactory;
    private SuggestionDismissController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);

        when(mRecyclerView.getResources().getDimension(anyInt())).thenReturn(50F);

        mController = new SuggestionDismissController(mContext, mRecyclerView,
                mSuggestionParser, mCallback);
    }

    @Test
    public void onMove_alwaysReturnTrue() {
        assertThat(mController.onMove(null, null, null)).isTrue();
    }

    @Test
    public void getSwipeDirs_isSuggestionTile_shouldReturnDirection() {
        final RecyclerView.ViewHolder vh = mock(RecyclerView.ViewHolder.class);
        when(vh.getItemViewType()).thenReturn(R.layout.suggestion_tile);

        assertThat(mController.getSwipeDirs(mRecyclerView, vh))
                .isEqualTo(ItemTouchHelper.START | ItemTouchHelper.END);
    }

    @Test
    public void getSwipeDirs_isSuggestionTileCard_shouldReturnDirection() {
        final RecyclerView.ViewHolder vh = mock(RecyclerView.ViewHolder.class);
        when(vh.getItemViewType()).thenReturn(R.layout.suggestion_tile_remote_container);

        assertThat(mController.getSwipeDirs(mRecyclerView, vh))
                .isEqualTo(ItemTouchHelper.START | ItemTouchHelper.END);
    }

    @Test
    public void getSwipeDirs_isNotSuggestionTile_shouldReturn0() {
        final RecyclerView.ViewHolder vh = mock(RecyclerView.ViewHolder.class);
        when(vh.getItemViewType()).thenReturn(R.layout.condition_tile);

        assertThat(mController.getSwipeDirs(mRecyclerView, vh))
                .isEqualTo(0);
    }

    @Test
    public void onSwiped_shouldTriggerDismissSuggestion() {
        final RecyclerView.ViewHolder vh = mock(RecyclerView.ViewHolder.class);

        mController.onSwiped(vh, ItemTouchHelper.START);

        verify(mFactory.suggestionsFeatureProvider).dismissSuggestion(
                eq(mContext), eq(mSuggestionParser), nullable(Tile.class));
        verify(mCallback).onSuggestionDismissed(nullable(Tile.class));
    }
}
