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
 * limitations under the License
 */
package com.android.settings.datausage;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.NetworkStatsHistory;
import android.net.NetworkStatsHistory.Entry;
import android.util.SparseIntArray;
import com.android.settings.graph.UsageView;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class ChartDataUsagePreferenceTest {

    private static final long MILLIS_IN_ONE_HOUR = 60 * 60 * 1000;
    private static final long MILLIS_IN_ONE_DAY = 24 * MILLIS_IN_ONE_HOUR;
    private static final long TIMESTAMP_NOW = Integer.MAX_VALUE;

    private NetworkStatsHistory mNetworkStatsHistory;
    private Context mContext;
    private ChartDataUsagePreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPreference = new ChartDataUsagePreference(mContext, null);
        mNetworkStatsHistory = spy(new NetworkStatsHistory(MILLIS_IN_ONE_HOUR, 10));
        addTestNetworkEntries();
        mPreference.setNetworkStats(mNetworkStatsHistory);
    }

    @Test
    public void calcPoints_notStartOfData_shouldAddDataPointsOnly() {
        final long start = TIMESTAMP_NOW - 20 * MILLIS_IN_ONE_DAY;
        final long end = TIMESTAMP_NOW - 5 * MILLIS_IN_ONE_DAY;
        mPreference.setVisibleRange(start, end);
        when(mNetworkStatsHistory.getIndexAfter(start)).thenReturn(2);
        when(mNetworkStatsHistory.getIndexAfter(end)).thenReturn(7);
        final UsageView usageView = mock(UsageView.class);
        final ArgumentCaptor<SparseIntArray> pointsCaptor =
            ArgumentCaptor.forClass(SparseIntArray.class);

        mPreference.calcPoints(usageView);

        verify(usageView).addPath(pointsCaptor.capture());
        SparseIntArray points = pointsCaptor.getValue();
        // the point should be normal usage data
        assertThat(points.valueAt(1)).isNotEqualTo(-1);
    }

    @Test
    public void calcPoints_startOfData_shouldIndicateStartOfData() {
        final long start = TIMESTAMP_NOW - 20 * MILLIS_IN_ONE_DAY;
        final long end = TIMESTAMP_NOW - 5 * MILLIS_IN_ONE_DAY;
        mPreference.setVisibleRange(start, end);
        when(mNetworkStatsHistory.getIndexAfter(start)).thenReturn(0);
        when(mNetworkStatsHistory.getIndexAfter(end)).thenReturn(5);
        final UsageView usageView = mock(UsageView.class);
        final ArgumentCaptor<SparseIntArray> pointsCaptor =
            ArgumentCaptor.forClass(SparseIntArray.class);

        mPreference.calcPoints(usageView);

        verify(usageView).addPath(pointsCaptor.capture());
        SparseIntArray points = pointsCaptor.getValue();
        // indicator that no data is available
        assertThat(points.keyAt(1)).isEqualTo(points.keyAt(2) - 1);
        assertThat(points.valueAt(1)).isEqualTo(-1);
    }

    private void addTestNetworkEntries() {
        // create 10 arbitary network data
        mNetworkStatsHistory.setValues(0, createEntry(1521583200000L, 743823454L, 16574289L));
        mNetworkStatsHistory.setValues(1, createEntry(1521586800000L, 64396L, 160364L));
        mNetworkStatsHistory.setValues(2, createEntry(1521590400000L, 2832L, 5299L));
        mNetworkStatsHistory.setValues(3, createEntry(1521655200000L, 83849690L, 3558238L));
        mNetworkStatsHistory.setValues(4, createEntry(1521658800000L, 1883657L, 353330L));
        mNetworkStatsHistory.setValues(5, createEntry(1521662400000L, 705259L, 279065L));
        mNetworkStatsHistory.setValues(6, createEntry(1521666000000L, 216169L, 155302L));
        mNetworkStatsHistory.setValues(7, createEntry(1521669600000L, 6069175L, 427581L));
        mNetworkStatsHistory.setValues(8, createEntry(1521673200000L, 120389L, 110807L));
        mNetworkStatsHistory.setValues(9, createEntry(1521676800000L, 29947L, 73257L));
    }

    /**
     * Create a network entry to be used to calculate the usage chart. In the calculation, we only
     * need bucketStart, total bytes (rx + tx), and bucketDuration (which is set when we create
     * the NetworkStatsHistory object). Other fields are ignored, so we don't initialize here.
     * @param start the timestamp when this entry begins
     * @param rx the total number of received bytes
     * @param tx the total number of transmitted bytes
     * @return the network entry with the corresponding start time and data usage
     */
    private Entry createEntry(long start, long rx, long tx) {
        Entry entry = new Entry();
        entry.bucketStart = start;
        entry.rxBytes = rx;
        entry.txBytes = tx;
        return entry;
    }
}
