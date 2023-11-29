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

import android.app.Activity;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.datausage.ChartDataUsagePreference.DataUsageSummaryNode;
import com.android.settings.datausage.lib.NetworkCycleChartData;
import com.android.settings.datausage.lib.NetworkUsageData;
import com.android.settings.widget.UsageView;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class ChartDataUsagePreferenceTest {

    // Test cycle start date, 20 Mar 2018 22:00: GMT
    private static final long TIMESTAMP_START = 1521583200000L;
    // Test bucket end date, 22 Mar 2018 00:00:00
    private static final long TIMESTAMP_END = 1521676800000L;

    private List<NetworkUsageData> mNetworkCycleData;
    private NetworkCycleChartData mNetworkCycleChartData;
    private ChartDataUsagePreference mPreference;
    private Activity mActivity;
    private PreferenceViewHolder mHolder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mActivity = spy(Robolectric.setupActivity(Activity.class));
        mPreference = new ChartDataUsagePreference(mActivity, null /* attrs */);
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View view = inflater.inflate(mPreference.getLayoutResource(), null /* root */,
                false /* attachToRoot */);
        mHolder = spy(PreferenceViewHolder.createInstanceForTests(view));
    }

    @Test
    public void calcPoints_dataAvailableFromCycleStart_shouldAddDataPointsOnly() {
        final UsageView usageView = mock(UsageView.class);
        final ArgumentCaptor<SparseIntArray> pointsCaptor =
                ArgumentCaptor.forClass(SparseIntArray.class);
        createTestNetworkData();
        mPreference.setTime(
                mNetworkCycleChartData.getTotal().getStartTime(),
                mNetworkCycleChartData.getTotal().getEndTime());
        mPreference.setNetworkCycleData(mNetworkCycleChartData);

        mPreference.calcPoints(usageView, mNetworkCycleData.subList(0, 5));

        verify(usageView).addPath(pointsCaptor.capture());
        final SparseIntArray points = pointsCaptor.getValue();
        // the point should be normal usage data
        assertThat(points.valueAt(1)).isNotEqualTo(-1);
    }

    @Ignore("b/313568482")
    @Test
    public void calcPoints_dataNotAvailableAtCycleStart_shouldIndicateStartOfData() {
        final UsageView usageView = mock(UsageView.class);
        final ArgumentCaptor<SparseIntArray> pointsCaptor =
                ArgumentCaptor.forClass(SparseIntArray.class);
        createTestNetworkData();
        mPreference.setTime(
                mNetworkCycleChartData.getTotal().getStartTime(),
                mNetworkCycleChartData.getTotal().getEndTime());
        mPreference.setNetworkCycleData(mNetworkCycleChartData);

        mPreference.calcPoints(usageView, mNetworkCycleData.subList(2, 7));

        verify(usageView).addPath(pointsCaptor.capture());
        final SparseIntArray points = pointsCaptor.getValue();
        // indicator that no data is available
        assertThat(points.keyAt(1)).isEqualTo(points.keyAt(2) - 1);
        assertThat(points.valueAt(1)).isEqualTo(-1);
    }

    @Ignore("b/313568482")
    @Test
    public void calcPoints_shouldNotDrawPointForFutureDate() {
        final UsageView usageView = mock(UsageView.class);
        final ArgumentCaptor<SparseIntArray> pointsCaptor =
                ArgumentCaptor.forClass(SparseIntArray.class);
        final long tonight = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(12);
        mNetworkCycleData = new ArrayList<>();
        // add test usage data for last 5 days
        mNetworkCycleData.add(new NetworkUsageData(
                tonight - TimeUnit.DAYS.toMillis(5),
                tonight - TimeUnit.DAYS.toMillis(4),
                743823454L));
        mNetworkCycleData.add(new NetworkUsageData(
                tonight - TimeUnit.DAYS.toMillis(4),
                tonight - TimeUnit.DAYS.toMillis(3),
                64396L));
        mNetworkCycleData.add(new NetworkUsageData(
                tonight - TimeUnit.DAYS.toMillis(3),
                tonight - TimeUnit.DAYS.toMillis(2),
                2832L));
        mNetworkCycleData.add(new NetworkUsageData(
                tonight - TimeUnit.DAYS.toMillis(2),
                tonight - TimeUnit.DAYS.toMillis(1),
                83849690L));
        mNetworkCycleData.add(new NetworkUsageData(
                tonight - TimeUnit.DAYS.toMillis(1), tonight, 1883657L));
        // add test usage data for next 5 days
        mNetworkCycleData.add(new NetworkUsageData(
                tonight, tonight + TimeUnit.DAYS.toMillis(1), 0L));
        mNetworkCycleData.add(new NetworkUsageData(
                tonight + TimeUnit.DAYS.toMillis(1),
                tonight + TimeUnit.DAYS.toMillis(2),
                0L));
        mNetworkCycleData.add(new NetworkUsageData(
                tonight + TimeUnit.DAYS.toMillis(2),
                tonight + TimeUnit.DAYS.toMillis(3),
                0L));
        mNetworkCycleData.add(new NetworkUsageData(
                tonight + TimeUnit.DAYS.toMillis(3),
                tonight + TimeUnit.DAYS.toMillis(4),
                0L));
        mNetworkCycleData.add(new NetworkUsageData(
                tonight + TimeUnit.DAYS.toMillis(4),
                tonight + TimeUnit.DAYS.toMillis(5),
                0L));
        mNetworkCycleData.add(new NetworkUsageData(
                tonight + TimeUnit.DAYS.toMillis(5),
                tonight + TimeUnit.DAYS.toMillis(6),
                0L));

        mNetworkCycleChartData = new NetworkCycleChartData(
                new NetworkUsageData(
                        tonight - TimeUnit.DAYS.toMillis(5),
                        tonight + TimeUnit.DAYS.toMillis(6),
                        0),
                mNetworkCycleData
        );
        mPreference.setTime(
                mNetworkCycleChartData.getTotal().getStartTime(),
                mNetworkCycleChartData.getTotal().getEndTime());
        mPreference.setNetworkCycleData(mNetworkCycleChartData);

        mPreference.calcPoints(usageView, mNetworkCycleData);

        verify(usageView).addPath(pointsCaptor.capture());
        final SparseIntArray points = pointsCaptor.getValue();
        // should only have 7 points: 1 test point indicating the start of data, starting point 0,
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

    @Test
    public void notifyChange_nonEmptyDataUsage_shouldHaveSingleContentDescription() {
        final UsageView chart = (UsageView) mHolder.findViewById(R.id.data_usage);
        final TextView labelTop = (TextView) mHolder.findViewById(R.id.label_top);
        final TextView labelMiddle = (TextView) mHolder.findViewById(R.id.label_middle);
        final TextView labelBottom = (TextView) mHolder.findViewById(R.id.label_bottom);
        final TextView labelStart = (TextView) mHolder.findViewById(R.id.label_start);
        final TextView labelEnd = (TextView) mHolder.findViewById(R.id.label_end);
        createTestNetworkData();
        mPreference.setTime(
                mNetworkCycleChartData.getTotal().getStartTime(),
                mNetworkCycleChartData.getTotal().getEndTime());
        mPreference.setNetworkCycleData(mNetworkCycleChartData);

        mPreference.onBindViewHolder(mHolder);

        assertThat(chart.getContentDescription()).isNotNull();
        assertThat(labelTop.getContentDescription()).isNull();
        assertThat(labelMiddle.getContentDescription()).isNull();
        assertThat(labelBottom.getContentDescription()).isNull();
        assertThat(labelStart.getContentDescription()).isNull();
        assertThat(labelEnd.getContentDescription()).isNull();
    }

    @Test
    public void getDensedStatsData_someSamePercentageNodes_getDifferentPercentageNodes() {
        createSomeSamePercentageNetworkData();
        final List<DataUsageSummaryNode> densedStatsData =
                mPreference.getDensedStatsData(mNetworkCycleData);

        assertThat(mNetworkCycleData.size()).isEqualTo(8);
        assertThat(densedStatsData.size()).isEqualTo(3);
        assertThat(densedStatsData.get(0).getDataUsagePercentage()).isEqualTo(33);
        assertThat(densedStatsData.get(1).getDataUsagePercentage()).isEqualTo(99);
        assertThat(densedStatsData.get(2).getDataUsagePercentage()).isEqualTo(100);
    }

    private void createTestNetworkData() {
        mNetworkCycleData = new ArrayList<>();
        // create 10 arbitrary network data
        mNetworkCycleData.add(new NetworkUsageData(1521583200000L, 1521586800000L, 743823454L));
        mNetworkCycleData.add(new NetworkUsageData(1521586800000L, 1521590400000L, 64396L));
        mNetworkCycleData.add(new NetworkUsageData(1521590400000L, 1521655200000L, 2832L));
        mNetworkCycleData.add(new NetworkUsageData(1521655200000L, 1521658800000L, 83849690L));
        mNetworkCycleData.add(new NetworkUsageData(1521658800000L, 1521662400000L, 1883657L));
        mNetworkCycleData.add(new NetworkUsageData(1521662400000L, 1521666000000L, 705259L));
        mNetworkCycleData.add(new NetworkUsageData(1521666000000L, 1521669600000L, 216169L));
        mNetworkCycleData.add(new NetworkUsageData(1521669600000L, 1521673200000L, 6069175L));
        mNetworkCycleData.add(new NetworkUsageData(1521673200000L, 1521676800000L, 120389L));
        mNetworkCycleData.add(new NetworkUsageData(1521676800000L, 1521678800000L, 29947L));

        mNetworkCycleChartData = new NetworkCycleChartData(
                new NetworkUsageData(TIMESTAMP_START, TIMESTAMP_END, 0),
                mNetworkCycleData
        );
    }

    private void createSomeSamePercentageNetworkData() {
        mNetworkCycleData = new ArrayList<>();
        mNetworkCycleData.add(new NetworkUsageData(1521583200000L, 1521586800000L, 100)); //33%
        mNetworkCycleData.add(new NetworkUsageData(1521586800000L, 1521590400000L, 1));   //33%
        mNetworkCycleData.add(new NetworkUsageData(1521590400000L, 1521655200000L, 0));   //33%
        mNetworkCycleData.add(new NetworkUsageData(1521655200000L, 1521658800000L, 0));   //33%
        mNetworkCycleData.add(new NetworkUsageData(1521658800000L, 1521662400000L, 200)); //99%
        mNetworkCycleData.add(new NetworkUsageData(1521662400000L, 1521666000000L, 1));   //99%
        mNetworkCycleData.add(new NetworkUsageData(1521666000000L, 1521669600000L, 1));   //100
        mNetworkCycleData.add(new NetworkUsageData(1521669600000L, 1521673200000L, 0));   //100%
    }

}
