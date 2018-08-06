/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.fuelgauge.batterytip;

import android.os.BatteryStats;

import com.android.settings.fuelgauge.BatteryInfo;

/**
 * DataParser used to go through battery data and detect whether battery is
 * heavily used.
 */
public class HighUsageDataParser implements BatteryInfo.BatteryDataParser {
    /**
     * time period to check the battery usage
     */
    private final long mTimePeriodMs;
    /**
     * treat device as heavily used if battery usage is more than {@code threshold}. 1 means 1%
     * battery usage.
     */
    private int mThreshold;
    private long mEndTimeMs;
    private byte mEndBatteryLevel;
    private byte mLastPeriodBatteryLevel;
    private int mBatteryDrain;

    public HighUsageDataParser(long timePeriodMs, int threshold) {
        mTimePeriodMs = timePeriodMs;
        mThreshold = threshold;
    }

    @Override
    public void onParsingStarted(long startTime, long endTime) {
        mEndTimeMs = endTime;
    }

    @Override
    public void onDataPoint(long time, BatteryStats.HistoryItem record) {
        if (time == 0 || record.currentTime <= mEndTimeMs - mTimePeriodMs) {
            // Since onDataPoint is invoked sorted by time, so we could use this way to get the
            // closet battery level 'mTimePeriodMs' time ago.
            mLastPeriodBatteryLevel = record.batteryLevel;
        }
        mEndBatteryLevel = record.batteryLevel;
    }

    @Override
    public void onDataGap() {
        // do nothing
    }

    @Override
    public void onParsingDone() {
        mBatteryDrain = mLastPeriodBatteryLevel - mEndBatteryLevel;
    }

    /**
     * Return {@code true} if the battery drain in {@link #mTimePeriodMs} is too much
     */
    public boolean isDeviceHeavilyUsed() {
        return mBatteryDrain > mThreshold;
    }
}

