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

import static android.os.BatteryStats.Uid.PROCESS_STATE_BACKGROUND;
import static android.os.BatteryStats.Uid.PROCESS_STATE_FOREGROUND;
import static android.os.BatteryStats.Uid.PROCESS_STATE_FOREGROUND_SERVICE;
import static android.os.BatteryStats.Uid.PROCESS_STATE_TOP;
import static android.os.BatteryStats.Uid.PROCESS_STATE_TOP_SLEEPING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.BatteryStats;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserManager;
import android.text.format.DateUtils;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper;
import com.android.settings.fuelgauge.batterytip.AnomalyInfo;
import com.android.settings.fuelgauge.batterytip.BatteryDatabaseManager;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settingslib.fuelgauge.Estimate;
import com.android.settingslib.fuelgauge.PowerWhitelistBackend;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BatteryUtilsTest {

    private static final String TAG = "BatteryUtilsTest";

    // unit that used to converted ms to us
    private static final long UNIT = 1000;
    private static final long TIME_STATE_TOP = 1500 * UNIT;
    private static final long TIME_STATE_FOREGROUND_SERVICE = 2000 * UNIT;
    private static final long TIME_STATE_TOP_SLEEPING = 2500 * UNIT;
    private static final long TIME_STATE_FOREGROUND = 3000 * UNIT;
    private static final long TIME_STATE_BACKGROUND = 6000 * UNIT;
    private static final long TIME_FOREGROUND_ZERO = 0;
    private static final long TIME_FOREGROUND = 100 * DateUtils.MINUTE_IN_MILLIS;
    private static final long TIME_SINCE_LAST_FULL_CHARGE_MS = 120 * 60 * 1000;
    private static final long TIME_SINCE_LAST_FULL_CHARGE_US =
            TIME_SINCE_LAST_FULL_CHARGE_MS * 1000;

    private static final int UID = 12345;
    private static final long TIME_EXPECTED_FOREGROUND = 1500;
    private static final long TIME_EXPECTED_BACKGROUND = 6000;
    private static final long TIME_EXPECTED_ALL = 7500;
    private static final double BATTERY_SCREEN_USAGE = 300;
    private static final double BATTERY_IDLE_USAGE = 600;
    private static final double BATTERY_SYSTEM_USAGE = 600;
    private static final double BATTERY_OVERACCOUNTED_USAGE = 500;
    private static final double BATTERY_UNACCOUNTED_USAGE = 700;
    private static final double BATTERY_APP_USAGE = 100;
    private static final double BATTERY_WIFI_USAGE = 200;
    private static final double BATTERY_BLUETOOTH_USAGE = 300;
    private static final double TOTAL_BATTERY_USAGE = 1000;
    private static final double HIDDEN_USAGE = 200;
    private static final int DISCHARGE_AMOUNT = 80;
    private static final double PERCENT_SYSTEM_USAGE = 60;
    private static final double PRECISION = 0.001;
    private static final int SDK_VERSION = Build.VERSION_CODES.L;
    private static final String PACKAGE_NAME = "com.android.app";
    private static final String HIGH_SDK_PACKAGE = "com.android.package.high";
    private static final String LOW_SDK_PACKAGE = "com.android.package.low";

    private static final String INFO_EXCESSIVE = "anomaly_type=4,auto_restriction=false";
    private static final String INFO_WAKELOCK = "anomaly_type=1,auto_restriction=false";

    @Mock
    private BatteryStats.Uid mUid;
    @Mock
    private BatteryStats.Timer mTimer;
    @Mock
    private BatterySipper mNormalBatterySipper;
    @Mock
    private BatterySipper mWifiBatterySipper;
    @Mock
    private BatterySipper mBluetoothBatterySipper;
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
    @Mock
    private BatterySipper mIdleBatterySipper;
    @Mock
    private Bundle mBundle;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private AppOpsManager mAppOpsManager;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BatteryStatsHelper mBatteryStatsHelper;
    @Mock
    private ApplicationInfo mHighApplicationInfo;
    @Mock
    private ApplicationInfo mLowApplicationInfo;
    @Mock
    private PowerWhitelistBackend mPowerWhitelistBackend;
    @Mock
    private BatteryDatabaseManager mBatteryDatabaseManager;
    private AnomalyInfo mAnomalyInfo;
    private BatteryUtils mBatteryUtils;
    private FakeFeatureFactory mFeatureFactory;
    private PowerUsageFeatureProvider mProvider;
    private List<BatterySipper> mUsageList;
    private Context mContext;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
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
        when(mBatteryStatsHelper.getStats().computeBatteryRealtime(anyLong(), anyInt())).thenReturn(
                TIME_SINCE_LAST_FULL_CHARGE_US);

        when(mPackageManager.getApplicationInfo(eq(HIGH_SDK_PACKAGE), anyInt()))
                .thenReturn(mHighApplicationInfo);
        when(mPackageManager.getApplicationInfo(eq(LOW_SDK_PACKAGE), anyInt()))
                .thenReturn(mLowApplicationInfo);
        mHighApplicationInfo.targetSdkVersion = Build.VERSION_CODES.O;
        mLowApplicationInfo.targetSdkVersion = Build.VERSION_CODES.L;

        mNormalBatterySipper.drainType = BatterySipper.DrainType.APP;
        mNormalBatterySipper.totalPowerMah = TOTAL_BATTERY_USAGE;
        doReturn(UID).when(mNormalBatterySipper).getUid();

        mWifiBatterySipper.drainType = BatterySipper.DrainType.WIFI;
        mWifiBatterySipper.totalPowerMah = BATTERY_WIFI_USAGE;

        mBluetoothBatterySipper.drainType = BatterySipper.DrainType.BLUETOOTH;
        mBluetoothBatterySipper.totalPowerMah = BATTERY_BLUETOOTH_USAGE;

        mScreenBatterySipper.drainType = BatterySipper.DrainType.SCREEN;
        mScreenBatterySipper.totalPowerMah = BATTERY_SCREEN_USAGE;

        mSystemBatterySipper.drainType = BatterySipper.DrainType.APP;
        mSystemBatterySipper.totalPowerMah = BATTERY_SYSTEM_USAGE;
        when(mSystemBatterySipper.getUid()).thenReturn(Process.SYSTEM_UID);

        mOvercountedBatterySipper.drainType = BatterySipper.DrainType.OVERCOUNTED;
        mOvercountedBatterySipper.totalPowerMah = BATTERY_OVERACCOUNTED_USAGE;

        mUnaccountedBatterySipper.drainType = BatterySipper.DrainType.UNACCOUNTED;
        mUnaccountedBatterySipper.totalPowerMah = BATTERY_UNACCOUNTED_USAGE;

        mIdleBatterySipper.drainType = BatterySipper.DrainType.IDLE;
        mIdleBatterySipper.totalPowerMah = BATTERY_IDLE_USAGE;

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(mAppOpsManager).when(mContext).getSystemService(Context.APP_OPS_SERVICE);
        mBatteryUtils = spy(new BatteryUtils(mContext));
        mBatteryUtils.mPowerUsageFeatureProvider = mProvider;
        doReturn(0L).when(mBatteryUtils)
            .getForegroundServiceTotalTimeUs(any(BatteryStats.Uid.class), anyLong());
        mAnomalyInfo = new AnomalyInfo(INFO_WAKELOCK);

        mUsageList = new ArrayList<>();
        mUsageList.add(mNormalBatterySipper);
        mUsageList.add(mScreenBatterySipper);
        mUsageList.add(mCellBatterySipper);
        when(mBatteryStatsHelper.getUsageList()).thenReturn(mUsageList);
        when(mBatteryStatsHelper.getTotalPower())
            .thenReturn(TOTAL_BATTERY_USAGE + BATTERY_SCREEN_USAGE);
        when(mBatteryStatsHelper.getStats().getDischargeAmount(anyInt()))
            .thenReturn(DISCHARGE_AMOUNT);
        BatteryDatabaseManager.setUpForTest(mBatteryDatabaseManager);
        ShadowThreadUtils.setIsMainThread(true);
    }

    @Test
    public void testGetProcessTimeMs_typeForeground_timeCorrect() {
        doReturn(TIME_STATE_FOREGROUND + 500).when(mBatteryUtils)
            .getForegroundActivityTotalTimeUs(eq(mUid), anyLong());

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
        doReturn(TIME_STATE_FOREGROUND + 500).when(mBatteryUtils)
            .getForegroundActivityTotalTimeUs(eq(mUid), anyLong());

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
        sippers.add(mWifiBatterySipper);
        sippers.add(mBluetoothBatterySipper);
        sippers.add(mIdleBatterySipper);
        when(mProvider.isTypeSystem(mSystemBatterySipper)).thenReturn(true);
        doNothing().when(mBatteryUtils).smearScreenBatterySipper(any(), any());

        final double totalUsage = mBatteryUtils.removeHiddenBatterySippers(sippers);

        assertThat(sippers).containsExactly(mNormalBatterySipper);
        assertThat(totalUsage).isWithin(PRECISION).of(BATTERY_SYSTEM_USAGE);
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
    public void testShouldHideSipper_TypeWifi_ReturnTrue() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.WIFI;
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
    public void testShouldHideSipper_hiddenSystemModule_ReturnTrue() {
        mNormalBatterySipper.drainType = BatterySipper.DrainType.APP;
        when(mNormalBatterySipper.getUid()).thenReturn(UID);
        when(mBatteryUtils.isHiddenSystemModule(mNormalBatterySipper)).thenReturn(true);

        assertThat(mBatteryUtils.shouldHideSipper(mNormalBatterySipper)).isTrue();
    }

    @Test
    public void testCalculateBatteryPercent() {
        assertThat(mBatteryUtils.calculateBatteryPercent(BATTERY_SYSTEM_USAGE, TOTAL_BATTERY_USAGE,
                HIDDEN_USAGE, DISCHARGE_AMOUNT))
                .isWithin(PRECISION).of(PERCENT_SYSTEM_USAGE);
    }

    @Test
    public void testSmearScreenBatterySipper() {
        final BatterySipper sipperNull = createTestSmearBatterySipper(TIME_FOREGROUND_ZERO,
                BATTERY_APP_USAGE, 0 /* uid */, true /* isUidNull */);
        final BatterySipper sipperBg = createTestSmearBatterySipper(TIME_FOREGROUND_ZERO,
                BATTERY_APP_USAGE, 1 /* uid */, false /* isUidNull */);
        final BatterySipper sipperFg = createTestSmearBatterySipper(TIME_FOREGROUND,
                BATTERY_APP_USAGE, 2 /* uid */, false /* isUidNull */);
        final BatterySipper sipperFg2 = createTestSmearBatterySipper(TIME_FOREGROUND,
                BATTERY_APP_USAGE, 3 /* uid */, false /* isUidNull */);

        final List<BatterySipper> sippers = new ArrayList<>();
        sippers.add(sipperNull);
        sippers.add(sipperBg);
        sippers.add(sipperFg);
        sippers.add(sipperFg2);

        mBatteryUtils.smearScreenBatterySipper(sippers, mScreenBatterySipper);

        assertThat(sipperNull.totalPowerMah).isWithin(PRECISION).of(BATTERY_APP_USAGE);
        assertThat(sipperBg.totalPowerMah).isWithin(PRECISION).of(BATTERY_APP_USAGE);
        assertThat(sipperFg.totalPowerMah).isWithin(PRECISION).of(
                BATTERY_APP_USAGE + BATTERY_SCREEN_USAGE / 2);
        assertThat(sipperFg2.totalPowerMah).isWithin(PRECISION).of(
                BATTERY_APP_USAGE + BATTERY_SCREEN_USAGE / 2);
    }

    @Test
    public void testSmearScreenBatterySipper_screenSipperNull_shouldNotCrash() {
        final BatterySipper sipperFg = createTestSmearBatterySipper(TIME_FOREGROUND,
                BATTERY_APP_USAGE, 2 /* uid */, false /* isUidNull */);

        final List<BatterySipper> sippers = new ArrayList<>();
        sippers.add(sipperFg);

        // Shouldn't crash
        mBatteryUtils.smearScreenBatterySipper(sippers, null /* screenSipper */);
    }

    @Test
    public void testCalculateRunningTimeBasedOnStatsType() {
        assertThat(mBatteryUtils.calculateRunningTimeBasedOnStatsType(mBatteryStatsHelper,
                BatteryStats.STATS_SINCE_CHARGED)).isEqualTo(TIME_SINCE_LAST_FULL_CHARGE_MS);
    }

    @Test
    public void testSortUsageList() {
        final List<BatterySipper> sippers = new ArrayList<>();
        sippers.add(mNormalBatterySipper);
        sippers.add(mScreenBatterySipper);
        sippers.add(mSystemBatterySipper);

        mBatteryUtils.sortUsageList(sippers);

        assertThat(sippers).containsExactly(mNormalBatterySipper, mSystemBatterySipper,
                mScreenBatterySipper);
    }

    @Test
    public void testCalculateLastFullChargeTime() {
        final long currentTimeMs = System.currentTimeMillis();
        when(mBatteryStatsHelper.getStats().getStartClockTime()).thenReturn(
                currentTimeMs - TIME_SINCE_LAST_FULL_CHARGE_MS);

        assertThat(mBatteryUtils.calculateLastFullChargeTime(
                mBatteryStatsHelper, currentTimeMs)).isEqualTo(TIME_SINCE_LAST_FULL_CHARGE_MS);
    }

    @Test
    public void testGetForegroundActivityTotalTimeMs_returnMilliseconds() {
        final long rawRealtimeUs = SystemClock.elapsedRealtime() * 1000;
        doReturn(mTimer).when(mUid).getForegroundActivityTimer();
        doReturn(TIME_SINCE_LAST_FULL_CHARGE_US).when(mTimer)
                .getTotalTimeLocked(rawRealtimeUs, BatteryStats.STATS_SINCE_CHARGED);

        assertThat(mBatteryUtils.getForegroundActivityTotalTimeUs(mUid, rawRealtimeUs)).isEqualTo(
                TIME_SINCE_LAST_FULL_CHARGE_US);
    }

    @Test
    public void testGetTargetSdkVersion_packageExist_returnSdk() throws
            PackageManager.NameNotFoundException {
        doReturn(mApplicationInfo).when(mPackageManager).getApplicationInfo(PACKAGE_NAME,
                PackageManager.GET_META_DATA);
        mApplicationInfo.targetSdkVersion = SDK_VERSION;

        assertThat(mBatteryUtils.getTargetSdkVersion(PACKAGE_NAME)).isEqualTo(SDK_VERSION);
    }

    @Test
    public void testGetTargetSdkVersion_packageNotExist_returnSdkNull() throws
            PackageManager.NameNotFoundException {
        doThrow(new PackageManager.NameNotFoundException()).when(
                mPackageManager).getApplicationInfo(PACKAGE_NAME, PackageManager.GET_META_DATA);

        assertThat(mBatteryUtils.getTargetSdkVersion(PACKAGE_NAME)).isEqualTo(
                BatteryUtils.SDK_NULL);
    }

    @Test
    public void testBackgroundRestrictionOn_restrictionOn_returnTrue() {
        doReturn(AppOpsManager.MODE_IGNORED).when(mAppOpsManager).checkOpNoThrow(
                AppOpsManager.OP_RUN_IN_BACKGROUND, UID, PACKAGE_NAME);

        assertThat(mBatteryUtils.isBackgroundRestrictionEnabled(SDK_VERSION, UID,
                PACKAGE_NAME)).isTrue();
    }

    @Test
    public void testBackgroundRestrictionOn_restrictionOff_returnFalse() {
        doReturn(AppOpsManager.MODE_ALLOWED).when(mAppOpsManager).checkOpNoThrow(
                AppOpsManager.OP_RUN_IN_BACKGROUND, UID, PACKAGE_NAME);

        assertThat(mBatteryUtils.isBackgroundRestrictionEnabled(SDK_VERSION, UID, PACKAGE_NAME))
            .isFalse();
    }

    private BatterySipper createTestSmearBatterySipper(
        long topTime, double totalPowerMah, int uidCode, boolean isUidNull) {
        final BatterySipper sipper = mock(BatterySipper.class);
        sipper.drainType = BatterySipper.DrainType.APP;
        sipper.totalPowerMah = totalPowerMah;
        doReturn(uidCode).when(sipper).getUid();
        if (!isUidNull) {
            final BatteryStats.Uid uid = mock(BatteryStats.Uid.class, RETURNS_DEEP_STUBS);
            doReturn(topTime).when(mBatteryUtils).getProcessTimeMs(
                    eq(BatteryUtils.StatusType.SCREEN_USAGE), eq(uid), anyInt());
            doReturn(uidCode).when(uid).getUid();
            sipper.uidObj = uid;
        }

        return sipper;
    }

    @Test
    public void testInitBatteryStatsHelper_init() {
        mBatteryUtils.initBatteryStatsHelper(mBatteryStatsHelper, mBundle, mUserManager);

        verify(mBatteryStatsHelper).create(mBundle);
        verify(mBatteryStatsHelper).refreshStats(BatteryStats.STATS_SINCE_CHARGED,
                mUserManager.getUserProfiles());
    }

    @Test
    public void testFindBatterySipperByType_findTypeScreen() {
        BatterySipper sipper = mBatteryUtils.findBatterySipperByType(mUsageList,
                BatterySipper.DrainType.SCREEN);

        assertThat(sipper).isSameAs(mScreenBatterySipper);
    }

    @Test
    public void testFindBatterySipperByType_findTypeApp() {
        BatterySipper sipper = mBatteryUtils.findBatterySipperByType(mUsageList,
                BatterySipper.DrainType.APP);

        assertThat(sipper).isSameAs(mNormalBatterySipper);
    }

    @Test
    public void testCalculateScreenUsageTime_returnCorrectTime() {
        mScreenBatterySipper.usageTimeMs = TIME_EXPECTED_FOREGROUND;

        assertThat(mBatteryUtils.calculateScreenUsageTime(mBatteryStatsHelper)).isEqualTo(
                TIME_EXPECTED_FOREGROUND);
    }

    @Test
    public void testIsPreOApp_SdkLowerThanO_ReturnTrue() {
        assertThat(mBatteryUtils.isPreOApp(LOW_SDK_PACKAGE)).isTrue();
    }

    @Test
    public void testIsPreOApp_SdkLargerOrEqualThanO_ReturnFalse() {
        assertThat(mBatteryUtils.isPreOApp(HIGH_SDK_PACKAGE)).isFalse();
    }

    @Test
    public void testIsPreOApp_containPreOApp_ReturnTrue() {
        assertThat(
                mBatteryUtils.isPreOApp(new String[]{HIGH_SDK_PACKAGE, LOW_SDK_PACKAGE})).isTrue();
    }

    @Test
    public void testIsPreOApp_emptyList_ReturnFalse() {
        assertThat(mBatteryUtils.isPreOApp(new String[]{})).isFalse();
    }

    @Ignore
    @Test
    public void testSetForceAppStandby_forcePreOApp_forceTwoRestrictions() {
        mBatteryUtils.setForceAppStandby(UID, LOW_SDK_PACKAGE, AppOpsManager.MODE_IGNORED);

        // Restrict both OP_RUN_IN_BACKGROUND and OP_RUN_ANY_IN_BACKGROUND
        verify(mAppOpsManager).setMode(AppOpsManager.OP_RUN_IN_BACKGROUND, UID, LOW_SDK_PACKAGE,
                AppOpsManager.MODE_IGNORED);
        verify(mAppOpsManager).setMode(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, UID, LOW_SDK_PACKAGE,
                AppOpsManager.MODE_IGNORED);
    }

    @Ignore
    @Test
    public void testSetForceAppStandby_forceOApp_forceOneRestriction() {
        mBatteryUtils.setForceAppStandby(UID, HIGH_SDK_PACKAGE, AppOpsManager.MODE_IGNORED);

        // Don't restrict OP_RUN_IN_BACKGROUND because it is already been restricted for O app
        verify(mAppOpsManager, never()).setMode(AppOpsManager.OP_RUN_IN_BACKGROUND, UID,
                HIGH_SDK_PACKAGE, AppOpsManager.MODE_IGNORED);
        // Restrict OP_RUN_ANY_IN_BACKGROUND
        verify(mAppOpsManager).setMode(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, UID,
                HIGH_SDK_PACKAGE, AppOpsManager.MODE_IGNORED);
    }

    @Test
    public void testSetForceAppStandby_restrictApp_recordTime() {
        mBatteryUtils.setForceAppStandby(UID, HIGH_SDK_PACKAGE, AppOpsManager.MODE_IGNORED);

        verify(mBatteryDatabaseManager).insertAction(
                eq(AnomalyDatabaseHelper.ActionType.RESTRICTION), eq(UID),
                eq(HIGH_SDK_PACKAGE), anyLong());
    }

    @Test
    public void testSetForceAppStandby_unrestrictApp_deleteTime() {
        mBatteryUtils.setForceAppStandby(UID, HIGH_SDK_PACKAGE, AppOpsManager.MODE_ALLOWED);

        verify(mBatteryDatabaseManager).deleteAction(AnomalyDatabaseHelper.ActionType.RESTRICTION,
                UID, HIGH_SDK_PACKAGE);
    }

    @Test
    public void testIsForceAppStandbyEnabled_enabled_returnTrue() {
        when(mAppOpsManager.checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, UID,
                PACKAGE_NAME)).thenReturn(AppOpsManager.MODE_IGNORED);

        assertThat(mBatteryUtils.isForceAppStandbyEnabled(UID, PACKAGE_NAME)).isTrue();
    }

    @Test
    public void testIsForceAppStandbyEnabled_disabled_returnFalse() {
        when(mAppOpsManager.checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, UID,
                PACKAGE_NAME)).thenReturn(AppOpsManager.MODE_ALLOWED);

        assertThat(mBatteryUtils.isForceAppStandbyEnabled(UID, PACKAGE_NAME)).isFalse();
    }

    @Test
    public void testShouldHideAnomaly_systemAppWithLauncher_returnTrue() {
        final List<ResolveInfo> resolveInfos = new ArrayList<>();
        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = HIGH_SDK_PACKAGE;

        doReturn(resolveInfos).when(mPackageManager).queryIntentActivities(any(), anyInt());
        doReturn(new String[]{HIGH_SDK_PACKAGE}).when(mPackageManager).getPackagesForUid(UID);
        mHighApplicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;

        assertThat(mBatteryUtils.shouldHideAnomaly(mPowerWhitelistBackend, UID,
                mAnomalyInfo)).isTrue();
    }

    @Test
    public void testShouldHideAnomaly_systemAppWithoutLauncher_returnTrue() {
        doReturn(new ArrayList<>()).when(mPackageManager).queryIntentActivities(any(), anyInt());
        doReturn(new String[]{HIGH_SDK_PACKAGE}).when(mPackageManager).getPackagesForUid(UID);
        mHighApplicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;

        assertThat(mBatteryUtils.shouldHideAnomaly(mPowerWhitelistBackend, UID,
                mAnomalyInfo)).isTrue();
    }

    @Test
    public void testShouldHideAnomaly_systemUid_returnTrue() {
        final int systemUid = Process.ROOT_UID;
        doReturn(new String[]{HIGH_SDK_PACKAGE}).when(mPackageManager).getPackagesForUid(systemUid);

        assertThat(mBatteryUtils.shouldHideAnomaly(mPowerWhitelistBackend, systemUid,
                mAnomalyInfo)).isTrue();
    }

    @Test
    public void testShouldHideAnomaly_AppInDozeList_returnTrue() {
        doReturn(new String[]{HIGH_SDK_PACKAGE}).when(mPackageManager).getPackagesForUid(UID);
        doReturn(true).when(mPowerWhitelistBackend).isWhitelisted(new String[]{HIGH_SDK_PACKAGE});

        assertThat(mBatteryUtils.shouldHideAnomaly(mPowerWhitelistBackend, UID,
                mAnomalyInfo)).isTrue();
    }

    @Test
    public void testShouldHideAnomaly_normalApp_returnFalse() {
        doReturn(new String[]{HIGH_SDK_PACKAGE}).when(mPackageManager).getPackagesForUid(UID);

        assertThat(mBatteryUtils.shouldHideAnomaly(mPowerWhitelistBackend, UID,
                mAnomalyInfo)).isFalse();
    }

    @Test
    public void testShouldHideAnomaly_excessivePriorOApp_returnFalse() {
        doReturn(new String[]{LOW_SDK_PACKAGE}).when(mPackageManager).getPackagesForUid(UID);
        mAnomalyInfo = new AnomalyInfo(INFO_EXCESSIVE);

        assertThat(mBatteryUtils.shouldHideAnomaly(mPowerWhitelistBackend, UID,
                mAnomalyInfo)).isFalse();
    }

    @Test
    public void testShouldHideAnomaly_excessiveOApp_returnTrue() {
        doReturn(new String[]{HIGH_SDK_PACKAGE}).when(mPackageManager).getPackagesForUid(UID);
        mAnomalyInfo = new AnomalyInfo(INFO_EXCESSIVE);

        assertThat(mBatteryUtils.shouldHideAnomaly(mPowerWhitelistBackend, UID,
                mAnomalyInfo)).isTrue();
    }

    @Test
    public void clearForceAppStandby_appRestricted_clearAndReturnTrue() {
        when(mBatteryUtils.getPackageUid(HIGH_SDK_PACKAGE)).thenReturn(UID);
        when(mAppOpsManager.checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, UID,
                HIGH_SDK_PACKAGE)).thenReturn(AppOpsManager.MODE_IGNORED);

        assertThat(mBatteryUtils.clearForceAppStandby(HIGH_SDK_PACKAGE)).isTrue();
        verify(mAppOpsManager).setMode(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, UID,
                HIGH_SDK_PACKAGE, AppOpsManager.MODE_ALLOWED);
    }

    @Test
    public void clearForceAppStandby_appInvalid_returnFalse() {
        when(mBatteryUtils.getPackageUid(PACKAGE_NAME)).thenReturn(BatteryUtils.UID_NULL);

        assertThat(mBatteryUtils.clearForceAppStandby(PACKAGE_NAME)).isFalse();
        verify(mAppOpsManager, never()).setMode(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, UID,
                PACKAGE_NAME, AppOpsManager.MODE_ALLOWED);
    }

    @Test
    public void clearForceAppStandby_appUnrestricted_returnFalse() {
        when(mBatteryUtils.getPackageUid(PACKAGE_NAME)).thenReturn(UID);
        when(mAppOpsManager.checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, UID,
                PACKAGE_NAME)).thenReturn(AppOpsManager.MODE_ALLOWED);

        assertThat(mBatteryUtils.clearForceAppStandby(PACKAGE_NAME)).isFalse();
        verify(mAppOpsManager, never()).setMode(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, UID,
                PACKAGE_NAME, AppOpsManager.MODE_ALLOWED);
    }

    @Test
    public void getBatteryInfo_providerNull_shouldNotCrash() {
        when(mProvider.isEnhancedBatteryPredictionEnabled(mContext)).thenReturn(true);
        when(mProvider.getEnhancedBatteryPrediction(mContext)).thenReturn(null);
        when(mContext.registerReceiver(nullable(BroadcastReceiver.class),
                any(IntentFilter.class))).thenReturn(new Intent());

        //Should not crash
        assertThat(mBatteryUtils.getBatteryInfo(mBatteryStatsHelper, TAG)).isNotNull();
    }

    @Test
    public void getEnhancedEstimate_doesNotUpdateCache_ifEstimateFresh() {
        Estimate estimate = new Estimate(1000, true, 1000);
        Estimate.storeCachedEstimate(mContext, estimate);

        estimate = mBatteryUtils.getEnhancedEstimate();

        // only pass if estimate has not changed
        assertThat(estimate).isNotNull();
        assertThat(estimate.isBasedOnUsage()).isTrue();
        assertThat(estimate.getAverageDischargeTime()).isEqualTo(1000);
    }
}
