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

import static com.android.settingslib.fuelgauge.BatteryStatus.BATTERY_LEVEL_UNKNOWN;

import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Wraps the battery timestamp and level data used for battery usage chart. */
public final class BatteryLevelData {
    private static final long MIN_SIZE = 2;
    private static final long TIME_SLOT = DateUtils.HOUR_IN_MILLIS * 2;

    /** A container for the battery timestamp and level data. */
    public static final class PeriodBatteryLevelData {
        // The length of mTimestamps and mLevels must be the same. mLevels[index] might be null when
        // there is no level data for the corresponding timestamp.
        private final List<Long> mTimestamps;
        private final List<Integer> mLevels;

        public PeriodBatteryLevelData(
                @NonNull Map<Long, Integer> batteryLevelMap, @NonNull List<Long> timestamps) {
            mTimestamps = timestamps;
            mLevels = new ArrayList<>(timestamps.size());
            for (Long timestamp : timestamps) {
                mLevels.add(
                        batteryLevelMap.containsKey(timestamp)
                                ? batteryLevelMap.get(timestamp)
                                : BATTERY_LEVEL_UNKNOWN);
            }
        }

        public List<Long> getTimestamps() {
            return mTimestamps;
        }

        public List<Integer> getLevels() {
            return mLevels;
        }

        @Override
        public String toString() {
            return String.format(
                    Locale.ENGLISH,
                    "timestamps: %s; levels: %s",
                    Objects.toString(mTimestamps),
                    Objects.toString(mLevels));
        }

        private int getIndexByTimestamps(long startTimestamp, long endTimestamp) {
            for (int index = 0; index < mTimestamps.size() - 1; index++) {
                if (mTimestamps.get(index) <= startTimestamp
                        && endTimestamp <= mTimestamps.get(index + 1)) {
                    return index;
                }
            }
            return BatteryChartViewModel.SELECTED_INDEX_INVALID;
        }
    }

    /**
     * There could be 2 cases for the daily battery levels: <br>
     * 1) length is 2: The usage data is within 1 day. Only contains start and end data, such as
     * data of 2022-01-01 06:00 and 2022-01-01 16:00. <br>
     * 2) length > 2: The usage data is more than 1 days. The data should be the start, end and 0am
     * data of every day between the start and end, such as data of 2022-01-01 06:00, 2022-01-02
     * 00:00, 2022-01-03 00:00 and 2022-01-03 08:00.
     */
    private final PeriodBatteryLevelData mDailyBatteryLevels;

    // The size of hourly data must be the size of daily data - 1.
    private final List<PeriodBatteryLevelData> mHourlyBatteryLevelsPerDay;

    public BatteryLevelData(@NonNull Map<Long, Integer> batteryLevelMap) {
        final int mapSize = batteryLevelMap.size();
        Preconditions.checkArgument(mapSize >= MIN_SIZE, "batteryLevelMap size:" + mapSize);

        final List<Long> timestampList = new ArrayList<>(batteryLevelMap.keySet());
        Collections.sort(timestampList);
        final List<Long> dailyTimestamps = getDailyTimestamps(timestampList);
        final List<List<Long>> hourlyTimestamps = getHourlyTimestamps(dailyTimestamps);

        mDailyBatteryLevels = new PeriodBatteryLevelData(batteryLevelMap, dailyTimestamps);
        mHourlyBatteryLevelsPerDay = new ArrayList<>(hourlyTimestamps.size());
        for (List<Long> hourlyTimestampsPerDay : hourlyTimestamps) {
            mHourlyBatteryLevelsPerDay.add(
                    new PeriodBatteryLevelData(batteryLevelMap, hourlyTimestampsPerDay));
        }
    }

    /** Gets daily and hourly index between start and end timestamps. */
    public Pair<Integer, Integer> getIndexByTimestamps(long startTimestamp, long endTimestamp) {
        final int dailyHighlightIndex =
                mDailyBatteryLevels.getIndexByTimestamps(startTimestamp, endTimestamp);
        final int hourlyHighlightIndex =
                (dailyHighlightIndex == BatteryChartViewModel.SELECTED_INDEX_INVALID)
                        ? BatteryChartViewModel.SELECTED_INDEX_INVALID
                        : mHourlyBatteryLevelsPerDay
                                .get(dailyHighlightIndex)
                                .getIndexByTimestamps(startTimestamp, endTimestamp);
        return Pair.create(dailyHighlightIndex, hourlyHighlightIndex);
    }

    public PeriodBatteryLevelData getDailyBatteryLevels() {
        return mDailyBatteryLevels;
    }

    public List<PeriodBatteryLevelData> getHourlyBatteryLevelsPerDay() {
        return mHourlyBatteryLevelsPerDay;
    }

    @Override
    public String toString() {
        return String.format(
                Locale.ENGLISH,
                "dailyBatteryLevels: %s; hourlyBatteryLevelsPerDay: %s",
                Objects.toString(mDailyBatteryLevels),
                Objects.toString(mHourlyBatteryLevelsPerDay));
    }

    @Nullable
    static BatteryLevelData combine(
            @Nullable BatteryLevelData existingBatteryLevelData,
            List<BatteryEvent> batteryLevelRecordEvents) {
        final Map<Long, Integer> batteryLevelMap = new ArrayMap<>(batteryLevelRecordEvents.size());
        for (BatteryEvent event : batteryLevelRecordEvents) {
            batteryLevelMap.put(event.getTimestamp(), event.getBatteryLevel());
        }
        if (existingBatteryLevelData != null) {
            List<PeriodBatteryLevelData> multiDaysData =
                    existingBatteryLevelData.getHourlyBatteryLevelsPerDay();
            for (int dayIndex = 0; dayIndex < multiDaysData.size(); dayIndex++) {
                PeriodBatteryLevelData oneDayData = multiDaysData.get(dayIndex);
                for (int hourIndex = 0; hourIndex < oneDayData.getLevels().size(); hourIndex++) {
                    batteryLevelMap.put(
                            oneDayData.getTimestamps().get(hourIndex),
                            oneDayData.getLevels().get(hourIndex));
                }
            }
        }
        return batteryLevelMap.size() < MIN_SIZE ? null : new BatteryLevelData(batteryLevelMap);
    }

    /**
     * Computes expected daily timestamp slots.
     *
     * <p>The valid result should be composed of 3 parts: <br>
     * 1) start timestamp <br>
     * 2) every 00:00 timestamp (default timezone) between the start and end <br>
     * 3) end timestamp Otherwise, returns an empty list.
     */
    @VisibleForTesting
    static List<Long> getDailyTimestamps(final List<Long> timestampList) {
        Preconditions.checkArgument(
                timestampList.size() >= MIN_SIZE, "timestampList size:" + timestampList.size());
        final List<Long> dailyTimestampList = new ArrayList<>();
        final long startTimestamp = timestampList.get(0);
        final long endTimestamp = timestampList.get(timestampList.size() - 1);
        for (long timestamp = startTimestamp;
                timestamp < endTimestamp;
                timestamp = TimestampUtils.getNextDayTimestamp(timestamp)) {
            dailyTimestampList.add(timestamp);
        }
        dailyTimestampList.add(endTimestamp);
        return dailyTimestampList;
    }

    private static List<List<Long>> getHourlyTimestamps(final List<Long> dailyTimestamps) {
        final List<List<Long>> hourlyTimestamps = new ArrayList<>();
        for (int dailyIndex = 0; dailyIndex < dailyTimestamps.size() - 1; dailyIndex++) {
            final List<Long> hourlyTimestampsPerDay = new ArrayList<>();
            final long startTime = dailyTimestamps.get(dailyIndex);
            final long endTime = dailyTimestamps.get(dailyIndex + 1);

            hourlyTimestampsPerDay.add(startTime);
            for (long timestamp = TimestampUtils.getNextEvenHourTimestamp(startTime);
                    timestamp < endTime;
                    timestamp += TIME_SLOT) {
                hourlyTimestampsPerDay.add(timestamp);
            }
            hourlyTimestampsPerDay.add(endTime);

            hourlyTimestamps.add(hourlyTimestampsPerDay);
        }
        return hourlyTimestamps;
    }
}
