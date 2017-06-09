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

import android.content.Context;
import android.graphics.drawable.Icon;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.DashboardAdapter;
import com.android.settingslib.drawer.Tile;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SuggestionAdapterTest {
    @Mock
    private Tile mSuggestion1;
    @Mock
    private Tile mSuggestion2;

    private Context mContext;
    private SuggestionAdapter mSuggestionAdapter;
    private List<Tile> mOneSuggestion;
    private List<Tile> mTwoSuggestions;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mSuggestion1.title = "Test Suggestion 1";
        mSuggestion1.icon = mock(Icon.class);
        mSuggestion2.title = "Test Suggestion 2";
        mSuggestion2.icon = mock(Icon.class);
        mOneSuggestion = new ArrayList<>();
        mOneSuggestion.add(mSuggestion1);
        mTwoSuggestions = new ArrayList<>();
        mTwoSuggestions.add(mSuggestion1);
        mTwoSuggestions.add(mSuggestion2);
    }

    @Test
    public void getItemCount_shouldReturnListSize() {
        mSuggestionAdapter = new SuggestionAdapter(mContext, mOneSuggestion, new ArrayList<>());
        assertThat(mSuggestionAdapter.getItemCount()).isEqualTo(1);

        mSuggestionAdapter = new SuggestionAdapter(mContext, mTwoSuggestions, new ArrayList<>());
        assertThat(mSuggestionAdapter.getItemCount()).isEqualTo(2);
    }

    @Test
    public void getItemViewType_shouldReturnSuggestionTile() {
        mSuggestionAdapter = new SuggestionAdapter(mContext, mOneSuggestion, new ArrayList<>());
        assertThat(mSuggestionAdapter.getItemViewType(0))
            .isEqualTo(R.layout.suggestion_tile_new_ui);
    }

    @Test
    public void onBindViewHolder_shouldSetListener() {
        final View view = spy(LayoutInflater.from(mContext).inflate(
            R.layout.suggestion_tile_new_ui, new LinearLayout(mContext), true));
        final DashboardAdapter.DashboardItemHolder viewHolder =
            new DashboardAdapter.DashboardItemHolder(view);
        mSuggestionAdapter = new SuggestionAdapter(mContext, mOneSuggestion, new ArrayList<>());

        mSuggestionAdapter.onBindViewHolder(viewHolder, 0);

        verify(view).setOnClickListener(any(View.OnClickListener.class));
    }

}
