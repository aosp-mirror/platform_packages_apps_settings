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
package com.android.settings.homepage.conditional;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardAdapter;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class ConditionAdapterTest {

    @Mock
    private ConditionalCard mCondition1;
    @Mock
    private ConditionalCard mCondition2;
    @Mock
    private ConditionManager mConditionManager;

    private Context mContext;
    private ConditionAdapter mConditionAdapter;
    private List<ConditionalCard> mOneCondition;
    private List<ConditionalCard> mTwoConditions;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        final CharSequence action = "action";
        when(mCondition1.getActionText()).thenReturn(action);
        mOneCondition = new ArrayList<>();
        mOneCondition.add(mCondition1);
        mTwoConditions = new ArrayList<>();
        mTwoConditions.add(mCondition1);
        mTwoConditions.add(mCondition2);
    }

    @Test
    public void getItemCount_notExpanded_shouldReturn0() {
        mConditionAdapter = new ConditionAdapter(mContext, mConditionManager, mOneCondition, false);
        assertThat(mConditionAdapter.getItemCount()).isEqualTo(0);
    }

    @Test
    public void getItemCount_expanded_shouldReturnListSize() {
        mConditionAdapter = new ConditionAdapter(mContext, mConditionManager, mOneCondition, true);
        assertThat(mConditionAdapter.getItemCount()).isEqualTo(1);

        mConditionAdapter = new ConditionAdapter(mContext, mConditionManager, mTwoConditions, true);
        assertThat(mConditionAdapter.getItemCount()).isEqualTo(2);
    }

    @Test
    public void getItemViewType_shouldReturnConditionTile() {
        mConditionAdapter = new ConditionAdapter(mContext, mConditionManager, mTwoConditions, true);
        assertThat(mConditionAdapter.getItemViewType(0)).isEqualTo(R.layout.condition_tile);
    }

    @Test
    public void onBindViewHolder_shouldSetListener() {
        final View view = LayoutInflater.from(mContext)
                .inflate(R.layout.condition_tile, new LinearLayout(mContext), true);
        final DashboardAdapter.DashboardItemHolder viewHolder =
                new DashboardAdapter.DashboardItemHolder(view);
        mConditionAdapter = new ConditionAdapter(mContext, mConditionManager, mOneCondition, true);

        mConditionAdapter.onBindViewHolder(viewHolder, 0);
        final View card = view.findViewById(R.id.content);
        assertThat(card).isNotNull();
        assertThat(card.hasOnClickListeners()).isTrue();
    }

    @Test
    public void viewClick_shouldInvokeConditionPrimaryClick() {
        final View view = LayoutInflater.from(mContext)
                .inflate(R.layout.condition_tile, new LinearLayout(mContext), true);
        final DashboardAdapter.DashboardItemHolder viewHolder =
                new DashboardAdapter.DashboardItemHolder(view);
        mConditionAdapter = new ConditionAdapter(mContext, mConditionManager, mOneCondition, true);

        mConditionAdapter.onBindViewHolder(viewHolder, 0);
        final View card = view.findViewById(R.id.content);
        assertThat(card).isNotNull();
        card.performClick();
        verify(mConditionManager).onPrimaryClick(any(Context.class), anyLong());
    }
}
