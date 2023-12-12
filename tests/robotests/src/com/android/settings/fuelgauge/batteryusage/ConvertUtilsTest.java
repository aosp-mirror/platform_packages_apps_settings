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

import static android.app.usage.UsageStatsManager.USAGE_SOURCE_CURRENT_ACTIVITY;
import static android.app.usage.UsageStatsManager.USAGE_SOURCE_TASK_ROOT_ACTIVITY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageEvents.Event;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.os.BatteryConsumer;
import android.os.BatteryManager;
import android.os.BatteryUsageStats;
import android.os.LocaleList;
import android.os.UserHandle;

import com.android.settings.fuelgauge.batteryusage.db.AppUsageEventEntity;
import com.android.settings.fuelgauge.batteryusage.db.BatteryEventEntity;
import com.android.settings.fuelgauge.batteryusage.db.BatteryUsageSlotEntity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public final class ConvertUtilsTest {

    private Context mContext;

    @Mock private PackageManager mMockPackageManager;
    @Mock private BatteryUsageStats mBatteryUsageStats;
    @Mock private BatteryEntry mMockBatteryEntry;
    @Mock private IUsageStatsManager mUsageStatsManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        mContext = spy(RuntimeEnvironment.application);
        ConvertUtils.sUsageSource = ConvertUtils.EMPTY_USAGE_SOURCE;
        when(mContext.getPackageManager()).thenReturn(mMockPackageManager);
        DataProcessor.sUsageStatsManager = mUsageStatsManager;
    }

    @Test
    public void convertBatteryEntryToContentValues_appEntry_returnsExpectedContentValues() {
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
        when(mMockBatteryEntry.getTimeInForegroundServiceMs()).thenReturn(3456L);
        when(mMockBatteryEntry.getPowerComponentId()).thenReturn(-1);
        when(mMockBatteryEntry.getConsumerType())
                .thenReturn(ConvertUtils.CONSUMER_TYPE_UID_BATTERY);

        final ContentValues values =
                ConvertUtils.convertBatteryEntryToContentValues(
                        mMockBatteryEntry,
                        mBatteryUsageStats,
                        /* batteryLevel= */ 12,
                        /* batteryStatus= */ BatteryManager.BATTERY_STATUS_FULL,
                        /* batteryHealth= */ BatteryManager.BATTERY_HEALTH_COLD,
                        /* bootTimestamp= */ 101L,
                        /* timestamp= */ 10001L,
                        /* isFullChargeStart= */ true);
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
                .isEqualTo(ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
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
        assertThat(batteryInformation.getForegroundServiceUsageTimeInMs()).isEqualTo(3456L);
        assertThat(batteryInformation.getDrainType()).isEqualTo(-1);
        assertThat(deviceBatteryState.getBatteryLevel()).isEqualTo(12);
        assertThat(deviceBatteryState.getBatteryStatus())
                .isEqualTo(BatteryManager.BATTERY_STATUS_FULL);
        assertThat(deviceBatteryState.getBatteryHealth())
                .isEqualTo(BatteryManager.BATTERY_HEALTH_COLD);
    }

    @Test
    public void convertBatteryEntryToContentValues_systemEntry_returnsExpectedContentValues() {
        when(mMockBatteryEntry.getUid()).thenReturn(-1);
        when(mMockBatteryEntry.getLabel()).thenReturn("CPU");
        when(mMockBatteryEntry.getDefaultPackageName()).thenReturn(null);
        when(mMockBatteryEntry.getPowerComponentId())
                .thenReturn(BatteryConsumer.POWER_COMPONENT_CPU);
        when(mBatteryUsageStats.getConsumedPower()).thenReturn(5.1);
        when(mMockBatteryEntry.getConsumedPower()).thenReturn(1.1);
        when(mMockBatteryEntry.getConsumedPowerInForeground()).thenReturn(1.2);
        when(mMockBatteryEntry.getConsumedPowerInForegroundService()).thenReturn(1.3);
        when(mMockBatteryEntry.getConsumedPowerInBackground()).thenReturn(1.4);
        when(mMockBatteryEntry.getConsumedPowerInCached()).thenReturn(1.5);
        mMockBatteryEntry.mPercent = 0.3;
        when(mMockBatteryEntry.getTimeInForegroundMs()).thenReturn(1234L);
        when(mMockBatteryEntry.getTimeInBackgroundMs()).thenReturn(5689L);
        when(mMockBatteryEntry.getTimeInForegroundServiceMs()).thenReturn(3456L);
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
                        /* isFullChargeStart= */ true);
        final BatteryInformation batteryInformation =
                ConvertUtils.getBatteryInformation(
                        values, BatteryHistEntry.KEY_BATTERY_INFORMATION);
        final DeviceBatteryState deviceBatteryState = batteryInformation.getDeviceBatteryState();

        assertThat(values.getAsLong(BatteryHistEntry.KEY_UID)).isEqualTo(-1);
        assertThat(values.getAsLong(BatteryHistEntry.KEY_USER_ID))
                .isEqualTo(UserHandle.getUserId(1001));
        assertThat(values.getAsString(BatteryHistEntry.KEY_PACKAGE_NAME)).isEqualTo("");
        assertThat(values.getAsLong(BatteryHistEntry.KEY_TIMESTAMP)).isEqualTo(10001L);
        assertThat(values.getAsInteger(BatteryHistEntry.KEY_CONSUMER_TYPE))
                .isEqualTo(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        assertThat(values.getAsBoolean(BatteryHistEntry.KEY_IS_FULL_CHARGE_CYCLE_START)).isTrue();
        assertThat(batteryInformation.getAppLabel()).isEqualTo("CPU");
        assertThat(batteryInformation.getIsHidden()).isFalse();
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
        assertThat(batteryInformation.getForegroundServiceUsageTimeInMs()).isEqualTo(3456L);
        assertThat(batteryInformation.getDrainType())
                .isEqualTo(BatteryConsumer.POWER_COMPONENT_CPU);
        assertThat(deviceBatteryState.getBatteryLevel()).isEqualTo(12);
        assertThat(deviceBatteryState.getBatteryStatus())
                .isEqualTo(BatteryManager.BATTERY_STATUS_FULL);
        assertThat(deviceBatteryState.getBatteryHealth())
                .isEqualTo(BatteryManager.BATTERY_HEALTH_COLD);
    }

    @Test
    public void convertBatteryEntryToContentValues_nullBatteryEntry_returnsExpectedContentValues() {
        final ContentValues values =
                ConvertUtils.convertBatteryEntryToContentValues(
                        /* entry= */ null,
                        /* batteryUsageStats= */ null,
                        /* batteryLevel= */ 12,
                        /* batteryStatus= */ BatteryManager.BATTERY_STATUS_FULL,
                        /* batteryHealth= */ BatteryManager.BATTERY_HEALTH_COLD,
                        /* bootTimestamp= */ 101L,
                        /* timestamp= */ 10001L,
                        /* isFullChargeStart= */ false);

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
        assertThat(values.getAsLong(BatteryHistEntry.KEY_TIMESTAMP)).isEqualTo(10001L);
        assertThat(values.getAsString(BatteryHistEntry.KEY_PACKAGE_NAME))
                .isEqualTo(ConvertUtils.FAKE_PACKAGE_NAME);
    }

    @Test
    public void convertAppUsageEventToContentValues_returnsExpectedContentValues() {
        final AppUsageEvent appUsageEvent =
                AppUsageEvent.newBuilder()
                        .setUid(101L)
                        .setUserId(1001L)
                        .setTimestamp(10001L)
                        .setType(AppUsageEventType.ACTIVITY_RESUMED)
                        .setPackageName("com.android.settings1")
                        .setInstanceId(100001)
                        .setTaskRootPackageName("com.android.settings2")
                        .build();
        final ContentValues values =
                ConvertUtils.convertAppUsageEventToContentValues(appUsageEvent);
        assertThat(values.getAsLong(AppUsageEventEntity.KEY_UID)).isEqualTo(101L);
        assertThat(values.getAsLong(AppUsageEventEntity.KEY_USER_ID)).isEqualTo(1001L);
        assertThat(values.getAsLong(AppUsageEventEntity.KEY_TIMESTAMP)).isEqualTo(10001L);
        assertThat(values.getAsInteger(AppUsageEventEntity.KEY_APP_USAGE_EVENT_TYPE)).isEqualTo(1);
        assertThat(values.getAsString(AppUsageEventEntity.KEY_PACKAGE_NAME))
                .isEqualTo("com.android.settings1");
        assertThat(values.getAsInteger(AppUsageEventEntity.KEY_INSTANCE_ID)).isEqualTo(100001);
        assertThat(values.getAsString(AppUsageEventEntity.KEY_TASK_ROOT_PACKAGE_NAME))
                .isEqualTo("com.android.settings2");
    }

    @Test
    public void convertBatteryEventToContentValues_normalCase_returnsExpectedContentValues() {
        final BatteryEvent batteryEvent =
                BatteryEvent.newBuilder()
                        .setTimestamp(10001L)
                        .setType(BatteryEventType.POWER_CONNECTED)
                        .setBatteryLevel(66)
                        .build();
        final ContentValues values = ConvertUtils.convertBatteryEventToContentValues(batteryEvent);
        assertThat(values.getAsLong(BatteryEventEntity.KEY_TIMESTAMP)).isEqualTo(10001L);
        assertThat(values.getAsInteger(BatteryEventEntity.KEY_BATTERY_EVENT_TYPE))
                .isEqualTo(BatteryEventType.POWER_CONNECTED.getNumber());
        assertThat(values.getAsInteger(BatteryEventEntity.KEY_BATTERY_LEVEL)).isEqualTo(66);
    }

    @Test
    public void convertBatteryUsageSlotToContentValues_normalCase_returnsExpectedContentValues() {
        final BatteryUsageSlot batteryUsageSlot =
                BatteryUsageSlot.newBuilder()
                        .setStartTimestamp(10001L)
                        .setEndTimestamp(30003L)
                        .setStartBatteryLevel(88)
                        .setEndBatteryLevel(66)
                        .setScreenOnTime(123L)
                        .build();
        final ContentValues values =
                ConvertUtils.convertBatteryUsageSlotToContentValues(batteryUsageSlot);
        assertThat(values.getAsLong(BatteryUsageSlotEntity.KEY_TIMESTAMP)).isEqualTo(10001L);
        assertThat(BatteryUsageSlotEntity.KEY_BATTERY_USAGE_SLOT).isNotEmpty();
    }

    @Test
    public void convertToBatteryHistEntry_returnsExpectedResult() {
        final int expectedType = 3;
        when(mMockBatteryEntry.getUid()).thenReturn(1001);
        when(mMockBatteryEntry.getLabel()).thenReturn("Settings");
        when(mMockBatteryEntry.getDefaultPackageName()).thenReturn("com.android.settings.battery");
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
        when(mMockBatteryEntry.getTimeInForegroundServiceMs()).thenReturn(3456L);
        when(mMockBatteryEntry.getPowerComponentId()).thenReturn(expectedType);
        when(mMockBatteryEntry.getConsumerType())
                .thenReturn(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);

        final BatteryHistEntry batteryHistEntry =
                ConvertUtils.convertToBatteryHistEntry(mMockBatteryEntry, mBatteryUsageStats);

        assertThat(batteryHistEntry.mUid).isEqualTo(1001L);
        assertThat(batteryHistEntry.mUserId).isEqualTo(UserHandle.getUserId(1001));
        assertThat(batteryHistEntry.mAppLabel).isEqualTo("Settings");
        assertThat(batteryHistEntry.mPackageName).isEqualTo("com.android.settings.battery");
        assertThat(batteryHistEntry.mIsHidden).isTrue();
        assertThat(batteryHistEntry.mBootTimestamp).isEqualTo(0L);
        assertThat(batteryHistEntry.mTimestamp).isEqualTo(0L);
        assertThat(batteryHistEntry.mZoneId).isEqualTo(TimeZone.getDefault().getID());
        assertThat(batteryHistEntry.mTotalPower).isEqualTo(5.1);
        assertThat(batteryHistEntry.mConsumePower).isEqualTo(1.1);
        assertThat(batteryHistEntry.mForegroundUsageConsumePower).isEqualTo(1.2);
        assertThat(batteryHistEntry.mForegroundServiceUsageConsumePower).isEqualTo(1.3);
        assertThat(batteryHistEntry.mBackgroundUsageConsumePower).isEqualTo(1.4);
        assertThat(batteryHistEntry.mCachedUsageConsumePower).isEqualTo(1.5);
        assertThat(batteryHistEntry.mPercentOfTotal).isEqualTo(0.3);
        assertThat(batteryHistEntry.mForegroundUsageTimeInMs).isEqualTo(1234L);
        assertThat(batteryHistEntry.mBackgroundUsageTimeInMs).isEqualTo(5689L);
        assertThat(batteryHistEntry.mForegroundServiceUsageTimeInMs).isEqualTo(3456L);
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
                        /* entry= */ null, /* batteryUsageStats= */ null);

        assertThat(batteryHistEntry.mBootTimestamp).isEqualTo(0L);
        assertThat(batteryHistEntry.mTimestamp).isEqualTo(0);
        assertThat(batteryHistEntry.mZoneId).isEqualTo(TimeZone.getDefault().getID());
        assertThat(batteryHistEntry.mBatteryLevel).isEqualTo(0);
        assertThat(batteryHistEntry.mBatteryStatus).isEqualTo(0);
        assertThat(batteryHistEntry.mBatteryHealth).isEqualTo(0);
        assertThat(batteryHistEntry.mPackageName).isEqualTo(ConvertUtils.FAKE_PACKAGE_NAME);
    }

    @Test
    public void convertToAppUsageEvent_returnsExpectedResult()
            throws PackageManager.NameNotFoundException {
        final Event event = new Event();
        event.mEventType = UsageEvents.Event.ACTIVITY_RESUMED;
        event.mPackage = "com.android.settings1";
        event.mTimeStamp = 101L;
        event.mInstanceId = 100001;
        event.mTaskRootPackage = "com.android.settings2";
        when(mMockPackageManager.getPackageUidAsUser(any(), anyInt())).thenReturn(1001);

        final long userId = 2;
        final AppUsageEvent appUsageEvent =
                ConvertUtils.convertToAppUsageEvent(mContext, mUsageStatsManager, event, userId);
        assertThat(appUsageEvent.getTimestamp()).isEqualTo(101L);
        assertThat(appUsageEvent.getType()).isEqualTo(AppUsageEventType.ACTIVITY_RESUMED);
        assertThat(appUsageEvent.getPackageName()).isEqualTo("com.android.settings1");
        assertThat(appUsageEvent.getInstanceId()).isEqualTo(100001);
        assertThat(appUsageEvent.getTaskRootPackageName()).isEqualTo("com.android.settings2");
        assertThat(appUsageEvent.getUid()).isEqualTo(1001L);
        assertThat(appUsageEvent.getUserId()).isEqualTo(userId);
    }

    @Test
    public void convertToAppUsageEvent_emptyInstanceIdAndRootName_returnsExpectedResult()
            throws PackageManager.NameNotFoundException {
        final Event event = new Event();
        event.mEventType = UsageEvents.Event.DEVICE_SHUTDOWN;
        event.mPackage = "com.android.settings1";
        event.mTimeStamp = 101L;
        when(mMockPackageManager.getPackageUidAsUser(any(), anyInt())).thenReturn(1001);

        final long userId = 1;
        final AppUsageEvent appUsageEvent =
                ConvertUtils.convertToAppUsageEvent(mContext, mUsageStatsManager, event, userId);
        assertThat(appUsageEvent.getTimestamp()).isEqualTo(101L);
        assertThat(appUsageEvent.getType()).isEqualTo(AppUsageEventType.DEVICE_SHUTDOWN);
        assertThat(appUsageEvent.getPackageName()).isEqualTo("com.android.settings1");
        assertThat(appUsageEvent.getInstanceId()).isEqualTo(0);
        assertThat(appUsageEvent.getTaskRootPackageName()).isEqualTo("");
        assertThat(appUsageEvent.getUid()).isEqualTo(1001L);
        assertThat(appUsageEvent.getUserId()).isEqualTo(userId);
    }

    @Test
    public void convertToAppUsageEvent_emptyPackageName_returnsNull() {
        final Event event = new Event();
        event.mPackage = null;

        final AppUsageEvent appUsageEvent =
                ConvertUtils.convertToAppUsageEvent(
                        mContext, mUsageStatsManager, event, /* userId= */ 0);

        assertThat(appUsageEvent).isNull();
    }

    @Test
    public void convertToAppUsageEvent_failToGetUid_returnsNull()
            throws PackageManager.NameNotFoundException {
        final Event event = new Event();
        event.mEventType = UsageEvents.Event.DEVICE_SHUTDOWN;
        event.mPackage = "com.android.settings1";
        when(mMockPackageManager.getPackageUidAsUser(any(), anyInt()))
                .thenThrow(new PackageManager.NameNotFoundException());

        final long userId = 1;
        final AppUsageEvent appUsageEvent =
                ConvertUtils.convertToAppUsageEvent(mContext, mUsageStatsManager, event, userId);

        assertThat(appUsageEvent).isNull();
    }

    @Test
    public void convertToAppUsageEvent_returnExpectedResult() {
        final MatrixCursor cursor =
                new MatrixCursor(
                        new String[] {
                            AppUsageEventEntity.KEY_UID,
                            AppUsageEventEntity.KEY_USER_ID,
                            AppUsageEventEntity.KEY_PACKAGE_NAME,
                            AppUsageEventEntity.KEY_TIMESTAMP,
                            AppUsageEventEntity.KEY_APP_USAGE_EVENT_TYPE,
                            AppUsageEventEntity.KEY_TASK_ROOT_PACKAGE_NAME,
                            AppUsageEventEntity.KEY_INSTANCE_ID
                        });
        cursor.addRow(
                new Object[] {
                    101L,
                    1001L,
                    "com.android.settings1",
                    10001L,
                    AppUsageEventType.DEVICE_SHUTDOWN.getNumber(),
                    "com.android.settings2",
                    100001L
                });
        cursor.moveToFirst();

        final AppUsageEvent appUsageEvent = ConvertUtils.convertToAppUsageEvent(cursor);

        assertThat(appUsageEvent.getUid()).isEqualTo(101L);
        assertThat(appUsageEvent.getUserId()).isEqualTo(1001L);
        assertThat(appUsageEvent.getPackageName()).isEqualTo("com.android.settings1");
        assertThat(appUsageEvent.getTimestamp()).isEqualTo(10001L);
        assertThat(appUsageEvent.getType()).isEqualTo(AppUsageEventType.DEVICE_SHUTDOWN);
        assertThat(appUsageEvent.getTaskRootPackageName()).isEqualTo("com.android.settings2");
        assertThat(appUsageEvent.getInstanceId()).isEqualTo(100001L);
    }

    @Test
    public void convertToAppUsageEvent_emptyInstanceIdAndRootName_returnExpectedResult() {
        final MatrixCursor cursor =
                new MatrixCursor(
                        new String[] {
                            AppUsageEventEntity.KEY_UID,
                            AppUsageEventEntity.KEY_USER_ID,
                            AppUsageEventEntity.KEY_PACKAGE_NAME,
                            AppUsageEventEntity.KEY_TIMESTAMP,
                            AppUsageEventEntity.KEY_APP_USAGE_EVENT_TYPE
                        });
        cursor.addRow(
                new Object[] {
                    101L,
                    1001L,
                    "com.android.settings1",
                    10001L,
                    AppUsageEventType.DEVICE_SHUTDOWN.getNumber()
                });
        cursor.moveToFirst();

        final AppUsageEvent appUsageEvent = ConvertUtils.convertToAppUsageEvent(cursor);

        assertThat(appUsageEvent.getUid()).isEqualTo(101L);
        assertThat(appUsageEvent.getUserId()).isEqualTo(1001L);
        assertThat(appUsageEvent.getPackageName()).isEqualTo("com.android.settings1");
        assertThat(appUsageEvent.getTimestamp()).isEqualTo(10001L);
        assertThat(appUsageEvent.getType()).isEqualTo(AppUsageEventType.DEVICE_SHUTDOWN);
        assertThat(appUsageEvent.getTaskRootPackageName()).isEqualTo("");
        assertThat(appUsageEvent.getInstanceId()).isEqualTo(0);
    }

    @Test
    public void convertToBatteryEvent_normalCase_returnsExpectedResult() {
        final BatteryEvent batteryEvent =
                ConvertUtils.convertToBatteryEvent(666L, BatteryEventType.POWER_DISCONNECTED, 88);
        assertThat(batteryEvent.getTimestamp()).isEqualTo(666L);
        assertThat(batteryEvent.getType()).isEqualTo(BatteryEventType.POWER_DISCONNECTED);
        assertThat(batteryEvent.getBatteryLevel()).isEqualTo(88);
    }

    @Test
    public void convertToBatteryEventList_normalCase_returnsExpectedResult() {
        final BatteryLevelData batteryLevelData =
                new BatteryLevelData(
                        Map.of(1691589600000L, 98, 1691596800000L, 90, 1691596812345L, 80));

        final List<BatteryEvent> batteryEventList =
                ConvertUtils.convertToBatteryEventList(batteryLevelData);

        assertThat(batteryEventList).hasSize(2);
        assertThat(batteryEventList.get(0).getTimestamp()).isEqualTo(1691589600000L);
        assertThat(batteryEventList.get(0).getType()).isEqualTo(BatteryEventType.EVEN_HOUR);
        assertThat(batteryEventList.get(0).getBatteryLevel()).isEqualTo(98);
        assertThat(batteryEventList.get(1).getTimestamp()).isEqualTo(1691596800000L);
        assertThat(batteryEventList.get(1).getType()).isEqualTo(BatteryEventType.EVEN_HOUR);
        assertThat(batteryEventList.get(1).getBatteryLevel()).isEqualTo(90);
    }

    @Test
    public void convertToBatteryUsageSlotList_normalCase_returnsExpectedResult() {
        BatteryDiffData batteryDiffData1 =
                new BatteryDiffData(
                        mContext, 11L, 12L, 13, 14, 15, List.of(), List.of(), Set.of(), Set.of(),
                        false);
        BatteryDiffData batteryDiffData2 =
                new BatteryDiffData(
                        mContext, 21L, 22L, 23, 24, 25, List.of(), List.of(), Set.of(), Set.of(),
                        false);
        BatteryDiffData batteryDiffData3 =
                new BatteryDiffData(
                        mContext, 31L, 32L, 33, 34, 35, List.of(), List.of(), Set.of(), Set.of(),
                        false);
        final Map<Long, BatteryDiffData> batteryDiffDataMap =
                Map.of(11L, batteryDiffData1, 21L, batteryDiffData2, 31L, batteryDiffData3);

        final List<BatteryUsageSlot> batteryUsageSlotList =
                ConvertUtils.convertToBatteryUsageSlotList(batteryDiffDataMap);

        assertThat(batteryUsageSlotList).hasSize(3);
        assertThat(batteryUsageSlotList.stream().map((s) -> s.getScreenOnTime()).sorted().toList())
                .isEqualTo(List.of(15L, 25L, 35L));
    }

    @Test
    public void getLocale_nullContext_returnDefaultLocale() {
        assertThat(ConvertUtils.getLocale(/* context= */ null)).isEqualTo(Locale.getDefault());
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

    @Test
    public void getEffectivePackageName_currentActivity_returnPackageName() {
        ConvertUtils.sUsageSource = USAGE_SOURCE_CURRENT_ACTIVITY;
        final String packageName = "com.android.settings1";
        final String taskRootPackageName = "com.android.settings2";

        assertThat(
                        ConvertUtils.getEffectivePackageName(
                                mContext, mUsageStatsManager, packageName, taskRootPackageName))
                .isEqualTo(packageName);
    }

    @Test
    public void getEffectivePackageName_emptyUsageSource_returnPackageName() {
        final String packageName = "com.android.settings1";
        final String taskRootPackageName = "com.android.settings2";

        assertThat(
                        ConvertUtils.getEffectivePackageName(
                                mContext, mUsageStatsManager, packageName, taskRootPackageName))
                .isEqualTo(packageName);
    }

    @Test
    public void getEffectivePackageName_rootActivity_returnTaskRootPackageName() {
        ConvertUtils.sUsageSource = USAGE_SOURCE_TASK_ROOT_ACTIVITY;
        final String packageName = "com.android.settings1";
        final String taskRootPackageName = "com.android.settings2";

        assertThat(
                        ConvertUtils.getEffectivePackageName(
                                mContext, mUsageStatsManager, packageName, taskRootPackageName))
                .isEqualTo(taskRootPackageName);
    }

    @Test
    public void getEffectivePackageName_nullOrEmptyTaskRoot_returnPackageName() {
        ConvertUtils.sUsageSource = USAGE_SOURCE_TASK_ROOT_ACTIVITY;
        final String packageName = "com.android.settings1";

        assertThat(
                        ConvertUtils.getEffectivePackageName(
                                mContext,
                                mUsageStatsManager,
                                packageName,
                                /* taskRootPackageName= */ null))
                .isEqualTo(packageName);
        assertThat(
                        ConvertUtils.getEffectivePackageName(
                                mContext,
                                mUsageStatsManager,
                                packageName,
                                /* taskRootPackageName= */ ""))
                .isEqualTo(packageName);
    }
}
