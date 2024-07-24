/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.fuelgauge.batteryusage;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatteryHistoryPreferenceTest {
    @Mock private PreferenceViewHolder mViewHolder;
    @Mock private TextView mTextView;
    @Mock private BatteryChartView mDailyChartView;
    @Mock private BatteryChartView mHourlyChartView;
    private BatteryHistoryPreference mBatteryHistoryPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context context = RuntimeEnvironment.application;
        final View itemView =
                LayoutInflater.from(context).inflate(R.layout.battery_chart_graph, null);

        mBatteryHistoryPreference = new BatteryHistoryPreference(context, null);
        mViewHolder = spy(PreferenceViewHolder.createInstanceForTests(itemView));
        when(mViewHolder.findViewById(R.id.daily_battery_chart)).thenReturn(mDailyChartView);
        when(mViewHolder.findViewById(R.id.hourly_battery_chart)).thenReturn(mHourlyChartView);
        when(mViewHolder.findViewById(R.id.companion_text)).thenReturn(mTextView);
    }

    @Test
    public void testOnBindViewHolder_updateBatteryUsage() {
        mBatteryHistoryPreference.onBindViewHolder(mViewHolder);

        verify(mViewHolder).findViewById(R.id.daily_battery_chart);
        verify(mDailyChartView).setCompanionTextView(mTextView);
        verify(mViewHolder).findViewById(R.id.hourly_battery_chart);
        verify(mHourlyChartView).setCompanionTextView(mTextView);
    }
}
