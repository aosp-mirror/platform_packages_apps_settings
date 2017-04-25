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
import android.os.BatteryStats;
import android.os.Process;

import com.android.internal.os.BatterySipper;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static android.os.BatteryStats.Uid.PROCESS_STATE_BACKGROUND;
import static android.os.BatteryStats.Uid.PROCESS_STATE_FOREGROUND;
import static android.os.BatteryStats.Uid.PROCESS_STATE_FOREGROUND_SERVICE;
import static android.os.BatteryStats.Uid.PROCESS_STATE_TOP;
import static android.os.BatteryStats.Uid.PROCESS_STATE_TOP_SLEEPING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BatteryUtilsTest {
    // unit that used to converted ms to us
    private static final long UNIT = 1000;
    private static final long TIME_STATE_TOP = 1500 * UNIT;
    private static final long TIME_STATE_FOREGROUND_SERVICE = 2000 * UNIT;
    private static final long TIME_STATE_TOP_SLEEPING = 2500 * UNIT;
    private static final long TIME_STATE_FOREGROUND = 3000 * UNIT;
    private static final long TIME_STATE_BACKGROUND = 6000 * UNIT;

    private static final int UID = 123;
    private static final long TIME_EXPECTED_FOREGROUND = 6500;
    private static final long TIME_EXPECTED_BACKGROUND = 6000;
    private static final long TIME_EXPECTED_ALL = 12500;
    private static final double BATTERY_SCREEN_USAGE = 300;
    private static final double BATTERY_SYSTEM_USAGE = 600;
    private static final double BATTERY_OVERACCOUNTED_USAGE = 500;
    private static final double BATTERY_UNACCOUNTED_USAGE = 700;
    private static final double TOTAL_BATTERY_USAGE = 1000;
    private static final double HIDDEN_USAGE = 200;
    private static final int DISCHARGE_AMOUNT = 80;
    private static final double PERCENT_SYSTEM_USAGE = 60;
    private static final double PRECISION = 0.001;

    @Mock
    private BatteryStats.Uid mUid;
    @Mock
    private BatterySipper mNormalBatterySipper;
    @Mock
    private BatterySipper mScreenBatterySipper;
    @Mock
    private BatterySipper mOvercountedBatterySipper;
    @Mock
    private BatterySipper mUnaccountedBatterySipper;
    @Mock
    private BatterySipper mSystemBatterySipper;
    @Mock
    private BatterySipper mCellBatterySipper;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private BatteryUtils mBatteryUtils;
    private FakeFeatureFactory mFeatureFactory;
    private PowerUsageFeatureProvider mProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mProvider = mFeatureFactory.powerUsageFeatureProvider;

        doReturn(TIME_STATE_TOP).when(mUid).getProcessStateTime(eq(PROCESS_STATE_TOP), anyLong(),
                anyInt());
        doReturn(TIME_STATE_FOREGROUND_SERVICE).when(mUid).getProcessStateTime(
                eq(PROCESS_STATE_FOREGROUND_SERVICE), anyLong(), anyInt());
        doReturn(TIME_STATE_TOP_SLEEPING).when(mUid).getProcessStateTime(
                eq(PROCESS_STATE_TOP_SLEEPING), anyLong(), anyInt());
        doReturn(TIME_STATE_FOREGROUND).when(mUid).getProcessStateTime(eq(PROCESS_STATE_FOREGROUND),
                anyLong(), anyInt());
        doReturn(TIME_STATE_BACKGROUND).when(mUid).getProcessStateTime(eq(PROCESS_STATE_BACKGROUND),
                anyLong(), anyInt());

        mNormalBatterySipper.drainType = BatterySipper.DrainType.APP;
        mNormalBatterySipper.totalPowerMah = TOTAL_BATTERY_USAGE;

        mScreenBatterySipper.drainType = BatterySipper.DrainType.SCREEN;
        mScreenBatterySipper.totalPowerMah = BATTERY_SCREEN_USAGE;

        mSystemBatterySipper.drainType = BatterySipper.DrainType.APP;
        mSystemBatterySipper.totalPowerMah = BATTERY_SYSTEM_USAGE;
        when(mSystemBatterySipper.getUid()).thenReturn(Process.SYSTEM_UID);

        mOvercountedBatterySipper.drainType = BatterySipper.DrainType.OVERCOUNTED;
        mOvercountedBatterySipper.totalPowerMah = BATTERY_OVERACCOUNTED_USAGE;

        mUnaccountedBatterySipper.drainType = BatterySipper.DrainType.UNACCOUNTED;
        mUnaccountedBatterySipper.totalPowerMah = BATTERY_UNACCOUNTED_USAGE;

        mBatteryUtils = BatteryUtils.getInstance(RuntimeEnvironment.application);
        mBatteryUtils.mPowerUsageFeatureProvider = mProvider;

        mBatteryUtils = spy(new BatteryUtils(RuntimeEnvironment.application));
    }

    @Test
    public void testGetProcessTimeMs_typeForeground_timeCorrect() {
        final long time = mBatteryUtils.getProcessTimeMs(BatteryUtils.StatusType.FOREGROUND, mUid,
                BatteryStats.STATS_SINCE_CHARGED);

        assertThat(time).isEqualTo(TIME_EXPECTED_FOREGROUND);
    }

    @Test
    public void testGetProcessTimeMs_typeBackground_timeCorrect() {
        final long time = mBatteryUtils.getProcessTimeMs(BatteryUtils.StatusType.BACKGROUND, mUid,
                BatteryStats.STATS_SINCE_CHARGED);

        assertThat(time).isEqualTo(TIME_EXPECTED_BACKGROUND);
    }

    @Test
    public void testGetProcessTimeMs_typeAll_timeCorrect() {
        final long time = mBatteryUtils.getProcessTimeMs(BatteryUtils.StatusType.ALL, mUid,
                BatteryStats.STATS_SINCE_CHARGED);

        assertThat(time).isEqualTo(TIME_EXPECTED_ALL);
    }

    @Test
    public void testGetProcessTimeMs_uidNull_returnZero() {
        final long time = mBatteryUtils.getProcessTimeMs(BatteryUtils.StatusType.ALL, null,
                BatteryStats.STATS_SINCE_CHARGED);

        assertThat(time).isEqualTo(0);
    }

    @Test
    public void testRemoveHiddenBatterySippers_ContainsHiddenSippers_RemoveAndReturnValue() {
        final List<BatterySipper> sippers = new ArrayList<>();
        sippers.add(mNormalBatterySipper);
        sippers.add(mScreenBatterySipper);
        sippers.add(mSystemBatterySipper);
        sippers.add(mOvercountedBatterySipper);
        sippers.add(mUnaccountedBatterySipper);
        when(mProvider.isTypeSystem(mSystemBatterySipper))
                .thenReturn(true);

        final double totalUsage = mBatteryUtils.removeHiddenBatterySippers(sippers);
        assertThat(sippers).containsExactly(mNormalBatterySipper);
        assertThat(totalUsage).isWithin(PRECISION).of(
                BATTERY_SCREEN_USAGE + BATTERY_SYSTEM_USAGE + BATTERY_UNACCOUNTED_USAGE);
    }

    @Test
    public void testShouldHideSipper_TypeUnAccounted_ReturnTrue() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.UNACCOUNTED;
        assertThat(mBatteryUtils.shouldHideSipper(mNormalBatterySipper)).isTrue();
    }

    @Test
    public void testShouldHideSipper_TypeOverAccounted_ReturnTrue() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.OVERCOUNTED;
        assertThat(mBatteryUtils.shouldHideSipper(mNormalBatterySipper)).isTrue();
    }

    @Test
    public void testShouldHideSipper_TypeIdle_ReturnTrue() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.IDLE;
        assertThat(mBatteryUtils.shouldHideSipper(mNormalBatterySipper)).isTrue();
    }

    @Test
    public void testShouldHideSipper_TypeWifi_ReturnTrue() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.WIFI;
        assertThat(mBatteryUtils.shouldHideSipper(mNormalBatterySipper)).isTrue();
    }

    @Test
    public void testShouldHideSipper_TypeCell_ReturnTrue() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.CELL;
        assertThat(mBatteryUtils.shouldHideSipper(mNormalBatterySipper)).isTrue();
    }

    @Test
    public void testShouldHideSipper_TypeScreen_ReturnTrue() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.SCREEN;
        assertThat(mBatteryUtils.shouldHideSipper(mNormalBatterySipper)).isTrue();
    }

    @Test
    public void testShouldHideSipper_TypeBluetooth_ReturnTrue() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.BLUETOOTH;
        assertThat(mBatteryUtils.shouldHideSipper(mNormalBatterySipper)).isTrue();
    }

    @Test
    public void testShouldHideSipper_TypeSystem_ReturnTrue() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.APP;
        when(mNormalBatterySipper.getUid()).thenReturn(Process.ROOT_UID);
        when(mProvider.isTypeSystem(any())).thenReturn(true);
        assertThat(mBatteryUtils.shouldHideSipper(mNormalBatterySipper)).isTrue();
    }

    @Test
    public void testShouldHideSipper_UidNormal_ReturnFalse() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.APP;
        when(mNormalBatterySipper.getUid()).thenReturn(UID);
        assertThat(mBatteryUtils.shouldHideSipper(mNormalBatterySipper)).isFalse();
    }

    @Test
    public void testShouldHideSipper_TypeService_ReturnTrue() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.APP;
        when(mNormalBatterySipper.getUid()).thenReturn(UID);
        when(mProvider.isTypeService(any())).thenReturn(true);

        assertThat(mBatteryUtils.shouldHideSipper(mNormalBatterySipper)).isTrue();
    }

    @Test
    public void testCalculateBatteryPercent() {
        assertThat(mBatteryUtils.calculateBatteryPercent(BATTERY_SYSTEM_USAGE, TOTAL_BATTERY_USAGE,
                HIDDEN_USAGE, DISCHARGE_AMOUNT))
                .isWithin(PRECISION).of(PERCENT_SYSTEM_USAGE);
    }
}
