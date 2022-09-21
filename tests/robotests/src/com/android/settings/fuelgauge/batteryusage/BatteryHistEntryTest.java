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

import static org.mockito.Mockito.when;

import android.content.ContentValues;
import android.database.MatrixCursor;
import android.os.BatteryManager;
import android.os.BatteryUsageStats;
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
    private BatteryEntry mMockBatteryEntry;
    @Mock
    private BatteryUsageStats mBatteryUsageStats;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConstructor_contentValues_returnsExpectedResult() {
        final int expectedType = 3;
        when(mMockBatteryEntry.getUid()).thenReturn(1001);
        when(mMockBatteryEntry.getLabel()).thenReturn("Settings");
        when(mMockBatteryEntry.getDefaultPackageName())
                .thenReturn("com.google.android.settings.battery");
        when(mMockBatteryEntry.isHidden()).thenReturn(true);
        when(mBatteryUsageStats.getConsumedPower()).thenReturn(5.1);
        when(mMockBatteryEntry.getConsumedPower()).thenReturn(1.1);
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
                        /*timestamp=*/ 10001L);

        assertBatteryHistEntry(
                new BatteryHistEntry(values),
                /*drainType=*/ expectedType,
                /*percentOfTotal=*/ mMockBatteryEntry.mPercent);
    }

    @Test
    public void testConstructor_invalidField_returnsInvalidEntry() {
        final BatteryHistEntry entry = new BatteryHistEntry(new ContentValues());
        assertThat(entry.isValidEntry()).isFalse();
    }

    @Test
    public void testConstructor_cursor_returnsExpectedResult() {
        assertBatteryHistEntry(
                createBatteryHistEntry(
                        /*bootTimestamp=*/ 101L,
                        /*timestamp=*/ 10001L,
                        /*totalPower=*/ 5.1,
                        /*consumePower=*/ 1.1,
                        /*foregroundUsageTimeInMs=*/ 1234L,
                        /*backgroundUsageTimeInMs=*/ 5689L,
                        /*batteryLevel=*/ 12),
                /*drainType=*/ 3,
                /*percentOfTotal=*/ 0.3);
    }

    @Test
    public void testGetKey_consumerUidType_returnExpectedString() {
        final ContentValues values = getContentValuesWithType(
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        values.put(BatteryHistEntry.KEY_UID, 3);
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        assertThat(batteryHistEntry.getKey()).isEqualTo("3");
    }

    @Test
    public void testGetKey_consumerUserType_returnExpectedString() {
        final ContentValues values = getContentValuesWithType(
                ConvertUtils.CONSUMER_TYPE_USER_BATTERY);
        values.put(BatteryHistEntry.KEY_USER_ID, 2);
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        assertThat(batteryHistEntry.getKey()).isEqualTo("U|2");
    }

    @Test
    public void testGetKey_consumerSystemType_returnExpectedString() {
        final ContentValues values = getContentValuesWithType(
                ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        values.put(BatteryHistEntry.KEY_DRAIN_TYPE, 1);
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        assertThat(batteryHistEntry.getKey()).isEqualTo("S|1");
    }

    @Test
    public void testIsAppEntry_returnExpectedResult() {
        assertThat(createEntry(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY).isAppEntry())
                .isFalse();
        assertThat(createEntry(ConvertUtils.CONSUMER_TYPE_USER_BATTERY).isAppEntry())
                .isFalse();
        assertThat(createEntry(ConvertUtils.CONSUMER_TYPE_UID_BATTERY).isAppEntry())
                .isTrue();
    }

    @Test
    public void testIsUserEntry_returnExpectedResult() {
        assertThat(createEntry(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY).isUserEntry())
                .isFalse();
        assertThat(createEntry(ConvertUtils.CONSUMER_TYPE_USER_BATTERY).isUserEntry())
                .isTrue();
        assertThat(createEntry(ConvertUtils.CONSUMER_TYPE_UID_BATTERY).isUserEntry())
                .isFalse();
    }

    @Test
    public void testIsSystemEntry_returnExpectedResult() {
        assertThat(createEntry(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY).isSystemEntry())
                .isTrue();
        assertThat(createEntry(ConvertUtils.CONSUMER_TYPE_USER_BATTERY).isSystemEntry())
                .isFalse();
        assertThat(createEntry(ConvertUtils.CONSUMER_TYPE_UID_BATTERY).isSystemEntry())
                .isFalse();
    }

    @Test
    public void testInterpolate_returnExpectedResult() {
        final long slotTimestamp = 200L;
        final long upperTimestamp = 300L;
        final long lowerTimestamp = 100L;
        final double ratio = 0.5;
        final BatteryHistEntry lowerHistEntry = createBatteryHistEntry(
                /*bootTimestamp=*/ 1000L,
                lowerTimestamp,
                /*totalPower=*/ 50,
                /*consumePower=*/ 10,
                /*foregroundUsageTimeInMs=*/ 100,
                /*backgroundUsageTimeInMs=*/ 200,
                /*batteryLevel=*/ 90);
        final BatteryHistEntry upperHistEntry = createBatteryHistEntry(
                /*bootTimestamp=*/ 1200L,
                upperTimestamp,
                /*totalPower=*/ 80,
                /*consumePower=*/ 20,
                /*foregroundUsageTimeInMs=*/ 200,
                /*backgroundUsageTimeInMs=*/ 300,
                /*batteryLevel=*/ 80);

        final BatteryHistEntry newEntry =
                BatteryHistEntry.interpolate(
                        slotTimestamp,
                        upperTimestamp,
                        ratio,
                        lowerHistEntry,
                        upperHistEntry);

        assertBatteryHistEntry(
                newEntry, 3, upperHistEntry.mPercentOfTotal,
                /*bootTimestamp=*/ 1200 - 100,
                /*timestamp=*/ slotTimestamp,
                /*totalPower=*/ 50 + 0.5 * (80 - 50),
                /*consumePower=*/ 10 + 0.5 * (20 - 10),
                /*foregroundUsageTimeInMs=*/ Math.round(100 + 0.5 * (200 - 100)),
                /*backgroundUsageTimeInMs=*/ Math.round(200 + 0.5 * (300 - 200)),
                /*batteryLevel=*/ (int) Math.round(90 + 0.5 * (80 - 90)));
    }

    @Test
    public void testInterpolate_withoutLowerEntryData_returnExpectedResult() {
        final long slotTimestamp = 200L;
        final long upperTimestamp = 300L;
        final long lowerTimestamp = 100L;
        final double ratio = 0.5;
        final BatteryHistEntry upperHistEntry = createBatteryHistEntry(
                /*bootTimestamp=*/ 1200L,
                upperTimestamp,
                /*totalPower=*/ 80,
                /*consumePower=*/ 20,
                /*foregroundUsageTimeInMs=*/ 200,
                /*backgroundUsageTimeInMs=*/ 300,
                /*batteryLevel=*/ 80);

        final BatteryHistEntry newEntry =
                BatteryHistEntry.interpolate(
                        slotTimestamp,
                        upperTimestamp,
                        ratio,
                        /*lowerHistEntry=*/ null,
                        upperHistEntry);

        assertBatteryHistEntry(
                newEntry, 3, upperHistEntry.mPercentOfTotal,
                /*bootTimestamp=*/ 1200 - 100,
                /*timestamp=*/ slotTimestamp,
                /*totalPower=*/ 0.5 * 80,
                /*consumePower=*/ 0.5 * 20,
                /*foregroundUsageTimeInMs=*/ Math.round(0.5 * 200),
                /*backgroundUsageTimeInMs=*/ Math.round(0.5 * 300),
                /*batteryLevel=*/ upperHistEntry.mBatteryLevel);
    }

    private static BatteryHistEntry createEntry(int consumerType) {
        return new BatteryHistEntry(getContentValuesWithType(consumerType));
    }

    private static ContentValues getContentValuesWithType(int consumerType) {
        final ContentValues values = new ContentValues();
        values.put(BatteryHistEntry.KEY_CONSUMER_TYPE,
                Integer.valueOf(consumerType));
        return values;
    }

    private void assertBatteryHistEntry(
            BatteryHistEntry entry, int drainType, double percentOfTotal) {
        assertBatteryHistEntry(
                entry, drainType, percentOfTotal,
                /*bootTimestamp=*/ 101L,
                /*timestamp=*/ 10001L,
                /*totalPower=*/ 5.1,
                /*consumePower=*/ 1.1,
                /*foregroundUsageTimeInMs=*/ 1234L,
                /*backgroundUsageTimeInMs=*/ 5689L,
                /*batteryLevel=*/ 12);
    }

    private void assertBatteryHistEntry(
            BatteryHistEntry entry,
            int drainType,
            double percentOfTotal,
            long bootTimestamp,
            long timestamp,
            double totalPower,
            double consumePower,
            long foregroundUsageTimeInMs,
            long backgroundUsageTimeInMs,
            int batteryLevel) {
        assertThat(entry.isValidEntry()).isTrue();
        assertThat(entry.mUid).isEqualTo(1001);
        assertThat(entry.mUserId).isEqualTo(UserHandle.getUserId(1001));
        assertThat(entry.mAppLabel).isEqualTo("Settings");
        assertThat(entry.mPackageName)
                .isEqualTo("com.google.android.settings.battery");
        assertThat(entry.mIsHidden).isTrue();
        assertThat(entry.mBootTimestamp).isEqualTo(bootTimestamp);
        assertThat(entry.mTimestamp).isEqualTo(timestamp);
        assertThat(entry.mZoneId).isEqualTo(TimeZone.getDefault().getID());
        assertThat(entry.mTotalPower).isEqualTo(totalPower);
        assertThat(entry.mConsumePower).isEqualTo(consumePower);
        assertThat(entry.mPercentOfTotal).isEqualTo(percentOfTotal);
        assertThat(entry.mForegroundUsageTimeInMs).isEqualTo(foregroundUsageTimeInMs);
        assertThat(entry.mBackgroundUsageTimeInMs).isEqualTo(backgroundUsageTimeInMs);
        assertThat(entry.mDrainType).isEqualTo(drainType);
        assertThat(entry.mConsumerType)
                .isEqualTo(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        assertThat(entry.mBatteryLevel).isEqualTo(batteryLevel);
        assertThat(entry.mBatteryStatus)
                .isEqualTo(BatteryManager.BATTERY_STATUS_FULL);
        assertThat(entry.mBatteryHealth)
                .isEqualTo(BatteryManager.BATTERY_HEALTH_COLD);
    }

    private BatteryHistEntry createBatteryHistEntry(
            long bootTimestamp,
            long timestamp,
            double totalPower,
            double consumePower,
            long foregroundUsageTimeInMs,
            long backgroundUsageTimeInMs,
            int batteryLevel) {
        final MatrixCursor cursor = new MatrixCursor(
            new String[]{
                BatteryHistEntry.KEY_UID,
                BatteryHistEntry.KEY_USER_ID,
                BatteryHistEntry.KEY_APP_LABEL,
                BatteryHistEntry.KEY_PACKAGE_NAME,
                BatteryHistEntry.KEY_IS_HIDDEN,
                BatteryHistEntry.KEY_BOOT_TIMESTAMP,
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
            new Object[]{
                Long.valueOf(1001),
                Long.valueOf(UserHandle.getUserId(1001)),
                "Settings",
                "com.google.android.settings.battery",
                Integer.valueOf(1),
                Long.valueOf(bootTimestamp),
                Long.valueOf(timestamp),
                TimeZone.getDefault().getID(),
                Double.valueOf(totalPower),
                Double.valueOf(consumePower),
                Double.valueOf(0.3),
                Long.valueOf(foregroundUsageTimeInMs),
                Long.valueOf(backgroundUsageTimeInMs),
                Integer.valueOf(3),
                Integer.valueOf(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY),
                Integer.valueOf(batteryLevel),
                Integer.valueOf(BatteryManager.BATTERY_STATUS_FULL),
                Integer.valueOf(BatteryManager.BATTERY_HEALTH_COLD)});
        cursor.moveToFirst();
        return new BatteryHistEntry(cursor);
    }
}
