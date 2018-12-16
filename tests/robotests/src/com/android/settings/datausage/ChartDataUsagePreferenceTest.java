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
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.util.SparseIntArray;

import com.android.settings.widget.UsageView;
import com.android.settingslib.net.NetworkCycleChartData;
import com.android.settingslib.net.NetworkCycleData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class ChartDataUsagePreferenceTest {

    // Test cycle start date, 20 Mar 2018 22:00: GMT
    private static final long TIMESTAMP_START = 1521583200000L;
    // Test bucket end date, 22 Mar 2018 00:00:00
    private static final long TIMESTAMP_END = 1521676800000L;

    private List<NetworkCycleData> mNetworkCycleData;
    private NetworkCycleChartData mNetworkCycleChartData;
    private Context mContext;
    private ChartDataUsagePreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPreference = new ChartDataUsagePreference(mContext, null);
    }

    @Test
    public void calcPoints_dataAvailableFromCycleStart_shouldAddDataPointsOnly() {
        final UsageView usageView = mock(UsageView.class);
        final ArgumentCaptor<SparseIntArray> pointsCaptor =
                ArgumentCaptor.forClass(SparseIntArray.class);
        createTestNetworkData();
        mPreference.setNetworkCycleData(mNetworkCycleChartData);

        mPreference.calcPoints(usageView, mNetworkCycleData.subList(0, 5));

        verify(usageView).addPath(pointsCaptor.capture());
        final SparseIntArray points = pointsCaptor.getValue();
        // the point should be normal usage data
        assertThat(points.valueAt(1)).isNotEqualTo(-1);
    }

    @Test
    public void calcPoints_dataNotAvailableAtCycleStart_shouldIndicateStartOfData() {
        final UsageView usageView = mock(UsageView.class);
        final ArgumentCaptor<SparseIntArray> pointsCaptor =
                ArgumentCaptor.forClass(SparseIntArray.class);
        createTestNetworkData();
        mPreference.setNetworkCycleData(mNetworkCycleChartData);

        mPreference.calcPoints(usageView, mNetworkCycleData.subList(2, 7));

        verify(usageView).addPath(pointsCaptor.capture());
        final SparseIntArray points = pointsCaptor.getValue();
        // indicator that no data is available
        assertThat(points.keyAt(1)).isEqualTo(points.keyAt(2) - 1);
        assertThat(points.valueAt(1)).isEqualTo(-1);
    }

    @Test
    public void calcPoints_shouldNotDrawPointForFutureDate() {
        final UsageView usageView = mock(UsageView.class);
        final ArgumentCaptor<SparseIntArray> pointsCaptor =
            ArgumentCaptor.forClass(SparseIntArray.class);
        final long tonight = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(12);
        mNetworkCycleData = new ArrayList<>();
        // add test usage data for last 5 days
        mNetworkCycleData.add(createNetworkCycleData(
            tonight - TimeUnit.DAYS.toMillis(5), tonight - TimeUnit.DAYS.toMillis(4), 743823454L));
        mNetworkCycleData.add(createNetworkCycleData(
            tonight - TimeUnit.DAYS.toMillis(4), tonight - TimeUnit.DAYS.toMillis(3), 64396L));
        mNetworkCycleData.add(createNetworkCycleData(
            tonight - TimeUnit.DAYS.toMillis(3), tonight - TimeUnit.DAYS.toMillis(2), 2832L));
        mNetworkCycleData.add(createNetworkCycleData(
            tonight - TimeUnit.DAYS.toMillis(2), tonight - TimeUnit.DAYS.toMillis(1), 83849690L));
        mNetworkCycleData.add(createNetworkCycleData(
            tonight - TimeUnit.DAYS.toMillis(1), tonight, 1883657L));
        // add dummy usage data for next 5 days
        mNetworkCycleData.add(createNetworkCycleData(
            tonight, tonight + TimeUnit.DAYS.toMillis(1), 0L));
        mNetworkCycleData.add(createNetworkCycleData(
            tonight + TimeUnit.DAYS.toMillis(1), tonight + TimeUnit.DAYS.toMillis(2), 0L));
        mNetworkCycleData.add(createNetworkCycleData(
            tonight + TimeUnit.DAYS.toMillis(2), tonight + TimeUnit.DAYS.toMillis(3), 0L));
        mNetworkCycleData.add(createNetworkCycleData(
            tonight + TimeUnit.DAYS.toMillis(3), tonight + TimeUnit.DAYS.toMillis(4), 0L));
        mNetworkCycleData.add(createNetworkCycleData(
            tonight + TimeUnit.DAYS.toMillis(4), tonight + TimeUnit.DAYS.toMillis(5), 0L));
        mNetworkCycleData.add(createNetworkCycleData(
            tonight + TimeUnit.DAYS.toMillis(5), tonight + TimeUnit.DAYS.toMillis(6), 0L));

        final NetworkCycleChartData.Builder builder = new NetworkCycleChartData.Builder();
        builder.setUsageBuckets(mNetworkCycleData)
            .setStartTime(tonight - TimeUnit.DAYS.toMillis(5))
            .setEndTime(tonight + TimeUnit.DAYS.toMillis(6));
        mNetworkCycleChartData = builder.build();
        mPreference.setNetworkCycleData(mNetworkCycleChartData);

        mPreference.calcPoints(usageView, mNetworkCycleData);

        verify(usageView).addPath(pointsCaptor.capture());
        final SparseIntArray points = pointsCaptor.getValue();
        // should only have 7 points: 1 dummy point indicating the start of data, starting point 0,
        // and 5 actual data point for each day
        assertThat(points.size()).isEqualTo(7);
        assertThat(points.keyAt(0)).isEqualTo(-1);
        assertThat(points.keyAt(1)).isEqualTo(0);
        assertThat(points.keyAt(2)).isEqualTo(TimeUnit.DAYS.toMinutes(1));
        assertThat(points.keyAt(3)).isEqualTo(TimeUnit.DAYS.toMinutes(2));
        assertThat(points.keyAt(4)).isEqualTo(TimeUnit.DAYS.toMinutes(3));
        assertThat(points.keyAt(5)).isEqualTo(TimeUnit.DAYS.toMinutes(4));
        assertThat(points.keyAt(6)).isEqualTo(TimeUnit.DAYS.toMinutes(5));
    }

    private void createTestNetworkData() {
        mNetworkCycleData = new ArrayList<>();
        // create 10 arbitrary network data
        mNetworkCycleData.add(createNetworkCycleData(1521583200000L, 1521586800000L, 743823454L));
        mNetworkCycleData.add(createNetworkCycleData(1521586800000L, 1521590400000L, 64396L));
        mNetworkCycleData.add(createNetworkCycleData(1521590400000L, 1521655200000L, 2832L));
        mNetworkCycleData.add(createNetworkCycleData(1521655200000L, 1521658800000L, 83849690L));
        mNetworkCycleData.add(createNetworkCycleData(1521658800000L, 1521662400000L, 1883657L));
        mNetworkCycleData.add(createNetworkCycleData(1521662400000L, 1521666000000L, 705259L));
        mNetworkCycleData.add(createNetworkCycleData(1521666000000L, 1521669600000L, 216169L));
        mNetworkCycleData.add(createNetworkCycleData(1521669600000L, 1521673200000L, 6069175L));
        mNetworkCycleData.add(createNetworkCycleData(1521673200000L, 1521676800000L, 120389L));
        mNetworkCycleData.add(createNetworkCycleData(1521676800000L, 1521678800000L, 29947L));

        final NetworkCycleChartData.Builder builder = new NetworkCycleChartData.Builder();
        builder.setUsageBuckets(mNetworkCycleData)
            .setStartTime(TIMESTAMP_START)
            .setEndTime(TIMESTAMP_END);
        mNetworkCycleChartData = builder.build();
    }

    private NetworkCycleData createNetworkCycleData(long start, long end, long usage) {
        return new NetworkCycleData.Builder()
            .setStartTime(start).setEndTime(end).setTotalUsage(usage).build();
    }
}
