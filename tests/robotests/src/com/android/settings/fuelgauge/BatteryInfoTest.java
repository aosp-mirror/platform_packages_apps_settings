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
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import android.os.BatteryUsageStats;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.SparseIntArray;

import com.android.internal.os.BatteryStatsHistoryIterator;
import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.UsageView;
import com.android.settingslib.fuelgauge.Estimate;
import com.android.settingslib.utils.PowerUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class BatteryInfoTest {

    private static final String STATUS_CHARGING_NO_TIME = "50% - charging";
    private static final String STATUS_CHARGING_TIME = "50% - 0 min left until full";
    private static final String STATUS_NOT_CHARGING = "Not charging";
    private static final String STATUS_CHARGING_FUTURE_BYPASS = "50% - Charging";
    private static final String STATUS_CHARGING_PAUSED =
            "50% - Charging on hold to protect battery";
    private static final long REMAINING_TIME_NULL = -1;
    private static final long REMAINING_TIME = 2;
    // Strings are defined in frameworks/base/packages/SettingsLib/res/values/strings.xml
    private static final String ENHANCED_STRING_SUFFIX = "based on your usage";
    private static final String BATTERY_RUN_OUT_PREFIX = "Battery may run out by";
    private static final long TEST_CHARGE_TIME_REMAINING = TimeUnit.MINUTES.toMicros(1);
    private static final String TEST_CHARGE_TIME_REMAINING_STRINGIFIED = "1 min left until full";
    private static final String TEST_BATTERY_LEVEL_10 = "10%";
    private static final String FIFTEEN_MIN_FORMATTED = "15 min";
    private static final Estimate MOCK_ESTIMATE =
            new Estimate(
                    1000, /* estimateMillis */
                    false, /* isBasedOnUsage */
                    1000 /* averageDischargeTime */);
    private static final Map<ChargingType, Integer> CHARGING_TYPE_MAP =
            Map.of(
                    ChargingType.WIRED, BatteryManager.BATTERY_PLUGGED_AC,
                    ChargingType.WIRELESS, BatteryManager.BATTERY_PLUGGED_WIRELESS,
                    ChargingType.DOCKED, BatteryManager.BATTERY_PLUGGED_DOCK,
                    ChargingType.NONE, 0);
    private static final Map<ChargingSpeed, Integer> CHARGING_SPEED_MAP =
            Map.of(
                    ChargingSpeed.FAST, 1501000,
                    ChargingSpeed.REGULAR, 1500000,
                    ChargingSpeed.SLOW, 999999);
    private static final long UNUSED_TIME_MS = -1L;

    private Intent mDisChargingBatteryBroadcast;
    private Intent mChargingBatteryBroadcast;
    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private TimeZone mOriginalTimeZone;

    @Mock private BatteryUsageStats mBatteryUsageStats;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mFeatureFactory = FakeFeatureFactory.setupForTest();

        mDisChargingBatteryBroadcast = BatteryTestUtils.getDischargingIntent();

        mChargingBatteryBroadcast = BatteryTestUtils.getChargingIntent();

        doReturn(false).when(mFeatureFactory.powerUsageFeatureProvider).isExtraDefend();
        Settings.Global.putInt(
                mContext.getContentResolver(),
                BatteryUtils.SETTINGS_GLOBAL_DOCK_DEFENDER_BYPASS,
                0);

        // Reset static cache for testing purpose.
        com.android.settingslib.fuelgauge.BatteryUtils.setChargingStringV2Enabled(null);

        mOriginalTimeZone = TimeZone.getDefault();
    }

    @After
    public void tearDown() throws Exception {
        TimeZone.setDefault(mOriginalTimeZone);
    }

    @Test
    public void getBatteryInfo_hasStatusLabel() {
        doReturn(REMAINING_TIME_NULL).when(mBatteryUsageStats).getBatteryTimeRemainingMs();
        BatteryInfo info =
                BatteryInfo.getBatteryInfoOld(
                        mContext,
                        mDisChargingBatteryBroadcast,
                        mBatteryUsageStats,
                        SystemClock.elapsedRealtime() * 1000,
                        true /* shortString */);

        assertThat(info.statusLabel).isEqualTo(STATUS_NOT_CHARGING);
    }

    @Test
    public void getBatteryInfo_doNotShowChargingMethod_hasRemainingTime() {
        doReturn(REMAINING_TIME).when(mBatteryUsageStats).getChargeTimeRemainingMs();
        BatteryInfo info =
                BatteryInfo.getBatteryInfoOld(
                        mContext,
                        mChargingBatteryBroadcast,
                        mBatteryUsageStats,
                        SystemClock.elapsedRealtime() * 1000,
                        false /* shortString */);

        assertThat(info.chargeLabel.toString()).isEqualTo(STATUS_CHARGING_TIME);
    }

    @Test
    public void getBatteryInfo_doNotShowChargingMethod_noRemainingTime() {
        doReturn(REMAINING_TIME_NULL).when(mBatteryUsageStats).getChargeTimeRemainingMs();
        BatteryInfo info =
                BatteryInfo.getBatteryInfoOld(
                        mContext,
                        mChargingBatteryBroadcast,
                        mBatteryUsageStats,
                        SystemClock.elapsedRealtime() * 1000,
                        false /* shortString */);

        assertThat(info.chargeLabel.toString()).ignoringCase().isEqualTo(STATUS_CHARGING_NO_TIME);
    }

    @Test
    public void getBatteryInfo_pluggedInUsingShortString_usesCorrectData() {
        doReturn(TEST_CHARGE_TIME_REMAINING / 1000)
                .when(mBatteryUsageStats)
                .getChargeTimeRemainingMs();
        BatteryInfo info =
                BatteryInfo.getBatteryInfoOld(
                        mContext,
                        mChargingBatteryBroadcast,
                        mBatteryUsageStats,
                        SystemClock.elapsedRealtime() * 1000,
                        true /* shortString */);

        assertThat(info.discharging).isEqualTo(false);
        assertThat(info.chargeLabel.toString()).isEqualTo("50% - 1 min left until full");
    }

    @Test
    public void getBatteryInfo_basedOnUsageTrueMoreThanFifteenMinutes_usesCorrectString() {
        Estimate estimate =
                new Estimate(
                        Duration.ofHours(4).toMillis(),
                        true /* isBasedOnUsage */,
                        1000 /* averageDischargeTime */);
        BatteryInfo info =
                BatteryInfo.getBatteryInfo(
                        mContext,
                        mDisChargingBatteryBroadcast,
                        mBatteryUsageStats,
                        estimate,
                        SystemClock.elapsedRealtime() * 1000,
                        false /* shortString */);
        BatteryInfo info2 =
                BatteryInfo.getBatteryInfo(
                        mContext,
                        mDisChargingBatteryBroadcast,
                        mBatteryUsageStats,
                        estimate,
                        SystemClock.elapsedRealtime() * 1000,
                        true /* shortString */);

        // Both long and short strings should not have extra text
        assertThat(info.remainingLabel.toString()).doesNotContain(ENHANCED_STRING_SUFFIX);
        assertThat(info.suggestionLabel).contains(BATTERY_RUN_OUT_PREFIX);
        assertThat(info2.remainingLabel.toString()).doesNotContain(ENHANCED_STRING_SUFFIX);
        assertThat(info2.suggestionLabel).contains(BATTERY_RUN_OUT_PREFIX);
    }

    @Test
    @Ignore
    public void getBatteryInfo_MoreThanOneDay_suggestionLabelIsCorrectString() {
        Estimate estimate =
                new Estimate(
                        Duration.ofDays(3).toMillis(),
                        true /* isBasedOnUsage */,
                        1000 /* averageDischargeTime */);
        BatteryInfo info =
                BatteryInfo.getBatteryInfo(
                        mContext,
                        mDisChargingBatteryBroadcast,
                        mBatteryUsageStats,
                        estimate,
                        SystemClock.elapsedRealtime() * 1000,
                        false /* shortString */);

        assertThat(info.suggestionLabel).doesNotContain(BATTERY_RUN_OUT_PREFIX);
    }

    @Test
    public void getBatteryInfo_basedOnUsageFalse_usesDefaultString() {
        BatteryInfo info =
                BatteryInfo.getBatteryInfo(
                        mContext,
                        mDisChargingBatteryBroadcast,
                        mBatteryUsageStats,
                        MOCK_ESTIMATE,
                        SystemClock.elapsedRealtime() * 1000,
                        false /* shortString */);
        BatteryInfo info2 =
                BatteryInfo.getBatteryInfo(
                        mContext,
                        mDisChargingBatteryBroadcast,
                        mBatteryUsageStats,
                        MOCK_ESTIMATE,
                        SystemClock.elapsedRealtime() * 1000,
                        true /* shortString */);

        assertThat(info.remainingLabel.toString()).doesNotContain(ENHANCED_STRING_SUFFIX);
        assertThat(info2.remainingLabel.toString()).doesNotContain(ENHANCED_STRING_SUFFIX);
    }

    @Test
    public void getBatteryInfo_charging_usesChargeTime() {
        doReturn(TEST_CHARGE_TIME_REMAINING / 1000)
                .when(mBatteryUsageStats)
                .getChargeTimeRemainingMs();

        BatteryInfo info =
                BatteryInfo.getBatteryInfo(
                        mContext,
                        mChargingBatteryBroadcast,
                        mBatteryUsageStats,
                        MOCK_ESTIMATE,
                        SystemClock.elapsedRealtime() * 1000,
                        false /* shortString */);

        assertThat(info.remainingTimeUs).isEqualTo(TEST_CHARGE_TIME_REMAINING);
        assertThat(info.remainingLabel.toString())
                .isEqualTo(TEST_CHARGE_TIME_REMAINING_STRINGIFIED);
    }

    @Test
    public void getBatteryInfo_pluggedInWithFullBattery_onlyShowBatteryLevel() {
        mChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_LEVEL, 100);

        BatteryInfo info =
                BatteryInfo.getBatteryInfo(
                        mContext,
                        mChargingBatteryBroadcast,
                        mBatteryUsageStats,
                        MOCK_ESTIMATE,
                        SystemClock.elapsedRealtime() * 1000,
                        false /* shortString */);

        assertThat(info.chargeLabel).isEqualTo("100%");
    }

    @Test
    public void getBatteryInfo_chargingWithDefender_updateChargeLabel() {
        doReturn(TEST_CHARGE_TIME_REMAINING).when(mBatteryUsageStats).getChargeTimeRemainingMs();
        doReturn(true)
                .when(mFeatureFactory.powerUsageFeatureProvider)
                .isBatteryDefend(any(BatteryInfo.class));
        mChargingBatteryBroadcast.putExtra(
                BatteryManager.EXTRA_CHARGING_STATUS,
                BatteryManager.CHARGING_POLICY_ADAPTIVE_LONGLIFE);

        BatteryInfo info =
                BatteryInfo.getBatteryInfo(
                        mContext,
                        mChargingBatteryBroadcast,
                        mBatteryUsageStats,
                        MOCK_ESTIMATE,
                        SystemClock.elapsedRealtime() * 1000,
                        false /* shortString */);

        assertThat(info.isBatteryDefender).isTrue();
        assertThat(info.chargeLabel.toString()).contains(STATUS_CHARGING_PAUSED);
    }

    @Test
    public void getBatteryInfo_getChargeTimeRemaining_updateSettingsGlobal() {
        doReturn(TEST_CHARGE_TIME_REMAINING).when(mBatteryUsageStats).getChargeTimeRemainingMs();

        BatteryInfo.getBatteryInfo(
                mContext,
                mChargingBatteryBroadcast,
                mBatteryUsageStats,
                MOCK_ESTIMATE,
                SystemClock.elapsedRealtime() * 1000,
                false /* shortString */);

        assertThat(BatteryInfo.getSettingsChargeTimeRemaining(mContext))
                .isEqualTo(TEST_CHARGE_TIME_REMAINING);
    }

    @Test
    public void getBatteryInfo_differentChargeTimeRemaining_updateSettingsGlobal() {
        doReturn(TEST_CHARGE_TIME_REMAINING).when(mBatteryUsageStats).getChargeTimeRemainingMs();
        final long newTimeToFull = 300L;
        doReturn(newTimeToFull).when(mBatteryUsageStats).getChargeTimeRemainingMs();

        BatteryInfo.getBatteryInfo(
                mContext,
                mChargingBatteryBroadcast,
                mBatteryUsageStats,
                MOCK_ESTIMATE,
                SystemClock.elapsedRealtime() * 1000,
                false /* shortString */);

        assertThat(BatteryInfo.getSettingsChargeTimeRemaining(mContext)).isEqualTo(newTimeToFull);
    }

    @Test
    public void getBatteryInfo_dockDefenderActive_updateChargeString() {
        doReturn(TEST_CHARGE_TIME_REMAINING / 1000)
                .when(mBatteryUsageStats)
                .getChargeTimeRemainingMs();
        doReturn(true).when(mFeatureFactory.powerUsageFeatureProvider).isExtraDefend();
        doReturn(true)
                .when(mFeatureFactory.powerUsageFeatureProvider)
                .isBatteryDefend(any(BatteryInfo.class));
        Intent intent =
                createBatteryIntent(
                                BatteryManager.BATTERY_PLUGGED_DOCK,
                                /* level= */ 50,
                                BatteryManager.BATTERY_STATUS_CHARGING)
                        .putExtra(
                                BatteryManager.EXTRA_CHARGING_STATUS,
                                BatteryManager.CHARGING_POLICY_ADAPTIVE_LONGLIFE);

        BatteryInfo info =
                BatteryInfo.getBatteryInfo(
                        mContext,
                        intent,
                        mBatteryUsageStats,
                        MOCK_ESTIMATE,
                        SystemClock.elapsedRealtime() * 1000,
                        false /* shortString */);

        assertThat(info.chargeLabel.toString()).contains(STATUS_CHARGING_PAUSED);
    }

    @Test
    public void getBatteryInfo_dockDefenderTemporarilyBypassed_updateChargeLabel() {
        doReturn(REMAINING_TIME).when(mBatteryUsageStats).getChargeTimeRemainingMs();
        mChargingBatteryBroadcast.putExtra(
                BatteryManager.EXTRA_CHARGING_STATUS, BatteryManager.CHARGING_POLICY_DEFAULT);
        Settings.Global.putInt(
                mContext.getContentResolver(),
                BatteryUtils.SETTINGS_GLOBAL_DOCK_DEFENDER_BYPASS,
                1);

        BatteryInfo info =
                BatteryInfo.getBatteryInfo(
                        mContext,
                        createBatteryIntent(
                                BatteryManager.BATTERY_PLUGGED_DOCK,
                                /* level= */ 50,
                                BatteryManager.BATTERY_STATUS_CHARGING),
                        mBatteryUsageStats,
                        MOCK_ESTIMATE,
                        SystemClock.elapsedRealtime() * 1000,
                        false /* shortString */);

        assertThat(info.chargeLabel.toString()).contains(STATUS_CHARGING_TIME);
    }

    @Test
    public void getBatteryInfo_dockDefenderFutureBypass_updateChargeLabel() {
        doReturn(false).when(mFeatureFactory.powerUsageFeatureProvider).isExtraDefend();
        mChargingBatteryBroadcast.putExtra(
                BatteryManager.EXTRA_CHARGING_STATUS, BatteryManager.CHARGING_POLICY_DEFAULT);

        BatteryInfo info =
                BatteryInfo.getBatteryInfo(
                        mContext,
                        createBatteryIntent(
                                BatteryManager.BATTERY_PLUGGED_DOCK,
                                /* level= */ 50,
                                BatteryManager.BATTERY_STATUS_CHARGING),
                        mBatteryUsageStats,
                        MOCK_ESTIMATE,
                        SystemClock.elapsedRealtime() * 1000,
                        false /* shortString */);

        assertThat(info.chargeLabel.toString()).contains(STATUS_CHARGING_FUTURE_BYPASS);
    }

    @Test
    public void getBatteryInfo_fastCharging_updateRemainingLabelAndStatusLabel() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofMinutes(90).toMillis(),
                /* chargingStringV2Enabled= */ false);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.WIRED, ChargingSpeed.FAST, /* batteryLevel= */ 61);
        var expectedStatusLabel = "Charging rapidly";
        var expectedRemainingLabel = "1 hr, 30 min left until full";
        var expectedChargeLabel = "61% - " + expectedRemainingLabel;

        assertGetBatteryInfo(
                batteryIntent,
                /* currentTimeMillis= */ UNUSED_TIME_MS,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedChargeLabel);
    }

    @Test
    public void getBatteryInfo_regularCharging_updateRemainingLabelAndStatusLabel() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofMinutes(80).toMillis(),
                /* chargingStringV2Enabled= */ false);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.WIRED, ChargingSpeed.REGULAR, /* batteryLevel= */ 33);
        var expectedStatusLabel = "Charging";
        var expectedRemainingLabel = "1 hr, 20 min left until full";
        var expectedChargeLabel = "33% - " + expectedRemainingLabel;

        assertGetBatteryInfo(
                batteryIntent,
                /* currentTimeMillis= */ UNUSED_TIME_MS,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedChargeLabel);
    }

    @Test
    public void getBatteryInfo_slowCharging_updateRemainingLabelAndStatusLabel() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofMinutes(100).toMillis(),
                /* chargingStringV2Enabled= */ false);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.WIRED, ChargingSpeed.SLOW, /* batteryLevel= */ 53);
        var expectedStatusLabel = "Charging slowly";
        var expectedRemainingLabel = "1 hr, 40 min left until full";
        var expectedChargeLabel = "53% - " + expectedRemainingLabel;

        assertGetBatteryInfo(
                batteryIntent,
                /* currentTimeMillis= */ UNUSED_TIME_MS,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedChargeLabel);
    }

    @Test
    public void getBatteryInfo_wirelessCharging_updateRemainingLabelAndStatusLabel() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofMinutes(130).toMillis(),
                /* chargingStringV2Enabled= */ false);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.WIRELESS, ChargingSpeed.REGULAR, /* batteryLevel= */ 10);
        var expectedStatusLabel = "Charging wirelessly";
        var expectedRemainingLabel = "2 hr, 10 min left until full";
        var expectedChargeLabel = "10% - " + expectedRemainingLabel;

        assertGetBatteryInfo(
                batteryIntent,
                /* currentTimeMillis= */ UNUSED_TIME_MS,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedChargeLabel);
    }

    @Test
    public void getBatteryInfo_dockedCharging_updateRemainingLabelAndStatusLabel() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofMinutes(30).toMillis(),
                /* chargingStringV2Enabled= */ false);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.DOCKED, ChargingSpeed.REGULAR, /* batteryLevel= */ 51);
        var expectedStatusLabel = "Charging";
        var expectedRemainingLabel = "30 min left until full";
        var expectedChargeLabel = "51% - " + expectedRemainingLabel;

        assertGetBatteryInfo(
                batteryIntent,
                /* currentTimeMillis= */ UNUSED_TIME_MS,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedChargeLabel);
    }

    @Test
    @Ignore
    public void getBatteryInfo_fastChargingV2_updateRemainingLabelAndStatusLabel() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofMinutes(30).toMillis(),
                /* chargingStringV2Enabled= */ true);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.WIRED, ChargingSpeed.FAST, /* batteryLevel= */ 56);
        var expectedStatusLabel = "Fast charging";
        var expectedRemainingLabel = "Full by ";
        var expectedChargeLabel = "56% - " + expectedStatusLabel + " - " + expectedRemainingLabel;
        var currentTimeMillis = Instant.parse("2024-04-01T13:00:00Z").toEpochMilli();

        assertGetBatteryInfo(
                batteryIntent,
                currentTimeMillis,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedChargeLabel);
    }

    @Test
    public void getBatteryInfo_regularChargingV2_updateRemainingLabelAndStatusLabel() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofHours(1).toMillis(),
                /* chargingStringV2Enabled= */ true);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.WIRED, ChargingSpeed.REGULAR, /* batteryLevel= */ 12);
        var expectedStatusLabel = "Charging";
        var expectedRemainingLabel = "Fully charged by ";
        var expectedChargeLabel = "12% - " + expectedRemainingLabel;
        var currentTimeMillis = Instant.parse("2024-04-01T13:00:00Z").toEpochMilli();

        assertGetBatteryInfo(
                batteryIntent,
                currentTimeMillis,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedChargeLabel);
    }

    @Test
    public void getBatteryInfo_slowChargingV2_updateRemainingLabelAndStatusLabel() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofHours(2).toMillis(),
                /* chargingStringV2Enabled= */ true);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.WIRED, ChargingSpeed.SLOW, /* batteryLevel= */ 18);
        var expectedStatusLabel = "Charging";
        var expectedRemainingLabel = "Fully charged by";
        var expectedChargeLabel = "18% - " + expectedRemainingLabel;
        var currentTimeMillis = Instant.parse("2024-04-01T13:00:00Z").toEpochMilli();

        assertGetBatteryInfo(
                batteryIntent,
                currentTimeMillis,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedChargeLabel);
    }

    @Test
    public void getBatteryInfo_wirelessChargingV2_updateRemainingLabelAndStatusLabel() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofHours(1).toMillis(),
                /* chargingStringV2Enabled= */ true);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.WIRELESS, ChargingSpeed.REGULAR, /* batteryLevel= */ 45);
        var expectedStatusLabel = "Charging";
        var expectedRemainingLabel = "Fully charged by";
        var expectedChargeLabel = "45% - " + expectedRemainingLabel;
        var currentTimeMillis = Instant.parse("2024-04-01T15:00:00Z").toEpochMilli();

        assertGetBatteryInfo(
                batteryIntent,
                currentTimeMillis,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedChargeLabel);
    }

    @Test
    public void getBatteryInfo_dockedChargingV2_updateRemainingLabelAndStatusLabel() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofHours(1).toMillis(),
                /* chargingStringV2Enabled= */ true);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.DOCKED, ChargingSpeed.REGULAR, /* batteryLevel= */ 66);
        var expectedStatusLabel = "Charging";
        var expectedRemainingLabel = "Fully charged by";
        var expectedChargeLabel = "66% - " + expectedRemainingLabel;
        var currentTimeMillis = Instant.parse("2021-02-09T13:00:00.00Z").toEpochMilli();

        assertGetBatteryInfo(
                batteryIntent,
                currentTimeMillis,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedChargeLabel);
    }

    @Test
    public void getBatteryInfo_customizedWLCLabel_updateRemainingLabelAndStatusLabel() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofHours(1).toMillis(),
                /* chargingStringV2Enabled= */ true);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.WIRELESS, ChargingSpeed.REGULAR, /* batteryLevel= */ 45);
        var expectedLabel = "Full by 8:00 AM";
        when(mFeatureFactory.batterySettingsFeatureProvider.getWirelessChargingRemainingLabel(
                        eq(mContext), anyLong(), anyLong()))
                .thenReturn(expectedLabel);
        var currentTimeMillis = Instant.parse("2021-02-09T13:00:00.00Z").toEpochMilli();
        var info =
                BatteryInfo.getBatteryInfo(
                        mContext,
                        batteryIntent,
                        mBatteryUsageStats,
                        MOCK_ESTIMATE,
                        /* elapsedRealtimeUs= */ UNUSED_TIME_MS,
                        /* shortString= */ false,
                        /* currentTimeMillis= */ currentTimeMillis);

        assertThat(info.remainingLabel).isEqualTo(expectedLabel);
    }

    @Test
    public void getBatteryInfo_noCustomizedWLCLabel_updateRemainingLabelAndStatusLabel() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofHours(1).toMillis(),
                /* chargingStringV2Enabled= */ true);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.WIRELESS, ChargingSpeed.REGULAR, /* batteryLevel= */ 45);
        when(mFeatureFactory.batterySettingsFeatureProvider.getWirelessChargingRemainingLabel(
                        eq(mContext), anyLong(), anyLong()))
                .thenReturn(null);
        var expectedStatusLabel = "Charging";
        var expectedRemainingLabel = "Fully charged by";
        var expectedChargeLabel = "45% - " + expectedRemainingLabel;
        var currentTimeMillis = Instant.parse("2024-04-01T15:00:00Z").toEpochMilli();

        assertGetBatteryInfo(
                batteryIntent,
                currentTimeMillis,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedChargeLabel);
    }

    @Test
    public void getBatteryInfo_noCustomWirelessChargingLabelWithV1_updateRemainingAndStatusLabel() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofMinutes(130).toMillis(),
                /* chargingStringV2Enabled= */ false);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.WIRELESS, ChargingSpeed.REGULAR, /* batteryLevel= */ 10);
        when(mFeatureFactory.batterySettingsFeatureProvider.getWirelessChargingRemainingLabel(
                        eq(mContext), anyLong(), anyLong()))
                .thenReturn(null);
        var expectedStatusLabel = "Charging wirelessly";
        var expectedRemainingLabel = "2 hr, 10 min left until full";
        var expectedChargeLabel = "10% - " + expectedRemainingLabel;

        assertGetBatteryInfo(
                batteryIntent,
                /* currentTimeMillis= */ UNUSED_TIME_MS,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedChargeLabel);
    }

    @Test
    public void getBatteryInfo_chargeOptimizationMode_updateRemainingAndStatusLabel() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofMinutes(130).toMillis(),
                /* chargingStringV2Enabled= */ false);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.WIRED, ChargingSpeed.REGULAR, /* batteryLevel= */ 65);
        var expectedRemainingLabel = "Expected remaining label";
        var expectedChargeLabel = "65% - " + expectedRemainingLabel;
        when(mFeatureFactory.batterySettingsFeatureProvider.isChargingOptimizationMode(mContext))
                .thenReturn(true);
        when(mFeatureFactory.batterySettingsFeatureProvider.getChargingOptimizationRemainingLabel(
                        eq(mContext), anyInt(), anyInt(), anyLong(), anyLong()))
                .thenReturn(expectedRemainingLabel);
        when(mFeatureFactory.batterySettingsFeatureProvider.getChargingOptimizationChargeLabel(
                        eq(mContext), anyInt(), anyString(), anyLong(), anyLong()))
                .thenReturn(expectedChargeLabel);
        var expectedStatusLabel = "Charging";

        assertGetBatteryInfo(
                batteryIntent,
                /* currentTimeMillis= */ UNUSED_TIME_MS,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedChargeLabel);
    }

    @Test
    public void getBatteryInfo_notChargeOptimizationModeWithV1_updateRemainingAndStatusLabel() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofMinutes(130).toMillis(),
                /* chargingStringV2Enabled= */ false);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.WIRED, ChargingSpeed.REGULAR, /* batteryLevel= */ 65);
        when(mFeatureFactory.batterySettingsFeatureProvider.isChargingOptimizationMode(mContext))
                .thenReturn(false);
        var expectedStatusLabel = "Charging";
        var expectedRemainingLabel = "2 hr, 10 min left until full";
        var expectedChargeLabel = "65% - " + expectedRemainingLabel;

        assertGetBatteryInfo(
                batteryIntent,
                /* currentTimeMillis= */ UNUSED_TIME_MS,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedChargeLabel);
    }

    @Test
    public void getBatteryInfo_notChargeOptimizationModeWithV2_updateRemainingAndStatusLabel() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofMinutes(130).toMillis(),
                /* chargingStringV2Enabled= */ true);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.WIRED, ChargingSpeed.REGULAR, /* batteryLevel= */ 65);
        when(mFeatureFactory.batterySettingsFeatureProvider.isChargingOptimizationMode(mContext))
                .thenReturn(false);
        var expectedStatusLabel = "Charging";
        var expectedRemainingLabel = "Fully charged by";
        var expectedChargeLabel = "65% - " + expectedRemainingLabel;
        var currentTimeMillis = Instant.parse("2024-04-01T15:00:00Z").toEpochMilli();

        assertGetBatteryInfo(
                batteryIntent,
                currentTimeMillis,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedChargeLabel);
    }

    @Test
    public void getBatteryInfo_longlife_shouldSetLonglife() {
        var batteryIntent = createIntentForLongLifeTest(/* hasLongLife= */ true);

        var batteryInfo =
                BatteryInfo.getBatteryInfo(
                        mContext,
                        batteryIntent,
                        mBatteryUsageStats,
                        /* estimate= */ MOCK_ESTIMATE,
                        /* elapsedRealtimeUs= */ 0L,
                        /* shortString= */ false,
                        /* currentTimeMs= */ 0L);

        assertThat(batteryInfo.isLongLife).isTrue();
    }

    @Test
    public void getBatteryInfo_noLonglife_shouldNotLonglife() {
        var batteryIntent = createIntentForLongLifeTest(/* hasLongLife= */ false);

        var batteryInfo =
                BatteryInfo.getBatteryInfo(
                        mContext,
                        batteryIntent,
                        mBatteryUsageStats,
                        /* estimate= */ MOCK_ESTIMATE,
                        /* elapsedRealtimeUs= */ 0L,
                        /* shortString= */ false,
                        /* currentTimeMs= */ 0L);

        assertThat(batteryInfo.isLongLife).isFalse();
    }

    @Test
    public void getBatteryInfo_plugTypeNoneWithLonglifeAndChargeOptimization_chargingString() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofMinutes(130).toMillis(),
                /* chargingStringV2Enabled= */ false);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.NONE,
                        ChargingSpeed.REGULAR,
                        /* batteryLevel= */ 85,
                        BatteryManager.BATTERY_STATUS_DISCHARGING,
                        /* isLonglife= */ true);
        var expectedRemainingLabel = "Expected remaining label";
        var expectedChargeLabel = "85% - " + expectedRemainingLabel;
        when(mFeatureFactory.batterySettingsFeatureProvider.isChargingOptimizationMode(mContext))
                .thenReturn(true);
        when(mFeatureFactory.batterySettingsFeatureProvider.getChargingOptimizationRemainingLabel(
                        eq(mContext), anyInt(), anyInt(), anyLong(), anyLong()))
                .thenReturn(expectedRemainingLabel);
        when(mFeatureFactory.batterySettingsFeatureProvider.getChargingOptimizationChargeLabel(
                        eq(mContext), anyInt(), anyString(), anyLong(), anyLong()))
                .thenReturn(expectedChargeLabel);
        var expectedStatusLabel = "Not charging";

        assertGetBatteryInfo(
                batteryIntent,
                /* currentTimeMillis= */ UNUSED_TIME_MS,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedChargeLabel);
    }

    @Test
    public void getBatteryInfo_plugTypeNoneNotChargeOptimizationLonglife_dischargingString() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofMinutes(130).toMillis(),
                /* chargingStringV2Enabled= */ false);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.NONE,
                        ChargingSpeed.REGULAR,
                        /* batteryLevel= */ 85,
                        BatteryManager.BATTERY_STATUS_DISCHARGING,
                        /* isLonglife= */ true);
        var expectedRemainingLabel =
                PowerUtil.getBatteryRemainingShortStringFormatted(
                        mContext, PowerUtil.convertUsToMs(1000L));
        when(mFeatureFactory.batterySettingsFeatureProvider.isChargingOptimizationMode(mContext))
                .thenReturn(false);
        var expectedStatusLabel = "Not charging";

        assertGetBatteryInfo(
                batteryIntent,
                /* currentTimeMillis= */ UNUSED_TIME_MS,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedRemainingLabel);
    }

    @Test
    public void getBatteryInfo_plugTypeNoneChargeOptimizationNotLonglife_dischargingString() {
        prepareTestGetBatteryInfoEnvironment(
                /* remainingTimeMs= */ Duration.ofMinutes(130).toMillis(),
                /* chargingStringV2Enabled= */ false);
        Intent batteryIntent =
                createIntentForGetBatteryInfoTest(
                        ChargingType.NONE,
                        ChargingSpeed.REGULAR,
                        /* batteryLevel= */ 85,
                        BatteryManager.BATTERY_STATUS_DISCHARGING,
                        /* isLonglife= */ false);
        var expectedRemainingLabel =
                PowerUtil.getBatteryRemainingShortStringFormatted(
                        mContext, PowerUtil.convertUsToMs(1000L));
        when(mFeatureFactory.batterySettingsFeatureProvider.isChargingOptimizationMode(mContext))
                .thenReturn(true);
        var expectedStatusLabel = "Not charging";

        assertGetBatteryInfo(
                batteryIntent,
                /* currentTimeMillis= */ UNUSED_TIME_MS,
                expectedStatusLabel,
                expectedRemainingLabel,
                expectedRemainingLabel);
    }

    private enum ChargingSpeed {
        FAST,
        REGULAR,
        SLOW
    }

    private enum ChargingType {
        WIRED,
        WIRELESS,
        DOCKED,
        NONE
    }

    private static Intent createIntentForLongLifeTest(Boolean hasLongLife) {
        return new Intent(Intent.ACTION_BATTERY_CHANGED)
                .putExtra(
                        BatteryManager.EXTRA_CHARGING_STATUS,
                        hasLongLife
                                ? BatteryManager.CHARGING_POLICY_ADAPTIVE_LONGLIFE
                                : BatteryManager.CHARGING_POLICY_DEFAULT);
    }

    private static Intent createIntentForGetBatteryInfoTest(
            ChargingType chargingType, ChargingSpeed chargingSpeed, int batteryLevel) {
        return createIntentForGetBatteryInfoTest(
                chargingType,
                chargingSpeed,
                batteryLevel,
                BatteryManager.BATTERY_STATUS_CHARGING,
                /* isLonglife= */ false);
    }

    private static Intent createIntentForGetBatteryInfoTest(
            ChargingType chargingType,
            ChargingSpeed chargingSpeed,
            int batteryLevel,
            int chargingStatus,
            boolean isLonglife) {
        return createBatteryIntent(
                        CHARGING_TYPE_MAP.get(chargingType), batteryLevel, chargingStatus)
                .putExtra(
                        BatteryManager.EXTRA_MAX_CHARGING_CURRENT,
                        CHARGING_SPEED_MAP.get(chargingSpeed))
                .putExtra(BatteryManager.EXTRA_MAX_CHARGING_VOLTAGE, 5000000)
                .putExtra(
                        BatteryManager.EXTRA_CHARGING_STATUS,
                        isLonglife
                                ? BatteryManager.CHARGING_POLICY_ADAPTIVE_LONGLIFE
                                : BatteryManager.CHARGING_POLICY_DEFAULT);
    }

    private void prepareTestGetBatteryInfoEnvironment(
            long remainingTimeMs, boolean chargingStringV2Enabled) {
        when(mBatteryUsageStats.getChargeTimeRemainingMs()).thenReturn(remainingTimeMs);
        SystemProperties.set(
                com.android.settingslib.fuelgauge.BatteryUtils.PROPERTY_CHARGING_STRING_V2_KEY,
                String.valueOf(chargingStringV2Enabled));
        Settings.Global.putInt(
                mContext.getContentResolver(),
                BatteryUtils.SETTINGS_GLOBAL_DOCK_DEFENDER_BYPASS,
                1);
    }

    private void assertGetBatteryInfo(
            Intent batteryIntent,
            long currentTimeMillis,
            String expectedStatusLabel,
            String expectedRemainingLabel,
            String expectedChargeLabel) {
        mContext.getResources().getConfiguration().setLocale(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        var info =
                BatteryInfo.getBatteryInfo(
                        mContext,
                        batteryIntent,
                        mBatteryUsageStats,
                        MOCK_ESTIMATE,
                        /* elapsedRealtimeUs= */ UNUSED_TIME_MS,
                        /* shortString= */ false,
                        /* currentTimeMillis= */ currentTimeMillis);

        assertWithMessage("statusLabel is incorrect")
                .that(info.statusLabel)
                .isEqualTo(expectedStatusLabel);
        assertWithMessage("remainingLabel is incorrect")
                .that(info.remainingLabel.toString())
                .contains(expectedRemainingLabel);
        assertWithMessage("chargeLabel is incorrect")
                .that(info.chargeLabel.toString())
                .contains(expectedChargeLabel);
    }

    private static Intent createBatteryIntent(int plugged, int level, int status) {
        return new Intent()
                .putExtra(BatteryManager.EXTRA_PLUGGED, plugged)
                .putExtra(BatteryManager.EXTRA_LEVEL, level)
                .putExtra(BatteryManager.EXTRA_SCALE, 100)
                .putExtra(BatteryManager.EXTRA_STATUS, status);
    }

    // Make our battery stats return a sequence of battery events.
    private void mockBatteryStatsHistory() {
        // Mock out new data every time iterateBatteryStatsHistory is called.
        doAnswer(
                        invocation -> {
                            BatteryStatsHistoryIterator iterator =
                                    mock(BatteryStatsHistoryIterator.class);
                            when(iterator.next())
                                    .thenReturn(
                                            makeHistoryIterm(1000, 99),
                                            makeHistoryIterm(1500, 98),
                                            makeHistoryIterm(2000, 97),
                                            null);
                            return iterator;
                        })
                .when(mBatteryUsageStats)
                .iterateBatteryStatsHistory();
    }

    private BatteryStats.HistoryItem makeHistoryIterm(long time, int batteryLevel) {
        BatteryStats.HistoryItem record = new BatteryStats.HistoryItem();
        record.cmd = BatteryStats.HistoryItem.CMD_UPDATE;
        record.time = time;
        record.batteryLevel = (byte) batteryLevel;
        return record;
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
        doReturn(pointsExpected)
                .when(mFeatureFactory.powerUsageFeatureProvider)
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
            doReturn(1000L).when(mBatteryUsageStats).getChargeTimeRemainingMs();
        } else {
            doReturn(0L).when(mBatteryUsageStats).getChargeTimeRemainingMs();
        }
        Estimate batteryEstimate =
                new Estimate(
                        estimate ? 1000 : 0,
                        false /* isBasedOnUsage */,
                        1000 /* averageDischargeTime */);
        BatteryInfo info =
                BatteryInfo.getBatteryInfo(
                        mContext,
                        charging ? mChargingBatteryBroadcast : mDisChargingBatteryBroadcast,
                        mBatteryUsageStats,
                        batteryEstimate,
                        SystemClock.elapsedRealtime() * 1000,
                        false);
        doReturn(enhanced)
                .when(mFeatureFactory.powerUsageFeatureProvider)
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
