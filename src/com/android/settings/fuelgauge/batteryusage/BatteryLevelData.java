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

import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Wraps the battery timestamp and level data used for battery usage chart. */
public final class BatteryLevelData {
    /** A container for the battery timestamp and level data. */
    public static final class PeriodBatteryLevelData {
        // The length of mTimestamps and mLevels must be the same. mLevels[index] might be null when
        // there is no level data for the corresponding timestamp.
        private final List<Long> mTimestamps;
        private final List<Integer> mLevels;

        public PeriodBatteryLevelData(
                @NonNull List<Long> timestamps, @NonNull List<Integer> levels) {
            Preconditions.checkArgument(timestamps.size() == levels.size(),
                    /* errorMessage= */ "Timestamp: " + timestamps.size() + ", Level: "
                            + levels.size());
            mTimestamps = timestamps;
            mLevels = levels;
        }

        public List<Long> getTimestamps() {
            return mTimestamps;
        }

        public List<Integer> getLevels() {
            return mLevels;
        }

        @Override
        public String toString() {
            return String.format(Locale.ENGLISH, "timestamps: %s; levels: %s",
                    Objects.toString(mTimestamps), Objects.toString(mLevels));
        }
    }

    /**
     * There could be 2 cases for the daily battery levels:
     * 1) length is 2: The usage data is within 1 day. Only contains start and end data, such as
     *    data of 2022-01-01 06:00 and 2022-01-01 16:00.
     * 2) length > 2: The usage data is more than 1 days. The data should be the start, end and 0am
     *    data of every day between the start and end, such as data of 2022-01-01 06:00,
     *    2022-01-02 00:00, 2022-01-03 00:00 and 2022-01-03 08:00.
     */
    private final PeriodBatteryLevelData mDailyBatteryLevels;
    // The size of hourly data must be the size of daily data - 1.
    private final List<PeriodBatteryLevelData> mHourlyBatteryLevelsPerDay;

    public BatteryLevelData(
            @NonNull PeriodBatteryLevelData dailyBatteryLevels,
            @NonNull List<PeriodBatteryLevelData> hourlyBatteryLevelsPerDay) {
        final long dailySize = dailyBatteryLevels.getTimestamps().size();
        final long hourlySize = hourlyBatteryLevelsPerDay.size();
        Preconditions.checkArgument(hourlySize == dailySize - 1,
                /* errorMessage= */ "DailySize: " + dailySize + ", HourlySize: " + hourlySize);
        mDailyBatteryLevels = dailyBatteryLevels;
        mHourlyBatteryLevelsPerDay = hourlyBatteryLevelsPerDay;
    }

    public PeriodBatteryLevelData getDailyBatteryLevels() {
        return mDailyBatteryLevels;
    }

    public List<PeriodBatteryLevelData> getHourlyBatteryLevelsPerDay() {
        return mHourlyBatteryLevelsPerDay;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,
                "dailyBatteryLevels: %s; hourlyBatteryLevelsPerDay: %s",
                Objects.toString(mDailyBatteryLevels),
                Objects.toString(mHourlyBatteryLevelsPerDay));
    }
}

