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

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.SystemClock;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;

import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BatteryInfoTest {

    private static final String STATUS_FULL = "Full";
    private static final String STATUS_CHARGING_NO_TIME = "50% - charging";
    private static final String STATUS_CHARGING_TIME = "50% - 0m until fully charged";
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

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BatteryStats mBatteryStats;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Resources mResources;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        FakeFeatureFactory.setupForTest(mContext);

        mDisChargingBatteryBroadcast = new Intent();
        mDisChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_PLUGGED, 0);
        mDisChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_LEVEL, 0);
        mDisChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_SCALE, 100);
        mDisChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_FULL);

        mChargingBatteryBroadcast = new Intent();
        mChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_PLUGGED,
                BatteryManager.BATTERY_PLUGGED_AC);
        mChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_LEVEL, 50);
        mChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_SCALE, 100);
        mChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);
    }

    @Test
    public void testGetBatteryInfo_hasStatusLabel() {
        doReturn(REMAINING_TIME_NULL).when(mBatteryStats).computeBatteryTimeRemaining(anyLong());
        BatteryInfo info = BatteryInfo.getBatteryInfoOld(mContext,
                mDisChargingBatteryBroadcast, mBatteryStats, SystemClock.elapsedRealtime() * 1000,
                true /* shortString */);

        assertThat(info.statusLabel).isEqualTo(STATUS_FULL);
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
    public void testGetBatteryInfo_pluggedIn_dischargingFalse() {
        BatteryInfo info = BatteryInfo.getBatteryInfoOld(mContext, mChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, true /* shortString */);

        assertThat(info.discharging).isEqualTo(false);
    }

    @Test
    public void testGetBatteryInfo_basedOnUsageTrue_usesUsageString() {
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, false /* shortString */,
                1000, true /* basedOnUsage */);
        BatteryInfo info2 = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, true /* shortString */,
                1000, true /* basedOnUsage */);

        assertThat(info.remainingLabel.toString()).contains(ENHANCED_STRING_SUFFIX);
        assertThat(info2.remainingLabel.toString()).contains(ENHANCED_STRING_SUFFIX);
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
}
