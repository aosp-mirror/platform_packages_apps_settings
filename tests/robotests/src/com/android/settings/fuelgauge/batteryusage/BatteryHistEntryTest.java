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

import static com.android.settings.fuelgauge.batteryusage.ConvertUtils.isSystemConsumer;
import static com.android.settings.fuelgauge.batteryusage.ConvertUtils.isUidConsumer;
import static com.android.settings.fuelgauge.batteryusage.ConvertUtils.isUserConsumer;

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

    @Mock private BatteryEntry mMockBatteryEntry;
    @Mock private BatteryUsageStats mBatteryUsageStats;

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
        when(mMockBatteryEntry.getConsumedPowerInForeground()).thenReturn(1.2);
        when(mMockBatteryEntry.getConsumedPowerInForegroundService()).thenReturn(1.3);
        when(mMockBatteryEntry.getConsumedPowerInBackground()).thenReturn(1.4);
        when(mMockBatteryEntry.getConsumedPowerInCached()).thenReturn(1.5);
        mMockBatteryEntry.mPercent = 0.3;
        when(mMockBatteryEntry.getTimeInForegroundMs()).thenReturn(1234L);
        when(mMockBatteryEntry.getTimeInForegroundServiceMs()).thenReturn(3456L);
        when(mMockBatteryEntry.getTimeInBackgroundMs()).thenReturn(5689L);
        when(mMockBatteryEntry.getPowerComponentId()).thenReturn(expectedType);
        when(mMockBatteryEntry.getConsumerType())
                .thenReturn(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        final ContentValues values =
                ConvertUtils.convertBatteryEntryToContentValues(
                        mMockBatteryEntry,
                        mBatteryUsageStats,
                        /* batteryLevel= */ 12,
                        /* batteryStatus= */ BatteryManager.BATTERY_STATUS_FULL,
                        /* batteryHealth= */ BatteryManager.BATTERY_HEALTH_COLD,
                        /* bootTimestamp= */ 101L,
                        /* timestamp= */ 10001L,
                        /* isFullChargeStart= */ false);

        assertBatteryHistEntry(
                new BatteryHistEntry(values),
                /* drainType= */ expectedType,
                /* percentOfTotal= */ mMockBatteryEntry.mPercent);
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
                        /* bootTimestamp= */ 101L,
                        /* timestamp= */ 10001L,
                        /* totalPower= */ 5.1,
                        /* consumePower= */ 1.1,
                        /* foregroundUsageConsumePower= */ 1.2,
                        /* foregroundServiceUsageConsumePower= */ 1.3,
                        /* backgroundUsageConsumePower= */ 1.4,
                        /* cachedUsageConsumePower= */ 1.5,
                        /* foregroundUsageTimeInMs= */ 1234L,
                        /* foregroundServiceUsageTimeInMs= */ 3456L,
                        /* backgroundUsageTimeInMs= */ 5689L,
                        /* batteryLevel= */ 12),
                /* drainType= */ 3,
                /* percentOfTotal= */ 0.3);
    }

    @Test
    public void testGetKey_consumerUidType_returnExpectedString() {
        final ContentValues values =
                getContentValuesWithType(ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        values.put(BatteryHistEntry.KEY_UID, 3);
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        assertThat(batteryHistEntry.getKey()).isEqualTo("3");
    }

    @Test
    public void testGetKey_consumerUserType_returnExpectedString() {
        final ContentValues values =
                getContentValuesWithType(ConvertUtils.CONSUMER_TYPE_USER_BATTERY);
        values.put(BatteryHistEntry.KEY_USER_ID, 2);
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        assertThat(batteryHistEntry.getKey()).isEqualTo("U|2");
    }

    @Test
    public void testGetKey_consumerSystemType_returnExpectedString() {
        final ContentValues values =
                getContentValuesWithType(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        final BatteryInformation batteryInformation =
                BatteryInformation.newBuilder().setDrainType(1).build();
        values.put(
                BatteryHistEntry.KEY_BATTERY_INFORMATION,
                ConvertUtils.convertBatteryInformationToString(batteryInformation));
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        assertThat(batteryHistEntry.getKey()).isEqualTo("S|1");
    }

    @Test
    public void testIsAppEntry_returnExpectedResult() {
        assertThat(
                        isUidConsumer(
                                createEntry(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY)
                                        .mConsumerType))
                .isFalse();
        assertThat(
                        isUidConsumer(
                                createEntry(ConvertUtils.CONSUMER_TYPE_USER_BATTERY).mConsumerType))
                .isFalse();
        assertThat(isUidConsumer(createEntry(ConvertUtils.CONSUMER_TYPE_UID_BATTERY).mConsumerType))
                .isTrue();
    }

    @Test
    public void testIsUserEntry_returnExpectedResult() {
        assertThat(
                        isUserConsumer(
                                createEntry(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY)
                                        .mConsumerType))
                .isFalse();
        assertThat(
                        isUserConsumer(
                                createEntry(ConvertUtils.CONSUMER_TYPE_USER_BATTERY).mConsumerType))
                .isTrue();
        assertThat(
                        isUserConsumer(
                                createEntry(ConvertUtils.CONSUMER_TYPE_UID_BATTERY).mConsumerType))
                .isFalse();
    }

    @Test
    public void testIsSystemEntry_returnExpectedResult() {
        assertThat(
                        isSystemConsumer(
                                createEntry(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY)
                                        .mConsumerType))
                .isTrue();
        assertThat(
                        isSystemConsumer(
                                createEntry(ConvertUtils.CONSUMER_TYPE_USER_BATTERY).mConsumerType))
                .isFalse();
        assertThat(
                        isSystemConsumer(
                                createEntry(ConvertUtils.CONSUMER_TYPE_UID_BATTERY).mConsumerType))
                .isFalse();
    }

    @Test
    public void testInterpolate_returnExpectedResult() {
        final long slotTimestamp = 200L;
        final long upperTimestamp = 300L;
        final long lowerTimestamp = 100L;
        final double ratio = 0.5;
        final BatteryHistEntry lowerHistEntry =
                createBatteryHistEntry(
                        /* bootTimestamp= */ 1000L,
                        lowerTimestamp,
                        /* totalPower= */ 50,
                        /* consumePower= */ 10,
                        /* foregroundUsageConsumePower= */ 1,
                        /* foregroundServiceUsageConsumePower= */ 2,
                        /* backgroundUsageConsumePower= */ 3,
                        /* cachedUsageConsumePower= */ 4,
                        /* foregroundUsageTimeInMs= */ 100,
                        /* foregroundServiceUsageTimeInMs= */ 150,
                        /* backgroundUsageTimeInMs= */ 200,
                        /* batteryLevel= */ 90);
        final BatteryHistEntry upperHistEntry =
                createBatteryHistEntry(
                        /* bootTimestamp= */ 1200L,
                        upperTimestamp,
                        /* totalPower= */ 80,
                        /* consumePower= */ 20,
                        /* foregroundUsageConsumePower= */ 4,
                        /* foregroundServiceUsageConsumePower= */ 5,
                        /* backgroundUsageConsumePower= */ 6,
                        /* cachedUsageConsumePower= */ 5,
                        /* foregroundUsageTimeInMs= */ 200,
                        /* foregroundServiceUsageTimeInMs= */ 250,
                        /* backgroundUsageTimeInMs= */ 300,
                        /* batteryLevel= */ 80);

        final BatteryHistEntry newEntry =
                BatteryHistEntry.interpolate(
                        slotTimestamp, upperTimestamp, ratio, lowerHistEntry, upperHistEntry);

        assertBatteryHistEntry(
                newEntry,
                3,
                upperHistEntry.mPercentOfTotal,
                /* bootTimestamp= */ 1200 - 100,
                /* timestamp= */ slotTimestamp,
                /* totalPower= */ 50 + 0.5 * (80 - 50),
                /* consumePower= */ 10 + 0.5 * (20 - 10),
                /* foregroundUsageConsumePower= */ 1 + 0.5 * (4 - 1),
                /* foregroundServiceUsageConsumePower= */ 2 + 0.5 * (5 - 2),
                /* backgroundUsageConsumePower= */ 3 + 0.5 * (6 - 3),
                /* cachedUsageConsumePower= */ 4 + 0.5 * (5 - 4),
                /* foregroundUsageTimeInMs= */ Math.round(100 + 0.5 * (200 - 100)),
                /* foregroundServiceUsageTimeInMs= */ Math.round(150 + 0.5 * (250 - 150)),
                /* backgroundUsageTimeInMs= */ Math.round(200 + 0.5 * (300 - 200)),
                /* batteryLevel= */ (int) Math.round(90 + 0.5 * (80 - 90)));
    }

    @Test
    public void testInterpolate_withoutLowerEntryData_returnExpectedResult() {
        final long slotTimestamp = 200L;
        final long upperTimestamp = 300L;
        final double ratio = 0.5;
        final BatteryHistEntry upperHistEntry =
                createBatteryHistEntry(
                        /* bootTimestamp= */ 1200L,
                        upperTimestamp,
                        /* totalPower= */ 80,
                        /* consumePower= */ 20,
                        /* foregroundUsageConsumePower= */ 4,
                        /* foregroundServiceUsageConsumePower= */ 5,
                        /* backgroundUsageConsumePower= */ 6,
                        /* cachedUsageConsumePower= */ 5,
                        /* foregroundUsageTimeInMs= */ 200,
                        /* foregroundServiceUsageTimeInMs= */ 250,
                        /* backgroundUsageTimeInMs= */ 300,
                        /* batteryLevel= */ 80);

        final BatteryHistEntry newEntry =
                BatteryHistEntry.interpolate(
                        slotTimestamp,
                        upperTimestamp,
                        ratio,
                        /* lowerHistEntry= */ null,
                        upperHistEntry);

        assertBatteryHistEntry(
                newEntry,
                3,
                upperHistEntry.mPercentOfTotal,
                /* bootTimestamp= */ 1200 - 100,
                /* timestamp= */ slotTimestamp,
                /* totalPower= */ 0.5 * 80,
                /* consumePower= */ 0.5 * 20,
                /* foregroundUsageConsumePower= */ 0.5 * 4,
                /* foregroundServiceUsageConsumePower= */ 0.5 * 5,
                /* backgroundUsageConsumePower= */ 0.5 * 6,
                /* cachedUsageConsumePower= */ 0.5 * 5,
                /* foregroundUsageTimeInMs= */ Math.round(0.5 * 200),
                /* foregroundServiceUsageTimeInMs= */ Math.round(0.5 * 250),
                /* backgroundUsageTimeInMs= */ Math.round(0.5 * 300),
                /* batteryLevel= */ upperHistEntry.mBatteryLevel);
    }

    private static BatteryHistEntry createEntry(int consumerType) {
        return new BatteryHistEntry(getContentValuesWithType(consumerType));
    }

    private static ContentValues getContentValuesWithType(int consumerType) {
        final ContentValues values = new ContentValues();
        values.put(BatteryHistEntry.KEY_CONSUMER_TYPE, Integer.valueOf(consumerType));
        return values;
    }

    private void assertBatteryHistEntry(
            BatteryHistEntry entry, int drainType, double percentOfTotal) {
        assertBatteryHistEntry(
                entry,
                drainType,
                percentOfTotal,
                /* bootTimestamp= */ 101L,
                /* timestamp= */ 10001L,
                /* totalPower= */ 5.1,
                /* consumePower= */ 1.1,
                /* foregroundUsageConsumePower= */ 1.2,
                /* foregroundServiceUsageConsumePower= */ 1.3,
                /* backgroundUsageConsumePower= */ 1.4,
                /* cachedUsageConsumePower= */ 1.5,
                /* foregroundUsageTimeInMs= */ 1234L,
                /*foregroundServiceUsageTimeInMs=*/ 3456L,
                /* backgroundUsageTimeInMs= */ 5689L,
                /* batteryLevel= */ 12);
    }

    private void assertBatteryHistEntry(
            BatteryHistEntry entry,
            int drainType,
            double percentOfTotal,
            long bootTimestamp,
            long timestamp,
            double totalPower,
            double consumePower,
            double foregroundUsageConsumePower,
            double foregroundServiceUsageConsumePower,
            double backgroundUsageConsumePower,
            double cachedUsageConsumePower,
            long foregroundUsageTimeInMs,
            long foregroundServiceUsageTimeInMs,
            long backgroundUsageTimeInMs,
            int batteryLevel) {
        assertThat(entry.isValidEntry()).isTrue();
        assertThat(entry.mUid).isEqualTo(1001);
        assertThat(entry.mUserId).isEqualTo(UserHandle.getUserId(1001));
        assertThat(entry.mAppLabel).isEqualTo("Settings");
        assertThat(entry.mPackageName).isEqualTo("com.google.android.settings.battery");
        assertThat(entry.mIsHidden).isTrue();
        assertThat(entry.mBootTimestamp).isEqualTo(bootTimestamp);
        assertThat(entry.mTimestamp).isEqualTo(timestamp);
        assertThat(entry.mZoneId).isEqualTo(TimeZone.getDefault().getID());
        assertThat(entry.mTotalPower).isEqualTo(totalPower);
        assertThat(entry.mConsumePower).isEqualTo(consumePower);
        assertThat(entry.mForegroundUsageConsumePower).isEqualTo(foregroundUsageConsumePower);
        assertThat(entry.mForegroundServiceUsageConsumePower)
                .isEqualTo(foregroundServiceUsageConsumePower);
        assertThat(entry.mBackgroundUsageConsumePower).isEqualTo(backgroundUsageConsumePower);
        assertThat(entry.mCachedUsageConsumePower).isEqualTo(cachedUsageConsumePower);
        assertThat(entry.mPercentOfTotal).isEqualTo(percentOfTotal);
        assertThat(entry.mForegroundUsageTimeInMs).isEqualTo(foregroundUsageTimeInMs);
        assertThat(entry.mForegroundServiceUsageTimeInMs).isEqualTo(foregroundServiceUsageTimeInMs);
        assertThat(entry.mBackgroundUsageTimeInMs).isEqualTo(backgroundUsageTimeInMs);
        assertThat(entry.mDrainType).isEqualTo(drainType);
        assertThat(entry.mConsumerType).isEqualTo(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        assertThat(entry.mBatteryLevel).isEqualTo(batteryLevel);
        assertThat(entry.mBatteryStatus).isEqualTo(BatteryManager.BATTERY_STATUS_FULL);
        assertThat(entry.mBatteryHealth).isEqualTo(BatteryManager.BATTERY_HEALTH_COLD);
    }

    private BatteryHistEntry createBatteryHistEntry(
            long bootTimestamp,
            long timestamp,
            double totalPower,
            double consumePower,
            double foregroundUsageConsumePower,
            double foregroundServiceUsageConsumePower,
            double backgroundUsageConsumePower,
            double cachedUsageConsumePower,
            long foregroundUsageTimeInMs,
            long foregroundServiceUsageTimeInMs,
            long backgroundUsageTimeInMs,
            int batteryLevel) {
        final MatrixCursor cursor =
                new MatrixCursor(
                        new String[] {
                            BatteryHistEntry.KEY_UID,
                            BatteryHistEntry.KEY_USER_ID,
                            BatteryHistEntry.KEY_PACKAGE_NAME,
                            BatteryHistEntry.KEY_TIMESTAMP,
                            BatteryHistEntry.KEY_CONSUMER_TYPE,
                            BatteryHistEntry.KEY_BATTERY_INFORMATION
                        });
        DeviceBatteryState deviceBatteryState =
                DeviceBatteryState.newBuilder()
                        .setBatteryLevel(batteryLevel)
                        .setBatteryStatus(BatteryManager.BATTERY_STATUS_FULL)
                        .setBatteryHealth(BatteryManager.BATTERY_HEALTH_COLD)
                        .build();
        BatteryInformation batteryInformation =
                BatteryInformation.newBuilder()
                        .setDeviceBatteryState(deviceBatteryState)
                        .setIsHidden(true)
                        .setBootTimestamp(bootTimestamp)
                        .setZoneId(TimeZone.getDefault().getID())
                        .setAppLabel("Settings")
                        .setTotalPower(totalPower)
                        .setConsumePower(consumePower)
                        .setForegroundUsageConsumePower(foregroundUsageConsumePower)
                        .setForegroundServiceUsageConsumePower(foregroundServiceUsageConsumePower)
                        .setBackgroundUsageConsumePower(backgroundUsageConsumePower)
                        .setCachedUsageConsumePower(cachedUsageConsumePower)
                        .setPercentOfTotal(0.3)
                        .setDrainType(3)
                        .setForegroundUsageTimeInMs(foregroundUsageTimeInMs)
                        .setForegroundServiceUsageTimeInMs(foregroundServiceUsageTimeInMs)
                        .setBackgroundUsageTimeInMs(backgroundUsageTimeInMs)
                        .build();
        cursor.addRow(
                new Object[] {
                    Long.valueOf(1001),
                    Long.valueOf(UserHandle.getUserId(1001)),
                    "com.google.android.settings.battery",
                    Long.valueOf(timestamp),
                    Integer.valueOf(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY),
                    ConvertUtils.convertBatteryInformationToString(batteryInformation)
                });
        cursor.moveToFirst();
        return new BatteryHistEntry(cursor);
    }
}
