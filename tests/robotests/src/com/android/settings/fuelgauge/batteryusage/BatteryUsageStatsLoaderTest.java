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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.BatteryStatsManager;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatteryUsageStatsLoaderTest {
    private Context mContext;
    @Mock private BatteryStatsManager mBatteryStatsManager;
    @Mock private BatteryUsageStats mBatteryUsageStats;
    @Captor private ArgumentCaptor<BatteryUsageStatsQuery> mUsageStatsQueryCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mBatteryStatsManager)
                .when(mContext)
                .getSystemService(Context.BATTERY_STATS_SERVICE);
    }

    @Test
    public void testLoadInBackground_loadWithoutHistory() {
        BatteryUsageStatsLoader loader =
                new BatteryUsageStatsLoader(mContext, /* includeBatteryHistory */ false);

        when(mBatteryStatsManager.getBatteryUsageStats(mUsageStatsQueryCaptor.capture()))
                .thenReturn(mBatteryUsageStats);

        loader.loadInBackground();

        final int queryFlags = mUsageStatsQueryCaptor.getValue().getFlags();
        assertThat(queryFlags & BatteryUsageStatsQuery.FLAG_BATTERY_USAGE_STATS_INCLUDE_HISTORY)
                .isEqualTo(0);
    }

    @Test
    public void testLoadInBackground_loadWithHistory() {
        BatteryUsageStatsLoader loader =
                new BatteryUsageStatsLoader(mContext, /* includeBatteryHistory */ true);

        when(mBatteryStatsManager.getBatteryUsageStats(mUsageStatsQueryCaptor.capture()))
                .thenReturn(mBatteryUsageStats);

        loader.loadInBackground();

        final int queryFlags = mUsageStatsQueryCaptor.getValue().getFlags();
        assertThat(queryFlags & BatteryUsageStatsQuery.FLAG_BATTERY_USAGE_STATS_INCLUDE_HISTORY)
                .isNotEqualTo(0);
    }
}
