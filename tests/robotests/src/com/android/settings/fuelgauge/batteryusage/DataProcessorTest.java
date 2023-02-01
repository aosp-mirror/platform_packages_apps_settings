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

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageEvents.Event;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.os.BatteryConsumer;
import android.os.BatteryManager;
import android.os.BatteryUsageStats;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserManager;
import android.text.format.DateUtils;

import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public final class DataProcessorTest {
    private static final String FAKE_ENTRY_KEY = "fake_entry_key";

    private Context mContext;

    private FakeFeatureFactory mFeatureFactory;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    @Mock private Intent mIntent;
    @Mock private BatteryUsageStats mBatteryUsageStats;
    @Mock private UserManager mUserManager;
    @Mock private IUsageStatsManager mUsageStatsManager;
    @Mock private BatteryEntry mMockBatteryEntry1;
    @Mock private BatteryEntry mMockBatteryEntry2;
    @Mock private BatteryEntry mMockBatteryEntry3;
    @Mock private BatteryEntry mMockBatteryEntry4;
    @Mock private UsageEvents mUsageEvents1;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));

        mContext = spy(RuntimeEnvironment.application);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mPowerUsageFeatureProvider = mFeatureFactory.powerUsageFeatureProvider;

        DataProcessor.sTestSystemAppsSet = Set.of();
        DataProcessor.sUsageStatsManager = mUsageStatsManager;
        doReturn(mIntent).when(mContext).registerReceiver(
                isA(BroadcastReceiver.class), isA(IntentFilter.class));
        doReturn(100).when(mIntent).getIntExtra(eq(BatteryManager.EXTRA_SCALE), anyInt());
        doReturn(66).when(mIntent).getIntExtra(eq(BatteryManager.EXTRA_LEVEL), anyInt());
        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(new int[]{0}).when(mUserManager).getProfileIdsWithDisabled(anyInt());
    }

    @Test
    public void getAppUsageEvents_returnExpectedResult() throws RemoteException {
        UserInfo userInfo = new UserInfo(/*id=*/ 0, "user_0", /*flags=*/ 0);
        final List<UserInfo> userInfoList = new ArrayList<>();
        userInfoList.add(userInfo);
        doReturn(userInfoList).when(mUserManager).getAliveUsers();
        doReturn(true).when(mUserManager).isUserUnlocked(userInfo.id);
        doReturn(mUsageEvents1)
                .when(mUsageStatsManager)
                .queryEventsForUser(anyLong(), anyLong(), anyInt(), anyString());

        final Map<Long, UsageEvents> resultMap = DataProcessor.getAppUsageEvents(mContext);

        assertThat(resultMap).hasSize(1);
        assertThat(resultMap.get(Long.valueOf(userInfo.id))).isEqualTo(mUsageEvents1);
    }

    @Test
    public void getAppUsageEvents_lockedUser_returnNull() {
        UserInfo userInfo = new UserInfo(/*id=*/ 0, "user_0", /*flags=*/ 0);
        final List<UserInfo> userInfoList = new ArrayList<>();
        userInfoList.add(userInfo);
        doReturn(userInfoList).when(mUserManager).getAliveUsers();
        // Test locked user.
        doReturn(false).when(mUserManager).isUserUnlocked(userInfo.id);

        final Map<Long, UsageEvents> resultMap = DataProcessor.getAppUsageEvents(mContext);

        assertThat(resultMap).isNull();
    }

    @Test
    public void getAppUsageEvents_nullUsageEvents_returnNull() throws RemoteException {
        UserInfo userInfo = new UserInfo(/*id=*/ 0, "user_0", /*flags=*/ 0);
        final List<UserInfo> userInfoList = new ArrayList<>();
        userInfoList.add(userInfo);
        doReturn(userInfoList).when(mUserManager).getAliveUsers();
        doReturn(true).when(mUserManager).isUserUnlocked(userInfo.id);
        doReturn(null).when(mUsageStatsManager)
                .queryEventsForUser(anyLong(), anyLong(), anyInt(), anyString());

        final Map<Long, UsageEvents> resultMap = DataProcessor.getAppUsageEvents(mContext);

        assertThat(resultMap).isNull();
    }

    @Test
    public void getAppUsageEventsForUser_returnExpectedResult() throws RemoteException {
        final int userId = 1;
        doReturn(true).when(mUserManager).isUserUnlocked(userId);
        doReturn(mUsageEvents1)
                .when(mUsageStatsManager)
                .queryEventsForUser(anyLong(), anyLong(), anyInt(), anyString());

        assertThat(DataProcessor.getAppUsageEventsForUser(mContext, userId, 0))
                .isEqualTo(mUsageEvents1);
    }

    @Test
    public void getAppUsageEventsForUser_lockedUser_returnNull() {
        final int userId = 1;
        // Test locked user.
        doReturn(false).when(mUserManager).isUserUnlocked(userId);

        assertThat(DataProcessor.getAppUsageEventsForUser(mContext, userId, 0)).isNull();
    }

    @Test
    public void getAppUsageEventsForUser_nullUsageEvents_returnNull() throws RemoteException {
        final int userId = 1;
        doReturn(true).when(mUserManager).isUserUnlocked(userId);
        doReturn(null).when(mUsageStatsManager)
                .queryEventsForUser(anyLong(), anyLong(), anyInt(), anyString());

        assertThat(DataProcessor.getAppUsageEventsForUser(mContext, userId, 0)).isNull();
    }

    @Test
    public void generateAppUsagePeriodMap_returnExpectedResult() {
        DataProcessor.sDebug = true;
        final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay =
                new ArrayList<>();
        final String packageName = "com.android.settings";
        // Adds the day 1 data.
        final List<Long> timestamps1 = List.of(14400000L, 18000000L, 21600000L);
        final List<Integer> levels1 = List.of(100, 100, 100);
        hourlyBatteryLevelsPerDay.add(
                new BatteryLevelData.PeriodBatteryLevelData(timestamps1, levels1));
        // Adds the day 2 data.
        hourlyBatteryLevelsPerDay.add(null);
        // Adds the day 3 data.
        final List<Long> timestamps2 = List.of(45200000L, 48800000L);
        final List<Integer> levels2 = List.of(100, 100);
        hourlyBatteryLevelsPerDay.add(
                new BatteryLevelData.PeriodBatteryLevelData(timestamps2, levels2));
        final List<AppUsageEvent> appUsageEventList = new ArrayList<>();
        // Adds some events before the start timestamp.
        appUsageEventList.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 1, /*userId=*/ 1,
                /*instanceId=*/ 2, packageName));
        appUsageEventList.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 2, /*userId=*/ 1,
                /*instanceId=*/ 2, packageName));
        // Adds the valid app usage events.
        appUsageEventList.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 4200000L, /*userId=*/ 1,
                /*instanceId=*/ 2, packageName));
        appUsageEventList.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 4500000L, /*userId=*/ 1,
                /*instanceId=*/ 2, packageName));
        appUsageEventList.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 12600000L, /*userId=*/ 2,
                /*instanceId=*/ 3, packageName));
        appUsageEventList.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 15600000L, /*userId=*/ 2,
                /*instanceId=*/ 3, packageName));
        appUsageEventList.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 16200000L, /*userId=*/ 2,
                /*instanceId=*/ 3, packageName));
        appUsageEventList.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 18000000L, /*userId=*/ 2,
                /*instanceId=*/ 3, packageName));
        appUsageEventList.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 17200000L, /*userId=*/ 1,
                /*instanceId=*/ 2, packageName));
        appUsageEventList.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 17800000L, /*userId=*/ 1,
                /*instanceId=*/ 2, packageName));
        appUsageEventList.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 46000000L, /*userId=*/ 1,
                /*instanceId=*/ 2, packageName));
        appUsageEventList.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 47800000L, /*userId=*/ 1,
                /*instanceId=*/ 2, packageName));
        appUsageEventList.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 49000000L, /*userId=*/ 1,
                /*instanceId=*/ 2, packageName));
        appUsageEventList.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 59600000L, /*userId=*/ 1,
                /*instanceId=*/ 4, packageName));
        appUsageEventList.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 61200000L, /*userId=*/ 1,
                /*instanceId=*/ 4, packageName));

        final Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>> periodMap =
                DataProcessor.generateAppUsagePeriodMap(
                        hourlyBatteryLevelsPerDay, appUsageEventList);

        assertThat(periodMap).hasSize(3);
        // Day 1
        assertThat(periodMap.get(0)).hasSize(2);
        Map<Long, Map<String, List<AppUsagePeriod>>> hourlyMap = periodMap.get(0).get(0);
        assertThat(hourlyMap).hasSize(2);
        Map<String, List<AppUsagePeriod>> userMap = hourlyMap.get(1L);
        assertThat(userMap).hasSize(1);
        assertThat(userMap.get(packageName)).hasSize(1);
        assertAppUsagePeriod(userMap.get(packageName).get(0), 17200000L, 17800000L);
        userMap = hourlyMap.get(2L);
        assertThat(userMap).hasSize(1);
        assertThat(userMap.get(packageName)).hasSize(2);
        assertAppUsagePeriod(userMap.get(packageName).get(0), 14400000L, 15600000L);
        assertAppUsagePeriod(userMap.get(packageName).get(1), 16200000L, 18000000L);
        hourlyMap = periodMap.get(0).get(1);
        assertThat(hourlyMap).isNull();
        // Day 2
        assertThat(periodMap.get(1)).hasSize(0);
        // Day 3
        assertThat(periodMap.get(2)).hasSize(1);
        hourlyMap = periodMap.get(2).get(0);
        assertThat(hourlyMap).hasSize(1);
        userMap = hourlyMap.get(1L);
        assertThat(userMap).hasSize(1);
        assertThat(userMap.get(packageName)).hasSize(2);
        assertAppUsagePeriod(userMap.get(packageName).get(0), 45970000L, 46000000L);
        assertAppUsagePeriod(userMap.get(packageName).get(1), 47800000L, 48800000L);
    }

    @Test
    public void generateAppUsagePeriodMap_emptyEventList_returnNull() {
        final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay =
                new ArrayList<>();
        hourlyBatteryLevelsPerDay.add(
                new BatteryLevelData.PeriodBatteryLevelData(new ArrayList<>(), new ArrayList<>()));
        assertThat(DataProcessor.generateAppUsagePeriodMap(
                hourlyBatteryLevelsPerDay, new ArrayList<>())).isNull();
    }

    @Test
    public void generateAppUsageEventListFromUsageEvents_returnExpectedResult() {
        Event event1 = getUsageEvent(Event.NOTIFICATION_INTERRUPTION, /*timestamp=*/ 1);
        Event event2 = getUsageEvent(Event.ACTIVITY_RESUMED, /*timestamp=*/ 2);
        Event event3 = getUsageEvent(Event.ACTIVITY_STOPPED, /*timestamp=*/ 3);
        Event event4 = getUsageEvent(Event.DEVICE_SHUTDOWN, /*timestamp=*/ 4);
        Event event5 = getUsageEvent(Event.ACTIVITY_RESUMED, /*timestamp=*/ 5);
        event5.mPackage = null;
        List<Event> events1 = new ArrayList<>();
        events1.add(event1);
        events1.add(event2);
        List<Event> events2 = new ArrayList<>();
        events2.add(event3);
        events2.add(event4);
        events2.add(event5);
        final long userId1 = 101L;
        final long userId2 = 102L;
        final long userId3 = 103L;
        final Map<Long, UsageEvents> appUsageEvents = new HashMap();
        appUsageEvents.put(userId1, getUsageEvents(events1));
        appUsageEvents.put(userId2, getUsageEvents(events2));
        appUsageEvents.put(userId3, getUsageEvents(new ArrayList<>()));

        final List<AppUsageEvent> appUsageEventList =
                DataProcessor.generateAppUsageEventListFromUsageEvents(mContext, appUsageEvents);

        assertThat(appUsageEventList).hasSize(3);
        assertAppUsageEvent(
                appUsageEventList.get(0), AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 2);
        assertAppUsageEvent(
                appUsageEventList.get(1), AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 3);
        assertAppUsageEvent(
                appUsageEventList.get(2), AppUsageEventType.DEVICE_SHUTDOWN, /*timestamp=*/ 4);
    }

    @Test
    public void getDeviceScreenOnTime_returnExpectedResult() {
        final Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>>
                appUsagePeriodMap = new HashMap<>();
        appUsagePeriodMap.put(0, new HashMap<>());
        appUsagePeriodMap.put(1, new HashMap<>());
        appUsagePeriodMap.put(2, null);
        final long userId1 = 1;
        final long userId2 = 2;
        // Adds the index [0][0].
        Map<Long, Map<String, List<AppUsagePeriod>>> appUsageMap = new HashMap<>();
        Map<String, List<AppUsagePeriod>> userPeriodMap = new HashMap<>();
        appUsageMap.put(userId1, userPeriodMap);
        userPeriodMap.put(
                "package1", List.of(buildAppUsagePeriod(0, 5), buildAppUsagePeriod(5, 7)));
        userPeriodMap.put("package2", List.of(buildAppUsagePeriod(10, 25)));
        userPeriodMap = new HashMap<>();
        appUsageMap.put(userId2, userPeriodMap);
        userPeriodMap.put("package3", List.of(buildAppUsagePeriod(15, 45)));
        appUsagePeriodMap.get(0).put(0, appUsageMap);
        // Adds the index [0][1].
        appUsageMap = new HashMap<>();
        userPeriodMap = new HashMap<>();
        appUsageMap.put(userId1, userPeriodMap);
        userPeriodMap.put(
                "package1", List.of(buildAppUsagePeriod(50, 60), buildAppUsagePeriod(70, 80)));
        appUsagePeriodMap.get(0).put(1, appUsageMap);
        // Adds the index [1][0].
        appUsageMap = new HashMap<>();
        userPeriodMap = new HashMap<>();
        appUsageMap.put(userId1, userPeriodMap);
        userPeriodMap.put("package2", List.of(buildAppUsagePeriod(0, 8000000L)));
        userPeriodMap.put("package3",
                List.of(buildAppUsagePeriod(10, 15), buildAppUsagePeriod(25, 29)));
        appUsagePeriodMap.get(1).put(0, appUsageMap);

        final Map<Integer, Map<Integer, Long>> deviceScreenOnTime =
                DataProcessor.getDeviceScreenOnTime(appUsagePeriodMap);

        assertThat(deviceScreenOnTime.get(0).get(0)).isEqualTo(42);
        assertThat(deviceScreenOnTime.get(0).get(1)).isEqualTo(20);
        assertThat(deviceScreenOnTime.get(1).get(0)).isEqualTo(7200000);
        assertThat(deviceScreenOnTime.get(0).get(DataProcessor.SELECTED_INDEX_ALL)).isEqualTo(62);
        assertThat(deviceScreenOnTime.get(1).get(DataProcessor.SELECTED_INDEX_ALL))
                .isEqualTo(7200000);
        assertThat(deviceScreenOnTime
                .get(DataProcessor.SELECTED_INDEX_ALL)
                .get(DataProcessor.SELECTED_INDEX_ALL))
                .isEqualTo(7200062);
    }

    @Test
    public void getDeviceScreenOnTime_nullUsageMap_returnNull() {
        assertThat(DataProcessor.getDeviceScreenOnTime(null)).isNull();
    }

    @Test
    public void getHistoryMapWithExpectedTimestamps_emptyHistoryMap_returnEmptyMap() {
        assertThat(DataProcessor
                .getHistoryMapWithExpectedTimestamps(mContext, new HashMap<>()))
                .isEmpty();
    }

    @Test
    public void getHistoryMapWithExpectedTimestamps_returnExpectedMap() {
        // Timezone GMT+8
        final long[] timestamps = {
                1640966700000L, // 2022-01-01 00:05:00
                1640970180000L, // 2022-01-01 01:03:00
                1640973840000L, // 2022-01-01 02:04:00
                1640978100000L, // 2022-01-01 03:15:00
                1640981400000L  // 2022-01-01 04:10:00
        };
        final int[] levels = {100, 94, 90, 82, 50};
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap =
                createHistoryMap(timestamps, levels);
        DataProcessor.sTestCurrentTimeMillis = timestamps[timestamps.length - 1];

        final Map<Long, Map<String, BatteryHistEntry>> resultMap =
                DataProcessor.getHistoryMapWithExpectedTimestamps(mContext, batteryHistoryMap);

        // Timezone GMT+8
        final long[] expectedTimestamps = {
                1640966400000L, // 2022-01-01 00:00:00
                1640970000000L, // 2022-01-01 01:00:00
                1640973600000L, // 2022-01-01 02:00:00
                1640977200000L, // 2022-01-01 03:00:00
                1640980800000L, // 2022-01-01 04:00:00
                1640984400000L, // 2022-01-01 05:00:00
                1640988000000L  // 2022-01-01 06:00:00
        };
        final int[] expectedLevels = {100, 94, 90, 84, 56, 98, 98};
        assertThat(resultMap).hasSize(expectedLevels.length);
        for (int index = 0; index < 5; index++) {
            assertThat(resultMap.get(expectedTimestamps[index]).get(FAKE_ENTRY_KEY).mBatteryLevel)
                    .isEqualTo(expectedLevels[index]);
        }
        for (int index = 5; index < 7; index++) {
            assertThat(resultMap.get(expectedTimestamps[index]).containsKey(
                    DataProcessor.CURRENT_TIME_BATTERY_HISTORY_PLACEHOLDER)).isTrue();
        }
    }

    @Test
    public void getLevelDataThroughProcessedHistoryMap_notEnoughData_returnNull() {
        final long[] timestamps = {100L};
        final int[] levels = {100};
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap =
                createHistoryMap(timestamps, levels);
        DataProcessor.sTestCurrentTimeMillis = timestamps[timestamps.length - 1];

        assertThat(
                DataProcessor.getLevelDataThroughProcessedHistoryMap(mContext, batteryHistoryMap))
                .isNull();
    }

    @Test
    public void getLevelDataThroughProcessedHistoryMap_OneDayData_returnExpectedResult() {
        // Timezone GMT+8
        final long[] timestamps = {
                1640966400000L, // 2022-01-01 00:00:00
                1640970000000L, // 2022-01-01 01:00:00
                1640973600000L, // 2022-01-01 02:00:00
                1640977200000L, // 2022-01-01 03:00:00
                1640980800000L  // 2022-01-01 04:00:00
        };
        final int[] levels = {100, 94, 90, 82, 50};
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap =
                createHistoryMap(timestamps, levels);
        DataProcessor.sTestCurrentTimeMillis = timestamps[timestamps.length - 1];

        final BatteryLevelData resultData =
                DataProcessor.getLevelDataThroughProcessedHistoryMap(mContext, batteryHistoryMap);

        final List<Long> expectedDailyTimestamps = List.of(timestamps[0], timestamps[4]);
        final List<Integer> expectedDailyLevels = List.of(levels[0], levels[4]);
        final List<List<Long>> expectedHourlyTimestamps = List.of(
                List.of(timestamps[0], timestamps[2], timestamps[4])
        );
        final List<List<Integer>> expectedHourlyLevels = List.of(
                List.of(levels[0], levels[2], levels[4])
        );
        verifyExpectedBatteryLevelData(
                resultData,
                expectedDailyTimestamps,
                expectedDailyLevels,
                expectedHourlyTimestamps,
                expectedHourlyLevels);
    }

    @Test
    public void getLevelDataThroughProcessedHistoryMap_MultipleDaysData_returnExpectedResult() {
        // Timezone GMT+8
        final long[] timestamps = {
                1641038400000L, // 2022-01-01 20:00:00
                1641060000000L, // 2022-01-02 02:00:00
                1641067200000L, // 2022-01-02 04:00:00
                1641081600000L, // 2022-01-02 08:00:00
        };
        final int[] levels = {100, 94, 90, 82};
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap =
                createHistoryMap(timestamps, levels);
        DataProcessor.sTestCurrentTimeMillis = timestamps[timestamps.length - 1];

        final BatteryLevelData resultData =
                DataProcessor.getLevelDataThroughProcessedHistoryMap(mContext, batteryHistoryMap);

        final List<Long> expectedDailyTimestamps = List.of(
                1641038400000L, // 2022-01-01 20:00:00
                1641052800000L, // 2022-01-02 00:00:00
                1641081600000L  // 2022-01-02 08:00:00
        );
        final List<Integer> expectedDailyLevels = new ArrayList<>();
        expectedDailyLevels.add(100);
        expectedDailyLevels.add(null);
        expectedDailyLevels.add(82);
        final List<List<Long>> expectedHourlyTimestamps = List.of(
                List.of(
                        1641038400000L, // 2022-01-01 20:00:00
                        1641045600000L, // 2022-01-01 22:00:00
                        1641052800000L  // 2022-01-02 00:00:00
                ),
                List.of(
                        1641052800000L, // 2022-01-02 00:00:00
                        1641060000000L, // 2022-01-02 02:00:00
                        1641067200000L, // 2022-01-02 04:00:00
                        1641074400000L, // 2022-01-02 06:00:00
                        1641081600000L  // 2022-01-02 08:00:00
                )
        );
        final List<Integer> expectedHourlyLevels1 = new ArrayList<>();
        expectedHourlyLevels1.add(100);
        expectedHourlyLevels1.add(null);
        expectedHourlyLevels1.add(null);
        final List<Integer> expectedHourlyLevels2 = new ArrayList<>();
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(94);
        expectedHourlyLevels2.add(90);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(82);
        final List<List<Integer>> expectedHourlyLevels = List.of(
                expectedHourlyLevels1,
                expectedHourlyLevels2
        );
        verifyExpectedBatteryLevelData(
                resultData,
                expectedDailyTimestamps,
                expectedDailyLevels,
                expectedHourlyTimestamps,
                expectedHourlyLevels);
    }

    @Test
    public void getLevelDataThroughProcessedHistoryMap_daylightSaving25Hour_returnExpectedResult() {
        // Timezone PST 2022-11-06 has an extra 01:00:00 - 01:59:59 for daylight saving.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        final long[] timestamps = {
                1667667600000L, // 2022-11-05 10:00:00
                1667829600000L  // 2022-11-07 06:00:00
        };
        final int[] levels = {100, 88};
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap =
                createHistoryMap(timestamps, levels);
        DataProcessor.sTestCurrentTimeMillis = timestamps[timestamps.length - 1];

        final BatteryLevelData resultData =
                DataProcessor.getLevelDataThroughProcessedHistoryMap(mContext, batteryHistoryMap);

        final List<Long> expectedDailyTimestamps = List.of(
                1667667600000L, // 2022-11-05 10:00:00
                1667718000000L, // 2022-11-06 00:00:00
                1667808000000L, // 2022-11-07 00:00:00
                1667829600000L  // 2022-11-07 06:00:00
        );
        final List<Integer> expectedDailyLevels = new ArrayList<>();
        expectedDailyLevels.add(100);
        expectedDailyLevels.add(null);
        expectedDailyLevels.add(null);
        expectedDailyLevels.add(88);
        final List<List<Long>> expectedHourlyTimestamps = List.of(
                List.of(
                        1667667600000L, // 2022-11-05 10:00:00
                        1667674800000L, // 2022-11-05 12:00:00
                        1667682000000L, // 2022-11-05 14:00:00
                        1667689200000L, // 2022-11-05 16:00:00
                        1667696400000L, // 2022-11-05 18:00:00
                        1667703600000L, // 2022-11-05 20:00:00
                        1667710800000L, // 2022-11-05 22:00:00
                        1667718000000L  // 2022-11-06 00:00:00
                ),
                List.of(
                        1667718000000L, // 2022-11-06 00:00:00
                        1667725200000L, // 2022-11-06 01:00:00  after daylight saving change
                        1667732400000L, // 2022-11-06 03:00:00
                        1667739600000L, // 2022-11-06 05:00:00
                        1667746800000L, // 2022-11-06 07:00:00
                        1667754000000L, // 2022-11-06 09:00:00
                        1667761200000L, // 2022-11-06 11:00:00
                        1667768400000L, // 2022-11-06 13:00:00
                        1667775600000L, // 2022-11-06 15:00:00
                        1667782800000L, // 2022-11-06 17:00:00
                        1667790000000L, // 2022-11-06 19:00:00
                        1667797200000L, // 2022-11-06 21:00:00
                        1667804400000L  // 2022-11-06 23:00:00
                ),
                List.of(
                        1667808000000L, // 2022-11-07 00:00:00
                        1667815200000L, // 2022-11-07 02:00:00
                        1667822400000L, // 2022-11-07 04:00:00
                        1667829600000L  // 2022-11-07 06:00:00
                )
        );
        final List<Integer> expectedHourlyLevels1 = new ArrayList<>();
        expectedHourlyLevels1.add(100);
        expectedHourlyLevels1.add(null);
        expectedHourlyLevels1.add(null);
        expectedHourlyLevels1.add(null);
        expectedHourlyLevels1.add(null);
        expectedHourlyLevels1.add(null);
        expectedHourlyLevels1.add(null);
        expectedHourlyLevels1.add(null);
        final List<Integer> expectedHourlyLevels2 = new ArrayList<>();
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        final List<Integer> expectedHourlyLevels3 = new ArrayList<>();
        expectedHourlyLevels3.add(null);
        expectedHourlyLevels3.add(null);
        expectedHourlyLevels3.add(null);
        expectedHourlyLevels3.add(88);
        final List<List<Integer>> expectedHourlyLevels = List.of(
                expectedHourlyLevels1,
                expectedHourlyLevels2,
                expectedHourlyLevels3
        );
        verifyExpectedBatteryLevelData(
                resultData,
                expectedDailyTimestamps,
                expectedDailyLevels,
                expectedHourlyTimestamps,
                expectedHourlyLevels);
    }

    @Test
    public void getLevelDataThroughProcessedHistoryMap_daylightSaving23Hour_returnExpectedResult() {
        // Timezone PST 2022-03-13 has no 02:00:00 - 02:59:59 for daylight saving.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        final long[] timestamps = {
                1647151200000L, // 2022-03-12 22:00:00
                1647262800000L  // 2022-03-14 06:00:00
        };
        final int[] levels = {100, 88};
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap =
                createHistoryMap(timestamps, levels);
        DataProcessor.sTestCurrentTimeMillis = timestamps[timestamps.length - 1];

        final BatteryLevelData resultData =
                DataProcessor.getLevelDataThroughProcessedHistoryMap(mContext, batteryHistoryMap);

        final List<Long> expectedDailyTimestamps = List.of(
                1647151200000L, // 2022-03-12 22:00:00
                1647158400000L, // 2022-03-13 00:00:00
                1647241200000L, // 2022-03-14 00:00:00
                1647262800000L  // 2022-03-14 06:00:00
        );
        final List<Integer> expectedDailyLevels = new ArrayList<>();
        expectedDailyLevels.add(100);
        expectedDailyLevels.add(null);
        expectedDailyLevels.add(null);
        expectedDailyLevels.add(88);
        final List<List<Long>> expectedHourlyTimestamps = List.of(
                List.of(
                        1647151200000L, // 2022-03-12 22:00:00
                        1647158400000L  // 2022-03-13 00:00:00
                ),
                List.of(
                        1647158400000L, // 2022-03-13 00:00:00
                        1647165600000L, // 2022-03-13 03:00:00  after daylight saving change
                        1647172800000L, // 2022-03-13 05:00:00
                        1647180000000L, // 2022-03-13 07:00:00
                        1647187200000L, // 2022-03-13 09:00:00
                        1647194400000L, // 2022-03-13 11:00:00
                        1647201600000L, // 2022-03-13 13:00:00
                        1647208800000L, // 2022-03-13 15:00:00
                        1647216000000L, // 2022-03-13 17:00:00
                        1647223200000L, // 2022-03-13 19:00:00
                        1647230400000L, // 2022-03-13 21:00:00
                        1647237600000L  // 2022-03-13 23:00:00
                ),
                List.of(
                        1647241200000L, // 2022-03-14 00:00:00
                        1647248400000L, // 2022-03-14 02:00:00
                        1647255600000L, // 2022-03-14 04:00:00
                        1647262800000L  // 2022-03-14 06:00:00
                )
        );
        final List<Integer> expectedHourlyLevels1 = new ArrayList<>();
        expectedHourlyLevels1.add(100);
        expectedHourlyLevels1.add(null);
        final List<Integer> expectedHourlyLevels2 = new ArrayList<>();
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        expectedHourlyLevels2.add(null);
        final List<Integer> expectedHourlyLevels3 = new ArrayList<>();
        expectedHourlyLevels3.add(null);
        expectedHourlyLevels3.add(null);
        expectedHourlyLevels3.add(null);
        expectedHourlyLevels3.add(88);
        final List<List<Integer>> expectedHourlyLevels = List.of(
                expectedHourlyLevels1,
                expectedHourlyLevels2,
                expectedHourlyLevels3
        );
        verifyExpectedBatteryLevelData(
                resultData,
                expectedDailyTimestamps,
                expectedDailyLevels,
                expectedHourlyTimestamps,
                expectedHourlyLevels);
    }

    @Test
    public void getTimestampSlots_emptyRawList_returnEmptyList() {
        final List<Long> resultList = DataProcessor.getTimestampSlots(
                new ArrayList<>(), 1641038400000L); // 2022-01-01 20:00:00
        assertThat(resultList).isEmpty();
    }

    @Test
    public void getTimestampSlots_startWithEvenHour_returnExpectedResult() {
        final Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(2022, 6, 5, 6, 30, 50); // 2022-07-05 06:30:50
        final Calendar endCalendar = Calendar.getInstance();
        endCalendar.set(2022, 6, 5, 22, 30, 50); // 2022-07-05 22:30:50

        final Calendar expectedStartCalendar = Calendar.getInstance();
        expectedStartCalendar.set(2022, 6, 5, 6, 0, 0); // 2022-07-05 06:00:00
        final Calendar expectedEndCalendar = Calendar.getInstance();
        expectedEndCalendar.set(2022, 6, 6, 0, 0, 0); // 2022-07-05 22:00:00
        verifyExpectedTimestampSlots(
                startCalendar, endCalendar, expectedStartCalendar, expectedEndCalendar);
    }

    @Test
    public void getTimestampSlots_startWithOddHour_returnExpectedResult() {
        final Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(2022, 6, 5, 5, 0, 50); // 2022-07-05 05:00:50
        final Calendar endCalendar = Calendar.getInstance();
        endCalendar.set(2022, 6, 6, 21, 0, 50); // 2022-07-06 21:00:50

        final Calendar expectedStartCalendar = Calendar.getInstance();
        expectedStartCalendar.set(2022, 6, 5, 6, 0, 0); // 2022-07-05 06:00:00
        final Calendar expectedEndCalendar = Calendar.getInstance();
        expectedEndCalendar.set(2022, 6, 6, 22, 0, 0); // 2022-07-06 20:00:00
        verifyExpectedTimestampSlots(
                startCalendar, endCalendar, expectedStartCalendar, expectedEndCalendar);
    }

    @Test
    public void getDailyTimestamps_notEnoughData_returnEmptyList() {
        assertThat(DataProcessor.getDailyTimestamps(new ArrayList<>())).isEmpty();
        assertThat(DataProcessor.getDailyTimestamps(List.of(100L))).isEmpty();
        assertThat(DataProcessor.getDailyTimestamps(List.of(100L, 5400000L))).isEmpty();
    }

    @Test
    public void getDailyTimestamps_OneHourDataPerDay_returnEmptyList() {
        // Timezone GMT+8
        final List<Long> timestamps = List.of(
                1641049200000L, // 2022-01-01 23:00:00
                1641052800000L, // 2022-01-02 00:00:00
                1641056400000L  // 2022-01-02 01:00:00
        );
        assertThat(DataProcessor.getDailyTimestamps(timestamps)).isEmpty();
    }

    @Test
    public void getDailyTimestamps_OneDayData_returnExpectedList() {
        // Timezone GMT+8
        final List<Long> timestamps = List.of(
                1640966400000L, // 2022-01-01 00:00:00
                1640970000000L, // 2022-01-01 01:00:00
                1640973600000L, // 2022-01-01 02:00:00
                1640977200000L, // 2022-01-01 03:00:00
                1640980800000L  // 2022-01-01 04:00:00
        );

        final List<Long> expectedTimestamps = List.of(
                1640966400000L, // 2022-01-01 00:00:00
                1640980800000L  // 2022-01-01 04:00:00
        );
        assertThat(DataProcessor.getDailyTimestamps(timestamps)).isEqualTo(expectedTimestamps);
    }

    @Test
    public void getDailyTimestamps_MultipleDaysData_returnExpectedList() {
        // Timezone GMT+8
        final List<Long> timestamps = List.of(
                1641045600000L, // 2022-01-01 22:00:00
                1641060000000L, // 2022-01-02 02:00:00
                1641160800000L, // 2022-01-03 06:00:00
                1641232800000L  // 2022-01-04 02:00:00
        );

        final List<Long> expectedTimestamps = List.of(
                1641045600000L, // 2022-01-01 22:00:00
                1641052800000L, // 2022-01-02 00:00:00
                1641139200000L, // 2022-01-03 00:00:00
                1641225600000L, // 2022-01-04 00:00:00
                1641232800000L  // 2022-01-04 02:00:00
        );
        assertThat(DataProcessor.getDailyTimestamps(timestamps)).isEqualTo(expectedTimestamps);
    }

    @Test
    public void getDailyTimestamps_FirstDayOneHourData_returnExpectedList() {
        // Timezone GMT+8
        final List<Long> timestamps = List.of(
                1641049200000L, // 2022-01-01 23:00:00
                1641060000000L, // 2022-01-02 02:00:00
                1641160800000L, // 2022-01-03 06:00:00
                1641254400000L  // 2022-01-04 08:00:00
        );

        final List<Long> expectedTimestamps = List.of(
                1641052800000L, // 2022-01-02 00:00:00
                1641139200000L, // 2022-01-03 00:00:00
                1641225600000L, // 2022-01-04 00:00:00
                1641254400000L  // 2022-01-04 08:00:00
        );
        assertThat(DataProcessor.getDailyTimestamps(timestamps)).isEqualTo(expectedTimestamps);
    }

    @Test
    public void getDailyTimestamps_LastDayNoData_returnExpectedList() {
        // Timezone GMT+8
        final List<Long> timestamps = List.of(
                1640988000000L, // 2022-01-01 06:00:00
                1641060000000L, // 2022-01-02 02:00:00
                1641160800000L, // 2022-01-03 06:00:00
                1641225600000L  // 2022-01-04 00:00:00
        );

        final List<Long> expectedTimestamps = List.of(
                1640988000000L, // 2022-01-01 06:00:00
                1641052800000L, // 2022-01-02 00:00:00
                1641139200000L, // 2022-01-03 00:00:00
                1641225600000L  // 2022-01-04 00:00:00
        );
        assertThat(DataProcessor.getDailyTimestamps(timestamps)).isEqualTo(expectedTimestamps);
    }

    @Test
    public void getDailyTimestamps_LastDayOneHourData_returnExpectedList() {
        // Timezone GMT+8
        final List<Long> timestamps = List.of(
                1640988000000L, // 2022-01-01 06:00:00
                1641060000000L, // 2022-01-02 02:00:00
                1641160800000L, // 2022-01-03 06:00:00
                1641229200000L  // 2022-01-04 01:00:00
        );

        final List<Long> expectedTimestamps = List.of(
                1640988000000L, // 2022-01-01 06:00:00
                1641052800000L, // 2022-01-02 00:00:00
                1641139200000L, // 2022-01-03 00:00:00
                1641225600000L  // 2022-01-04 00:00:00
        );
        assertThat(DataProcessor.getDailyTimestamps(timestamps)).isEqualTo(expectedTimestamps);
    }

    @Test
    public void isFromFullCharge_emptyData_returnFalse() {
        assertThat(DataProcessor.isFromFullCharge(null)).isFalse();
        assertThat(DataProcessor.isFromFullCharge(new HashMap<>())).isFalse();
    }

    @Test
    public void isFromFullCharge_notChargedData_returnFalse() {
        final Map<String, BatteryHistEntry> entryMap = new HashMap<>();
        final ContentValues values = new ContentValues();
        values.put("batteryLevel", 98);
        final BatteryHistEntry entry = new BatteryHistEntry(values);
        entryMap.put(FAKE_ENTRY_KEY, entry);

        assertThat(DataProcessor.isFromFullCharge(entryMap)).isFalse();
    }

    @Test
    public void isFromFullCharge_chargedData_returnTrue() {
        final Map<String, BatteryHistEntry> entryMap = new HashMap<>();
        final ContentValues values = getContentValuesWithBatteryLevel(100);
        final BatteryHistEntry entry = new BatteryHistEntry(values);
        entryMap.put(FAKE_ENTRY_KEY, entry);

        assertThat(DataProcessor.isFromFullCharge(entryMap)).isTrue();
    }

    @Test
    public void findNearestTimestamp_returnExpectedResult() {
        long[] results = DataProcessor.findNearestTimestamp(
                Arrays.asList(10L, 20L, 30L, 40L), /*target=*/ 15L);
        assertThat(results).isEqualTo(new long[] {10L, 20L});

        results = DataProcessor.findNearestTimestamp(
                Arrays.asList(10L, 20L, 30L, 40L), /*target=*/ 10L);
        assertThat(results).isEqualTo(new long[] {10L, 10L});

        results = DataProcessor.findNearestTimestamp(
                Arrays.asList(10L, 20L, 30L, 40L), /*target=*/ 5L);
        assertThat(results).isEqualTo(new long[] {0L, 10L});

        results = DataProcessor.findNearestTimestamp(
                Arrays.asList(10L, 20L, 30L, 40L), /*target=*/ 50L);
        assertThat(results).isEqualTo(new long[] {40L, 0L});
    }

    @Test
    public void getTimestampOfNextDay_returnExpectedResult() {
        // 2021-02-28 06:00:00 => 2021-03-01 00:00:00
        assertThat(DataProcessor.getTimestampOfNextDay(1614463200000L))
                .isEqualTo(1614528000000L);
        // 2021-12-31 16:00:00 => 2022-01-01 00:00:00
        assertThat(DataProcessor.getTimestampOfNextDay(1640937600000L))
                .isEqualTo(1640966400000L);
    }

    @Test
    public void isForDailyChart_returnExpectedResult() {
        assertThat(DataProcessor.isForDailyChart(/*isStartOrEnd=*/ true, 0L)).isTrue();
        // 2022-01-01 00:00:00
        assertThat(DataProcessor.isForDailyChart(/*isStartOrEnd=*/ false, 1640966400000L))
                .isTrue();
        // 2022-01-01 01:00:05
        assertThat(DataProcessor.isForDailyChart(/*isStartOrEnd=*/ false, 1640970005000L))
                .isFalse();
    }

    @Test
    public void getBatteryUsageMap_emptyHistoryMap_returnNull() {
        final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay =
                new ArrayList<>();

        hourlyBatteryLevelsPerDay.add(
                new BatteryLevelData.PeriodBatteryLevelData(new ArrayList<>(), new ArrayList<>()));

        assertThat(DataProcessor.getBatteryUsageMap(
                mContext, hourlyBatteryLevelsPerDay, new HashMap<>(), /*appUsagePeriodMap=*/ null))
                .isNull();
    }

    @Test
    public void getBatteryUsageMap_returnsExpectedResult() {
        final long[] batteryHistoryKeys = new long[]{
                1641045600000L, // 2022-01-01 22:00:00
                1641049200000L, // 2022-01-01 23:00:00
                1641052800000L, // 2022-01-02 00:00:00
                1641056400000L, // 2022-01-02 01:00:00
                1641060000000L, // 2022-01-02 02:00:00
        };
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap = new HashMap<>();
        final int currentUserId = mContext.getUserId();
        final BatteryHistEntry fakeEntry = createBatteryHistEntry(
                ConvertUtils.FAKE_PACKAGE_NAME, "fake_label", /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 0L, currentUserId, ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                /*foregroundUsageTimeInMs=*/ 0L,  /*backgroundUsageTimeInMs=*/ 0L,
                /*isHidden=*/ false);
        // Adds the index = 0 data.
        Map<String, BatteryHistEntry> entryMap = new HashMap<>();
        BatteryHistEntry entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 5.0,
                /*foregroundUsageConsumePower=*/ 2, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 3, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 10L,
                /*backgroundUsageTimeInMs=*/ 20L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entryMap.put(fakeEntry.getKey(), fakeEntry);
        batteryHistoryMap.put(batteryHistoryKeys[0], entryMap);
        // Adds the index = 1 data.
        entryMap = new HashMap<>();
        entryMap.put(fakeEntry.getKey(), fakeEntry);
        batteryHistoryMap.put(batteryHistoryKeys[1], entryMap);
        // Adds the index = 2 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 20.0,
                /*foregroundUsageConsumePower=*/ 5, /*foregroundServiceUsageConsumePower=*/ 5,
                /*backgroundUsageConsumePower=*/ 5, /*cachedUsageConsumePower=*/ 5,
                /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 15L,
                /*backgroundUsageTimeInMs=*/ 25L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entryMap.put(fakeEntry.getKey(), fakeEntry);
        batteryHistoryMap.put(batteryHistoryKeys[2], entryMap);
        // Adds the index = 3 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 40.0,
                /*foregroundUsageConsumePower=*/ 8, /*foregroundServiceUsageConsumePower=*/ 8,
                /*backgroundUsageConsumePower=*/ 8, /*cachedUsageConsumePower=*/ 8,
                /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 25L,
                /*backgroundUsageTimeInMs=*/ 35L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package3", "label3", /*consumePower=*/ 10.0,
                /*foregroundUsageConsumePower=*/ 4, /*foregroundServiceUsageConsumePower=*/ 2,
                /*backgroundUsageConsumePower=*/ 2, /*cachedUsageConsumePower=*/ 2,
                /*uid=*/ 3L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY, /*foregroundUsageTimeInMs=*/ 40L,
                /*backgroundUsageTimeInMs=*/ 50L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package4", "label3", /*consumePower=*/ 15.0,
                /*foregroundUsageConsumePower=*/ 6, /*foregroundServiceUsageConsumePower=*/ 3,
                /*backgroundUsageConsumePower=*/ 3, /*cachedUsageConsumePower=*/ 3,
                /*uid=*/ 4L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 5L,
                /*backgroundUsageTimeInMs=*/ 5L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entryMap.put(fakeEntry.getKey(), fakeEntry);
        batteryHistoryMap.put(batteryHistoryKeys[3], entryMap);
        // Adds the index = 4 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 40.0,
                /*foregroundUsageConsumePower=*/ 14, /*foregroundServiceUsageConsumePower=*/ 9,
                /*backgroundUsageConsumePower=*/ 9, /*cachedUsageConsumePower=*/ 8,
                /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 30L,
                /*backgroundUsageTimeInMs=*/ 40L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package3", "label3", /*consumePower=*/ 20.0,
                /*foregroundUsageConsumePower=*/ 5, /*foregroundServiceUsageConsumePower=*/ 5,
                /*backgroundUsageConsumePower=*/ 5, /*cachedUsageConsumePower=*/ 5,
                /*uid=*/ 3L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY, /*foregroundUsageTimeInMs=*/ 50L,
                /*backgroundUsageTimeInMs=*/ 60L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package4", "label4", /*consumePower=*/ 40.0,
                /*foregroundUsageConsumePower=*/ 8, /*foregroundServiceUsageConsumePower=*/ 8,
                /*backgroundUsageConsumePower=*/ 8, /*cachedUsageConsumePower=*/ 8,
                /*uid=*/ 4L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 5L,
                /*backgroundUsageTimeInMs=*/ 5L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entryMap.put(fakeEntry.getKey(), fakeEntry);
        batteryHistoryMap.put(batteryHistoryKeys[4], entryMap);
        final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay =
                new ArrayList<>();
        // Adds the day 1 data.
        List<Long> timestamps =
                List.of(batteryHistoryKeys[0], batteryHistoryKeys[2]);
        final List<Integer> levels = List.of(100, 100);
        hourlyBatteryLevelsPerDay.add(
                new BatteryLevelData.PeriodBatteryLevelData(timestamps, levels));
        // Adds the day 2 data.
        timestamps = List.of(batteryHistoryKeys[2], batteryHistoryKeys[4]);
        hourlyBatteryLevelsPerDay.add(
                new BatteryLevelData.PeriodBatteryLevelData(timestamps, levels));

        // Adds app usage data to test screen on time.
        final Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>>
                appUsagePeriodMap = new HashMap<>();
        appUsagePeriodMap.put(0, new HashMap<>());
        appUsagePeriodMap.put(1, new HashMap<>());
        // Adds the index [0][0].
        Map<Long, Map<String, List<AppUsagePeriod>>> appUsageMap = new HashMap<>();
        Map<String, List<AppUsagePeriod>> userPeriodMap = new HashMap<>();
        appUsageMap.put(Long.valueOf(currentUserId), userPeriodMap);
        userPeriodMap.put("package2", List.of(buildAppUsagePeriod(0, 5)));
        userPeriodMap.put("package3", List.of(buildAppUsagePeriod(10, 25)));
        appUsagePeriodMap.get(0).put(0, appUsageMap);
        // Adds the index [1][0].
        appUsageMap = new HashMap<>();
        userPeriodMap = new HashMap<>();
        appUsageMap.put(Long.valueOf(currentUserId), userPeriodMap);
        userPeriodMap.put("package2",
                List.of(buildAppUsagePeriod(2, 7), buildAppUsagePeriod(5, 9)));
        userPeriodMap.put("package3",
                List.of(buildAppUsagePeriod(10, 15), buildAppUsagePeriod(25, 29)));
        appUsagePeriodMap.get(1).put(0, appUsageMap);

        final Map<Integer, Map<Integer, BatteryDiffData>> resultMap =
                DataProcessor.getBatteryUsageMap(
                        mContext, hourlyBatteryLevelsPerDay, batteryHistoryMap, appUsagePeriodMap);

        BatteryDiffData resultDiffData =
                resultMap
                        .get(DataProcessor.SELECTED_INDEX_ALL)
                        .get(DataProcessor.SELECTED_INDEX_ALL);
        assertBatteryDiffEntry(
                resultDiffData.getAppDiffEntryList().get(0), currentUserId, /*uid=*/ 2L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 50.0,
                /*foregroundUsageConsumePower=*/ 14, /*foregroundServiceUsageConsumePower=*/ 9,
                /*backgroundUsageConsumePower=*/ 9, /*cachedUsageConsumePower=*/ 8,
                /*foregroundUsageTimeInMs=*/ 30, /*backgroundUsageTimeInMs=*/ 40,
                /*screenOnTimeInMs=*/ 12);
        assertBatteryDiffEntry(
                resultDiffData.getAppDiffEntryList().get(1), currentUserId, /*uid=*/ 4L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 50.0,
                /*foregroundUsageConsumePower=*/ 8, /*foregroundServiceUsageConsumePower=*/ 8,
                /*backgroundUsageConsumePower=*/ 8, /*cachedUsageConsumePower=*/ 8,
                /*foregroundUsageTimeInMs=*/ 5, /*backgroundUsageTimeInMs=*/ 5,
                /*screenOnTimeInMs=*/ 0);
        assertBatteryDiffEntry(
                resultDiffData.getSystemDiffEntryList().get(0), currentUserId, /*uid=*/ 3L,
                ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY, /*consumePercentage=*/ 100.0,
                /*foregroundUsageConsumePower=*/ 5, /*foregroundServiceUsageConsumePower=*/ 5,
                /*backgroundUsageConsumePower=*/ 5, /*cachedUsageConsumePower=*/ 5,
                /*foregroundUsageTimeInMs=*/ 50, /*backgroundUsageTimeInMs=*/ 60,
                /*screenOnTimeInMs=*/ 9);
        resultDiffData = resultMap.get(0).get(DataProcessor.SELECTED_INDEX_ALL);
        assertBatteryDiffEntry(
                resultDiffData.getAppDiffEntryList().get(0), currentUserId, /*uid=*/ 2L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 100.0,
                /*foregroundUsageConsumePower=*/ 5, /*foregroundServiceUsageConsumePower=*/ 5,
                /*backgroundUsageConsumePower=*/ 5, /*cachedUsageConsumePower=*/ 5,
                /*foregroundUsageTimeInMs=*/ 15, /*backgroundUsageTimeInMs=*/ 25,
                /*screenOnTimeInMs=*/ 5);
        resultDiffData = resultMap.get(1).get(DataProcessor.SELECTED_INDEX_ALL);
        assertBatteryDiffEntry(
                resultDiffData.getAppDiffEntryList().get(0), currentUserId, /*uid=*/ 4L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 66.66666666666666,
                /*foregroundUsageConsumePower=*/ 8, /*foregroundServiceUsageConsumePower=*/ 8,
                /*backgroundUsageConsumePower=*/ 8, /*cachedUsageConsumePower=*/ 8,
                /*foregroundUsageTimeInMs=*/ 5, /*backgroundUsageTimeInMs=*/ 5,
                /*screenOnTimeInMs=*/ 0);
        assertBatteryDiffEntry(
                resultDiffData.getAppDiffEntryList().get(1), currentUserId, /*uid=*/ 2L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 33.33333333333333,
                /*foregroundUsageConsumePower=*/ 9, /*foregroundServiceUsageConsumePower=*/ 4,
                /*backgroundUsageConsumePower=*/ 4, /*cachedUsageConsumePower=*/ 3,
                /*foregroundUsageTimeInMs=*/ 15, /*backgroundUsageTimeInMs=*/ 15,
                /*screenOnTimeInMs=*/ 7);
        assertBatteryDiffEntry(
                resultDiffData.getSystemDiffEntryList().get(0), currentUserId, /*uid=*/ 3L,
                ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY, /*consumePercentage=*/ 100.0,
                /*foregroundUsageConsumePower=*/ 5, /*foregroundServiceUsageConsumePower=*/ 5,
                /*backgroundUsageConsumePower=*/ 5, /*cachedUsageConsumePower=*/ 5,
                /*foregroundUsageTimeInMs=*/ 50, /*backgroundUsageTimeInMs=*/ 60,
                /*screenOnTimeInMs=*/ 9);
    }

    @Test
    public void getBatteryUsageMap_multipleUsers_returnsExpectedResult() {
        final long[] batteryHistoryKeys = new long[]{
                1641052800000L, // 2022-01-02 00:00:00
                1641056400000L, // 2022-01-02 01:00:00
                1641060000000L  // 2022-01-02 02:00:00
        };
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap = new HashMap<>();
        final int currentUserId = mContext.getUserId();
        // Adds the index = 0 data.
        Map<String, BatteryHistEntry> entryMap = new HashMap<>();
        BatteryHistEntry entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 5.0,
                /*foregroundUsageConsumePower=*/ 5, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 10L,
                /*backgroundUsageTimeInMs=*/ 20L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 10.0,
                /*foregroundUsageConsumePower=*/ 7, /*foregroundServiceUsageConsumePower=*/ 1,
                /*backgroundUsageConsumePower=*/ 1, /*cachedUsageConsumePower=*/ 1,
                /*uid=*/ 2L, currentUserId + 1,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 10L,
                /*backgroundUsageTimeInMs=*/ 20L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 5.0,
                /*foregroundUsageConsumePower=*/ 5, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 3L, currentUserId + 2,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 20L,
                /*backgroundUsageTimeInMs=*/ 30L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[0], entryMap);
        // Adds the index = 1 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 15.0,
                /*foregroundUsageConsumePower=*/ 9, /*foregroundServiceUsageConsumePower=*/ 2,
                /*backgroundUsageConsumePower=*/ 2, /*cachedUsageConsumePower=*/ 2,
                /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 20L,
                /*backgroundUsageTimeInMs=*/ 30L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 30.0,
                /*foregroundUsageConsumePower=*/ 20, /*foregroundServiceUsageConsumePower=*/ 6,
                /*backgroundUsageConsumePower=*/ 2, /*cachedUsageConsumePower=*/ 2,
                /*uid=*/ 2L, currentUserId + 1,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 10L,
                /*backgroundUsageTimeInMs=*/ 20L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 15.0,
                /*foregroundUsageConsumePower=*/ 10, /*foregroundServiceUsageConsumePower=*/ 5,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 3L, currentUserId + 2,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 30L,
                /*backgroundUsageTimeInMs=*/ 30L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[1], entryMap);
        // Adds the index = 2 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 25.0,
                /*foregroundUsageConsumePower=*/ 10, /*foregroundServiceUsageConsumePower=*/ 5,
                /*backgroundUsageConsumePower=*/ 5, /*cachedUsageConsumePower=*/ 5,
                /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 20L,
                /*backgroundUsageTimeInMs=*/ 30L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 50.0,
                /*foregroundUsageConsumePower=*/ 20, /*foregroundServiceUsageConsumePower=*/ 10,
                /*backgroundUsageConsumePower=*/ 10, /*cachedUsageConsumePower=*/ 10,
                /*uid=*/ 2L, currentUserId + 1,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 20L,
                /*backgroundUsageTimeInMs=*/ 20L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 25.0,
                /*foregroundUsageConsumePower=*/ 10, /*foregroundServiceUsageConsumePower=*/ 10,
                /*backgroundUsageConsumePower=*/ 5, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 3L, currentUserId + 2,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 30L,
                /*backgroundUsageTimeInMs=*/ 30L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[2], entryMap);
        final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay =
                new ArrayList<>();
        List<Long> timestamps = List.of(batteryHistoryKeys[0], batteryHistoryKeys[2]);
        final List<Integer> levels = List.of(100, 100);
        hourlyBatteryLevelsPerDay.add(
                new BatteryLevelData.PeriodBatteryLevelData(timestamps, levels));

        final Map<Integer, Map<Integer, BatteryDiffData>> resultMap =
                DataProcessor.getBatteryUsageMap(
                        mContext, hourlyBatteryLevelsPerDay, batteryHistoryMap,
                        /*appUsagePeriodMap=*/ null);

        final BatteryDiffData resultDiffData =
                resultMap
                        .get(DataProcessor.SELECTED_INDEX_ALL)
                        .get(DataProcessor.SELECTED_INDEX_ALL);
        assertBatteryDiffEntry(
                resultDiffData.getAppDiffEntryList().get(0), currentUserId, /*uid=*/ 1L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 100.0,
                /*foregroundUsageConsumePower=*/ 5, /*foregroundServiceUsageConsumePower=*/ 5,
                /*backgroundUsageConsumePower=*/ 5, /*cachedUsageConsumePower=*/ 5,
                /*foregroundUsageTimeInMs=*/ 10, /*backgroundUsageTimeInMs=*/ 10,
                /*screenOnTimeInMs=*/ 0);
        assertThat(resultDiffData.getSystemDiffEntryList()).isEmpty();
        assertThat(resultMap.get(0).get(0)).isNotNull();
        assertThat(resultMap.get(0).get(DataProcessor.SELECTED_INDEX_ALL)).isNotNull();
    }

    @Test
    public void getBatteryUsageMap_usageTimeExceed_returnsExpectedResult() {
        final long[] batteryHistoryKeys = new long[]{
                1641052800000L, // 2022-01-02 00:00:00
                1641056400000L, // 2022-01-02 01:00:00
                1641060000000L  // 2022-01-02 02:00:00
        };
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap = new HashMap<>();
        final int currentUserId = mContext.getUserId();
        // Adds the index = 0 data.
        Map<String, BatteryHistEntry> entryMap = new HashMap<>();
        BatteryHistEntry entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[0], entryMap);
        // Adds the index = 1 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[1], entryMap);
        // Adds the index = 2 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 500.0,
                /*foregroundUsageConsumePower=*/ 200, /*foregroundServiceUsageConsumePower=*/ 100,
                /*backgroundUsageConsumePower=*/ 100, /*cachedUsageConsumePower=*/ 100,
                /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 3600000L,
                /*backgroundUsageTimeInMs=*/ 7200000L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[2], entryMap);
        final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay =
                new ArrayList<>();
        List<Long> timestamps = List.of(batteryHistoryKeys[0], batteryHistoryKeys[2]);
        final List<Integer> levels = List.of(100, 100);
        hourlyBatteryLevelsPerDay.add(
                new BatteryLevelData.PeriodBatteryLevelData(timestamps, levels));

        // Adds app usage data to test screen on time.
        final Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>>
                appUsagePeriodMap = new HashMap<>();
        appUsagePeriodMap.put(0, new HashMap<>());
        // Adds the index [0][0].
        final Map<Long, Map<String, List<AppUsagePeriod>>> appUsageMap = new HashMap<>();
        final Map<String, List<AppUsagePeriod>> userPeriodMap = new HashMap<>();
        appUsageMap.put(Long.valueOf(currentUserId), userPeriodMap);
        userPeriodMap.put("package1", List.of(buildAppUsagePeriod(0, 8000000)));
        appUsagePeriodMap.get(0).put(0, appUsageMap);

        final Map<Integer, Map<Integer, BatteryDiffData>> resultMap =
                DataProcessor.getBatteryUsageMap(
                        mContext, hourlyBatteryLevelsPerDay, batteryHistoryMap, appUsagePeriodMap);

        final BatteryDiffData resultDiffData =
                resultMap
                        .get(DataProcessor.SELECTED_INDEX_ALL)
                        .get(DataProcessor.SELECTED_INDEX_ALL);
        // Verifies the clipped usage time.
        final float ratio = (float) (7200) / (float) (3600 + 7200);
        final BatteryDiffEntry resultEntry = resultDiffData.getAppDiffEntryList().get(0);
        assertThat(resultEntry.mForegroundUsageTimeInMs)
                .isEqualTo(Math.round(entry.mForegroundUsageTimeInMs * ratio));
        assertThat(resultEntry.mBackgroundUsageTimeInMs)
                .isEqualTo(0);
        assertThat(resultEntry.mConsumePower)
                .isEqualTo(entry.mConsumePower * ratio);
        assertThat(resultEntry.mForegroundUsageConsumePower)
                .isEqualTo(entry.mForegroundUsageConsumePower * ratio);
        assertThat(resultEntry.mForegroundServiceUsageConsumePower)
                .isEqualTo(entry.mForegroundServiceUsageConsumePower * ratio);
        assertThat(resultEntry.mBackgroundUsageConsumePower)
                .isEqualTo(entry.mBackgroundUsageConsumePower * ratio);
        assertThat(resultEntry.mCachedUsageConsumePower)
                .isEqualTo(entry.mCachedUsageConsumePower * ratio);
        assertThat(resultEntry.mScreenOnTimeInMs).isEqualTo(7200000L);
        assertThat(resultMap.get(0).get(0)).isNotNull();
        assertThat(resultMap.get(0).get(DataProcessor.SELECTED_INDEX_ALL)).isNotNull();
    }

    @Test
    public void getBatteryUsageMap_hideApplicationEntries_returnsExpectedResult() {
        final long[] batteryHistoryKeys = new long[]{
                1641052800000L, // 2022-01-02 00:00:00
                1641056400000L, // 2022-01-02 01:00:00
                1641060000000L  // 2022-01-02 02:00:00
        };
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap = new HashMap<>();
        final int currentUserId = mContext.getUserId();
        // Adds the index = 0 data.
        Map<String, BatteryHistEntry> entryMap = new HashMap<>();
        BatteryHistEntry entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[0], entryMap);
        // Adds the index = 1 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[1], entryMap);
        // Adds the index = 2 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 10.0,
                /*foregroundUsageConsumePower=*/ 5, /*foregroundServiceUsageConsumePower=*/ 5,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 10L,
                /*backgroundUsageTimeInMs=*/ 20L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 10.0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 5, /*cachedUsageConsumePower=*/ 5,
                /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 10L,
                /*backgroundUsageTimeInMs=*/ 20L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[2], entryMap);
        final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay =
                new ArrayList<>();
        List<Long> timestamps = List.of(batteryHistoryKeys[0], batteryHistoryKeys[2]);
        final List<Integer> levels = List.of(100, 100);
        hourlyBatteryLevelsPerDay.add(
                new BatteryLevelData.PeriodBatteryLevelData(timestamps, levels));
        when(mPowerUsageFeatureProvider.getHideApplicationSet())
                .thenReturn(Set.of("package1"));

        final Map<Integer, Map<Integer, BatteryDiffData>> resultMap =
                DataProcessor.getBatteryUsageMap(
                        mContext, hourlyBatteryLevelsPerDay, batteryHistoryMap,
                        /*appUsagePeriodMap=*/ null);

        final BatteryDiffData resultDiffData =
                resultMap
                        .get(DataProcessor.SELECTED_INDEX_ALL)
                        .get(DataProcessor.SELECTED_INDEX_ALL);
        assertBatteryDiffEntry(
                resultDiffData.getAppDiffEntryList().get(0), currentUserId, /*uid=*/ 2L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 100.0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 5, /*cachedUsageConsumePower=*/ 5,
                /*foregroundUsageTimeInMs=*/ 10, /*backgroundUsageTimeInMs=*/ 20,
                /*screenOnTimeInMs=*/ 0);
    }

    @Test
    public void getBatteryUsageMap_hideBackgroundUsageTime_returnsExpectedResult() {
        final long[] batteryHistoryKeys = new long[]{
                1641052800000L, // 2022-01-02 00:00:00
                1641056400000L, // 2022-01-02 01:00:00
                1641060000000L  // 2022-01-02 02:00:00
        };
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap = new HashMap<>();
        final int currentUserId = mContext.getUserId();
        // Adds the index = 0 data.
        Map<String, BatteryHistEntry> entryMap = new HashMap<>();
        BatteryHistEntry entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[0], entryMap);
        // Adds the index = 1 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[1], entryMap);
        // Adds the index = 2 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 10.0,
                /*foregroundUsageConsumePower=*/ 5, /*foregroundServiceUsageConsumePower=*/ 5,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 10L,
                /*backgroundUsageTimeInMs=*/ 20L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 10.0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 5, /*cachedUsageConsumePower=*/ 5,
                /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 10L,
                /*backgroundUsageTimeInMs=*/ 20L, /*isHidden=*/ false);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[2], entryMap);
        final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay =
                new ArrayList<>();
        List<Long> timestamps = List.of(batteryHistoryKeys[0], batteryHistoryKeys[2]);
        final List<Integer> levels = List.of(100, 100);
        hourlyBatteryLevelsPerDay.add(
                new BatteryLevelData.PeriodBatteryLevelData(timestamps, levels));
        when(mPowerUsageFeatureProvider.getHideBackgroundUsageTimeSet())
                .thenReturn(new HashSet(Arrays.asList((CharSequence) "package2")));

        final Map<Integer, Map<Integer, BatteryDiffData>> resultMap =
                DataProcessor.getBatteryUsageMap(
                        mContext, hourlyBatteryLevelsPerDay, batteryHistoryMap,
                        /*appUsagePeriodMap=*/ null);

        final BatteryDiffData resultDiffData =
                resultMap
                        .get(DataProcessor.SELECTED_INDEX_ALL)
                        .get(DataProcessor.SELECTED_INDEX_ALL);
        BatteryDiffEntry resultEntry = resultDiffData.getAppDiffEntryList().get(0);
        assertThat(resultEntry.mBackgroundUsageTimeInMs).isEqualTo(20);
        resultEntry = resultDiffData.getAppDiffEntryList().get(1);
        assertThat(resultEntry.mBackgroundUsageTimeInMs).isEqualTo(0);
    }

    @Test
    public void generateBatteryDiffData_emptyBatteryEntryList_returnNull() {
        assertThat(DataProcessor.generateBatteryDiffData(mContext,
                DataProcessor.convertToBatteryHistEntry(null, mBatteryUsageStats))).isNull();
    }

    @Test
    public void generateBatteryDiffData_returnsExpectedResult() {
        final List<BatteryEntry> batteryEntryList = new ArrayList<>();
        batteryEntryList.add(mMockBatteryEntry1);
        batteryEntryList.add(mMockBatteryEntry2);
        batteryEntryList.add(mMockBatteryEntry3);
        batteryEntryList.add(mMockBatteryEntry4);
        doReturn(0.0).when(mMockBatteryEntry1).getConsumedPower();
        doReturn(0.0).when(mMockBatteryEntry1).getConsumedPowerInForeground();
        doReturn(0.0).when(mMockBatteryEntry1).getConsumedPowerInForegroundService();
        doReturn(0.0).when(mMockBatteryEntry1).getConsumedPowerInBackground();
        doReturn(0.0).when(mMockBatteryEntry1).getConsumedPowerInCached();
        doReturn(30L).when(mMockBatteryEntry1).getTimeInForegroundMs();
        doReturn(40L).when(mMockBatteryEntry1).getTimeInBackgroundMs();
        doReturn(1).when(mMockBatteryEntry1).getUid();
        doReturn(ConvertUtils.CONSUMER_TYPE_UID_BATTERY).when(mMockBatteryEntry1).getConsumerType();
        doReturn(0.5).when(mMockBatteryEntry2).getConsumedPower();
        doReturn(0.5).when(mMockBatteryEntry2).getConsumedPowerInForeground();
        doReturn(0.0).when(mMockBatteryEntry2).getConsumedPowerInForegroundService();
        doReturn(0.0).when(mMockBatteryEntry2).getConsumedPowerInBackground();
        doReturn(0.0).when(mMockBatteryEntry2).getConsumedPowerInCached();
        doReturn(20L).when(mMockBatteryEntry2).getTimeInForegroundMs();
        doReturn(20L).when(mMockBatteryEntry2).getTimeInBackgroundMs();
        doReturn(2).when(mMockBatteryEntry2).getUid();
        doReturn(ConvertUtils.CONSUMER_TYPE_UID_BATTERY).when(mMockBatteryEntry2).getConsumerType();
        doReturn(0.0).when(mMockBatteryEntry3).getConsumedPower();
        doReturn(0.0).when(mMockBatteryEntry3).getConsumedPowerInForeground();
        doReturn(0.0).when(mMockBatteryEntry3).getConsumedPowerInForegroundService();
        doReturn(0.0).when(mMockBatteryEntry3).getConsumedPowerInBackground();
        doReturn(0.0).when(mMockBatteryEntry3).getConsumedPowerInCached();
        doReturn(0L).when(mMockBatteryEntry3).getTimeInForegroundMs();
        doReturn(0L).when(mMockBatteryEntry3).getTimeInBackgroundMs();
        doReturn(3).when(mMockBatteryEntry3).getUid();
        doReturn(ConvertUtils.CONSUMER_TYPE_UID_BATTERY).when(mMockBatteryEntry3).getConsumerType();
        doReturn(1.5).when(mMockBatteryEntry4).getConsumedPower();
        doReturn(0.9).when(mMockBatteryEntry4).getConsumedPowerInForeground();
        doReturn(0.2).when(mMockBatteryEntry4).getConsumedPowerInForegroundService();
        doReturn(0.3).when(mMockBatteryEntry4).getConsumedPowerInBackground();
        doReturn(0.1).when(mMockBatteryEntry4).getConsumedPowerInCached();
        doReturn(10L).when(mMockBatteryEntry4).getTimeInForegroundMs();
        doReturn(10L).when(mMockBatteryEntry4).getTimeInBackgroundMs();
        doReturn(4).when(mMockBatteryEntry4).getUid();
        doReturn(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY)
                .when(mMockBatteryEntry4).getConsumerType();
        doReturn(BatteryConsumer.POWER_COMPONENT_CAMERA)
                .when(mMockBatteryEntry4).getPowerComponentId();

        final BatteryDiffData batteryDiffData = DataProcessor.generateBatteryDiffData(mContext,
                DataProcessor.convertToBatteryHistEntry(batteryEntryList, mBatteryUsageStats));

        assertBatteryDiffEntry(
                batteryDiffData.getAppDiffEntryList().get(0), 0, /*uid=*/ 2L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 100.0,
                /*foregroundUsageConsumePower=*/ 0.5, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*foregroundUsageTimeInMs=*/ 20, /*backgroundUsageTimeInMs=*/ 20,
                /*screenOnTimeInMs=*/ 0);
        assertBatteryDiffEntry(
                batteryDiffData.getAppDiffEntryList().get(1), 0, /*uid=*/ 1L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 0.0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*foregroundUsageTimeInMs=*/ 30, /*backgroundUsageTimeInMs=*/ 40,
                /*screenOnTimeInMs=*/ 0);
        assertBatteryDiffEntry(
                batteryDiffData.getSystemDiffEntryList().get(0), 0, /*uid=*/ 4L,
                ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY, /*consumePercentage=*/ 100.0,
                /*foregroundUsageConsumePower=*/ 0.9, /*foregroundServiceUsageConsumePower=*/ 0.2,
                /*backgroundUsageConsumePower=*/ 0.3, /*cachedUsageConsumePower=*/ 0.1,
                /*foregroundUsageTimeInMs=*/ 10, /*backgroundUsageTimeInMs=*/ 10,
                /*screenOnTimeInMs=*/ 0);
    }

    @Test
    public void buildAppUsagePeriodList_returnExpectedResult() {
        final List<AppUsageEvent> appUsageEvents = new ArrayList<>();
        final String packageName1 = "com.android.settings1";
        final String packageName2 = "com.android.settings2";
        // Fake multiple instances in one package.
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 1, /*userId=*/ 1,
                /*instanceId=*/ 2, packageName1));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 2, /*userId=*/ 1,
                /*instanceId=*/ 2, packageName1));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 3, /*userId=*/ 1,
                /*instanceId=*/ 2, packageName1));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 4, /*userId=*/ 1,
                /*instanceId=*/ 2, packageName1));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 2, /*userId=*/ 1,
                /*instanceId=*/ 3, packageName1));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 4, /*userId=*/ 1,
                /*instanceId=*/ 3, packageName1));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 2, /*userId=*/ 1,
                /*instanceId=*/ 5, packageName2));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 4, /*userId=*/ 1,
                /*instanceId=*/ 5, packageName2));
        // Fake one instance in one package.
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 1, /*userId=*/ 2,
                /*instanceId=*/ 4, packageName2));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 2, /*userId=*/ 2,
                /*instanceId=*/ 4, packageName2));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 3, /*userId=*/ 2,
                /*instanceId=*/ 4, packageName2));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 4, /*userId=*/ 2,
                /*instanceId=*/ 4, packageName2));

        final Map<Long, Map<String, List<AppUsagePeriod>>> appUsagePeriodMap =
                DataProcessor.buildAppUsagePeriodList(appUsageEvents, 0, 5);

        assertThat(appUsagePeriodMap).hasSize(2);
        final Map<String, List<AppUsagePeriod>> userMap1 = appUsagePeriodMap.get(1L);
        assertThat(userMap1).hasSize(2);
        List<AppUsagePeriod> appUsagePeriodList = userMap1.get(packageName1);
        assertThat(appUsagePeriodList).hasSize(3);
        assertAppUsagePeriod(appUsagePeriodList.get(0), 1, 2);
        assertAppUsagePeriod(appUsagePeriodList.get(1), 2, 4);
        assertAppUsagePeriod(appUsagePeriodList.get(2), 3, 4);
        appUsagePeriodList = userMap1.get(packageName2);
        assertThat(appUsagePeriodList).hasSize(1);
        assertAppUsagePeriod(appUsagePeriodList.get(0), 2, 4);
        final Map<String, List<AppUsagePeriod>> userMap2 = appUsagePeriodMap.get(2L);
        assertThat(userMap2).hasSize(1);
        appUsagePeriodList = userMap2.get(packageName2);
        assertThat(appUsagePeriodList).hasSize(2);
        assertAppUsagePeriod(appUsagePeriodList.get(0), 1, 2);
        assertAppUsagePeriod(appUsagePeriodList.get(1), 3, 4);
    }

    @Test
    public void buildAppUsagePeriodList_emptyEventList_returnNull() {
        assertThat(DataProcessor.buildAppUsagePeriodList(
                new ArrayList<>(), 0, 1)).isNull();
    }

    @Test
    public void buildAppUsagePeriodList_emptyActivityList_returnNull() {
        final List<AppUsageEvent> appUsageEvents = new ArrayList<>();
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.DEVICE_SHUTDOWN, /*timestamp=*/ 1));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.DEVICE_SHUTDOWN, /*timestamp=*/ 2));

        assertThat(DataProcessor.buildAppUsagePeriodList(
                appUsageEvents, 0, 3)).isNull();
    }

    @Test
    public void buildAppUsagePeriodListPerInstance_returnExpectedResult() {
        final List<AppUsageEvent> appUsageEvents = new ArrayList<>();
        // Fake data earlier than time range.
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 1));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 2));
        // Fake resume event earlier than time range.
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 3));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 120000));
        // Fake normal data.
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 150000));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 200000));
        // Fake two adjacent resume events.
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 300000));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 400000));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 500000));
        // Fake no start event when stop event happens.
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 600000));
        // There exists start event when device shutdown event happens. Shutdown is later than
        // default complete time.
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 700000));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.DEVICE_SHUTDOWN, /*timestamp=*/ 800000));
        // There exists start event when device shutdown event happens. Shutdown is earlier than
        // default complete time.
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 900000));
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.DEVICE_SHUTDOWN, /*timestamp=*/ 910000));
        // There exists start event when the period ends.
        appUsageEvents.add(buildAppUsageEvent(
                AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 1000000));

        final List<AppUsagePeriod> appUsagePeriodList =
                DataProcessor.buildAppUsagePeriodListPerInstance(appUsageEvents, 100000, 1100000);

        assertThat(appUsagePeriodList).hasSize(7);
        assertAppUsagePeriod(appUsagePeriodList.get(0), 100000, 120000);
        assertAppUsagePeriod(appUsagePeriodList.get(1), 150000, 200000);
        assertAppUsagePeriod(appUsagePeriodList.get(2), 300000, 500000);
        assertAppUsagePeriod(appUsagePeriodList.get(3), 570000, 600000);
        assertAppUsagePeriod(appUsagePeriodList.get(4), 700000, 730000);
        assertAppUsagePeriod(appUsagePeriodList.get(5), 900000, 910000);
        assertAppUsagePeriod(appUsagePeriodList.get(6), 1000000, 1100000);
    }

    @Test
    public void getScreenOnTime_returnExpectedResult() {
        final long userId = 1;
        final String packageName = "com.android.settings";
        final Map<Long, Map<String, List<AppUsagePeriod>>> appUsageMap = new HashMap<>();
        final List<AppUsagePeriod> appUsagePeriodList = new ArrayList<>();
        appUsageMap.put(userId, new HashMap<>());
        appUsageMap.get(userId).put(packageName, appUsagePeriodList);
        // Fake overlapped case.
        appUsagePeriodList.add(buildAppUsagePeriod(0, 5));
        appUsagePeriodList.add(buildAppUsagePeriod(2, 3));
        appUsagePeriodList.add(buildAppUsagePeriod(2, 4));
        appUsagePeriodList.add(buildAppUsagePeriod(5, 7));
        // Fake same case.
        appUsagePeriodList.add(buildAppUsagePeriod(10, 12));
        appUsagePeriodList.add(buildAppUsagePeriod(10, 12));
        appUsagePeriodList.add(buildAppUsagePeriod(10, 12));
        // Fake normal case.
        appUsagePeriodList.add(buildAppUsagePeriod(15, 20));
        appUsagePeriodList.add(buildAppUsagePeriod(35, 40));
        appUsagePeriodList.add(buildAppUsagePeriod(25, 30));

        assertThat(DataProcessor.getScreenOnTime(appUsageMap, userId, packageName)).isEqualTo(24);
    }

    @Test
    public void getScreenOnTime_nullInput_returnZero() {
        final long userId = 1;
        final String packageName = "com.android.settings";
        final Map<Long, Map<String, List<AppUsagePeriod>>> appUsageMap = new HashMap<>();
        appUsageMap.put(userId, new HashMap<>());

        assertThat(DataProcessor.getScreenOnTime(null, userId, packageName)).isEqualTo(0);
        assertThat(DataProcessor.getScreenOnTime(new HashMap<>(), userId, packageName))
                .isEqualTo(0);
        assertThat(DataProcessor.getScreenOnTime(appUsageMap, userId, packageName)).isEqualTo(0);
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
                DeviceBatteryState
                        .newBuilder()
                        .setBatteryLevel(level)
                        .build();
        final BatteryInformation batteryInformation =
                BatteryInformation
                        .newBuilder()
                        .setDeviceBatteryState(deviceBatteryState)
                        .build();
        values.put(BatteryHistEntry.KEY_BATTERY_INFORMATION,
                ConvertUtils.convertBatteryInformationToString(batteryInformation));
        return values;
    }

    private static BatteryHistEntry createBatteryHistEntry(
            final String packageName, final String appLabel, final double consumePower,
            final double foregroundUsageConsumePower,
            final double foregroundServiceUsageConsumePower,
            final double backgroundUsageConsumePower, final double cachedUsageConsumePower,
            final long uid, final long userId, final int consumerType,
            final long foregroundUsageTimeInMs, final long backgroundUsageTimeInMs,
            final boolean isHidden) {
        // Only insert required fields.
        final BatteryInformation batteryInformation =
                BatteryInformation
                        .newBuilder()
                        .setAppLabel(appLabel)
                        .setConsumePower(consumePower)
                        .setForegroundUsageConsumePower(foregroundUsageConsumePower)
                        .setForegroundServiceUsageConsumePower(foregroundServiceUsageConsumePower)
                        .setBackgroundUsageConsumePower(backgroundUsageConsumePower)
                        .setCachedUsageConsumePower(cachedUsageConsumePower)
                        .setForegroundUsageTimeInMs(foregroundUsageTimeInMs)
                        .setBackgroundUsageTimeInMs(backgroundUsageTimeInMs)
                        .setIsHidden(isHidden)
                        .build();
        final ContentValues values = new ContentValues();
        values.put(BatteryHistEntry.KEY_PACKAGE_NAME, packageName);
        values.put(BatteryHistEntry.KEY_UID, uid);
        values.put(BatteryHistEntry.KEY_USER_ID, userId);
        values.put(BatteryHistEntry.KEY_CONSUMER_TYPE, consumerType);
        values.put(BatteryHistEntry.KEY_BATTERY_INFORMATION,
                ConvertUtils.convertBatteryInformationToString(batteryInformation));
        return new BatteryHistEntry(values);
    }

    private UsageEvents getUsageEvents(final List<Event> events) {
        UsageEvents usageEvents = new UsageEvents(events, new String[] {"package"});
        Parcel parcel = Parcel.obtain();
        parcel.setDataPosition(0);
        usageEvents.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return UsageEvents.CREATOR.createFromParcel(parcel);
    }

    private Event getUsageEvent(
            final int eventType, final long timestamp) {
        final Event event = new Event();
        event.mEventType = eventType;
        event.mPackage = "package";
        event.mTimeStamp = timestamp;
        return event;
    }

    private AppUsageEvent buildAppUsageEvent(final AppUsageEventType type, final long timestamp) {
        return buildAppUsageEvent(
                type, timestamp, /*userId=*/ 1,  /*instanceId=*/ 2,
                "com.android.settings");
    }

    private AppUsageEvent buildAppUsageEvent(
            final AppUsageEventType type,
            final long timestamp,
            final long userId,
            final int instanceId,
            final String packageName) {
        return AppUsageEvent.newBuilder()
                .setType(type)
                .setTimestamp(timestamp)
                .setUserId(userId)
                .setPackageName(packageName)
                .setInstanceId(instanceId)
                .build();
    }

    private AppUsagePeriod buildAppUsagePeriod(final long start, final long end) {
        return AppUsagePeriod.newBuilder()
                .setStartTime(start)
                .setEndTime(end)
                .build();
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

    private static void verifyExpectedTimestampSlots(
            final Calendar start,
            final Calendar current,
            final Calendar expectedStart,
            final Calendar expectedEnd) {
        expectedStart.set(Calendar.MILLISECOND, 0);
        expectedEnd.set(Calendar.MILLISECOND, 0);
        final ArrayList<Long> timestampSlots = new ArrayList<>();
        timestampSlots.add(start.getTimeInMillis());
        final List<Long> resultList =
                DataProcessor.getTimestampSlots(timestampSlots, current.getTimeInMillis());

        for (int index = 0; index < resultList.size(); index++) {
            final long expectedTimestamp =
                    expectedStart.getTimeInMillis() + index * DateUtils.HOUR_IN_MILLIS;
            assertThat(resultList.get(index)).isEqualTo(expectedTimestamp);
        }
        assertThat(resultList.get(resultList.size() - 1))
                .isEqualTo(expectedEnd.getTimeInMillis());
    }

    private static void assertBatteryDiffEntry(
            final BatteryDiffEntry entry, final long userId, final long uid,
            final int consumerType, final double consumePercentage,
            final double foregroundUsageConsumePower,
            final double foregroundServiceUsageConsumePower,
            final double backgroundUsageConsumePower, final double cachedUsageConsumePower,
            final long foregroundUsageTimeInMs, final long backgroundUsageTimeInMs,
            final long screenOnTimeInMs) {
        assertThat(entry.mBatteryHistEntry.mUserId).isEqualTo(userId);
        assertThat(entry.mBatteryHistEntry.mUid).isEqualTo(uid);
        assertThat(entry.mBatteryHistEntry.mConsumerType).isEqualTo(consumerType);
        assertThat(entry.getPercentOfTotal()).isEqualTo(consumePercentage);
        assertThat(entry.mForegroundUsageConsumePower).isEqualTo(foregroundUsageConsumePower);
        assertThat(entry.mForegroundServiceUsageConsumePower)
                .isEqualTo(foregroundServiceUsageConsumePower);
        assertThat(entry.mBackgroundUsageConsumePower).isEqualTo(backgroundUsageConsumePower);
        assertThat(entry.mCachedUsageConsumePower).isEqualTo(cachedUsageConsumePower);
        assertThat(entry.mForegroundUsageTimeInMs).isEqualTo(foregroundUsageTimeInMs);
        assertThat(entry.mBackgroundUsageTimeInMs).isEqualTo(backgroundUsageTimeInMs);
        assertThat(entry.mScreenOnTimeInMs).isEqualTo(screenOnTimeInMs);
    }
}
