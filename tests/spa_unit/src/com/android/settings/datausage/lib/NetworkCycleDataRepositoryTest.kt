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

package com.android.settings.datausage.lib

import android.content.Context
import android.net.NetworkPolicy
import android.net.NetworkTemplate
import android.text.format.DateUtils
import android.util.Range
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class NetworkCycleDataRepositoryTest {
    private val mockNetworkStatsRepository = mock<NetworkStatsRepository> {
        on { querySummaryForDevice(CYCLE1_START_TIME, CYCLE1_END_TIME) } doReturn CYCLE1_BYTES

        on {
            querySummaryForDevice(
                startTime = CYCLE2_END_TIME - DateUtils.WEEK_IN_MILLIS * 4,
                endTime = CYCLE2_END_TIME,
            )
        } doReturn CYCLE2_BYTES

        on { querySummaryForDevice(CYCLE3_START_TIME, CYCLE4_END_TIME) } doReturn
            CYCLE3_BYTES + CYCLE4_BYTES

        on { querySummaryForDevice(CYCLE3_START_TIME, CYCLE3_END_TIME) } doReturn CYCLE3_BYTES
        on { querySummaryForDevice(CYCLE4_START_TIME, CYCLE4_END_TIME) } doReturn CYCLE4_BYTES
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val template = mock<NetworkTemplate>()

    private val repository =
        spy(NetworkCycleDataRepository(context, template, mockNetworkStatsRepository))

    @Test
    fun loadCycles_byPolicy() = runTest {
        val policy = mock<NetworkPolicy> {
            on { cycleIterator() } doReturn listOf(
                Range(zonedDateTime(CYCLE1_START_TIME), zonedDateTime(CYCLE1_END_TIME))
            ).iterator()
        }
        doReturn(policy).whenever(repository).getPolicy()

        val cycles = repository.loadCycles()

        assertThat(cycles).containsExactly(
            NetworkUsageData(startTime = 1, endTime = 2, usage = CYCLE1_BYTES),
        )
    }

    @Test
    fun loadCycles_asFourWeeks() = runTest {
        doReturn(null).whenever(repository).getPolicy()
        mockNetworkStatsRepository.stub {
            on { getTimeRange() } doReturn Range(CYCLE2_START_TIME, CYCLE2_END_TIME)
        }

        val cycles = repository.loadCycles()

        assertThat(cycles).containsExactly(
            NetworkUsageData(
                startTime = CYCLE2_END_TIME - DateUtils.WEEK_IN_MILLIS * 4,
                endTime = CYCLE2_END_TIME,
                usage = CYCLE2_BYTES,
            ),
        )
    }

    @Test
    fun querySummary() = runTest {
        val summary = repository.queryChartData(CYCLE3_START_TIME, CYCLE4_END_TIME)

        assertThat(summary).isEqualTo(
            NetworkCycleChartData(
                total = NetworkUsageData(
                    startTime = CYCLE3_START_TIME,
                    endTime = CYCLE4_END_TIME,
                    usage = CYCLE3_BYTES + CYCLE4_BYTES,
                ),
                dailyUsage = listOf(
                    NetworkUsageData(
                        startTime = CYCLE3_START_TIME,
                        endTime = CYCLE3_END_TIME,
                        usage = CYCLE3_BYTES,
                    ),
                    NetworkUsageData(
                        startTime = CYCLE4_START_TIME,
                        endTime = CYCLE4_END_TIME,
                        usage = CYCLE4_BYTES,
                    ),
                ),
            )
        )
    }

    private fun zonedDateTime(epochMilli: Long): ZonedDateTime? =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault())

    private companion object {
        const val CYCLE1_START_TIME = 1L
        const val CYCLE1_END_TIME = 2L
        const val CYCLE1_BYTES = 11L

        const val CYCLE2_START_TIME = 1695555555000L
        const val CYCLE2_END_TIME = 1695566666000L
        const val CYCLE2_BYTES = 22L

        const val CYCLE3_START_TIME = 1695555555000L
        const val CYCLE3_END_TIME = CYCLE3_START_TIME + DateUtils.DAY_IN_MILLIS
        const val CYCLE3_BYTES = 33L

        const val CYCLE4_START_TIME = CYCLE3_END_TIME
        const val CYCLE4_END_TIME = CYCLE4_START_TIME + DateUtils.DAY_IN_MILLIS
        const val CYCLE4_BYTES = 44L
    }
}
