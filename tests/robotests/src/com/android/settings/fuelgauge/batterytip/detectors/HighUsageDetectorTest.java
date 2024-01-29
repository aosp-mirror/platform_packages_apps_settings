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

package com.android.settings.fuelgauge.batterytip.detectors;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.BatteryStatsManager;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.UidBatteryConsumer;

import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.HighUsageDataParser;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.HighUsageTip;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class HighUsageDetectorTest {
    private static final String TAG = "HighUsageDetectorTest";

    private static final int UID_HIGH = 123;
    private static final int UID_LOW = 345;
    private static final double POWER_HIGH = 20000;
    private static final double POWER_LOW = 10000;

    private Context mContext;
    @Mock private UidBatteryConsumer mHighBatteryConsumer;
    @Mock private UidBatteryConsumer mLowBatteryConsumer;
    @Mock private UidBatteryConsumer mSystemBatteryConsumer;
    @Mock private HighUsageDataParser mDataParser;
    @Mock private BatteryUsageStats mBatteryUsageStats;
    @Mock private BatteryStatsManager mBatteryStatsManager;

    private AppInfo mHighAppInfo;
    private AppInfo mLowAppInfo;
    private BatteryTipPolicy mPolicy;
    private BatteryUtils mBatteryUtils;
    private HighUsageDetector mHighUsageDetector;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mPolicy = spy(new BatteryTipPolicy(mContext));
        mBatteryUtils = spy(new BatteryUtils(mContext));

        when(mContext.getSystemService(eq(Context.BATTERY_STATS_SERVICE)))
                .thenReturn(mBatteryStatsManager);
        when(mBatteryStatsManager.getBatteryUsageStats(any(BatteryUsageStatsQuery.class)))
                .thenReturn(mBatteryUsageStats);

        mContext.sendStickyBroadcast(
                new Intent(Intent.ACTION_BATTERY_CHANGED)
                        .putExtra(BatteryManager.EXTRA_PLUGGED, 0));

        mHighUsageDetector =
                spy(
                        new HighUsageDetector(
                                mContext,
                                mPolicy,
                                mBatteryUsageStats,
                                mBatteryUtils.getBatteryInfo(TAG)));
        mHighUsageDetector.mBatteryUtils = mBatteryUtils;
        mHighUsageDetector.mDataParser = mDataParser;
        doNothing().when(mHighUsageDetector).parseBatteryData();
        doReturn(UID_HIGH).when(mHighBatteryConsumer).getUid();
        doReturn(UID_LOW).when(mLowBatteryConsumer).getUid();
        doReturn(POWER_HIGH).when(mHighBatteryConsumer).getConsumedPower();
        doReturn(POWER_LOW).when(mLowBatteryConsumer).getConsumedPower();
        doReturn(false).when(mBatteryUtils).shouldHideUidBatteryConsumer(mHighBatteryConsumer);
        doReturn(false).when(mBatteryUtils).shouldHideUidBatteryConsumer(mLowBatteryConsumer);
        when(mBatteryUsageStats.getDischargePercentage()).thenReturn(100);
        when(mBatteryUsageStats.getConsumedPower()).thenReturn(POWER_HIGH + POWER_LOW);

        mHighAppInfo = new AppInfo.Builder().setUid(UID_HIGH).build();
        mLowAppInfo = new AppInfo.Builder().setUid(UID_LOW).build();

        ArrayList<UidBatteryConsumer> consumers = new ArrayList<>();
        consumers.add(mSystemBatteryConsumer);
        consumers.add(mLowBatteryConsumer);
        consumers.add(mHighBatteryConsumer);
        when(mBatteryUsageStats.getUidBatteryConsumers()).thenReturn(consumers);
    }

    @Test
    public void testDetect_disabledByPolicy_tipInvisible() {
        ReflectionHelpers.setField(mPolicy, "highUsageEnabled", false);

        assertThat(mHighUsageDetector.detect().isVisible()).isFalse();
    }

    @Test
    public void testDetect_deviceCharging_tipInvisible() {
        ReflectionHelpers.setField(mPolicy, "highUsageEnabled", true);
        doReturn(true).when(mDataParser).isDeviceHeavilyUsed();
        mHighUsageDetector.mDischarging = false;

        assertThat(mHighUsageDetector.detect().isVisible()).isFalse();
    }

    @Test
    public void testDetect_testFeatureOn_tipNew() {
        doReturn(false).when(mDataParser).isDeviceHeavilyUsed();
        ReflectionHelpers.setField(mPolicy, "testHighUsageTip", true);

        assertThat(mHighUsageDetector.detect().getState()).isEqualTo(BatteryTip.StateType.NEW);
    }

    @Test
    public void testDetect_containsHighUsageApp_tipVisibleAndSorted() {
        doReturn(true).when(mDataParser).isDeviceHeavilyUsed();

        final HighUsageTip highUsageTip = (HighUsageTip) mHighUsageDetector.detect();
        assertThat(highUsageTip.isVisible()).isTrue();

        // Contain two appInfo and large one comes first
        final List<AppInfo> appInfos = highUsageTip.getHighUsageAppList();
        assertThat(appInfos).containsExactly(mLowAppInfo, mHighAppInfo);
        assertThat(appInfos.get(0)).isEqualTo(mHighAppInfo);
    }
}
