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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.ContentValues;
import android.content.Context;
import android.os.BatteryConsumer;
import android.os.BatteryUsageStats;
import android.text.format.DateUtils;

import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

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
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public class DataProcessorTest {
    private static final String FAKE_ENTRY_KEY = "fake_entry_key";

    private Context mContext;

    private FakeFeatureFactory mFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    @Mock private BatteryUsageStats mBatteryUsageStats;
    @Mock private BatteryEntry mMockBatteryEntry1;
    @Mock private BatteryEntry mMockBatteryEntry2;
    @Mock private BatteryEntry mMockBatteryEntry3;
    @Mock private BatteryEntry mMockBatteryEntry4;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));

        mContext = spy(RuntimeEnvironment.application);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFeatureFactory.metricsFeatureProvider;
        mPowerUsageFeatureProvider = mFeatureFactory.powerUsageFeatureProvider;
    }

    @Test
    public void getBatteryLevelData_emptyHistoryMap_returnNull() {
        assertThat(DataProcessor.getBatteryLevelData(
                mContext,
                /*handler=*/ null,
                /*batteryHistoryMap=*/ null,
                /*asyncResponseDelegate=*/ null))
                .isNull();
        assertThat(DataProcessor.getBatteryLevelData(
                mContext, /*handler=*/ null, new HashMap<>(), /*asyncResponseDelegate=*/ null))
                .isNull();
        verify(mMetricsFeatureProvider, never())
                .action(mContext, SettingsEnums.ACTION_BATTERY_USAGE_SHOWN_APP_COUNT);
        verify(mMetricsFeatureProvider, never())
                .action(mContext, SettingsEnums.ACTION_BATTERY_USAGE_HIDDEN_APP_COUNT);
    }

    @Test
    public void getBatteryLevelData_notEnoughData_returnNull() {
        // The timestamps are within 1 hour.
        final long[] timestamps = {1000000L, 2000000L, 3000000L};
        final int[] levels = {100, 99, 98};
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap =
                createHistoryMap(timestamps, levels);

        assertThat(DataProcessor.getBatteryLevelData(
                mContext, /*handler=*/ null, batteryHistoryMap, /*asyncResponseDelegate=*/ null))
                .isNull();
        verify(mMetricsFeatureProvider, never())
                .action(mContext, SettingsEnums.ACTION_BATTERY_USAGE_SHOWN_APP_COUNT);
        verify(mMetricsFeatureProvider, never())
                .action(mContext, SettingsEnums.ACTION_BATTERY_USAGE_HIDDEN_APP_COUNT);
    }

    @Test
    public void getBatteryLevelData_returnExpectedResult() {
        // Timezone GMT+8: 2022-01-01 00:00:00, 2022-01-01 01:00:00, 2022-01-01 02:00:00
        final long[] timestamps = {1640966400000L, 1640970000000L, 1640973600000L};
        final int[] levels = {100, 99, 98};
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap =
                createHistoryMap(timestamps, levels);

        final BatteryLevelData resultData =
                DataProcessor.getBatteryLevelData(
                        mContext,
                        /*handler=*/ null,
                        batteryHistoryMap,
                        /*asyncResponseDelegate=*/ null);

        final List<Long> expectedDailyTimestamps = List.of(timestamps[0], timestamps[2]);
        final List<Integer> expectedDailyLevels = List.of(levels[0], levels[2]);
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

        final Map<Long, Map<String, BatteryHistEntry>> resultMap =
                DataProcessor.getHistoryMapWithExpectedTimestamps(mContext, batteryHistoryMap);

        // Timezone GMT+8
        final long[] expectedTimestamps = {
                1640966400000L, // 2022-01-01 00:00:00
                1640970000000L, // 2022-01-01 01:00:00
                1640973600000L, // 2022-01-01 02:00:00
                1640977200000L, // 2022-01-01 03:00:00
                1640980800000L  // 2022-01-01 04:00:00
        };
        final int[] expectedLevels = {100, 94, 90, 84, 56};
        assertThat(resultMap).hasSize(expectedLevels.length);
        for (int index = 0; index < expectedLevels.length; index++) {
            assertThat(resultMap.get(expectedTimestamps[index]).get(FAKE_ENTRY_KEY).mBatteryLevel)
                    .isEqualTo(expectedLevels[index]);
        }
    }

    @Test
    public void getLevelDataThroughProcessedHistoryMap_notEnoughData_returnNull() {
        final long[] timestamps = {100L};
        final int[] levels = {100};
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap =
                createHistoryMap(timestamps, levels);

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
    public void getTimestampSlots_emptyRawList_returnEmptyList() {
        final List<Long> resultList =
                DataProcessor.getTimestampSlots(new ArrayList<>());
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
        expectedEndCalendar.set(2022, 6, 5, 22, 0, 0); // 2022-07-05 22:00:00
        verifyExpectedTimestampSlots(
                startCalendar, endCalendar, expectedStartCalendar, expectedEndCalendar);
    }

    @Test
    public void getTimestampSlots_startWithOddHour_returnExpectedResult() {
        final Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(2022, 6, 5, 5, 0, 50); // 2022-07-05 05:00:50
        final Calendar endCalendar = Calendar.getInstance();
        endCalendar.set(2022, 6, 6, 21, 00, 50); // 2022-07-06 21:00:50

        final Calendar expectedStartCalendar = Calendar.getInstance();
        expectedStartCalendar.set(2022, 6, 5, 6, 00, 00); // 2022-07-05 06:00:00
        final Calendar expectedEndCalendar = Calendar.getInstance();
        expectedEndCalendar.set(2022, 6, 6, 20, 00, 00); // 2022-07-06 20:00:00
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
        final ContentValues values = new ContentValues();
        values.put("batteryLevel", 100);
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
                mContext, hourlyBatteryLevelsPerDay, new HashMap<>())).isNull();
        verify(mMetricsFeatureProvider, never())
                .action(mContext, SettingsEnums.ACTION_BATTERY_USAGE_SHOWN_APP_COUNT);
        verify(mMetricsFeatureProvider, never())
                .action(mContext, SettingsEnums.ACTION_BATTERY_USAGE_HIDDEN_APP_COUNT);
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
                ConvertUtils.FAKE_PACKAGE_NAME, "fake_label", /*consumePower=*/ 0, /*uid=*/ 0L,
                currentUserId, ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                /*foregroundUsageTimeInMs=*/ 0L, /*backgroundUsageTimeInMs=*/ 0L);
        // Adds the index = 0 data.
        Map<String, BatteryHistEntry> entryMap = new HashMap<>();
        BatteryHistEntry entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 5.0, /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 10L,
                /*backgroundUsageTimeInMs=*/ 20L);
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
                "package2", "label2", /*consumePower=*/ 20.0, /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 15L,
                25L);
        entryMap.put(entry.getKey(), entry);
        entryMap.put(fakeEntry.getKey(), fakeEntry);
        batteryHistoryMap.put(batteryHistoryKeys[2], entryMap);
        // Adds the index = 3 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 40.0, /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 25L,
                /*backgroundUsageTimeInMs=*/ 35L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 10.0, /*uid=*/ 3L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY, /*foregroundUsageTimeInMs=*/ 40L,
                /*backgroundUsageTimeInMs=*/ 50L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package3", "label3", /*consumePower=*/ 15.0, /*uid=*/ 4L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 5L,
                /*backgroundUsageTimeInMs=*/ 5L);
        entryMap.put(entry.getKey(), entry);
        entryMap.put(fakeEntry.getKey(), fakeEntry);
        batteryHistoryMap.put(batteryHistoryKeys[3], entryMap);
        // Adds the index = 4 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 40.0, /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 30L,
                /*backgroundUsageTimeInMs=*/ 40L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 20.0, /*uid=*/ 3L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY, /*foregroundUsageTimeInMs=*/ 50L,
                /*backgroundUsageTimeInMs=*/ 60L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package3", "label3", /*consumePower=*/ 40.0, /*uid=*/ 4L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 5L,
                /*backgroundUsageTimeInMs=*/ 5L);
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

        final Map<Integer, Map<Integer, BatteryDiffData>> resultMap =
                DataProcessor.getBatteryUsageMap(
                        mContext, hourlyBatteryLevelsPerDay, batteryHistoryMap);

        BatteryDiffData resultDiffData =
                resultMap
                        .get(DataProcessor.SELECTED_INDEX_ALL)
                        .get(DataProcessor.SELECTED_INDEX_ALL);
        assertBatteryDiffEntry(
                resultDiffData.getAppDiffEntryList().get(0), currentUserId, /*uid=*/ 2L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 40.0,
                /*foregroundUsageTimeInMs=*/ 30, /*backgroundUsageTimeInMs=*/ 40);
        assertBatteryDiffEntry(
                resultDiffData.getAppDiffEntryList().get(1), currentUserId, /*uid=*/ 4L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 40.0,
                /*foregroundUsageTimeInMs=*/ 5, /*backgroundUsageTimeInMs=*/ 5);
        assertBatteryDiffEntry(
                resultDiffData.getSystemDiffEntryList().get(0), currentUserId, /*uid=*/ 3L,
                ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY, /*consumePercentage=*/ 20.0,
                /*foregroundUsageTimeInMs=*/ 50, /*backgroundUsageTimeInMs=*/ 60);
        resultDiffData = resultMap.get(0).get(DataProcessor.SELECTED_INDEX_ALL);
        assertBatteryDiffEntry(
                resultDiffData.getAppDiffEntryList().get(0), currentUserId, /*uid=*/ 2L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 100.0,
                /*foregroundUsageTimeInMs=*/ 15, /*backgroundUsageTimeInMs=*/ 25);
        resultDiffData = resultMap.get(1).get(DataProcessor.SELECTED_INDEX_ALL);
        assertBatteryDiffEntry(
                resultDiffData.getAppDiffEntryList().get(0), currentUserId, /*uid=*/ 4L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 50.0,
                /*foregroundUsageTimeInMs=*/ 5, /*backgroundUsageTimeInMs=*/ 5);
        assertBatteryDiffEntry(
                resultDiffData.getAppDiffEntryList().get(1), currentUserId, /*uid=*/ 2L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 25.0,
                /*foregroundUsageTimeInMs=*/ 15, /*backgroundUsageTimeInMs=*/ 15);
        assertBatteryDiffEntry(
                resultDiffData.getSystemDiffEntryList().get(0), currentUserId, /*uid=*/ 3L,
                ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY, /*consumePercentage=*/ 25.0,
                /*foregroundUsageTimeInMs=*/ 50, /*backgroundUsageTimeInMs=*/ 60);
        verify(mMetricsFeatureProvider)
                .action(mContext.getApplicationContext(),
                        SettingsEnums.ACTION_BATTERY_USAGE_SHOWN_APP_COUNT,
                        3);
        verify(mMetricsFeatureProvider)
                .action(mContext.getApplicationContext(),
                        SettingsEnums.ACTION_BATTERY_USAGE_HIDDEN_APP_COUNT,
                        0);
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
                "package1", "label1", /*consumePower=*/ 5.0, /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 10L,
                /*backgroundUsageTimeInMs=*/ 20L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 10.0, /*uid=*/ 2L, currentUserId + 1,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 10L,
                /*backgroundUsageTimeInMs=*/ 20L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 5.0, /*uid=*/ 3L, currentUserId + 2,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 20L,
                /*backgroundUsageTimeInMs=*/ 30L);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[0], entryMap);
        // Adds the index = 1 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 15.0, /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 20L,
                /*backgroundUsageTimeInMs=*/ 30L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 30.0, /*uid=*/ 2L, currentUserId + 1,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 10L,
                /*backgroundUsageTimeInMs=*/ 20L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 15.0, /*uid=*/ 3L, currentUserId + 2,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 30L,
                /*backgroundUsageTimeInMs=*/ 30L);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[1], entryMap);
        // Adds the index = 2 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 25.0, /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 20L,
                /*backgroundUsageTimeInMs=*/ 30L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 50.0, /*uid=*/ 2L, currentUserId + 1,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 20L,
                /*backgroundUsageTimeInMs=*/ 20L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 25.0, /*uid=*/ 3L, currentUserId + 2,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 30L,
                /*backgroundUsageTimeInMs=*/ 30L);
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
                        mContext, hourlyBatteryLevelsPerDay, batteryHistoryMap);

        final BatteryDiffData resultDiffData =
                resultMap
                        .get(DataProcessor.SELECTED_INDEX_ALL)
                        .get(DataProcessor.SELECTED_INDEX_ALL);
        assertBatteryDiffEntry(
                resultDiffData.getAppDiffEntryList().get(0), currentUserId, /*uid=*/ 1L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 25.0,
                /*foregroundUsageTimeInMs=*/ 10, /*backgroundUsageTimeInMs=*/ 10);
        assertBatteryDiffEntry(
                resultDiffData.getSystemDiffEntryList().get(0), BatteryUtils.UID_OTHER_USERS,
                /*uid=*/ BatteryUtils.UID_OTHER_USERS, ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                /*consumePercentage=*/ 75.0, /*foregroundUsageTimeInMs=*/ 0,
                /*backgroundUsageTimeInMs=*/ 0);
        assertThat(resultMap.get(0).get(0)).isNotNull();
        assertThat(resultMap.get(0).get(DataProcessor.SELECTED_INDEX_ALL)).isNotNull();
        verify(mMetricsFeatureProvider)
                .action(mContext.getApplicationContext(),
                        SettingsEnums.ACTION_BATTERY_USAGE_SHOWN_APP_COUNT,
                        2);
        verify(mMetricsFeatureProvider)
                .action(mContext.getApplicationContext(),
                        SettingsEnums.ACTION_BATTERY_USAGE_HIDDEN_APP_COUNT,
                        0);
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
                "package1", "label1", /*consumePower=*/ 0, /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[0], entryMap);
        // Adds the index = 1 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 0, /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[1], entryMap);
        // Adds the index = 2 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 500.0, /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 3600000L,
                /*backgroundUsageTimeInMs=*/ 7200000L);
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
                        mContext, hourlyBatteryLevelsPerDay, batteryHistoryMap);

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
                .isEqualTo(Math.round(entry.mBackgroundUsageTimeInMs * ratio));
        assertThat(resultEntry.mConsumePower)
                .isEqualTo(entry.mConsumePower * ratio);
        assertThat(resultMap.get(0).get(0)).isNotNull();
        assertThat(resultMap.get(0).get(DataProcessor.SELECTED_INDEX_ALL)).isNotNull();
        verify(mMetricsFeatureProvider)
                .action(mContext.getApplicationContext(),
                        SettingsEnums.ACTION_BATTERY_USAGE_SHOWN_APP_COUNT,
                        1);
        verify(mMetricsFeatureProvider)
                .action(mContext.getApplicationContext(),
                        SettingsEnums.ACTION_BATTERY_USAGE_HIDDEN_APP_COUNT,
                        0);
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
                "package1", "label1", /*consumePower=*/ 0, /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 0, /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[0], entryMap);
        // Adds the index = 1 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 0, /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 0, /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[1], entryMap);
        // Adds the index = 2 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 10.0, /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 10L,
                /*backgroundUsageTimeInMs=*/ 20L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 10.0, /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 10L,
                /*backgroundUsageTimeInMs=*/ 20L);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[2], entryMap);
        final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay =
                new ArrayList<>();
        List<Long> timestamps = List.of(batteryHistoryKeys[0], batteryHistoryKeys[2]);
        final List<Integer> levels = List.of(100, 100);
        hourlyBatteryLevelsPerDay.add(
                new BatteryLevelData.PeriodBatteryLevelData(timestamps, levels));
        when(mPowerUsageFeatureProvider.getHideApplicationEntries(mContext))
                .thenReturn(new CharSequence[]{"package1"});

        final Map<Integer, Map<Integer, BatteryDiffData>> resultMap =
                DataProcessor.getBatteryUsageMap(
                        mContext, hourlyBatteryLevelsPerDay, batteryHistoryMap);

        final BatteryDiffData resultDiffData =
                resultMap
                        .get(DataProcessor.SELECTED_INDEX_ALL)
                        .get(DataProcessor.SELECTED_INDEX_ALL);
        assertBatteryDiffEntry(
                resultDiffData.getAppDiffEntryList().get(0), currentUserId, /*uid=*/ 2L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 50.0,
                /*foregroundUsageTimeInMs=*/ 10, /*backgroundUsageTimeInMs=*/ 20);
        verify(mMetricsFeatureProvider)
                .action(mContext.getApplicationContext(),
                        SettingsEnums.ACTION_BATTERY_USAGE_SHOWN_APP_COUNT,
                        1);
        verify(mMetricsFeatureProvider)
                .action(mContext.getApplicationContext(),
                        SettingsEnums.ACTION_BATTERY_USAGE_HIDDEN_APP_COUNT,
                        1);
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
                "package1", "label1", /*consumePower=*/ 0, /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 0, /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[0], entryMap);
        // Adds the index = 1 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 0, /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 0, /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 0L,
                /*backgroundUsageTimeInMs=*/ 0L);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[1], entryMap);
        // Adds the index = 2 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
                "package1", "label1", /*consumePower=*/ 10.0, /*uid=*/ 1L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 10L,
                /*backgroundUsageTimeInMs=*/ 20L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
                "package2", "label2", /*consumePower=*/ 10.0, /*uid=*/ 2L, currentUserId,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*foregroundUsageTimeInMs=*/ 10L,
                /*backgroundUsageTimeInMs=*/ 20L);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(batteryHistoryKeys[2], entryMap);
        final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay =
                new ArrayList<>();
        List<Long> timestamps = List.of(batteryHistoryKeys[0], batteryHistoryKeys[2]);
        final List<Integer> levels = List.of(100, 100);
        hourlyBatteryLevelsPerDay.add(
                new BatteryLevelData.PeriodBatteryLevelData(timestamps, levels));
        when(mPowerUsageFeatureProvider.getHideBackgroundUsageTimeSet(mContext))
                .thenReturn(new HashSet(Arrays.asList((CharSequence) "package2")));

        final Map<Integer, Map<Integer, BatteryDiffData>> resultMap =
                DataProcessor.getBatteryUsageMap(
                        mContext, hourlyBatteryLevelsPerDay, batteryHistoryMap);

        final BatteryDiffData resultDiffData =
                resultMap
                        .get(DataProcessor.SELECTED_INDEX_ALL)
                        .get(DataProcessor.SELECTED_INDEX_ALL);
        BatteryDiffEntry resultEntry = resultDiffData.getAppDiffEntryList().get(0);
        assertThat(resultEntry.mBackgroundUsageTimeInMs).isEqualTo(20);
        resultEntry = resultDiffData.getAppDiffEntryList().get(1);
        assertThat(resultEntry.mBackgroundUsageTimeInMs).isEqualTo(0);
        verify(mMetricsFeatureProvider)
                .action(mContext.getApplicationContext(),
                        SettingsEnums.ACTION_BATTERY_USAGE_SHOWN_APP_COUNT,
                        2);
        verify(mMetricsFeatureProvider)
                .action(mContext.getApplicationContext(),
                        SettingsEnums.ACTION_BATTERY_USAGE_HIDDEN_APP_COUNT,
                        0);
    }

    @Test
    public void generateBatteryDiffData_emptyBatteryEntryList_returnNull() {
        assertThat(DataProcessor.generateBatteryDiffData(
                mContext, null, mBatteryUsageStats)).isNull();
    }

    @Test
    public void generateBatteryDiffData_returnsExpectedResult() {
        final List<BatteryEntry> batteryEntryList = new ArrayList<>();
        batteryEntryList.add(mMockBatteryEntry1);
        batteryEntryList.add(mMockBatteryEntry2);
        batteryEntryList.add(mMockBatteryEntry3);
        batteryEntryList.add(mMockBatteryEntry4);
        doReturn(0.0).when(mMockBatteryEntry1).getConsumedPower();
        doReturn(30L).when(mMockBatteryEntry1).getTimeInForegroundMs();
        doReturn(40L).when(mMockBatteryEntry1).getTimeInBackgroundMs();
        doReturn(1).when(mMockBatteryEntry1).getUid();
        doReturn(ConvertUtils.CONSUMER_TYPE_UID_BATTERY).when(mMockBatteryEntry1).getConsumerType();
        doReturn(0.5).when(mMockBatteryEntry2).getConsumedPower();
        doReturn(20L).when(mMockBatteryEntry2).getTimeInForegroundMs();
        doReturn(20L).when(mMockBatteryEntry2).getTimeInBackgroundMs();
        doReturn(2).when(mMockBatteryEntry2).getUid();
        doReturn(ConvertUtils.CONSUMER_TYPE_UID_BATTERY).when(mMockBatteryEntry2).getConsumerType();
        doReturn(0.0).when(mMockBatteryEntry3).getConsumedPower();
        doReturn(0L).when(mMockBatteryEntry3).getTimeInForegroundMs();
        doReturn(0L).when(mMockBatteryEntry3).getTimeInBackgroundMs();
        doReturn(3).when(mMockBatteryEntry3).getUid();
        doReturn(ConvertUtils.CONSUMER_TYPE_UID_BATTERY).when(mMockBatteryEntry3).getConsumerType();
        doReturn(1.5).when(mMockBatteryEntry4).getConsumedPower();
        doReturn(10L).when(mMockBatteryEntry4).getTimeInForegroundMs();
        doReturn(10L).when(mMockBatteryEntry4).getTimeInBackgroundMs();
        doReturn(4).when(mMockBatteryEntry4).getUid();
        doReturn(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY)
                .when(mMockBatteryEntry4).getConsumerType();
        doReturn(BatteryConsumer.POWER_COMPONENT_CAMERA)
                .when(mMockBatteryEntry4).getPowerComponentId();

        final BatteryDiffData batteryDiffData = DataProcessor.generateBatteryDiffData(
                mContext, batteryEntryList, mBatteryUsageStats);

        assertBatteryDiffEntry(
                batteryDiffData.getAppDiffEntryList().get(0), 0, /*uid=*/ 2L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 25.0,
                /*foregroundUsageTimeInMs=*/ 20, /*backgroundUsageTimeInMs=*/ 20);
        assertBatteryDiffEntry(
                batteryDiffData.getAppDiffEntryList().get(1), 0, /*uid=*/ 1L,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY, /*consumePercentage=*/ 0.0,
                /*foregroundUsageTimeInMs=*/ 30, /*backgroundUsageTimeInMs=*/ 40);
        assertBatteryDiffEntry(
                batteryDiffData.getSystemDiffEntryList().get(0), 0, /*uid=*/ 4L,
                ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY, /*consumePercentage=*/ 75.0,
                /*foregroundUsageTimeInMs=*/ 10, /*backgroundUsageTimeInMs=*/ 10);
    }

    private static Map<Long, Map<String, BatteryHistEntry>> createHistoryMap(
            final long[] timestamps, final int[] levels) {
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap = new HashMap<>();
        for (int index = 0; index < timestamps.length; index++) {
            final Map<String, BatteryHistEntry> entryMap = new HashMap<>();
            final ContentValues values = new ContentValues();
            values.put(BatteryHistEntry.KEY_BATTERY_LEVEL, levels[index]);
            final BatteryHistEntry entry = new BatteryHistEntry(values);
            entryMap.put(FAKE_ENTRY_KEY, entry);
            batteryHistoryMap.put(timestamps[index], entryMap);
        }
        return batteryHistoryMap;
    }

    private static BatteryHistEntry createBatteryHistEntry(
            final String packageName, final String appLabel, final double consumePower,
            final long uid, final long userId, final int consumerType,
            final long foregroundUsageTimeInMs, final long backgroundUsageTimeInMs) {
        // Only insert required fields.
        final ContentValues values = new ContentValues();
        values.put(BatteryHistEntry.KEY_PACKAGE_NAME, packageName);
        values.put(BatteryHistEntry.KEY_APP_LABEL, appLabel);
        values.put(BatteryHistEntry.KEY_UID, uid);
        values.put(BatteryHistEntry.KEY_USER_ID, userId);
        values.put(BatteryHistEntry.KEY_CONSUMER_TYPE, consumerType);
        values.put(BatteryHistEntry.KEY_CONSUME_POWER, consumePower);
        values.put(BatteryHistEntry.KEY_FOREGROUND_USAGE_TIME, foregroundUsageTimeInMs);
        values.put(BatteryHistEntry.KEY_BACKGROUND_USAGE_TIME, backgroundUsageTimeInMs);
        return new BatteryHistEntry(values);
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
            final Calendar end,
            final Calendar expectedStart,
            final Calendar expectedEnd) {
        expectedStart.set(Calendar.MILLISECOND, 0);
        expectedEnd.set(Calendar.MILLISECOND, 0);
        final ArrayList<Long> timestampSlots = new ArrayList<>();
        timestampSlots.add(start.getTimeInMillis());
        timestampSlots.add(end.getTimeInMillis());
        final List<Long> resultList =
                DataProcessor.getTimestampSlots(timestampSlots);

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
            final long foregroundUsageTimeInMs, final long backgroundUsageTimeInMs) {
        assertThat(entry.mBatteryHistEntry.mUserId).isEqualTo(userId);
        assertThat(entry.mBatteryHistEntry.mUid).isEqualTo(uid);
        assertThat(entry.mBatteryHistEntry.mConsumerType).isEqualTo(consumerType);
        assertThat(entry.getPercentOfTotal()).isEqualTo(consumePercentage);
        assertThat(entry.mForegroundUsageTimeInMs).isEqualTo(foregroundUsageTimeInMs);
        assertThat(entry.mBackgroundUsageTimeInMs).isEqualTo(backgroundUsageTimeInMs);
    }
}
