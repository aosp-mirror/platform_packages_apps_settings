/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.content.ContentValues;
import android.os.BatteryConsumer;
import android.os.BatteryManager;
import android.os.BatteryUsageStats;
import android.os.SystemBatteryConsumer;
import android.os.UserHandle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public final class BatteryHistEntryTest {

    @Mock
    private BatteryEntry mockBatteryEntry;
    @Mock
    private BatteryUsageStats mBatteryUsageStats;
    @Mock
    private SystemBatteryConsumer mockSystemBatteryConsumer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConstructor_returnsExpectedResult() {
        final int expectedType = 3;
        when(mockBatteryEntry.getUid()).thenReturn(1001);
        when(mockBatteryEntry.getLabel()).thenReturn("Settings");
        when(mockBatteryEntry.getDefaultPackageName())
            .thenReturn("com.google.android.settings.battery");
        when(mockBatteryEntry.isHidden()).thenReturn(true);
        when(mBatteryUsageStats.getConsumedPower()).thenReturn(5.1);
        when(mockBatteryEntry.getConsumedPower()).thenReturn(1.1);
        mockBatteryEntry.percent = 0.3;
        when(mockBatteryEntry.getTimeInForegroundMs()).thenReturn(1234L);
        when(mockBatteryEntry.getTimeInBackgroundMs()).thenReturn(5689L);
        when(mockBatteryEntry.getBatteryConsumer())
            .thenReturn(mockSystemBatteryConsumer);
        when(mockSystemBatteryConsumer.getDrainType()).thenReturn(expectedType);
        final ContentValues values =
            ConvertUtils.convert(
                mockBatteryEntry,
                mBatteryUsageStats,
                /*batteryLevel=*/ 12,
                /*batteryStatus=*/ BatteryManager.BATTERY_STATUS_FULL,
                /*batteryHealth=*/ BatteryManager.BATTERY_HEALTH_COLD,
                /*timestamp=*/ 10001L);

        final BatteryHistEntry entry = new BatteryHistEntry(values);

        assertThat(entry.isValidEntry()).isTrue();
        assertThat(entry.mUid).isEqualTo(1001);
        assertThat(entry.mUserId).isEqualTo(UserHandle.getUserId(1001));
        assertThat(entry.mAppLabel).isEqualTo("Settings");
        assertThat(entry.mPackageName)
            .isEqualTo("com.google.android.settings.battery");
        assertThat(entry.mIsHidden).isTrue();
        assertThat(entry.mTimestamp).isEqualTo(10001L);
        assertThat(entry.mZoneId).isEqualTo(TimeZone.getDefault().getID());
        assertThat(entry.mTotalPower).isEqualTo(5.1);
        assertThat(entry.mConsumePower).isEqualTo(1.1);
        assertThat(entry.mPercentOfTotal).isEqualTo(mockBatteryEntry.percent);
        assertThat(entry.mForegroundUsageTimeInMs).isEqualTo(1234L);
        assertThat(entry.mBackgroundUsageTimeInMs).isEqualTo(5689L);
        assertThat(entry.mDrainType).isEqualTo(expectedType);
        assertThat(entry.mConsumerType)
            .isEqualTo(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        assertThat(entry.mBatteryLevel).isEqualTo(12);
        assertThat(entry.mBatteryStatus)
            .isEqualTo(BatteryManager.BATTERY_STATUS_FULL);
        assertThat(entry.mBatteryHealth)
            .isEqualTo(BatteryManager.BATTERY_HEALTH_COLD);
    }

    @Test
    public void testConstructor_invalidField_returnsInvalidEntry() {
        final BatteryHistEntry entry = new BatteryHistEntry(new ContentValues());
        assertThat(entry.isValidEntry()).isFalse();
    }
}
