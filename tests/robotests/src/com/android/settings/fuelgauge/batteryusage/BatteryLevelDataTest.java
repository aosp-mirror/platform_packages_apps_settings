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

import android.util.Pair;

import com.android.settings.testutils.BatteryTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public class BatteryLevelDataTest {

    @Before
    public void setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
    }

    @Test
    public void getDailyTimestamps_allDataInOneHour_returnExpectedList() {
        // Timezone GMT+8
        final List<Long> timestamps =
                List.of(
                        1640970006000L, // 2022-01-01 01:00:06
                        1640973608000L // 2022-01-01 01:00:08
                        );

        final List<Long> expectedTimestamps =
                List.of(
                        1640970006000L, // 2022-01-01 01:00:06
                        1640973608000L // 2022-01-01 01:00:08
                        );
        assertThat(BatteryLevelData.getDailyTimestamps(timestamps)).isEqualTo(expectedTimestamps);
    }

    @Test
    public void getDailyTimestamps_OneHourDataPerDay_returnExpectedList() {
        // Timezone GMT+8
        final List<Long> timestamps =
                List.of(
                        1641049200000L, // 2022-01-01 23:00:00
                        1641052800000L, // 2022-01-02 00:00:00
                        1641056400000L // 2022-01-02 01:00:00
                        );

        final List<Long> expectedTimestamps =
                List.of(
                        1641049200000L, // 2022-01-01 23:00:00
                        1641052800000L, // 2022-01-02 00:00:00
                        1641056400000L // 2022-01-02 01:00:00
                        );
        assertThat(BatteryLevelData.getDailyTimestamps(timestamps)).isEqualTo(expectedTimestamps);
    }

    @Test
    public void getDailyTimestamps_OneDayData_returnExpectedList() {
        // Timezone GMT+8
        final List<Long> timestamps =
                List.of(
                        1640966400000L, // 2022-01-01 00:00:00
                        1640970000000L, // 2022-01-01 01:00:00
                        1640973600000L, // 2022-01-01 02:00:00
                        1640977200000L, // 2022-01-01 03:00:00
                        1640980800000L // 2022-01-01 04:00:00
                        );

        final List<Long> expectedTimestamps =
                List.of(
                        1640966400000L, // 2022-01-01 00:00:00
                        1640980800000L // 2022-01-01 04:00:00
                        );
        assertThat(BatteryLevelData.getDailyTimestamps(timestamps)).isEqualTo(expectedTimestamps);
    }

    @Test
    public void getDailyTimestamps_MultipleDaysData_returnExpectedList() {
        // Timezone GMT+8
        final List<Long> timestamps =
                List.of(
                        1641045600000L, // 2022-01-01 22:00:00
                        1641060000000L, // 2022-01-02 02:00:00
                        1641160800000L, // 2022-01-03 06:00:00
                        1641232800000L // 2022-01-04 02:00:00
                        );

        final List<Long> expectedTimestamps =
                List.of(
                        1641045600000L, // 2022-01-01 22:00:00
                        1641052800000L, // 2022-01-02 00:00:00
                        1641139200000L, // 2022-01-03 00:00:00
                        1641225600000L, // 2022-01-04 00:00:00
                        1641232800000L // 2022-01-04 02:00:00
                        );
        assertThat(BatteryLevelData.getDailyTimestamps(timestamps)).isEqualTo(expectedTimestamps);
    }

    @Test
    public void getDailyTimestamps_FirstDayOneHourData_returnExpectedList() {
        // Timezone GMT+8
        final List<Long> timestamps =
                List.of(
                        1641049200000L, // 2022-01-01 23:00:00
                        1641060000000L, // 2022-01-02 02:00:00
                        1641160800000L, // 2022-01-03 06:00:00
                        1641254400000L // 2022-01-04 08:00:00
                        );

        final List<Long> expectedTimestamps =
                List.of(
                        1641049200000L, // 2022-01-01 23:00:00
                        1641052800000L, // 2022-01-02 00:00:00
                        1641139200000L, // 2022-01-03 00:00:00
                        1641225600000L, // 2022-01-04 00:00:00
                        1641254400000L // 2022-01-04 08:00:00
                        );
        assertThat(BatteryLevelData.getDailyTimestamps(timestamps)).isEqualTo(expectedTimestamps);
    }

    @Test
    public void getDailyTimestamps_LastDayNoData_returnExpectedList() {
        // Timezone GMT+8
        final List<Long> timestamps =
                List.of(
                        1640988000000L, // 2022-01-01 06:00:00
                        1641060000000L, // 2022-01-02 02:00:00
                        1641160800000L, // 2022-01-03 06:00:00
                        1641225600000L // 2022-01-04 00:00:00
                        );

        final List<Long> expectedTimestamps =
                List.of(
                        1640988000000L, // 2022-01-01 06:00:00
                        1641052800000L, // 2022-01-02 00:00:00
                        1641139200000L, // 2022-01-03 00:00:00
                        1641225600000L // 2022-01-04 00:00:00
                        );
        assertThat(BatteryLevelData.getDailyTimestamps(timestamps)).isEqualTo(expectedTimestamps);
    }

    @Test
    public void getDailyTimestamps_LastDayOneHourData_returnExpectedList() {
        // Timezone GMT+8
        final List<Long> timestamps =
                List.of(
                        1640988000000L, // 2022-01-01 06:00:00
                        1641060000000L, // 2022-01-02 02:00:00
                        1641160800000L, // 2022-01-03 06:00:00
                        1641229200000L // 2022-01-04 01:00:00
                        );

        final List<Long> expectedTimestamps =
                List.of(
                        1640988000000L, // 2022-01-01 06:00:00
                        1641052800000L, // 2022-01-02 00:00:00
                        1641139200000L, // 2022-01-03 00:00:00
                        1641225600000L, // 2022-01-04 00:00:00
                        1641229200000L // 2022-01-04 01:00:00
                        );
        assertThat(BatteryLevelData.getDailyTimestamps(timestamps)).isEqualTo(expectedTimestamps);
    }

    @Test
    public void combine_normalFlow_returnExpectedResult() {
        final BatteryLevelData batteryLevelData =
                new BatteryLevelData(Map.of(1691596800000L, 90, 1691604000000L, 80));
        final List<BatteryEvent> batteryLevelRecordEvents =
                List.of(
                        BatteryEvent.newBuilder()
                                .setTimestamp(1691586000166L)
                                .setBatteryLevel(100)
                                .setType(BatteryEventType.FULL_CHARGED)
                                .build(),
                        BatteryEvent.newBuilder()
                                .setTimestamp(1691589600000L)
                                .setBatteryLevel(98)
                                .setType(BatteryEventType.EVEN_HOUR)
                                .build());

        BatteryLevelData result =
                BatteryLevelData.combine(batteryLevelData, batteryLevelRecordEvents);

        assertThat(result.getDailyBatteryLevels().getTimestamps())
                .isEqualTo(List.of(1691586000166L, 1691596800000L, 1691604000000L));
        assertThat(result.getDailyBatteryLevels().getLevels()).isEqualTo(List.of(100, 90, 80));
        assertThat(result.getHourlyBatteryLevelsPerDay()).hasSize(2);
        assertThat(result.getHourlyBatteryLevelsPerDay().get(0).getTimestamps())
                .isEqualTo(List.of(1691586000166L, 1691589600000L, 1691596800000L));
        assertThat(result.getHourlyBatteryLevelsPerDay().get(0).getLevels())
                .isEqualTo(List.of(100, 98, 90));
        assertThat(result.getHourlyBatteryLevelsPerDay().get(1).getTimestamps())
                .isEqualTo(List.of(1691596800000L, 1691604000000L));
        assertThat(result.getHourlyBatteryLevelsPerDay().get(1).getLevels())
                .isEqualTo(List.of(90, 80));
    }

    @Test
    public void combine_existingBatteryLevelDataIsNull_returnExpectedResult() {
        final List<BatteryEvent> batteryLevelRecordEvents =
                List.of(
                        BatteryEvent.newBuilder()
                                .setTimestamp(1691586000166L)
                                .setBatteryLevel(100)
                                .setType(BatteryEventType.FULL_CHARGED)
                                .build(),
                        BatteryEvent.newBuilder()
                                .setTimestamp(1691589600000L)
                                .setBatteryLevel(98)
                                .setType(BatteryEventType.EVEN_HOUR)
                                .build());

        BatteryLevelData result = BatteryLevelData.combine(null, batteryLevelRecordEvents);

        assertThat(result.getHourlyBatteryLevelsPerDay()).hasSize(1);
        assertThat(result.getHourlyBatteryLevelsPerDay().get(0).getTimestamps())
                .isEqualTo(List.of(1691586000166L, 1691589600000L));
        assertThat(result.getHourlyBatteryLevelsPerDay().get(0).getLevels())
                .isEqualTo(List.of(100, 98));
    }

    @Test
    public void getIndexByTimestamps_returnExpectedResult() {
        final BatteryLevelData batteryLevelData =
                new BatteryLevelData(
                        Map.of(
                                1694354400000L, 1, // 2023-09-10 22:00:00
                                1694361600000L, 2, // 2023-09-11 00:00:00
                                1694368800000L, 3)); // 2023-09-11 02:00:00
        final PowerAnomalyEvent event = BatteryTestUtils.createAppAnomalyEvent();

        assertThat(batteryLevelData.getIndexByTimestamps(0L, 0L))
                .isEqualTo(
                        Pair.create(
                                BatteryChartViewModel.SELECTED_INDEX_INVALID,
                                BatteryChartViewModel.SELECTED_INDEX_INVALID));
        assertThat(batteryLevelData.getIndexByTimestamps(1694361600000L + 1L, 1694368800000L + 1L))
                .isEqualTo(
                        Pair.create(
                                BatteryChartViewModel.SELECTED_INDEX_INVALID,
                                BatteryChartViewModel.SELECTED_INDEX_INVALID));
        assertThat(batteryLevelData.getIndexByTimestamps(1694361600000L, 1694368800000L))
                .isEqualTo(Pair.create(1, 0));
        assertThat(batteryLevelData.getIndexByTimestamps(1694361600000L + 1L, 1694368800000L - 1L))
                .isEqualTo(Pair.create(1, 0));
        assertThat(
                        batteryLevelData.getIndexByTimestamps(
                                event.getWarningItemInfo().getStartTimestamp(),
                                event.getWarningItemInfo().getEndTimestamp()))
                .isEqualTo(Pair.create(1, 0));
    }
}
