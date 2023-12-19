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
import com.android.settings.datausage.lib.NetworkStatsRepository.Companion.Bucket
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class NetworkCycleBucketRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val template = mock<NetworkTemplate>()

    private val mockNetworkCycleDataRepository = mock<NetworkCycleDataRepository>()

    @Test
    fun loadCycles_byPolicy() {
        val policy = mock<NetworkPolicy> {
            on { cycleIterator() } doReturn listOf(
                Range(zonedDateTime(CYCLE1_START_TIME), zonedDateTime(CYCLE1_END_TIME)),
            ).iterator()
        }
        mockNetworkCycleDataRepository.stub {
            on { getPolicy() } doReturn policy
        }
        val repository = NetworkCycleBucketRepository(
            context = context,
            networkTemplate = template,
            buckets = listOf(
                Bucket(
                    uid = 0,
                    bytes = CYCLE1_BYTES,
                    startTimeStamp = CYCLE1_START_TIME,
                    endTimeStamp = CYCLE1_END_TIME,
                )
            ),
            networkCycleDataRepository = mockNetworkCycleDataRepository,
        )

        val cycles = repository.loadCycles()

        assertThat(cycles).containsExactly(
            NetworkUsageData(
                startTime = CYCLE1_START_TIME,
                endTime = CYCLE1_END_TIME,
                usage = CYCLE1_BYTES,
            ),
        )
    }

    @Test
    fun loadCycles_asFourWeeks() {
        mockNetworkCycleDataRepository.stub {
            on { getPolicy() } doReturn null
        }
        val repository = NetworkCycleBucketRepository(
            context = context,
            networkTemplate = template,
            buckets = listOf(
                Bucket(
                    uid = 0,
                    bytes = CYCLE2_BYTES,
                    startTimeStamp = CYCLE2_START_TIME,
                    endTimeStamp = CYCLE2_END_TIME,
                )
            ),
            networkCycleDataRepository = mockNetworkCycleDataRepository,
        )

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
    fun queryChartData() {
        val cycle = NetworkUsageData(
            startTime = CYCLE3_START_TIME,
            endTime = CYCLE4_END_TIME,
            usage = CYCLE3_BYTES + CYCLE4_BYTES,
        )
        val repository = NetworkCycleBucketRepository(
            context = context,
            networkTemplate = template,
            buckets = listOf(
                Bucket(
                    uid = 0,
                    bytes = CYCLE3_BYTES,
                    startTimeStamp = CYCLE3_START_TIME,
                    endTimeStamp = CYCLE3_END_TIME,
                ),
                Bucket(
                    uid = 0,
                    bytes = CYCLE4_BYTES,
                    startTimeStamp = CYCLE4_START_TIME,
                    endTimeStamp = CYCLE4_END_TIME,
                ),
            ),
            networkCycleDataRepository = mockNetworkCycleDataRepository,
        )

        val summary = repository.queryChartData(cycle)

        assertThat(summary).isEqualTo(
            NetworkCycleChartData(
                total = cycle,
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
