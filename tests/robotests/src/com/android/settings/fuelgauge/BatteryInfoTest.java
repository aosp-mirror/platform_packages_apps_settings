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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.SystemClock;
import android.util.SparseIntArray;

import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.UsageView;
import com.android.settingslib.R;
import com.android.settingslib.fuelgauge.Estimate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class BatteryInfoTest {

    private static final String STATUS_CHARGING_NO_TIME = "50% - charging";
    private static final String STATUS_CHARGING_TIME = "50% - 0 min until charged";
    private static final String STATUS_NOT_CHARGING = "Not charging";
    private static final long REMAINING_TIME_NULL = -1;
    private static final long REMAINING_TIME = 2;
    // Strings are defined in frameworks/base/packages/SettingsLib/res/values/strings.xml
    private static final String ENHANCED_STRING_SUFFIX = "based on your usage";
    private static final String BATTERY_RUN_OUT_PREFIX = "Battery may run out by";
    private static final long TEST_CHARGE_TIME_REMAINING = TimeUnit.MINUTES.toMicros(1);
    private static final String TEST_CHARGE_TIME_REMAINING_STRINGIFIED =
            "1 min left until charged";
    private static final String TEST_BATTERY_LEVEL_10 = "10%";
    private static final String FIFTEEN_MIN_FORMATTED = "15 min";
    private static final Estimate DUMMY_ESTIMATE = new Estimate(
            1000, /* estimateMillis */
            false, /* isBasedOnUsage */
            1000 /* averageDischargeTime */);

    private Intent mDisChargingBatteryBroadcast;
    private Intent mChargingBatteryBroadcast;
    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BatteryStats mBatteryStats;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mFeatureFactory = FakeFeatureFactory.setupForTest();

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
        assertThat(info.chargeLabel.toString()).isEqualTo("50% - 1 min until charged");
    }

    @Test
    public void testGetBatteryInfo_basedOnUsageTrueMoreThanFifteenMinutes_usesCorrectString() {
        Estimate estimate = new Estimate(Duration.ofHours(4).toMillis(),
                true /* isBasedOnUsage */,
                1000 /* averageDischargeTime */);
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, estimate, SystemClock.elapsedRealtime() * 1000,
                false /* shortString */);
        BatteryInfo info2 = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, estimate, SystemClock.elapsedRealtime() * 1000,
                true /* shortString */);

        // We only add special mention for the long string
        assertThat(info.remainingLabel.toString()).contains(ENHANCED_STRING_SUFFIX);
        assertThat(info.suggestionLabel).contains(BATTERY_RUN_OUT_PREFIX);
        // shortened string should not have extra text
        assertThat(info2.remainingLabel.toString()).doesNotContain(ENHANCED_STRING_SUFFIX);
        assertThat(info2.suggestionLabel).contains(BATTERY_RUN_OUT_PREFIX);
    }

    @Test
    public void testGetBatteryInfo_basedOnUsageTrueLessThanSevenMinutes_usesCorrectString() {
        Estimate estimate = new Estimate(Duration.ofMinutes(7).toMillis(),
                true /* isBasedOnUsage */,
                1000 /* averageDischargeTime */);
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, estimate, SystemClock.elapsedRealtime() * 1000,
                false /* shortString */);
        BatteryInfo info2 = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, estimate, SystemClock.elapsedRealtime() * 1000,
                true /* shortString */);

        // These should be identical in either case
        assertThat(info.remainingLabel.toString()).isEqualTo(
                mContext.getString(R.string.power_remaining_duration_only_shutdown_imminent));
        assertThat(info2.remainingLabel.toString()).isEqualTo(
                mContext.getString(R.string.power_remaining_duration_only_shutdown_imminent));
        assertThat(info2.suggestionLabel).contains(BATTERY_RUN_OUT_PREFIX);
    }

    @Test
    public void getBatteryInfo_MoreThanOneDay_suggestionLabelIsCorrectString() {
        Estimate estimate = new Estimate(Duration.ofDays(3).toMillis(),
                true /* isBasedOnUsage */,
                1000 /* averageDischargeTime */);
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, estimate, SystemClock.elapsedRealtime() * 1000,
                false /* shortString */);

        assertThat(info.suggestionLabel).doesNotContain(BATTERY_RUN_OUT_PREFIX);
    }

    @Test
    public void
    testGetBatteryInfo_basedOnUsageTrueBetweenSevenAndFifteenMinutes_usesCorrectString() {
        Estimate estimate = new Estimate(Duration.ofMinutes(10).toMillis(),
                true /* isBasedOnUsage */,
                1000 /* averageDischargeTime */);
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, estimate, SystemClock.elapsedRealtime() * 1000,
                false /* shortString */);

        // Check that strings are showing less than 15 minutes remaining regardless of exact time.
        assertThat(info.chargeLabel.toString()).isEqualTo(
                mContext.getString(R.string.power_remaining_less_than_duration,
                        FIFTEEN_MIN_FORMATTED, TEST_BATTERY_LEVEL_10));
        assertThat(info.remainingLabel.toString()).isEqualTo(
                mContext.getString(R.string.power_remaining_less_than_duration_only,
                        FIFTEEN_MIN_FORMATTED));
    }

    @Test
    public void testGetBatteryInfo_basedOnUsageFalse_usesDefaultString() {
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, DUMMY_ESTIMATE, SystemClock.elapsedRealtime() * 1000,
                false /* shortString */);
        BatteryInfo info2 = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, DUMMY_ESTIMATE, SystemClock.elapsedRealtime() * 1000,
                true /* shortString */);

        assertThat(info.remainingLabel.toString()).doesNotContain(ENHANCED_STRING_SUFFIX);
        assertThat(info2.remainingLabel.toString()).doesNotContain(ENHANCED_STRING_SUFFIX);
    }

    @Test
    public void testGetBatteryInfo_charging_usesChargeTime() {
        doReturn(TEST_CHARGE_TIME_REMAINING)
                .when(mBatteryStats)
                .computeChargeTimeRemaining(anyLong());

        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mChargingBatteryBroadcast,
                mBatteryStats, DUMMY_ESTIMATE, SystemClock.elapsedRealtime() * 1000,
                false /* shortString */);
        assertThat(info.remainingTimeUs).isEqualTo(TEST_CHARGE_TIME_REMAINING);
        assertThat(info.remainingLabel.toString())
                .isEqualTo(TEST_CHARGE_TIME_REMAINING_STRINGIFIED);
    }

    @Test
    public void testGetBatteryInfo_pluggedInWithFullBattery_onlyShowBatteryLevel() {
        mChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_LEVEL, 100);

        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mChargingBatteryBroadcast,
                mBatteryStats, DUMMY_ESTIMATE, SystemClock.elapsedRealtime() * 1000,
                false /* shortString */);

        assertThat(info.chargeLabel).isEqualTo("100%");
    }

    @Test
    public void testGetBatteryInfo_chargingWithOverheated_updateChargeLabel() {
        doReturn(TEST_CHARGE_TIME_REMAINING)
            .when(mBatteryStats)
            .computeChargeTimeRemaining(anyLong());
        mChargingBatteryBroadcast
                .putExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_OVERHEAT);

        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mChargingBatteryBroadcast,
                mBatteryStats, DUMMY_ESTIMATE, SystemClock.elapsedRealtime() * 1000,
                false /* shortString */);

        assertThat(info.isOverheated).isTrue();
        assertThat(info.chargeLabel).isEqualTo("50% - Battery limited temporarily");
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
        when(view.getContext()).thenReturn(mContext);

        info.bindHistory(view);
        verify(view, times(1)).configureGraph(anyInt(), anyInt());
        verify(view, times(1)).addPath(any(SparseIntArray.class));
        verify(view, never()).addProjectedPath(any(SparseIntArray.class));
    }

    private void assertHistoryAndLinearProjection(BatteryInfo info) {
        mockBatteryStatsHistory();
        UsageView view = mock(UsageView.class);
        when(view.getContext()).thenReturn(mContext);

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
        when(view.getContext()).thenReturn(mContext);
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
        Estimate batteryEstimate = new Estimate(
                estimate ? 1000 : 0,
                false /* isBasedOnUsage */,
                1000 /* averageDischargeTime */);
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext,
                charging ? mChargingBatteryBroadcast : mDisChargingBatteryBroadcast,
                mBatteryStats, batteryEstimate, SystemClock.elapsedRealtime() * 1000, false);
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
