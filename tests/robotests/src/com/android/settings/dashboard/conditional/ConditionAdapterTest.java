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
package com.android.settings.dashboard.conditional;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.DashboardAdapter;
import com.android.settings.dashboard.DashboardData;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ConditionAdapterTest {
    @Mock
    private Condition mCondition1;
    @Mock
    private Condition mCondition2;

    private Context mContext;
    private ConditionAdapter mConditionAdapter;
    private List<Condition> mOneCondition;
    private List<Condition> mTwoConditions;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        final CharSequence[] actions = new CharSequence[2];
        when(mCondition1.getActions()).thenReturn(actions);
        when(mCondition1.shouldShow()).thenReturn(true);
        mOneCondition = new ArrayList<>();
        mOneCondition.add(mCondition1);
        mTwoConditions = new ArrayList<>();
        mTwoConditions.add(mCondition1);
        mTwoConditions.add(mCondition2);
    }

    @Test
    public void getItemCount_notFullyExpanded_shouldReturn0() {
        mConditionAdapter = new ConditionAdapter(
            mContext, mOneCondition, DashboardData.HEADER_MODE_DEFAULT);
        assertThat(mConditionAdapter.getItemCount()).isEqualTo(0);

        mConditionAdapter = new ConditionAdapter(
            mContext, mOneCondition, DashboardData.HEADER_MODE_SUGGESTION_EXPANDED);
        assertThat(mConditionAdapter.getItemCount()).isEqualTo(0);

        mConditionAdapter = new ConditionAdapter(
            mContext, mOneCondition, DashboardData.HEADER_MODE_COLLAPSED);
        assertThat(mConditionAdapter.getItemCount()).isEqualTo(0);
    }

    @Test
    public void getItemCount_fullyExpanded_shouldReturnListSize() {
        mConditionAdapter = new ConditionAdapter(
            mContext, mOneCondition, DashboardData.HEADER_MODE_FULLY_EXPANDED);
        assertThat(mConditionAdapter.getItemCount()).isEqualTo(1);

        mConditionAdapter = new ConditionAdapter(
            mContext, mTwoConditions, DashboardData.HEADER_MODE_FULLY_EXPANDED);
        assertThat(mConditionAdapter.getItemCount()).isEqualTo(2);
    }

    @Test
    public void getItemViewType_shouldReturnConditionTile() {
        mConditionAdapter = new ConditionAdapter(
            mContext, mTwoConditions, DashboardData.HEADER_MODE_FULLY_EXPANDED);
        assertThat(mConditionAdapter.getItemViewType(0)).isEqualTo(R.layout.condition_tile);
    }

    @Test
    public void onBindViewHolder_shouldSetListener() {
        final View view = LayoutInflater.from(mContext).inflate(
            R.layout.condition_tile, new LinearLayout(mContext), true);
        final DashboardAdapter.DashboardItemHolder viewHolder =
            new DashboardAdapter.DashboardItemHolder(view);
        mConditionAdapter = new ConditionAdapter(
            mContext, mOneCondition, DashboardData.HEADER_MODE_SUGGESTION_EXPANDED);

        mConditionAdapter.onBindViewHolder(viewHolder, 0);
        final View card = view.findViewById(R.id.content);
        assertThat(card.hasOnClickListeners()).isTrue();
    }

    @Test
    public void viewClick_shouldInvokeConditionPrimaryClick() {
        final View view = LayoutInflater.from(mContext).inflate(
            R.layout.condition_tile, new LinearLayout(mContext), true);
        final DashboardAdapter.DashboardItemHolder viewHolder =
            new DashboardAdapter.DashboardItemHolder(view);
        mConditionAdapter = new ConditionAdapter(
            mContext, mOneCondition, DashboardData.HEADER_MODE_SUGGESTION_EXPANDED);

        mConditionAdapter.onBindViewHolder(viewHolder, 0);
        final View card = view.findViewById(R.id.content);
        card.performClick();
        verify(mCondition1).onPrimaryClick();
    }

    @Test
    public void onSwiped_nullCondition_shouldNotCrash() {
        final RecyclerView recyclerView = new RecyclerView(mContext);
        final View view = LayoutInflater.from(mContext).inflate(
                R.layout.condition_tile, new LinearLayout(mContext), true);
        final DashboardAdapter.DashboardItemHolder viewHolder =
                new DashboardAdapter.DashboardItemHolder(view);
        mConditionAdapter = new ConditionAdapter(
                mContext, mOneCondition, DashboardData.HEADER_MODE_SUGGESTION_EXPANDED);
        mConditionAdapter.addDismissHandling(recyclerView);

        // do not bind viewholder to simulate the null condition scenario
        mConditionAdapter.mSwipeCallback.onSwiped(viewHolder, 0);
        // no crash
    }

}
