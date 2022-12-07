/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentValues;
import android.content.Context;
import android.os.BatteryManager;
import android.os.BatteryUsageStats;
import android.os.LocaleList;
import android.os.UserHandle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Locale;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public final class ConvertUtilsTest {

    private Context mContext;

    @Mock
    private BatteryUsageStats mBatteryUsageStats;
    @Mock
    private BatteryEntry mMockBatteryEntry;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
    }

    @Test
    public void convertToContentValues_returnsExpectedContentValues() {
        final int expectedType = 3;
        when(mMockBatteryEntry.getUid()).thenReturn(1001);
        when(mMockBatteryEntry.getLabel()).thenReturn("Settings");
        when(mMockBatteryEntry.getDefaultPackageName())
                .thenReturn("com.google.android.settings.battery");
        when(mMockBatteryEntry.isHidden()).thenReturn(true);
        when(mBatteryUsageStats.getConsumedPower()).thenReturn(5.1);
        when(mMockBatteryEntry.getConsumedPower()).thenReturn(1.1);
        when(mMockBatteryEntry.getConsumedPowerInForeground()).thenReturn(1.2);
        when(mMockBatteryEntry.getConsumedPowerInForegroundService()).thenReturn(1.3);
        when(mMockBatteryEntry.getConsumedPowerInBackground()).thenReturn(1.4);
        when(mMockBatteryEntry.getConsumedPowerInCached()).thenReturn(1.5);
        mMockBatteryEntry.mPercent = 0.3;
        when(mMockBatteryEntry.getTimeInForegroundMs()).thenReturn(1234L);
        when(mMockBatteryEntry.getTimeInBackgroundMs()).thenReturn(5689L);
        when(mMockBatteryEntry.getPowerComponentId()).thenReturn(expectedType);
        when(mMockBatteryEntry.getConsumerType())
                .thenReturn(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);

        final ContentValues values =
                ConvertUtils.convertToContentValues(
                        mMockBatteryEntry,
                        mBatteryUsageStats,
                        /*batteryLevel=*/ 12,
                        /*batteryStatus=*/ BatteryManager.BATTERY_STATUS_FULL,
                        /*batteryHealth=*/ BatteryManager.BATTERY_HEALTH_COLD,
                        /*bootTimestamp=*/ 101L,
                        /*timestamp=*/ 10001L,
                        /*isFullChargeStart=*/ true);
        final BatteryInformation batteryInformation =
                ConvertUtils.getBatteryInformation(
                        values, BatteryHistEntry.KEY_BATTERY_INFORMATION);
        final DeviceBatteryState deviceBatteryState = batteryInformation.getDeviceBatteryState();

        assertThat(values.getAsLong(BatteryHistEntry.KEY_UID)).isEqualTo(1001L);
        assertThat(values.getAsLong(BatteryHistEntry.KEY_USER_ID))
                .isEqualTo(UserHandle.getUserId(1001));
        assertThat(values.getAsString(BatteryHistEntry.KEY_PACKAGE_NAME))
                .isEqualTo("com.google.android.settings.battery");
        assertThat(values.getAsLong(BatteryHistEntry.KEY_TIMESTAMP)).isEqualTo(10001L);
        assertThat(values.getAsInteger(BatteryHistEntry.KEY_CONSUMER_TYPE))
                .isEqualTo(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        assertThat(values.getAsBoolean(BatteryHistEntry.KEY_IS_FULL_CHARGE_CYCLE_START)).isTrue();
        assertThat(batteryInformation.getAppLabel()).isEqualTo("Settings");
        assertThat(batteryInformation.getIsHidden()).isTrue();
        assertThat(batteryInformation.getBootTimestamp()).isEqualTo(101L);
        assertThat(batteryInformation.getZoneId()).isEqualTo(TimeZone.getDefault().getID());
        assertThat(batteryInformation.getTotalPower()).isEqualTo(5.1);
        assertThat(batteryInformation.getConsumePower()).isEqualTo(1.1);
        assertThat(batteryInformation.getForegroundUsageConsumePower()).isEqualTo(1.2);
        assertThat(batteryInformation.getForegroundServiceUsageConsumePower()).isEqualTo(1.3);
        assertThat(batteryInformation.getBackgroundUsageConsumePower()).isEqualTo(1.4);
        assertThat(batteryInformation.getCachedUsageConsumePower()).isEqualTo(1.5);
        assertThat(batteryInformation.getPercentOfTotal()).isEqualTo(0.3);
        assertThat(batteryInformation.getForegroundUsageTimeInMs()).isEqualTo(1234L);
        assertThat(batteryInformation.getBackgroundUsageTimeInMs()).isEqualTo(5689L);
        assertThat(batteryInformation.getDrainType()).isEqualTo(expectedType);
        assertThat(deviceBatteryState.getBatteryLevel()).isEqualTo(12);
        assertThat(deviceBatteryState.getBatteryStatus())
                .isEqualTo(BatteryManager.BATTERY_STATUS_FULL);
        assertThat(deviceBatteryState.getBatteryHealth())
                .isEqualTo(BatteryManager.BATTERY_HEALTH_COLD);
    }

    @Test
    public void convertToContentValues_nullBatteryEntry_returnsExpectedContentValues() {
        final ContentValues values =
                ConvertUtils.convertToContentValues(
                        /*entry=*/ null,
                        /*batteryUsageStats=*/ null,
                        /*batteryLevel=*/ 12,
                        /*batteryStatus=*/ BatteryManager.BATTERY_STATUS_FULL,
                        /*batteryHealth=*/ BatteryManager.BATTERY_HEALTH_COLD,
                        /*bootTimestamp=*/ 101L,
                        /*timestamp=*/ 10001L,
                        /*isFullChargeStart=*/ false);

        final BatteryInformation batteryInformation =
                ConvertUtils.getBatteryInformation(
                        values, BatteryHistEntry.KEY_BATTERY_INFORMATION);
        final DeviceBatteryState deviceBatteryState = batteryInformation.getDeviceBatteryState();
        assertThat(batteryInformation.getBootTimestamp()).isEqualTo(101L);
        assertThat(batteryInformation.getZoneId()).isEqualTo(TimeZone.getDefault().getID());
        assertThat(values.getAsBoolean(BatteryHistEntry.KEY_IS_FULL_CHARGE_CYCLE_START)).isFalse();
        assertThat(deviceBatteryState.getBatteryLevel()).isEqualTo(12);
        assertThat(deviceBatteryState.getBatteryStatus())
                .isEqualTo(BatteryManager.BATTERY_STATUS_FULL);
        assertThat(deviceBatteryState.getBatteryHealth())
                .isEqualTo(BatteryManager.BATTERY_HEALTH_COLD);
        assertThat(values.getAsLong(BatteryHistEntry.KEY_TIMESTAMP))
                .isEqualTo(10001L);
        assertThat(values.getAsString(BatteryHistEntry.KEY_PACKAGE_NAME))
                .isEqualTo(ConvertUtils.FAKE_PACKAGE_NAME);
    }

    @Test
    public void convertToBatteryHistEntry_returnsExpectedResult() {
        final int expectedType = 3;
        when(mMockBatteryEntry.getUid()).thenReturn(1001);
        when(mMockBatteryEntry.getLabel()).thenReturn("Settings");
        when(mMockBatteryEntry.getDefaultPackageName())
                .thenReturn("com.android.settings.battery");
        when(mMockBatteryEntry.isHidden()).thenReturn(true);
        when(mBatteryUsageStats.getConsumedPower()).thenReturn(5.1);
        when(mMockBatteryEntry.getConsumedPower()).thenReturn(1.1);
        when(mMockBatteryEntry.getConsumedPowerInForeground()).thenReturn(1.2);
        when(mMockBatteryEntry.getConsumedPowerInForegroundService()).thenReturn(1.3);
        when(mMockBatteryEntry.getConsumedPowerInBackground()).thenReturn(1.4);
        when(mMockBatteryEntry.getConsumedPowerInCached()).thenReturn(1.5);
        mMockBatteryEntry.mPercent = 0.3;
        when(mMockBatteryEntry.getTimeInForegroundMs()).thenReturn(1234L);
        when(mMockBatteryEntry.getTimeInBackgroundMs()).thenReturn(5689L);
        when(mMockBatteryEntry.getPowerComponentId()).thenReturn(expectedType);
        when(mMockBatteryEntry.getConsumerType())
                .thenReturn(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);

        final BatteryHistEntry batteryHistEntry =
                ConvertUtils.convertToBatteryHistEntry(
                        mMockBatteryEntry,
                        mBatteryUsageStats);

        assertThat(batteryHistEntry.mUid).isEqualTo(1001L);
        assertThat(batteryHistEntry.mUserId)
                .isEqualTo(UserHandle.getUserId(1001));
        assertThat(batteryHistEntry.mAppLabel)
                .isEqualTo("Settings");
        assertThat(batteryHistEntry.mPackageName)
                .isEqualTo("com.android.settings.battery");
        assertThat(batteryHistEntry.mIsHidden).isTrue();
        assertThat(batteryHistEntry.mBootTimestamp)
                .isEqualTo(0L);
        assertThat(batteryHistEntry.mTimestamp).isEqualTo(0L);
        assertThat(batteryHistEntry.mZoneId)
                .isEqualTo(TimeZone.getDefault().getID());
        assertThat(batteryHistEntry.mTotalPower).isEqualTo(5.1);
        assertThat(batteryHistEntry.mConsumePower).isEqualTo(1.1);
        assertThat(batteryHistEntry.mForegroundUsageConsumePower).isEqualTo(1.2);
        assertThat(batteryHistEntry.mForegroundServiceUsageConsumePower).isEqualTo(1.3);
        assertThat(batteryHistEntry.mBackgroundUsageConsumePower).isEqualTo(1.4);
        assertThat(batteryHistEntry.mCachedUsageConsumePower).isEqualTo(1.5);
        assertThat(batteryHistEntry.mPercentOfTotal).isEqualTo(0.3);
        assertThat(batteryHistEntry.mForegroundUsageTimeInMs)
                .isEqualTo(1234L);
        assertThat(batteryHistEntry.mBackgroundUsageTimeInMs)
                .isEqualTo(5689L);
        assertThat(batteryHistEntry.mDrainType).isEqualTo(expectedType);
        assertThat(batteryHistEntry.mConsumerType)
                .isEqualTo(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        assertThat(batteryHistEntry.mBatteryLevel).isEqualTo(0);
        assertThat(batteryHistEntry.mBatteryStatus).isEqualTo(0);
        assertThat(batteryHistEntry.mBatteryHealth).isEqualTo(0);
    }

    @Test
    public void convertToBatteryHistEntry_nullBatteryEntry_returnsExpectedResult() {
        final BatteryHistEntry batteryHistEntry =
                ConvertUtils.convertToBatteryHistEntry(
                        /*entry=*/ null,
                        /*batteryUsageStats=*/ null);

        assertThat(batteryHistEntry.mBootTimestamp)
                .isEqualTo(0L);
        assertThat(batteryHistEntry.mTimestamp)
                .isEqualTo(0);
        assertThat(batteryHistEntry.mZoneId)
                .isEqualTo(TimeZone.getDefault().getID());
        assertThat(batteryHistEntry.mBatteryLevel).isEqualTo(0);
        assertThat(batteryHistEntry.mBatteryStatus).isEqualTo(0);
        assertThat(batteryHistEntry.mBatteryHealth).isEqualTo(0);
        assertThat(batteryHistEntry.mPackageName)
                .isEqualTo(ConvertUtils.FAKE_PACKAGE_NAME);
    }

    @Test
    public void getLocale_nullContext_returnDefaultLocale() {
        assertThat(ConvertUtils.getLocale(/*context=*/ null))
                .isEqualTo(Locale.getDefault());
    }

    @Test
    public void getLocale_nullLocaleList_returnDefaultLocale() {
        mContext.getResources().getConfiguration().setLocales(null);
        assertThat(ConvertUtils.getLocale(mContext)).isEqualTo(Locale.getDefault());
    }

    @Test
    public void getLocale_emptyLocaleList_returnDefaultLocale() {
        mContext.getResources().getConfiguration().setLocales(new LocaleList());
        assertThat(ConvertUtils.getLocale(mContext)).isEqualTo(Locale.getDefault());
    }
}
