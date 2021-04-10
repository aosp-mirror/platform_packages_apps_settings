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

import android.database.MatrixCursor;
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
    public void testConstructor_contentValues_returnsExpectedResult() {
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

        assertBatteryHistEntry(
            new BatteryHistEntry(values),
            /*drainType=*/ expectedType,
            /*percentOfTotal=*/ mockBatteryEntry.percent);
    }

    @Test
    public void testConstructor_invalidField_returnsInvalidEntry() {
        final BatteryHistEntry entry = new BatteryHistEntry(new ContentValues());
        assertThat(entry.isValidEntry()).isFalse();
    }

    @Test
    public void testConstructor_cursor_returnsExpectedResult() {
        final MatrixCursor cursor = new MatrixCursor(
            new String[] {
                BatteryHistEntry.KEY_UID,
                BatteryHistEntry.KEY_USER_ID,
                BatteryHistEntry.KEY_APP_LABEL,
                BatteryHistEntry.KEY_PACKAGE_NAME,
                BatteryHistEntry.KEY_IS_HIDDEN,
                BatteryHistEntry.KEY_TIMESTAMP,
                BatteryHistEntry.KEY_ZONE_ID,
                BatteryHistEntry.KEY_TOTAL_POWER,
                BatteryHistEntry.KEY_CONSUME_POWER,
                BatteryHistEntry.KEY_PERCENT_OF_TOTAL,
                BatteryHistEntry.KEY_FOREGROUND_USAGE_TIME,
                BatteryHistEntry.KEY_BACKGROUND_USAGE_TIME,
                BatteryHistEntry.KEY_DRAIN_TYPE,
                BatteryHistEntry.KEY_CONSUMER_TYPE,
                BatteryHistEntry.KEY_BATTERY_LEVEL,
                BatteryHistEntry.KEY_BATTERY_STATUS,
                BatteryHistEntry.KEY_BATTERY_HEALTH});
        cursor.addRow(
            new Object[] {
                Long.valueOf(1001),
                Long.valueOf(UserHandle.getUserId(1001)),
                "Settings",
                "com.google.android.settings.battery",
                Integer.valueOf(1),
                Long.valueOf(10001L),
                TimeZone.getDefault().getID(),
                Double.valueOf(5.1),
                Double.valueOf(1.1),
                Double.valueOf(0.3),
                Long.valueOf(1234L),
                Long.valueOf(5689L),
                Integer.valueOf(3),
                Integer.valueOf(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY),
                Integer.valueOf(12),
                Integer.valueOf(BatteryManager.BATTERY_STATUS_FULL),
                Integer.valueOf(BatteryManager.BATTERY_HEALTH_COLD)});
        cursor.moveToFirst();

        assertBatteryHistEntry(
            new BatteryHistEntry(cursor),
            /*drainType=*/ 3,
            /*percentOfTotal=*/ 0.3);
    }

    private void assertBatteryHistEntry(
        BatteryHistEntry entry, int drainType, double percentOfTotal) {
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
        assertThat(entry.mPercentOfTotal).isEqualTo(percentOfTotal);
        assertThat(entry.mForegroundUsageTimeInMs).isEqualTo(1234L);
        assertThat(entry.mBackgroundUsageTimeInMs).isEqualTo(5689L);
        assertThat(entry.mDrainType).isEqualTo(drainType);
        assertThat(entry.mConsumerType)
            .isEqualTo(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        assertThat(entry.mBatteryLevel).isEqualTo(12);
        assertThat(entry.mBatteryStatus)
            .isEqualTo(BatteryManager.BATTERY_STATUS_FULL);
        assertThat(entry.mBatteryHealth)
            .isEqualTo(BatteryManager.BATTERY_HEALTH_COLD);
    }
}
