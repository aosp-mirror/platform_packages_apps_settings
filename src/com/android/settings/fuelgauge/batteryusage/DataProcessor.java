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

import static com.android.settings.fuelgauge.batteryusage.ConvertUtils.utcToLocalTime;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.fuelgauge.BatteryStatus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility class to process data loaded from database and make the data easy to use for battery
 * usage UI.
 */
public final class DataProcessor {
    private static final boolean DEBUG = false;
    private static final String TAG = "DataProcessor";
    private static final int MIN_DAILY_DATA_SIZE = 2;
    private static final int MIN_TIMESTAMP_DATA_SIZE = 2;

    /** A fake package name to represent no BatteryEntry data. */
    public static final String FAKE_PACKAGE_NAME = "fake_package";

    private DataProcessor() {
    }

    /**
     * @return Returns battery level data and start async task to compute battery diff usage data
     * and load app labels + icons.
     * Returns null if the input is invalid or not having at least 2 hours data.
     */
    @Nullable
    public static BatteryLevelData getBatteryLevelData(
            Context context,
            @Nullable final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap) {
        if (batteryHistoryMap == null || batteryHistoryMap.isEmpty()) {
            Log.d(TAG, "getBatteryLevelData() returns null");
            return null;
        }
        // Process raw history map data into hourly timestamps.
        final Map<Long, Map<String, BatteryHistEntry>> processedBatteryHistoryMap =
                getHistoryMapWithExpectedTimestamps(context, batteryHistoryMap);
        // Wrap and processed history map into easy-to-use format for UI rendering.
        final BatteryLevelData batteryLevelData =
                getLevelDataThroughProcessedHistoryMap(context, processedBatteryHistoryMap);

        //TODO: Add the async task to compute diff usage data and load labels and icons.

        return batteryLevelData;
    }

    /**
     * @return Returns the processed history map which has interpolated to every hour data.
     * The start and end timestamp must be the even hours.
     * The keys of processed history map should contain every hour between the start and end
     * timestamp. If there's no data in some key, the value will be the empty hashmap.
     */
    @VisibleForTesting
    static Map<Long, Map<String, BatteryHistEntry>> getHistoryMapWithExpectedTimestamps(
            Context context,
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap) {
        final long startTime = System.currentTimeMillis();
        final List<Long> rawTimestampList = new ArrayList<>(batteryHistoryMap.keySet());
        final Map<Long, Map<String, BatteryHistEntry>> resultMap = new HashMap();
        if (rawTimestampList.isEmpty()) {
            Log.d(TAG, "empty batteryHistoryMap in getHistoryMapWithExpectedTimestamps()");
            return resultMap;
        }
        Collections.sort(rawTimestampList);
        final List<Long> expectedTimestampList = getTimestampSlots(rawTimestampList);
        final boolean isFromFullCharge =
                isFromFullCharge(batteryHistoryMap.get(rawTimestampList.get(0)));
        interpolateHistory(
                context, rawTimestampList, expectedTimestampList, isFromFullCharge,
                batteryHistoryMap, resultMap);
        Log.d(TAG, String.format("getHistoryMapWithExpectedTimestamps() size=%d in %d/ms",
                resultMap.size(), (System.currentTimeMillis() - startTime)));
        return resultMap;
    }

    @VisibleForTesting
    @Nullable
    static BatteryLevelData getLevelDataThroughProcessedHistoryMap(
            Context context,
            final Map<Long, Map<String, BatteryHistEntry>> processedBatteryHistoryMap) {
        final List<Long> timestampList = new ArrayList<>(processedBatteryHistoryMap.keySet());
        Collections.sort(timestampList);
        final List<Long> dailyTimestamps = getDailyTimestamps(timestampList);
        // There should be at least the start and end timestamps. Otherwise, return null to not show
        // data in usage chart.
        if (dailyTimestamps.size() < MIN_DAILY_DATA_SIZE) {
            return null;
        }

        final List<List<Long>> hourlyTimestamps = getHourlyTimestamps(dailyTimestamps);
        final BatteryLevelData.PeriodBatteryLevelData dailyLevelData =
                getPeriodBatteryLevelData(context, processedBatteryHistoryMap, dailyTimestamps);
        final List<BatteryLevelData.PeriodBatteryLevelData> hourlyLevelData =
                getHourlyPeriodBatteryLevelData(
                        context, processedBatteryHistoryMap, hourlyTimestamps);
        return new BatteryLevelData(dailyLevelData, hourlyLevelData);
    }

    /**
     * Computes expected timestamp slots for last full charge, which will return hourly timestamps
     * between start and end two even hour values.
     */
    @VisibleForTesting
    static List<Long> getTimestampSlots(final List<Long> rawTimestampList) {
        final List<Long> timestampSlots = new ArrayList<>();
        final int rawTimestampListSize = rawTimestampList.size();
        // If timestamp number is smaller than 2, the following computation is not necessary.
        if (rawTimestampListSize < MIN_TIMESTAMP_DATA_SIZE) {
            return timestampSlots;
        }
        final long rawStartTimestamp = rawTimestampList.get(0);
        final long rawEndTimestamp = rawTimestampList.get(rawTimestampListSize - 1);
        // No matter the start is from last full charge or 6 days ago, use the nearest even hour.
        final long startTimestamp = getNearestEvenHourTimestamp(rawStartTimestamp);
        // Use the even hour before the raw end timestamp as the end.
        final long endTimestamp = getLastEvenHourBeforeTimestamp(rawEndTimestamp);
        // If the start timestamp is later or equal the end one, return the empty list.
        if (startTimestamp >= endTimestamp) {
            return timestampSlots;
        }
        for (long timestamp = startTimestamp; timestamp <= endTimestamp;
                timestamp += DateUtils.HOUR_IN_MILLIS) {
            timestampSlots.add(timestamp);
        }
        return timestampSlots;
    }

    /**
     * Computes expected daily timestamp slots.
     *
     * The valid result should be composed of 3 parts:
     * 1) start timestamp
     * 2) every 0am timestamp (default timezone) between the start and end
     * 3) end timestamp
     * Otherwise, returns an empty list.
     */
    @VisibleForTesting
    static List<Long> getDailyTimestamps(final List<Long> timestampList) {
        final List<Long> dailyTimestampList = new ArrayList<>();
        // If timestamp number is smaller than 2, the following computation is not necessary.
        if (timestampList.size() < MIN_TIMESTAMP_DATA_SIZE) {
            return dailyTimestampList;
        }
        final long startTime = timestampList.get(0);
        final long endTime = timestampList.get(timestampList.size() - 1);
        long nextDay = getTimestampOfNextDay(startTime);
        dailyTimestampList.add(startTime);
        while (nextDay < endTime) {
            dailyTimestampList.add(nextDay);
            nextDay += DateUtils.DAY_IN_MILLIS;
        }
        dailyTimestampList.add(endTime);
        return dailyTimestampList;
    }

    @VisibleForTesting
    static boolean isFromFullCharge(@Nullable final Map<String, BatteryHistEntry> entryList) {
        if (entryList == null) {
            Log.d(TAG, "entryList is nul in isFromFullCharge()");
            return false;
        }
        final List<String> entryKeys = new ArrayList<>(entryList.keySet());
        if (entryKeys.isEmpty()) {
            Log.d(TAG, "empty entryList in isFromFullCharge()");
            return false;
        }
        // The hist entries in the same timestamp should have same battery status and level.
        // Checking the first one should be enough.
        final BatteryHistEntry firstHistEntry = entryList.get(entryKeys.get(0));
        return BatteryStatus.isCharged(firstHistEntry.mBatteryStatus, firstHistEntry.mBatteryLevel);
    }

    @VisibleForTesting
    static long[] findNearestTimestamp(final List<Long> timestamps, final long target) {
        final long[] results = new long[] {Long.MIN_VALUE, Long.MAX_VALUE};
        // Searches the nearest lower and upper timestamp value.
        for (long timestamp : timestamps) {
            if (timestamp <= target && timestamp > results[0]) {
                results[0] = timestamp;
            }
            if (timestamp >= target && timestamp < results[1]) {
                results[1] = timestamp;
            }
        }
        // Uses zero value to represent invalid searching result.
        results[0] = results[0] == Long.MIN_VALUE ? 0 : results[0];
        results[1] = results[1] == Long.MAX_VALUE ? 0 : results[1];
        return results;
    }

    /**
     * @return Returns the timestamp for 0am 1 day after the given timestamp based on local
     * timezone.
     */
    @VisibleForTesting
     static long getTimestampOfNextDay(long timestamp) {
        final Calendar nextDayCalendar = Calendar.getInstance();
        nextDayCalendar.setTimeInMillis(timestamp);
        nextDayCalendar.add(Calendar.DAY_OF_YEAR, 1);
        nextDayCalendar.set(Calendar.HOUR_OF_DAY, 0);
        nextDayCalendar.set(Calendar.MINUTE, 0);
        nextDayCalendar.set(Calendar.SECOND, 0);
        return nextDayCalendar.getTimeInMillis();
    }

    /**
     * Interpolates history map based on expected timestamp slots and processes the corner case when
     * the expected start timestamp is earlier than what we have.
     */
    private static void interpolateHistory(
            Context context,
            final List<Long> rawTimestampList,
            final List<Long> expectedTimestampSlots,
            final boolean isFromFullCharge,
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap,
            final Map<Long, Map<String, BatteryHistEntry>> resultMap) {
        if (rawTimestampList.isEmpty() || expectedTimestampSlots.isEmpty()) {
            return;
        }
        final long expectedStartTimestamp = expectedTimestampSlots.get(0);
        final long rawStartTimestamp = rawTimestampList.get(0);
        int startIndex = 0;
        // If the expected start timestamp is full charge or earlier than what we have, use the
        // first data of what we have directly. This should be OK because the expected start
        // timestamp is the nearest even hour of the raw start timestamp, their time diff is no
        // more than 1 hour.
        if (isFromFullCharge || expectedStartTimestamp < rawStartTimestamp) {
            startIndex = 1;
            resultMap.put(expectedStartTimestamp, batteryHistoryMap.get(rawStartTimestamp));
        }
        for (int index = startIndex; index < expectedTimestampSlots.size(); index++) {
            final long currentSlot = expectedTimestampSlots.get(index);
            interpolateHistoryForSlot(
                    context, currentSlot, rawTimestampList, batteryHistoryMap, resultMap);
        }
    }

    private static void interpolateHistoryForSlot(
            Context context,
            final long currentSlot,
            final List<Long> rawTimestampList,
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap,
            final Map<Long, Map<String, BatteryHistEntry>> resultMap) {
        final long[] nearestTimestamps = findNearestTimestamp(rawTimestampList, currentSlot);
        final long lowerTimestamp = nearestTimestamps[0];
        final long upperTimestamp = nearestTimestamps[1];
        // Case 1: upper timestamp is zero since scheduler is delayed!
        if (upperTimestamp == 0) {
            log(context, "job scheduler is delayed", currentSlot, null);
            resultMap.put(currentSlot, new HashMap<>());
            return;
        }
        // Case 2: upper timestamp is closed to the current timestamp.
        if ((upperTimestamp - currentSlot) < 5 * DateUtils.SECOND_IN_MILLIS) {
            log(context, "force align into the nearest slot", currentSlot, null);
            resultMap.put(currentSlot, batteryHistoryMap.get(upperTimestamp));
            return;
        }
        // Case 3: lower timestamp is zero before starting to collect data.
        if (lowerTimestamp == 0) {
            log(context, "no lower timestamp slot data", currentSlot, null);
            resultMap.put(currentSlot, new HashMap<>());
            return;
        }
        interpolateHistoryForSlot(context,
                currentSlot, lowerTimestamp, upperTimestamp, batteryHistoryMap, resultMap);
    }

    private static void interpolateHistoryForSlot(
            Context context,
            final long currentSlot,
            final long lowerTimestamp,
            final long upperTimestamp,
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap,
            final Map<Long, Map<String, BatteryHistEntry>> resultMap) {
        final Map<String, BatteryHistEntry> lowerEntryDataMap =
                batteryHistoryMap.get(lowerTimestamp);
        final Map<String, BatteryHistEntry> upperEntryDataMap =
                batteryHistoryMap.get(upperTimestamp);
        // Verifies whether the lower data is valid to use or not by checking boot time.
        final BatteryHistEntry upperEntryDataFirstEntry =
                upperEntryDataMap.values().stream().findFirst().get();
        final long upperEntryDataBootTimestamp =
                upperEntryDataFirstEntry.mTimestamp - upperEntryDataFirstEntry.mBootTimestamp;
        // Lower data is captured before upper data corresponding device is booting.
        if (lowerTimestamp < upperEntryDataBootTimestamp) {
            // Provides an opportunity to force align the slot directly.
            if ((upperTimestamp - currentSlot) < 10 * DateUtils.MINUTE_IN_MILLIS) {
                log(context, "force align into the nearest slot", currentSlot, null);
                resultMap.put(currentSlot, upperEntryDataMap);
            } else {
                log(context, "in the different booting section", currentSlot, null);
                resultMap.put(currentSlot, new HashMap<>());
            }
            return;
        }
        log(context, "apply interpolation arithmetic", currentSlot, null);
        final Map<String, BatteryHistEntry> newHistEntryMap = new HashMap<>();
        final double timestampLength = upperTimestamp - lowerTimestamp;
        final double timestampDiff = currentSlot - lowerTimestamp;
        // Applies interpolation arithmetic for each BatteryHistEntry.
        for (String entryKey : upperEntryDataMap.keySet()) {
            final BatteryHistEntry lowerEntry = lowerEntryDataMap.get(entryKey);
            final BatteryHistEntry upperEntry = upperEntryDataMap.get(entryKey);
            // Checks whether there is any abnormal battery reset conditions.
            if (lowerEntry != null) {
                final boolean invalidForegroundUsageTime =
                        lowerEntry.mForegroundUsageTimeInMs > upperEntry.mForegroundUsageTimeInMs;
                final boolean invalidBackgroundUsageTime =
                        lowerEntry.mBackgroundUsageTimeInMs > upperEntry.mBackgroundUsageTimeInMs;
                if (invalidForegroundUsageTime || invalidBackgroundUsageTime) {
                    newHistEntryMap.put(entryKey, upperEntry);
                    log(context, "abnormal reset condition is found", currentSlot, upperEntry);
                    continue;
                }
            }
            final BatteryHistEntry newEntry =
                    BatteryHistEntry.interpolate(
                            currentSlot,
                            upperTimestamp,
                            /*ratio=*/ timestampDiff / timestampLength,
                            lowerEntry,
                            upperEntry);
            newHistEntryMap.put(entryKey, newEntry);
            if (lowerEntry == null) {
                log(context, "cannot find lower entry data", currentSlot, upperEntry);
                continue;
            }
        }
        resultMap.put(currentSlot, newHistEntryMap);
    }

    /**
     * @return Returns the nearest even hour timestamp of the given timestamp.
     */
    private static long getNearestEvenHourTimestamp(long rawTimestamp) {
        // If raw hour is even, the nearest even hour should be the even hour before raw
        // start. The hour doesn't need to change and just set the minutes and seconds to 0.
        // Otherwise, the nearest even hour should be raw hour + 1.
        // For example, the nearest hour of 14:30:50 should be 14:00:00. While the nearest
        // hour of 15:30:50 should be 16:00:00.
        return getEvenHourTimestamp(rawTimestamp, /*addHourOfDay*/ 1);
    }

    /**
     * @return Returns the last even hour timestamp before the given timestamp.
     */
    private static long getLastEvenHourBeforeTimestamp(long rawTimestamp) {
        // If raw hour is even, the hour doesn't need to change as well.
        // Otherwise, the even hour before raw end should be raw hour - 1.
        // For example, the even hour before 14:30:50 should be 14:00:00. While the even
        // hour before 15:30:50 should be 14:00:00.
        return getEvenHourTimestamp(rawTimestamp, /*addHourOfDay*/ -1);
    }

    private static long getEvenHourTimestamp(long rawTimestamp, int addHourOfDay) {
        final Calendar evenHourCalendar = Calendar.getInstance();
        evenHourCalendar.setTimeInMillis(rawTimestamp);
        // Before computing the evenHourCalendar, record raw hour based on local timezone.
        final int rawHour = evenHourCalendar.get(Calendar.HOUR_OF_DAY);
        if (rawHour % 2 != 0) {
            evenHourCalendar.add(Calendar.HOUR_OF_DAY, addHourOfDay);
        }
        evenHourCalendar.set(Calendar.MINUTE, 0);
        evenHourCalendar.set(Calendar.SECOND, 0);
        evenHourCalendar.set(Calendar.MILLISECOND, 0);
        return evenHourCalendar.getTimeInMillis();
    }

    private static List<List<Long>> getHourlyTimestamps(final List<Long> dailyTimestamps) {
        final List<List<Long>> hourlyTimestamps = new ArrayList<>();
        if (dailyTimestamps.size() < MIN_DAILY_DATA_SIZE) {
            return hourlyTimestamps;
        }

        for (int dailyStartIndex = 0; dailyStartIndex < dailyTimestamps.size() - 1;
                dailyStartIndex++) {
            long currentTimestamp = dailyTimestamps.get(dailyStartIndex);
            final long dailyEndTimestamp = dailyTimestamps.get(dailyStartIndex + 1);
            final List<Long> hourlyTimestampsPerDay = new ArrayList<>();
            while (currentTimestamp <= dailyEndTimestamp) {
                hourlyTimestampsPerDay.add(currentTimestamp);
                currentTimestamp += 2 * DateUtils.HOUR_IN_MILLIS;
            }
            hourlyTimestamps.add(hourlyTimestampsPerDay);
        }
        return hourlyTimestamps;
    }

    private static List<BatteryLevelData.PeriodBatteryLevelData> getHourlyPeriodBatteryLevelData(
            Context context,
            final Map<Long, Map<String, BatteryHistEntry>> processedBatteryHistoryMap,
            final List<List<Long>> timestamps) {
        final List<BatteryLevelData.PeriodBatteryLevelData> levelData = new ArrayList<>();
        timestamps.forEach(
                timestampList -> levelData.add(
                        getPeriodBatteryLevelData(
                                context, processedBatteryHistoryMap, timestampList)));
        return levelData;
    }

    private static BatteryLevelData.PeriodBatteryLevelData getPeriodBatteryLevelData(
            Context context,
            final Map<Long, Map<String, BatteryHistEntry>> processedBatteryHistoryMap,
            final List<Long> timestamps) {
        final List<Integer> levels = new ArrayList<>();
        timestamps.forEach(
                timestamp -> levels.add(getLevel(context, processedBatteryHistoryMap, timestamp)));
        return new BatteryLevelData.PeriodBatteryLevelData(timestamps, levels);
    }

    private static Integer getLevel(
            Context context,
            final Map<Long, Map<String, BatteryHistEntry>> processedBatteryHistoryMap,
            final long timestamp) {
        final Map<String, BatteryHistEntry> entryMap = processedBatteryHistoryMap.get(timestamp);
        if (entryMap == null || entryMap.isEmpty()) {
            Log.e(TAG, "abnormal entry list in the timestamp:"
                    + utcToLocalTime(context, timestamp));
            return null;
        }
        // Averages the battery level in each time slot to avoid corner conditions.
        float batteryLevelCounter = 0;
        for (BatteryHistEntry entry : entryMap.values()) {
            batteryLevelCounter += entry.mBatteryLevel;
        }
        return Math.round(batteryLevelCounter / entryMap.size());
    }

    private static void log(Context context, String content, long timestamp,
            BatteryHistEntry entry) {
        if (DEBUG) {
            Log.d(TAG, String.format(entry != null ? "%s %s:\n%s" : "%s %s:%s",
                    utcToLocalTime(context, timestamp), content, entry));
        }
    }
}
