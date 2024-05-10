/*
 * Copyright (C) 2023 The Android Open Source Project
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public class TimestampUtilsTest {

    @Before
    public void setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
    }

    @Test
    public void getNextHourTimestamp_returnExpectedResult() {
        // 2021-02-28 06:00:00 => 2021-02-28 07:00:00
        assertThat(TimestampUtils.getNextHourTimestamp(1614463200000L)).isEqualTo(1614466800000L);
        // 2021-12-31 23:59:59 => 2022-01-01 00:00:00
        assertThat(TimestampUtils.getNextHourTimestamp(16409663999999L)).isEqualTo(16409664000000L);
    }

    @Test
    public void getNextEvenHourTimestamp_returnExpectedResult() {
        // 2021-02-28 06:00:00 => 2021-02-28 08:00:00
        assertThat(TimestampUtils.getNextEvenHourTimestamp(1614463200000L))
                .isEqualTo(1614470400000L);
        // 2021-12-31 23:59:59 => 2022-01-01 00:00:00
        assertThat(TimestampUtils.getNextEvenHourTimestamp(16409663999999L))
                .isEqualTo(16409664000000L);
    }

    @Test
    public void getLastEvenHourTimestamp_returnExpectedResult() {
        // 2021-02-28 06:00:06 => 2021-02-28 06:00:00
        assertThat(TimestampUtils.getLastEvenHourTimestamp(1614463206000L))
                .isEqualTo(1614463200000L);
        // 2021-12-31 23:59:59 => 2021-12-31 22:00:00
        assertThat(TimestampUtils.getLastEvenHourTimestamp(16409663999999L))
                .isEqualTo(16409656800000L);
    }

    @Test
    public void getTimestampOfNextDay_returnExpectedResult() {
        // 2021-02-28 06:00:00 => 2021-03-01 00:00:00
        assertThat(TimestampUtils.getNextDayTimestamp(1614463200000L)).isEqualTo(1614528000000L);
        // 2021-12-31 16:00:00 => 2022-01-01 00:00:00
        assertThat(TimestampUtils.getNextDayTimestamp(1640937600000L)).isEqualTo(1640966400000L);
    }

    @Test
    public void isMidnight_returnExpectedResult() {
        // 2022-01-01 00:00:00
        assertThat(TimestampUtils.isMidnight(1640966400000L)).isTrue();
        // 2022-01-01 01:00:05
        assertThat(TimestampUtils.isMidnight(1640970005000L)).isFalse();
    }
}
