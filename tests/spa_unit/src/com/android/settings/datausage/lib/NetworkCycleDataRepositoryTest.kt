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

import android.app.usage.NetworkStats.Bucket
import android.app.usage.NetworkStatsManager
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
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class NetworkCycleDataRepositoryTest {
    private val mockNetworkStatsManager = mock<NetworkStatsManager> {
        on { querySummaryForDevice(any(), eq(CYCLE1_START_TIME), eq(CYCLE1_END_TIME)) } doReturn
            CYCLE1_BUCKET

        on {
            querySummaryForDevice(
                any(),
                eq(CYCLE2_END_TIME - DateUtils.WEEK_IN_MILLIS * 4),
                eq(CYCLE2_END_TIME),
            )
        } doReturn CYCLE2_BUCKET

        on { querySummaryForDevice(any(), eq(CYCLE3_START_TIME), eq(CYCLE4_END_TIME)) } doReturn
            CYCLE3_AND_4_BUCKET

        on { querySummaryForDevice(any(), eq(CYCLE3_START_TIME), eq(CYCLE3_END_TIME)) } doReturn
            CYCLE3_BUCKET

        on { querySummaryForDevice(any(), eq(CYCLE4_START_TIME), eq(CYCLE4_END_TIME)) } doReturn
            CYCLE4_BUCKET
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(NetworkStatsManager::class.java) } doReturn mockNetworkStatsManager
    }

    private val template = mock<NetworkTemplate>()

    private val repository = spy(NetworkCycleDataRepository(context, template))

    @Test
    fun loadCycles_byPolicy() = runTest {
        val policy = mock<NetworkPolicy> {
            on { cycleIterator() } doReturn listOf(
                Range(zonedDateTime(CYCLE1_START_TIME), zonedDateTime(CYCLE1_END_TIME))
            ).iterator()
        }
        doReturn(policy).whenever(repository).getPolicy()

        val cycles = repository.loadCycles()

        assertThat(cycles).containsExactly(NetworkUsageData(startTime = 1, endTime = 2, usage = 11))
    }

    @Test
    fun loadCycles_asFourWeeks() = runTest {
        doReturn(null).whenever(repository).getPolicy()
        doReturn(Range(CYCLE2_START_TIME, CYCLE2_END_TIME)).whenever(repository).getTimeRange()

        val cycles = repository.loadCycles()

        assertThat(cycles).containsExactly(
            NetworkUsageData(
                startTime = CYCLE2_END_TIME - DateUtils.WEEK_IN_MILLIS * 4,
                endTime = CYCLE2_END_TIME,
                usage = 22,
            ),
        )
    }

    @Test
    fun querySummary() = runTest {
        val summary = repository.querySummary(CYCLE3_START_TIME, CYCLE4_END_TIME)

        assertThat(summary).isEqualTo(
            NetworkCycleChartData(
                total = NetworkUsageData(
                    startTime = CYCLE3_START_TIME,
                    endTime = CYCLE4_END_TIME,
                    usage = 77,
                ),
                dailyUsage = listOf(
                    NetworkUsageData(
                        startTime = CYCLE3_START_TIME,
                        endTime = CYCLE3_END_TIME,
                        usage = 33,
                    ),
                    NetworkUsageData(
                        startTime = CYCLE4_START_TIME,
                        endTime = CYCLE4_END_TIME,
                        usage = 44,
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
        val CYCLE1_BUCKET = mock<Bucket> {
            on { rxBytes } doReturn 1
            on { txBytes } doReturn 10
        }

        const val CYCLE2_START_TIME = 1695555555000L
        const val CYCLE2_END_TIME = 1695566666000L
        val CYCLE2_BUCKET = mock<Bucket> {
            on { rxBytes } doReturn 2
            on { txBytes } doReturn 20
        }

        const val CYCLE3_START_TIME = 1695555555000L
        const val CYCLE3_END_TIME = CYCLE3_START_TIME + DateUtils.DAY_IN_MILLIS
        val CYCLE3_BUCKET = mock<Bucket> {
            on { rxBytes } doReturn 3
            on { txBytes } doReturn 30
        }

        const val CYCLE4_START_TIME = CYCLE3_END_TIME
        const val CYCLE4_END_TIME = CYCLE4_START_TIME + DateUtils.DAY_IN_MILLIS
        val CYCLE4_BUCKET = mock<Bucket> {
            on { rxBytes } doReturn 4
            on { txBytes } doReturn 40
        }

        val CYCLE3_AND_4_BUCKET = mock<Bucket> {
            on { rxBytes } doReturn 7
            on { txBytes } doReturn 70
        }
    }
}
