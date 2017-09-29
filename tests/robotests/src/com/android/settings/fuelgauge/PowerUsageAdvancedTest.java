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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.UserManager;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceManager;

import android.view.View;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatterySipper.DrainType;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.PowerUsageAdvanced.PowerUsageData;
import com.android.settings.fuelgauge.PowerUsageAdvanced.PowerUsageData.UsageType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PowerUsageAdvancedTest {
    private static final int FAKE_UID_1 = 50;
    private static final int FAKE_UID_2 = 100;
    private static final int DISCHARGE_AMOUNT = 60;
    private static final double TYPE_APP_USAGE = 80;
    private static final double TYPE_BLUETOOTH_USAGE = 50;
    private static final double TYPE_WIFI_USAGE = 0;
    private static final double TOTAL_USAGE = TYPE_APP_USAGE * 2 + TYPE_BLUETOOTH_USAGE
            + TYPE_WIFI_USAGE;
    private static final double TOTAL_POWER = 500;
    private static final double PRECISION = 0.001;
    private static final String STUB_STRING = "stub_string";
    @Mock
    private BatterySipper mNormalBatterySipper;
    @Mock
    private BatterySipper mMaxBatterySipper;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BatteryStatsHelper mBatteryStatsHelper;
    @Mock
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private BatteryHistoryPreference mHistPref;
    @Mock
    private PreferenceGroup mUsageListGroup;
    @Mock
    private ConnectivityManager mConnectivityManager;
    private PowerUsageAdvanced mPowerUsageAdvanced;
    private PowerUsageData mPowerUsageData;
    private Context mShadowContext;
    private Intent mDischargingBatteryIntent;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mShadowContext = spy(RuntimeEnvironment.application);
        mPowerUsageAdvanced = spy(new PowerUsageAdvanced());

        List<BatterySipper> batterySippers = new ArrayList<>();
        batterySippers.add(new BatterySipper(DrainType.APP,
                new FakeUid(FAKE_UID_1), TYPE_APP_USAGE));
        batterySippers.add(new BatterySipper(DrainType.APP,
                new FakeUid(FAKE_UID_2), TYPE_APP_USAGE));
        batterySippers.add(new BatterySipper(DrainType.BLUETOOTH, new FakeUid(FAKE_UID_1),
                TYPE_BLUETOOTH_USAGE));
        batterySippers.add(new BatterySipper(DrainType.WIFI, new FakeUid(FAKE_UID_1),
                TYPE_WIFI_USAGE));

        mDischargingBatteryIntent = BatteryTestUtils.getDischargingIntent();
        doReturn(mDischargingBatteryIntent).when(mShadowContext).registerReceiver(any(), any());
        when(mBatteryStatsHelper.getStats().getDischargeAmount(anyInt())).thenReturn(
                DISCHARGE_AMOUNT);
        when(mBatteryStatsHelper.getUsageList()).thenReturn(batterySippers);
        when(mBatteryStatsHelper.getTotalPower()).thenReturn(TOTAL_USAGE);
        when(mPowerUsageAdvanced.getContext()).thenReturn(mShadowContext);
        doReturn(STUB_STRING).when(mPowerUsageAdvanced).getString(anyInt(), any(), any());
        doReturn(STUB_STRING).when(mPowerUsageAdvanced).getString(anyInt(), any());
        doReturn(mShadowContext.getText(R.string.battery_used_for)).when(
                mPowerUsageAdvanced).getText(R.string.battery_used_for);
        mPowerUsageAdvanced.setPackageManager(mPackageManager);
        mPowerUsageAdvanced.setPowerUsageFeatureProvider(mPowerUsageFeatureProvider);
        mPowerUsageAdvanced.setUserManager(mUserManager);
        mPowerUsageAdvanced.setBatteryUtils(BatteryUtils.getInstance(mShadowContext));
        when(mShadowContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(
                mConnectivityManager);

        mPowerUsageData = new PowerUsageData(UsageType.USER);
        mMaxBatterySipper.totalPowerMah = TYPE_BLUETOOTH_USAGE;
        mMaxBatterySipper.drainType = DrainType.BLUETOOTH;
        mNormalBatterySipper.drainType = DrainType.SCREEN;
    }

    @Test
    public void testPrefs_shouldNotBeSelectable() {
        PreferenceManager pm = new PreferenceManager(mShadowContext);
        when(mPowerUsageAdvanced.getPreferenceManager()).thenReturn(pm);
        PreferenceGroup prefGroup = spy(new PreferenceCategory(mShadowContext));
        when(prefGroup.getPreferenceManager()).thenReturn(pm);

        mPowerUsageAdvanced.refreshPowerUsageDataList(mBatteryStatsHelper, prefGroup);
        assertThat(prefGroup.getPreferenceCount()).isAtLeast(1);
        for (int i = 0, count = prefGroup.getPreferenceCount(); i < count; i++) {
            PowerGaugePreference pref = (PowerGaugePreference) prefGroup.getPreference(i);
            assertThat(pref.isSelectable()).isFalse();
        }
    }

    @Test
    public void testExtractUsageType_TypeSystem_ReturnSystem() {
        mNormalBatterySipper.drainType = DrainType.APP;
        when(mPowerUsageFeatureProvider.isTypeSystem(any())).thenReturn(true);

        assertThat(mPowerUsageAdvanced.extractUsageType(mNormalBatterySipper))
                .isEqualTo(UsageType.SYSTEM);
    }

    @Test
    public void testExtractUsageType_TypeEqualsToDrainType_ReturnRelevantType() {
        final DrainType drainTypes[] = {DrainType.WIFI, DrainType.BLUETOOTH, DrainType.IDLE,
                DrainType.USER, DrainType.CELL, DrainType.UNACCOUNTED};
        final int usageTypes[] = {UsageType.WIFI, UsageType.BLUETOOTH, UsageType.IDLE,
                UsageType.USER, UsageType.CELL, UsageType.UNACCOUNTED};

        assertThat(drainTypes.length).isEqualTo(usageTypes.length);
        for (int i = 0, size = drainTypes.length; i < size; i++) {
            mNormalBatterySipper.drainType = drainTypes[i];
            assertThat(mPowerUsageAdvanced.extractUsageType(mNormalBatterySipper))
                    .isEqualTo(usageTypes[i]);
        }
    }

    @Test
    public void testExtractUsageType_TypeService_ReturnSystem() {
        mNormalBatterySipper.drainType = DrainType.APP;
        when(mNormalBatterySipper.getUid()).thenReturn(FAKE_UID_1);
        when(mPowerUsageFeatureProvider.isTypeService(any())).thenReturn(true);

        assertThat(mPowerUsageAdvanced.extractUsageType(mNormalBatterySipper))
                .isEqualTo(UsageType.SYSTEM);
    }

    @Test
    public void testParsePowerUsageData_PercentageCalculatedCorrectly() {
        final double percentApp = TYPE_APP_USAGE * 2 / TOTAL_USAGE * DISCHARGE_AMOUNT;
        final double percentWifi = TYPE_WIFI_USAGE / TOTAL_USAGE * DISCHARGE_AMOUNT;
        final double percentBluetooth = TYPE_BLUETOOTH_USAGE / TOTAL_USAGE * DISCHARGE_AMOUNT;

        List<PowerUsageData> batteryData =
                mPowerUsageAdvanced.parsePowerUsageData(mBatteryStatsHelper);
        for (PowerUsageData data : batteryData) {
            switch (data.usageType) {
                case UsageType.WIFI:
                    assertThat(data.percentage).isWithin(PRECISION).of(percentWifi);
                    break;
                case UsageType.APP:
                    assertThat(data.percentage).isWithin(PRECISION).of(percentApp);
                    break;
                case UsageType.BLUETOOTH:
                    assertThat(data.percentage).isWithin(PRECISION).of(percentBluetooth);
                    break;
                default:
                    break;
            }
        }
    }

    @Test
    public void testUpdateUsageDataSummary_onlyOneApp_showUsageTime() {
        final String expectedSummary = "Used for 0m";
        mPowerUsageData.usageList.add(mNormalBatterySipper);

        mPowerUsageAdvanced.updateUsageDataSummary(mPowerUsageData, TOTAL_POWER, DISCHARGE_AMOUNT);

        assertThat(mPowerUsageData.summary.toString()).isEqualTo(expectedSummary);
    }

    @Test
    public void testUpdateUsageDataSummary_typeIdle_showUsageTime() {
        mPowerUsageData.usageType = UsageType.IDLE;
        mPowerUsageData.usageList.add(mNormalBatterySipper);

        mPowerUsageAdvanced.updateUsageDataSummary(mPowerUsageData, TOTAL_POWER, DISCHARGE_AMOUNT);

        assertThat(mPowerUsageData.summary.toString()).isEqualTo("0m");
    }

    @Test
    public void testUpdateUsageDataSummary_moreThanOneApp_showMaxUsageApp() {
        mPowerUsageData.usageList.add(mNormalBatterySipper);
        mPowerUsageData.usageList.add(mMaxBatterySipper);
        doReturn(mMaxBatterySipper).when(mPowerUsageAdvanced).findBatterySipperWithMaxBatteryUsage(
                mPowerUsageData.usageList);
        final double percentage = (TYPE_BLUETOOTH_USAGE / TOTAL_POWER) * DISCHARGE_AMOUNT;
        mPowerUsageAdvanced.updateUsageDataSummary(mPowerUsageData, TOTAL_POWER, DISCHARGE_AMOUNT);

        verify(mPowerUsageAdvanced).getString(eq(R.string.battery_used_by),
                eq(Utils.formatPercentage(percentage, true)), any());
    }

    @Test
    public void testFindBatterySipperWithMaxBatteryUsage_findCorrectOne() {
        mPowerUsageData.usageList.add(mNormalBatterySipper);
        mPowerUsageData.usageList.add(mMaxBatterySipper);
        BatterySipper sipper = mPowerUsageAdvanced.findBatterySipperWithMaxBatteryUsage(
                mPowerUsageData.usageList);

        assertThat(sipper).isEqualTo(mMaxBatterySipper);
    }

    @Test
    public void testInit_ContainsAllUsageType() {
        final int[] usageTypeSet = mPowerUsageAdvanced.mUsageTypes;

        assertThat(usageTypeSet).asList().containsExactly(UsageType.APP, UsageType.WIFI,
                UsageType.CELL, UsageType.BLUETOOTH, UsageType.IDLE, UsageType.USER,
                UsageType.SYSTEM, UsageType.UNACCOUNTED, UsageType.OVERCOUNTED);
    }

    @Test
    public void testPowerUsageData_SortedByUsage() {
        List<PowerUsageData> dataList = new ArrayList<>();

        dataList.add(new PowerUsageData(UsageType.WIFI, TYPE_WIFI_USAGE));
        dataList.add(new PowerUsageData(UsageType.BLUETOOTH, TYPE_BLUETOOTH_USAGE));
        dataList.add(new PowerUsageData(UsageType.APP, TYPE_APP_USAGE));
        Collections.sort(dataList);

        for (int i = 1, size = dataList.size(); i < size; i++) {
            assertThat(dataList.get(i - 1).totalPowerMah).isAtLeast(dataList.get(i).totalPowerMah);
        }
    }

    @Test
    public void testShouldHideCategory_typeUnAccounted_returnTrue() {
        mPowerUsageData.usageType = UsageType.UNACCOUNTED;

        assertThat(mPowerUsageAdvanced.shouldHideCategory(mPowerUsageData)).isTrue();
    }

    @Test
    public void testShouldHideCategory_typeOverCounted_returnTrue() {
        mPowerUsageData.usageType = UsageType.OVERCOUNTED;

        assertThat(mPowerUsageAdvanced.shouldHideCategory(mPowerUsageData)).isTrue();
    }

    @Test
    public void testShouldHideCategory_typeUserAndOnlyOne_returnTrue() {
        mPowerUsageData.usageType = UsageType.USER;
        doReturn(1).when(mUserManager).getUserCount();

        assertThat(mPowerUsageAdvanced.shouldHideCategory(mPowerUsageData)).isTrue();
    }

    @Test
    public void testShouldHideCategory_typeCellWhileNotSupported_returnTrue() {
        mPowerUsageData.usageType = UsageType.CELL;
        doReturn(false).when(mConnectivityManager).isNetworkSupported(
                ConnectivityManager.TYPE_MOBILE);

        assertThat(mPowerUsageAdvanced.shouldHideCategory(mPowerUsageData)).isTrue();
    }

    @Test
    public void testShouldHideCategory_typeCellWhileSupported_returnFalse() {
        mPowerUsageData.usageType = UsageType.CELL;
        doReturn(true).when(mConnectivityManager).isNetworkSupported(
                ConnectivityManager.TYPE_MOBILE);

        assertThat(mPowerUsageAdvanced.shouldHideCategory(mPowerUsageData)).isFalse();
    }

    @Test
    public void testShouldHideCategory_typeUserAndMoreThanOne_returnFalse() {
        mPowerUsageData.usageType = UsageType.USER;
        doReturn(2).when(mUserManager).getUserCount();

        assertThat(mPowerUsageAdvanced.shouldHideCategory(mPowerUsageData)).isFalse();
    }

    @Test
    public void testShouldHideCategory_typeNormal_returnFalse() {
        mPowerUsageData.usageType = UsageType.APP;

        assertThat(mPowerUsageAdvanced.shouldHideCategory(mPowerUsageData)).isFalse();
    }

    @Test
    public void testShouldHideSummary_typeCell_returnTrue() {
        mPowerUsageData.usageType = UsageType.CELL;

        assertThat(mPowerUsageAdvanced.shouldHideSummary(mPowerUsageData)).isTrue();
    }

    @Test
    public void testShouldHideSummary_typeSystem_returnTrue() {
        mPowerUsageData.usageType = UsageType.SYSTEM;

        assertThat(mPowerUsageAdvanced.shouldHideSummary(mPowerUsageData)).isTrue();
    }

    @Test
    public void testShouldHideSummary_typeWifi_returnTrue() {
        mPowerUsageData.usageType = UsageType.WIFI;

        assertThat(mPowerUsageAdvanced.shouldHideSummary(mPowerUsageData)).isTrue();
    }

    @Test
    public void testShouldHideSummary_typeBluetooth_returnTrue() {
        mPowerUsageData.usageType = UsageType.BLUETOOTH;

        assertThat(mPowerUsageAdvanced.shouldHideSummary(mPowerUsageData)).isTrue();
    }

    @Test
    public void testShouldHideSummary_typeApp_returnTrue() {
        mPowerUsageData.usageType = UsageType.APP;

        assertThat(mPowerUsageAdvanced.shouldHideSummary(mPowerUsageData)).isTrue();
    }

    @Test
    public void testShouldHideSummary_typeNormal_returnFalse() {
        mPowerUsageData.usageType = UsageType.IDLE;

        assertThat(mPowerUsageAdvanced.shouldHideSummary(mPowerUsageData)).isFalse();
    }

    @Test
    public void testShouldShowBatterySipper_typeScreen_returnFalse() {
        mNormalBatterySipper.drainType = DrainType.SCREEN;

        assertThat(mPowerUsageAdvanced.shouldShowBatterySipper(mNormalBatterySipper)).isFalse();
    }

    @Test
    public void testShouldShowBatterySipper_typeNormal_returnTrue() {
        mNormalBatterySipper.drainType = DrainType.APP;

        assertThat(mPowerUsageAdvanced.shouldShowBatterySipper(mNormalBatterySipper)).isTrue();
    }

    @Test
    public void testCalculateHiddenPower_returnCorrectPower() {
        List<PowerUsageData> powerUsageDataList = new ArrayList<>();
        final double unaccountedPower = 100;
        final double normalPower = 150;
        powerUsageDataList.add(new PowerUsageData(UsageType.UNACCOUNTED, unaccountedPower));
        powerUsageDataList.add(new PowerUsageData(UsageType.APP, normalPower));
        powerUsageDataList.add(new PowerUsageData(UsageType.CELL, normalPower));

        assertThat(mPowerUsageAdvanced.calculateHiddenPower(powerUsageDataList)).isWithin(
                PRECISION).of(unaccountedPower);
    }

    @Test
    public void testRefreshUi_addsSubtextWhenAppropriate() {
        // Mock out all the battery stuff
        mPowerUsageAdvanced.mHistPref = mHistPref;
        mPowerUsageAdvanced.mStatsHelper = mBatteryStatsHelper;
        doReturn(new ArrayList<PowerUsageData>())
                .when(mPowerUsageAdvanced).parsePowerUsageData(any());
        doReturn("").when(mPowerUsageAdvanced).getString(anyInt());
        mPowerUsageAdvanced.mUsageListGroup = mUsageListGroup;

        // refresh the ui and check that text was not updated when enhanced prediction disabled
        when(mPowerUsageFeatureProvider.isEnhancedBatteryPredictionEnabled(any()))
                .thenReturn(false);
        mPowerUsageAdvanced.refreshUi();
        verify(mHistPref, never()).setBottomSummary(any());

        // refresh the ui and check that text was updated when enhanced prediction enabled
        when(mPowerUsageFeatureProvider.isEnhancedBatteryPredictionEnabled(any())).thenReturn(true);
        mPowerUsageAdvanced.refreshUi();
        verify(mHistPref, atLeastOnce()).setBottomSummary(any());
    }
}
