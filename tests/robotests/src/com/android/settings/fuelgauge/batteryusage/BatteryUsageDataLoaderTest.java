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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public final class BatteryUsageDataLoaderTest {

    private Context mContext;
    @Mock
    private ContentResolver mMockContentResolver;
    @Mock
    private BatteryStatsManager mBatteryStatsManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private BatteryUsageStats mBatteryUsageStats;
    @Mock
    private BatteryEntry mMockBatteryEntry;
    @Captor
    private ArgumentCaptor<BatteryUsageStatsQuery> mStatsQueryCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mBatteryStatsManager).when(mContext).getSystemService(
                Context.BATTERY_STATS_SERVICE);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(mMockContentResolver).when(mContext).getContentResolver();
        doReturn(new Intent()).when(mContext).registerReceiver(any(), any());
    }

    @Test
    public void loadUsageData_loadUsageDataWithHistory() {
        final List<BatteryEntry> batteryEntryList = new ArrayList<>();
        batteryEntryList.add(mMockBatteryEntry);
        when(mBatteryStatsManager.getBatteryUsageStats(mStatsQueryCaptor.capture()))
                .thenReturn(mBatteryUsageStats);
        BatteryUsageDataLoader.sFakeBatteryEntryListSupplier = () -> batteryEntryList;

        BatteryUsageDataLoader.loadUsageData(mContext);

        final int queryFlags = mStatsQueryCaptor.getValue().getFlags();
        assertThat(queryFlags
                & BatteryUsageStatsQuery.FLAG_BATTERY_USAGE_STATS_INCLUDE_HISTORY)
                .isNotEqualTo(0);
        verify(mMockContentResolver).insert(any(), any());
    }

    @Test
    public void loadUsageData_nullBatteryEntryList_insertFakeDataIntoProvider() {
        when(mBatteryStatsManager.getBatteryUsageStats(mStatsQueryCaptor.capture()))
                .thenReturn(mBatteryUsageStats);
        BatteryUsageDataLoader.sFakeBatteryEntryListSupplier = () -> null;

        BatteryUsageDataLoader.loadUsageData(mContext);

        verify(mMockContentResolver).insert(any(), any());
    }

    @Test
    public void loadUsageData_emptyBatteryEntryList_insertFakeDataIntoProvider() {
        when(mBatteryStatsManager.getBatteryUsageStats(mStatsQueryCaptor.capture()))
                .thenReturn(mBatteryUsageStats);
        BatteryUsageDataLoader.sFakeBatteryEntryListSupplier = () -> new ArrayList<BatteryEntry>();

        BatteryUsageDataLoader.loadUsageData(mContext);

        verify(mMockContentResolver).insert(any(), any());
    }
}
