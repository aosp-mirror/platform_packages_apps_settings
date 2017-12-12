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
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.SystemClock;
import android.util.SparseIntArray;

import com.android.settings.TestConfig;
import com.android.settings.graph.UsageView;
import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.TimeUnit;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BatteryInfoTest {

    private static final String STATUS_FULL = "Full";
    private static final String STATUS_CHARGING_NO_TIME = "50% - charging";
    private static final String STATUS_CHARGING_TIME = "50% - 0m until fully charged";
    private static final String STATUS_NOT_CHARGING = "Not charging";
    private static final int PLUGGED_IN = 1;
    private static final long REMAINING_TIME_NULL = -1;
    private static final long REMAINING_TIME = 2;
    public static final String ENHANCED_STRING_SUFFIX = "left based on your usage";
    public static final long TEST_CHARGE_TIME_REMAINING = TimeUnit.MINUTES.toMicros(1);
    public static final String TEST_CHARGE_TIME_REMAINING_STRINGIFIED =
            "1m left until fully charged";
    private Intent mDisChargingBatteryBroadcast;
    private Intent mChargingBatteryBroadcast;
    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BatteryStats mBatteryStats;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Resources mResources;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mFeatureFactory = FakeFeatureFactory.setupForTest(mContext);

        mDisChargingBatteryBroadcast = BatteryTestUtils.getDischargingIntent();

        mChargingBatteryBroadcast = BatteryTestUtils.getChargingIntent();
    }

    @Test
    public void testGetBatteryInfo_hasStatusLabel() {
        doReturn(REMAINING_TIME_NULL).when(mBatteryStats).computeBatteryTimeRemaining(anyLong());
        BatteryInfo info = BatteryInfo.getBatteryInfoOld(mContext,
                mDisChargingBatteryBroadcast, mBatteryStats, SystemClock.elapsedRealtime() * 1000,
                true /* shortString */);

        assertThat(info.statusLabel).isEqualTo(STATUS_NOT_CHARGING);
    }

    @Test
    public void testGetBatteryInfo_doNotShowChargingMethod_hasRemainingTime() {
        doReturn(REMAINING_TIME).when(mBatteryStats).computeChargeTimeRemaining(anyLong());
        BatteryInfo info = BatteryInfo.getBatteryInfoOld(mContext, mChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, false /* shortString */);

        assertThat(info.chargeLabel.toString()).isEqualTo(STATUS_CHARGING_TIME);
    }

    @Test
    public void testGetBatteryInfo_doNotShowChargingMethod_noRemainingTime() {
        doReturn(REMAINING_TIME_NULL).when(mBatteryStats).computeChargeTimeRemaining(anyLong());
        BatteryInfo info = BatteryInfo.getBatteryInfoOld(mContext, mChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, false /* shortString */);

        assertThat(info.chargeLabel.toString()).isEqualTo(STATUS_CHARGING_NO_TIME);
    }

    @Test
    public void testGetBatteryInfo_pluggedInUsingShortString_usesCorrectData() {
        doReturn(TEST_CHARGE_TIME_REMAINING).when(mBatteryStats).computeChargeTimeRemaining(
                anyLong());
        BatteryInfo info = BatteryInfo.getBatteryInfoOld(mContext, mChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, true /* shortString */);

        assertThat(info.discharging).isEqualTo(false);
        assertThat(info.chargeLabel.toString()).isEqualTo("50% - 1m until fully charged");
    }

    @Test
    public void testGetBatteryInfo_basedOnUsageTrue_usesCorrectString() {
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, false /* shortString */,
                1000, true /* basedOnUsage */);
        BatteryInfo info2 = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, true /* shortString */,
                1000, true /* basedOnUsage */);

        // We only add special mention for the long string
        assertThat(info.remainingLabel.toString()).contains(ENHANCED_STRING_SUFFIX);
        // shortened string should not have extra text
        assertThat(info2.remainingLabel.toString()).doesNotContain(ENHANCED_STRING_SUFFIX);
    }

    @Test
    public void testGetBatteryInfo_basedOnUsageFalse_usesDefaultString() {
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, false /* shortString */,
                1000, false /* basedOnUsage */);
        BatteryInfo info2 = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, true /* shortString */,
                1000, false /* basedOnUsage */);

        assertThat(info.remainingLabel.toString()).doesNotContain(ENHANCED_STRING_SUFFIX);
        assertThat(info2.remainingLabel.toString()).doesNotContain(ENHANCED_STRING_SUFFIX);
    }

    @Test
    public void testGetBatteryInfo_charging_usesChargeTime() {
        doReturn(TEST_CHARGE_TIME_REMAINING)
                .when(mBatteryStats)
                .computeChargeTimeRemaining(anyLong());
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, false, 1000, false);
        assertThat(info.remainingTimeUs).isEqualTo(TEST_CHARGE_TIME_REMAINING);
        assertThat(info.remainingLabel.toString())
                .isEqualTo(TEST_CHARGE_TIME_REMAINING_STRINGIFIED);
    }

    @Test
    public void testGetBatteryInfo_pluggedInWithFullBattery_onlyShowBatteryLevel() {
        mChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_LEVEL, 100);

        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, false /* shortString */,
                1000, false /* basedOnUsage */);

        assertThat(info.chargeLabel).isEqualTo("100%");
    }

    // Make our battery stats return a sequence of battery events.
    private void mockBatteryStatsHistory() {
        // Mock out new data every time start...Locked is called.
        doAnswer(invocation -> {
            doAnswer(new Answer() {
                private int count = 0;
                private long[] times = {1000, 1500, 2000};
                private byte[] levels = {99, 98, 97};

                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    if (count == times.length) {
                        return false;
                    }
                    BatteryStats.HistoryItem record = invocation.getArgument(0);
                    record.cmd = BatteryStats.HistoryItem.CMD_UPDATE;
                    record.time = times[count];
                    record.batteryLevel = levels[count];
                    count++;
                    return true;
                }
            }).when(mBatteryStats).getNextHistoryLocked(any(BatteryStats.HistoryItem.class));
            return true;
        }).when(mBatteryStats).startIteratingHistoryLocked();
    }

    private void assertOnlyHistory(BatteryInfo info) {
        mockBatteryStatsHistory();
        UsageView view = mock(UsageView.class);
        doReturn(mContext).when(view).getContext();

        info.bindHistory(view);
        verify(view, times(1)).configureGraph(anyInt(), anyInt());
        verify(view, times(1)).addPath(any(SparseIntArray.class));
        verify(view, never()).addProjectedPath(any(SparseIntArray.class));
    }

    private void assertHistoryAndLinearProjection(BatteryInfo info) {
        mockBatteryStatsHistory();
        UsageView view = mock(UsageView.class);
        doReturn(mContext).when(view).getContext();

        info.bindHistory(view);
        verify(view, times(2)).configureGraph(anyInt(), anyInt());
        verify(view, times(1)).addPath(any(SparseIntArray.class));
        ArgumentCaptor<SparseIntArray> pointsActual = ArgumentCaptor.forClass(SparseIntArray.class);
        verify(view, times(1)).addProjectedPath(pointsActual.capture());

        // Check that we have two points and the first is correct.
        assertThat(pointsActual.getValue().size()).isEqualTo(2);
        assertThat(pointsActual.getValue().keyAt(0)).isEqualTo(2000);
        assertThat(pointsActual.getValue().valueAt(0)).isEqualTo(97);
    }

    private void assertHistoryAndEnhancedProjection(BatteryInfo info) {
        mockBatteryStatsHistory();
        UsageView view = mock(UsageView.class);
        doReturn(mContext).when(view).getContext();
        SparseIntArray pointsExpected = new SparseIntArray();
        pointsExpected.append(2000, 96);
        pointsExpected.append(2500, 95);
        pointsExpected.append(3000, 94);
        doReturn(pointsExpected).when(mFeatureFactory.powerUsageFeatureProvider)
                .getEnhancedBatteryPredictionCurve(any(Context.class), anyLong());

        info.bindHistory(view);
        verify(view, times(2)).configureGraph(anyInt(), anyInt());
        verify(view, times(1)).addPath(any(SparseIntArray.class));
        ArgumentCaptor<SparseIntArray> pointsActual = ArgumentCaptor.forClass(SparseIntArray.class);
        verify(view, times(1)).addProjectedPath(pointsActual.capture());
        assertThat(pointsActual.getValue()).isEqualTo(pointsExpected);
    }

    private BatteryInfo getBatteryInfo(boolean charging, boolean enhanced, boolean estimate) {
        if (charging && estimate) {
            doReturn(1000L).when(mBatteryStats).computeChargeTimeRemaining(anyLong());
        } else {
            doReturn(0L).when(mBatteryStats).computeChargeTimeRemaining(anyLong());
        }
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext,
                charging ? mChargingBatteryBroadcast : mDisChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, false,
                estimate ? 1000 : 0 /* drainTimeUs */, false);
        doReturn(enhanced).when(mFeatureFactory.powerUsageFeatureProvider)
                .isEnhancedBatteryPredictionEnabled(mContext);
        return info;
    }

    @Test
    public void testBindHistory() {
        BatteryInfo info;

        info = getBatteryInfo(false /* charging */, false /* enhanced */, false /* estimate */);
        assertOnlyHistory(info);

        info = getBatteryInfo(false /* charging */, false /* enhanced */, true /* estimate */);
        assertHistoryAndLinearProjection(info);

        info = getBatteryInfo(false /* charging */, true /* enhanced */, false /* estimate */);
        assertOnlyHistory(info);

        info = getBatteryInfo(false /* charging */, true /* enhanced */, true /* estimate */);
        assertHistoryAndEnhancedProjection(info);

        info = getBatteryInfo(true /* charging */, false /* enhanced */, false /* estimate */);
        assertOnlyHistory(info);

        info = getBatteryInfo(true /* charging */, false /* enhanced */, true /* estimate */);
        assertHistoryAndLinearProjection(info);

        info = getBatteryInfo(true /* charging */, true /* enhanced */, false /* estimate */);
        assertOnlyHistory(info);

        // Linear projection for charging even in enhanced mode.
        info = getBatteryInfo(true /* charging */, true /* enhanced */, true /* estimate */);
        assertHistoryAndLinearProjection(info);
    }
}
