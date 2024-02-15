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

import static com.google.common.truth.Truth.assertThat;

import android.os.BatteryStats;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.time.Duration;

@RunWith(RobolectricTestRunner.class)
public class HighUsageDataParserTest {

    private static final long PERIOD_ONE_MINUTE_MS = Duration.ofMinutes(1).toMillis();
    private static final long PERIOD_ONE_HOUR_MS = Duration.ofHours(1).toMillis();
    private static final long END_TIME_MS = 2 * PERIOD_ONE_MINUTE_MS;
    private static final int THRESHOLD_LOW = 10;
    private static final int THRESHOLD_HIGH = 20;
    private HighUsageDataParser mDataParser;
    private BatteryStats.HistoryItem mFirstItem;
    private BatteryStats.HistoryItem mSecondItem;
    private BatteryStats.HistoryItem mThirdItem;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFirstItem = new BatteryStats.HistoryItem();
        mFirstItem.batteryLevel = 100;
        mFirstItem.currentTime = 0;
        mSecondItem = new BatteryStats.HistoryItem();
        mSecondItem.batteryLevel = 95;
        mSecondItem.currentTime = PERIOD_ONE_MINUTE_MS;
        mThirdItem = new BatteryStats.HistoryItem();
        mThirdItem.batteryLevel = 80;
        mThirdItem.currentTime = END_TIME_MS;
    }

    @Test
    public void testDataParser_thresholdLow_isHeavilyUsed() {
        mDataParser = new HighUsageDataParser(PERIOD_ONE_MINUTE_MS, THRESHOLD_LOW);
        parseData();

        assertThat(mDataParser.isDeviceHeavilyUsed()).isTrue();
    }

    @Test
    public void testDataParser_thresholdHigh_notHeavilyUsed() {
        mDataParser = new HighUsageDataParser(PERIOD_ONE_MINUTE_MS, THRESHOLD_HIGH);
        parseData();

        assertThat(mDataParser.isDeviceHeavilyUsed()).isFalse();
    }

    @Test
    public void testDataParser_heavilyUsedInShortTime_stillReportHeavilyUsed() {
        // Set threshold to 1 hour however device only used for 2 minutes
        mDataParser = new HighUsageDataParser(PERIOD_ONE_HOUR_MS, THRESHOLD_LOW);
        parseData();

        assertThat(mDataParser.isDeviceHeavilyUsed()).isTrue();
    }

    private void parseData() {
        // Report the battery usage in END_TIME_MS(2 minutes)
        mDataParser.onParsingStarted(0, END_TIME_MS);
        mDataParser.onDataPoint(0, mFirstItem);
        mDataParser.onDataPoint(PERIOD_ONE_MINUTE_MS, mSecondItem);
        mDataParser.onDataPoint(END_TIME_MS, mThirdItem);

        mDataParser.onParsingDone();
    }
}
