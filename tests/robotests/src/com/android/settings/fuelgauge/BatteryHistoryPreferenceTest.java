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
package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.widget.UsageView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatteryHistoryPreferenceTest {

    private static final String TEST_STRING = "test";
    @Mock
    private PreferenceViewHolder mViewHolder;
    @Mock
    private BatteryInfo mBatteryInfo;
    @Mock
    private TextView mTextView;
    @Mock
    private UsageView mUsageView;
    @Mock
    private View mLabelView;
    private BatteryHistoryPreference mBatteryHistoryPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context context = RuntimeEnvironment.application;
        final View itemView =
                LayoutInflater.from(context).inflate(R.layout.battery_usage_graph, null);

        mBatteryHistoryPreference = new BatteryHistoryPreference(context, null);
        mBatteryHistoryPreference.mBatteryInfo = mBatteryInfo;
        mViewHolder = spy(PreferenceViewHolder.createInstanceForTests(itemView));
        when(mViewHolder.findViewById(R.id.battery_usage)).thenReturn(mUsageView);
        when(mViewHolder.findViewById(R.id.charge)).thenReturn(mTextView);
        when(mUsageView.findViewById(anyInt())).thenReturn(mLabelView);
    }

    @Test
    public void testOnBindViewHolder_updateBatteryUsage() {
        mBatteryHistoryPreference.onBindViewHolder(mViewHolder);

        verify(mViewHolder).findViewById(R.id.battery_usage);
        verify(mTextView).setText(nullable(String.class));
        verify(mBatteryInfo).bindHistory(mUsageView);
    }

    @Test
    public void testSetBottomSummary_updatesBottomSummaryTextIfSet() {
        mBatteryHistoryPreference.setBottomSummary(TEST_STRING);
        mBatteryHistoryPreference.onBindViewHolder(mViewHolder);

        TextView view = (TextView) mViewHolder.findViewById(R.id.bottom_summary);
        assertThat(view.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.getText()).isEqualTo(TEST_STRING);
        assertThat(mBatteryHistoryPreference.hideSummary).isFalse();
    }

    @Test
    public void testSetBottomSummary_leavesBottomSummaryTextBlankIfNotSet() {
        mBatteryHistoryPreference.hideBottomSummary();
        mBatteryHistoryPreference.onBindViewHolder(mViewHolder);

        TextView view = (TextView) mViewHolder.findViewById(R.id.bottom_summary);
        assertThat(view.getVisibility()).isEqualTo(View.GONE);
        assertThat(view.getText()).isEqualTo("");
        assertThat(mBatteryHistoryPreference.hideSummary).isTrue();
    }
}
