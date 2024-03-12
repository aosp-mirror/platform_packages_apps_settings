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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageEvents;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.BatteryManager;
import android.os.BatteryStatsManager;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserManager;
import android.text.format.DateUtils;

import com.android.settings.fuelgauge.batteryusage.db.AppUsageEventEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@RunWith(RobolectricTestRunner.class)
public final class DataProcessManagerTest {
    private static final String FAKE_ENTRY_KEY = "fake_entry_key";

    private Context mContext;
    private DataProcessManager mDataProcessManager;

    @Mock private IUsageStatsManager mUsageStatsManager;
    @Mock private UserManager mUserManager;
    @Mock private BatteryStatsManager mBatteryStatsManager;
    @Mock private BatteryUsageStats mBatteryUsageStats;
    @Mock private Intent mIntent;
    @Captor private ArgumentCaptor<BatteryUsageStatsQuery> mBatteryUsageStatsQueryCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        DataProcessor.sTestSystemAppsPackageNames = Set.of();
        DataProcessor.sUsageStatsManager = mUsageStatsManager;
        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(mBatteryStatsManager)
                .when(mContext)
                .getSystemService(Context.BATTERY_STATS_SERVICE);
        doReturn(mBatteryUsageStats)
                .when(mBatteryStatsManager)
                .getBatteryUsageStats(mBatteryUsageStatsQueryCaptor.capture());
        doReturn(mIntent).when(mContext).registerReceiver(any(), any());
        doReturn(100).when(mIntent).getIntExtra(eq(BatteryManager.EXTRA_SCALE), anyInt());
        doReturn(66).when(mIntent).getIntExtra(eq(BatteryManager.EXTRA_LEVEL), anyInt());

        mDataProcessManager =
                new DataProcessManager(
                        mContext,
                        /* handler= */ null,
                        /* rawStartTimestamp= */ 0L,
                        /* lastFullChargeTimestamp= */ 0L,
                        /* callbackFunction= */ null,
                        /* hourlyBatteryLevelsPerDay= */ new ArrayList<>(),
                        /* batteryHistoryMap= */ new HashMap<>());
    }

    @After
    public void cleanUp() {
        DatabaseUtils.sFakeSupplier = null;
        DataProcessManager.sFakeBatteryHistoryMap = null;
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    public void constructor_noLevelData() {
        final DataProcessManager dataProcessManager =
                new DataProcessManager(mContext, /* handler= */ null, /* callbackFunction= */ null);
        assertThat(dataProcessManager.getShowScreenOnTime()).isFalse();
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    public void start_loadEmptyDatabaseAppUsageData() {
        final MatrixCursor cursor =
                new MatrixCursor(
                        new String[] {
                            AppUsageEventEntity.KEY_UID,
                            AppUsageEventEntity.KEY_PACKAGE_NAME,
                            AppUsageEventEntity.KEY_TIMESTAMP
                        });
        DatabaseUtils.sFakeSupplier = () -> cursor;
        doReturn(true).when(mUserManager).isUserUnlocked(anyInt());

        mDataProcessManager.start();

        assertThat(mDataProcessManager.getIsCurrentAppUsageLoaded()).isTrue();
        assertThat(mDataProcessManager.getIsDatabaseAppUsageLoaded()).isTrue();
        assertThat(mDataProcessManager.getIsCurrentBatteryHistoryLoaded()).isTrue();
        assertThat(mDataProcessManager.getShowScreenOnTime()).isTrue();
        assertThat(mDataProcessManager.getAppUsageEventList()).isEmpty();
        assertThat(mDataProcessManager.getAppUsagePeriodMap()).isNull();
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    public void start_loadExpectedAppUsageData() throws RemoteException {
        final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay =
                new ArrayList<>();
        final String packageName = "package";
        // Adds the day 1 data.
        final List<Long> timestamps1 = List.of(2L, 3L, 4L);
        final Map<Long, Integer> batteryLevelMap1 =
                Map.of(timestamps1.get(0), 100, timestamps1.get(1), 100, timestamps1.get(2), 100);
        hourlyBatteryLevelsPerDay.add(
                new BatteryLevelData.PeriodBatteryLevelData(batteryLevelMap1, timestamps1));
        // Adds the day 2 data.
        hourlyBatteryLevelsPerDay.add(null);
        // Adds the day 3 data.
        final List<Long> timestamps2 = List.of(5L, 6L);
        final Map<Long, Integer> batteryLevelMap2 =
                Map.of(timestamps2.get(0), 100, timestamps2.get(1), 100);
        hourlyBatteryLevelsPerDay.add(
                new BatteryLevelData.PeriodBatteryLevelData(batteryLevelMap2, timestamps2));
        // Fake current usage data.
        final UsageEvents.Event event1 =
                getUsageEvent(UsageEvents.Event.ACTIVITY_RESUMED, /* timestamp= */ 1, packageName);
        final UsageEvents.Event event2 =
                getUsageEvent(UsageEvents.Event.ACTIVITY_STOPPED, /* timestamp= */ 2, packageName);
        final List<UsageEvents.Event> events = new ArrayList<>();
        events.add(event1);
        events.add(event2);
        doReturn(getUsageEvents(events))
                .when(mUsageStatsManager)
                .queryEventsForUser(anyLong(), anyLong(), anyInt(), any());
        doReturn(true).when(mUserManager).isUserUnlocked(anyInt());
        // Assign current user id.
        doReturn(1).when(mContext).getUserId();
        // No work profile.
        doReturn(new ArrayList<>()).when(mUserManager).getUserProfiles();

        // Fake database usage data.
        final MatrixCursor cursor =
                new MatrixCursor(
                        new String[] {
                            AppUsageEventEntity.KEY_APP_USAGE_EVENT_TYPE,
                            AppUsageEventEntity.KEY_TIMESTAMP,
                            AppUsageEventEntity.KEY_USER_ID,
                            AppUsageEventEntity.KEY_INSTANCE_ID,
                            AppUsageEventEntity.KEY_PACKAGE_NAME
                        });
        // Adds fake data into the cursor.
        cursor.addRow(
                new Object[] {
                    AppUsageEventType.ACTIVITY_RESUMED.getNumber(),
                    /* timestamp= */ 3,
                    /* userId= */ 1,
                    /* instanceId= */ 2,
                    packageName
                });
        cursor.addRow(
                new Object[] {
                    AppUsageEventType.ACTIVITY_STOPPED.getNumber(),
                    /* timestamp= */ 4,
                    /* userId= */ 1,
                    /* instanceId= */ 2,
                    packageName
                });
        cursor.addRow(
                new Object[] {
                    AppUsageEventType.ACTIVITY_RESUMED.getNumber(),
                    /* timestamp= */ 5,
                    /* userId= */ 1,
                    /* instanceId= */ 2,
                    packageName
                });
        cursor.addRow(
                new Object[] {
                    AppUsageEventType.ACTIVITY_STOPPED.getNumber(),
                    /* timestamp= */ 6,
                    /* userId= */ 1,
                    /* instanceId= */ 2,
                    packageName
                });
        DatabaseUtils.sFakeSupplier =
                new Supplier<>() {
                    private int mTimes = 0;

                    @Override
                    public Cursor get() {
                        mTimes++;
                        return mTimes <= 2 ? null : cursor;
                    }
                };

        final DataProcessManager dataProcessManager =
                new DataProcessManager(
                        mContext,
                        /* handler= */ null,
                        /* rawStartTimestamp= */ 2L,
                        /* lastFullChargeTimestamp= */ 1L,
                        /* callbackFunction= */ null,
                        hourlyBatteryLevelsPerDay,
                        /* batteryHistoryMap= */ new HashMap<>());
        dataProcessManager.start();

        assertThat(dataProcessManager.getIsCurrentAppUsageLoaded()).isTrue();
        assertThat(dataProcessManager.getIsDatabaseAppUsageLoaded()).isTrue();
        assertThat(dataProcessManager.getIsCurrentBatteryHistoryLoaded()).isTrue();
        assertThat(dataProcessManager.getShowScreenOnTime()).isTrue();
        final List<AppUsageEvent> appUsageEventList = dataProcessManager.getAppUsageEventList();
        Collections.sort(appUsageEventList, DataProcessor.APP_USAGE_EVENT_TIMESTAMP_COMPARATOR);
        assertThat(appUsageEventList.size()).isEqualTo(6);
        assertAppUsageEvent(
                appUsageEventList.get(0), AppUsageEventType.ACTIVITY_RESUMED, /* timestamp= */ 1);
        assertAppUsageEvent(
                appUsageEventList.get(1), AppUsageEventType.ACTIVITY_STOPPED, /* timestamp= */ 2);
        assertAppUsageEvent(
                appUsageEventList.get(2), AppUsageEventType.ACTIVITY_RESUMED, /* timestamp= */ 3);
        assertAppUsageEvent(
                appUsageEventList.get(3), AppUsageEventType.ACTIVITY_STOPPED, /* timestamp= */ 4);
        assertAppUsageEvent(
                appUsageEventList.get(4), AppUsageEventType.ACTIVITY_RESUMED, /* timestamp= */ 5);
        assertAppUsageEvent(
                appUsageEventList.get(5), AppUsageEventType.ACTIVITY_STOPPED, /* timestamp= */ 6);

        final Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>>
                appUsagePeriodMap = dataProcessManager.getAppUsagePeriodMap();
        assertThat(appUsagePeriodMap.size()).isEqualTo(3);
        // Day 1
        assertThat(appUsagePeriodMap.get(0).size()).isEqualTo(2);
        Map<Long, Map<String, List<AppUsagePeriod>>> hourlyMap = appUsagePeriodMap.get(0).get(0);
        assertThat(hourlyMap).isNull();
        hourlyMap = appUsagePeriodMap.get(0).get(1);
        assertThat(hourlyMap.size()).isEqualTo(1);
        Map<String, List<AppUsagePeriod>> userMap = hourlyMap.get(1L);
        assertThat(userMap.size()).isEqualTo(1);
        assertThat(userMap.get(packageName).size()).isEqualTo(1);
        assertAppUsagePeriod(userMap.get(packageName).get(0), 3, 4);
        // Day 2
        assertThat(appUsagePeriodMap.get(1).size()).isEqualTo(0);
        // Day 3
        assertThat(appUsagePeriodMap.get(2).size()).isEqualTo(1);
        hourlyMap = appUsagePeriodMap.get(2).get(0);
        assertThat(hourlyMap.size()).isEqualTo(1);
        userMap = hourlyMap.get(1L);
        assertThat(userMap.size()).isEqualTo(1);
        assertThat(userMap.get(packageName).size()).isEqualTo(1);
        assertAppUsagePeriod(userMap.get(packageName).get(0), 5, 6);
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    public void start_currentUserLocked_emptyAppUsageList() throws RemoteException {
        final UsageEvents.Event event =
                getUsageEvent(UsageEvents.Event.ACTIVITY_RESUMED, /* timestamp= */ 1, "package");
        final List<UsageEvents.Event> events = new ArrayList<>();
        events.add(event);
        doReturn(getUsageEvents(events))
                .when(mUsageStatsManager)
                .queryEventsForUser(anyLong(), anyLong(), anyInt(), any());
        doReturn(false).when(mUserManager).isUserUnlocked(anyInt());
        final MatrixCursor cursor =
                new MatrixCursor(
                        new String[] {
                            AppUsageEventEntity.KEY_UID,
                            AppUsageEventEntity.KEY_PACKAGE_NAME,
                            AppUsageEventEntity.KEY_TIMESTAMP
                        });
        // Adds fake data into the cursor.
        cursor.addRow(new Object[] {101L, "app name1", 1001L});
        DatabaseUtils.sFakeSupplier = () -> cursor;

        mDataProcessManager.start();

        assertThat(mDataProcessManager.getAppUsageEventList()).isEmpty();
        assertThat(mDataProcessManager.getAppUsagePeriodMap()).isNull();
        assertThat(mDataProcessManager.getShowScreenOnTime()).isFalse();
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    public void getBatteryLevelData_emptyHistoryMap_returnNull() {
        assertThat(
                        DataProcessManager.getBatteryLevelData(
                                mContext,
                                /* handler= */ null,
                                /* isFromPeriodJob= */ false,
                                /* asyncResponseDelegate= */ null))
                .isNull();
        assertThat(
                        DataProcessManager.getBatteryLevelData(
                                mContext,
                                /* handler= */ null,
                                /* isFromPeriodJob= */ true,
                                /* asyncResponseDelegate= */ null))
                .isNull();
    }

    @Test
    public void getBatteryLevelData_allDataInOneHour_returnExpectedResult() {
        // The timestamps and the current time are within half hour before an even hour.
        final long[] timestamps = {
            DateUtils.HOUR_IN_MILLIS * 2 - 300L,
            DateUtils.HOUR_IN_MILLIS * 2 - 200L,
            DateUtils.HOUR_IN_MILLIS * 2 - 100L
        };
        final int[] levels = {100, 99, 98};
        DataProcessManager.sFakeBatteryHistoryMap = createHistoryMap(timestamps, levels);
        DataProcessor.sTestCurrentTimeMillis = timestamps[timestamps.length - 1];

        final BatteryLevelData resultData =
                DataProcessManager.getBatteryLevelData(
                        mContext,
                        /* handler= */ null,
                        /* isFromPeriodJob= */ false,
                        /* asyncResponseDelegate= */ null);

        final List<Long> expectedDailyTimestamps =
                List.of(DateUtils.HOUR_IN_MILLIS * 2 - 300L, DateUtils.HOUR_IN_MILLIS * 2 - 100L);
        final List<Integer> expectedDailyLevels = List.of(100, 66);
        final List<List<Long>> expectedHourlyTimestamps = List.of(expectedDailyTimestamps);
        final List<List<Integer>> expectedHourlyLevels = List.of(expectedDailyLevels);
        verifyExpectedBatteryLevelData(
                resultData,
                expectedDailyTimestamps,
                expectedDailyLevels,
                expectedHourlyTimestamps,
                expectedHourlyLevels);
    }

    @Test
    public void getBatteryLevelData_returnExpectedResult() {
        // Timezone GMT+8: 2022-01-01 00:00:00, 2022-01-01 01:00:00
        final long[] timestamps = {1640966400000L, 1640970000000L};
        final int[] levels = {100, 99};
        DataProcessManager.sFakeBatteryHistoryMap = createHistoryMap(timestamps, levels);
        DataProcessor.sTestCurrentTimeMillis = timestamps[timestamps.length - 1];

        final BatteryLevelData resultData =
                DataProcessManager.getBatteryLevelData(
                        mContext,
                        /* handler= */ null,
                        /* isFromPeriodJob= */ false,
                        /* asyncResponseDelegate= */ null);

        final List<Long> expectedDailyTimestamps =
                List.of(
                        1640966400000L, // 2022-01-01 00:00:00
                        1640970000000L); // 2022-01-01 01:00:00
        final List<Integer> expectedDailyLevels = List.of(100, 66);
        final List<List<Long>> expectedHourlyTimestamps = List.of(expectedDailyTimestamps);
        final List<List<Integer>> expectedHourlyLevels = List.of(expectedDailyLevels);
        verifyExpectedBatteryLevelData(
                resultData,
                expectedDailyTimestamps,
                expectedDailyLevels,
                expectedHourlyTimestamps,
                expectedHourlyLevels);
    }

    private UsageEvents getUsageEvents(final List<UsageEvents.Event> events) {
        UsageEvents usageEvents = new UsageEvents(events, new String[] {"package"});
        Parcel parcel = Parcel.obtain();
        parcel.setDataPosition(0);
        usageEvents.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return UsageEvents.CREATOR.createFromParcel(parcel);
    }

    private UsageEvents.Event getUsageEvent(
            final int eventType, final long timestamp, final String packageName) {
        final UsageEvents.Event event = new UsageEvents.Event();
        event.mEventType = eventType;
        event.mPackage = packageName;
        event.mTimeStamp = timestamp;
        return event;
    }

    private static Map<Long, Map<String, BatteryHistEntry>> createHistoryMap(
            final long[] timestamps, final int[] levels) {
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap = new HashMap<>();
        for (int index = 0; index < timestamps.length; index++) {
            final Map<String, BatteryHistEntry> entryMap = new HashMap<>();
            final ContentValues values = getContentValuesWithBatteryLevel(levels[index]);
            final BatteryHistEntry entry = new BatteryHistEntry(values);
            entryMap.put(FAKE_ENTRY_KEY, entry);
            batteryHistoryMap.put(timestamps[index], entryMap);
        }
        return batteryHistoryMap;
    }

    private static ContentValues getContentValuesWithBatteryLevel(final int level) {
        final ContentValues values = new ContentValues();
        final DeviceBatteryState deviceBatteryState =
                DeviceBatteryState.newBuilder().setBatteryLevel(level).build();
        final BatteryInformation batteryInformation =
                BatteryInformation.newBuilder().setDeviceBatteryState(deviceBatteryState).build();
        values.put(
                BatteryHistEntry.KEY_BATTERY_INFORMATION,
                ConvertUtils.convertBatteryInformationToString(batteryInformation));
        return values;
    }

    private void assertAppUsageEvent(
            final AppUsageEvent event, final AppUsageEventType eventType, final long timestamp) {
        assertThat(event.getType()).isEqualTo(eventType);
        assertThat(event.getTimestamp()).isEqualTo(timestamp);
    }

    private void assertAppUsagePeriod(
            final AppUsagePeriod period, final long startTime, final long endTime) {
        assertThat(period.getStartTime()).isEqualTo(startTime);
        assertThat(period.getEndTime()).isEqualTo(endTime);
    }

    private static void verifyExpectedBatteryLevelData(
            final BatteryLevelData resultData,
            final List<Long> expectedDailyTimestamps,
            final List<Integer> expectedDailyLevels,
            final List<List<Long>> expectedHourlyTimestamps,
            final List<List<Integer>> expectedHourlyLevels) {
        final BatteryLevelData.PeriodBatteryLevelData dailyResultData =
                resultData.getDailyBatteryLevels();
        final List<BatteryLevelData.PeriodBatteryLevelData> hourlyResultData =
                resultData.getHourlyBatteryLevelsPerDay();
        verifyExpectedDailyBatteryLevelData(
                dailyResultData, expectedDailyTimestamps, expectedDailyLevels);
        verifyExpectedHourlyBatteryLevelData(
                hourlyResultData, expectedHourlyTimestamps, expectedHourlyLevels);
    }

    private static void verifyExpectedDailyBatteryLevelData(
            final BatteryLevelData.PeriodBatteryLevelData dailyResultData,
            final List<Long> expectedDailyTimestamps,
            final List<Integer> expectedDailyLevels) {
        assertThat(dailyResultData.getTimestamps()).isEqualTo(expectedDailyTimestamps);
        assertThat(dailyResultData.getLevels()).isEqualTo(expectedDailyLevels);
    }

    private static void verifyExpectedHourlyBatteryLevelData(
            final List<BatteryLevelData.PeriodBatteryLevelData> hourlyResultData,
            final List<List<Long>> expectedHourlyTimestamps,
            final List<List<Integer>> expectedHourlyLevels) {
        final int expectedHourlySize = expectedHourlyTimestamps.size();
        assertThat(hourlyResultData).hasSize(expectedHourlySize);
        for (int dailyIndex = 0; dailyIndex < expectedHourlySize; dailyIndex++) {
            assertThat(hourlyResultData.get(dailyIndex).getTimestamps())
                    .isEqualTo(expectedHourlyTimestamps.get(dailyIndex));
            assertThat(hourlyResultData.get(dailyIndex).getLevels())
                    .isEqualTo(expectedHourlyLevels.get(dailyIndex));
        }
    }
}
