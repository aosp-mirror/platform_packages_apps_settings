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

import android.app.usage.NetworkStats
import android.content.Context
import android.net.NetworkTemplate
import android.util.Range
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import com.android.settings.datausage.lib.NetworkStatsRepository.Companion.Bucket

@RunWith(AndroidJUnit4::class)
class AppDataUsageDetailsRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val template = mock<NetworkTemplate>()

    private val networkCycleDataRepository = mock<INetworkCycleDataRepository> {
        on { getCycles() } doReturn listOf(Range(CYCLE1_END_TIME, CYCLE2_END_TIME))
    }

    private val networkStatsRepository = mock<NetworkStatsRepository>()

    @Test
    fun queryDetailsForCycles_hasCycles(): Unit = runBlocking {
        networkStatsRepository.stub {
            on { queryBuckets(CYCLE1_START_TIME, CYCLE1_END_TIME) } doReturn listOf(
                Bucket(
                    uid = UID,
                    state = NetworkStats.Bucket.STATE_DEFAULT,
                    bytes = BACKGROUND_USAGE,
                ),
                Bucket(
                    uid = UID,
                    state = NetworkStats.Bucket.STATE_FOREGROUND,
                    bytes = FOREGROUND_USAGE,
                ),
            )
        }
        val repository = AppDataUsageDetailsRepository(
            context = context,
            cycles = listOf(CYCLE1_END_TIME, CYCLE1_START_TIME),
            template = template,
            uids = listOf(UID),
            networkCycleDataRepository = networkCycleDataRepository,
            networkStatsRepository = networkStatsRepository,
        )

        val detailsForCycles = repository.queryDetailsForCycles()

        assertThat(detailsForCycles).containsExactly(
            NetworkUsageDetailsData(
                range = Range(CYCLE1_START_TIME, CYCLE1_END_TIME),
                totalUsage = BACKGROUND_USAGE + FOREGROUND_USAGE,
                foregroundUsage = FOREGROUND_USAGE,
                backgroundUsage = BACKGROUND_USAGE,
            )
        )
    }

    @Test
    fun queryDetailsForCycles_defaultCycles(): Unit = runBlocking {
        networkStatsRepository.stub {
            on { queryBuckets(CYCLE1_END_TIME, CYCLE2_END_TIME) } doReturn listOf(
                Bucket(
                    uid = UID,
                    state = NetworkStats.Bucket.STATE_DEFAULT,
                    bytes = BACKGROUND_USAGE,
                ),
                Bucket(
                    uid = UID,
                    state = NetworkStats.Bucket.STATE_FOREGROUND,
                    bytes = FOREGROUND_USAGE,
                ),
            )
        }
        val repository = AppDataUsageDetailsRepository(
            context = context,
            cycles = null,
            template = template,
            uids = listOf(UID),
            networkCycleDataRepository = networkCycleDataRepository,
            networkStatsRepository = networkStatsRepository,
        )

        val detailsForCycles = repository.queryDetailsForCycles()

        assertThat(detailsForCycles).containsExactly(
            NetworkUsageDetailsData(
                range = Range(CYCLE1_END_TIME, CYCLE2_END_TIME),
                totalUsage = BACKGROUND_USAGE + FOREGROUND_USAGE,
                foregroundUsage = FOREGROUND_USAGE,
                backgroundUsage = BACKGROUND_USAGE,
            )
        )
    }

    private companion object {
        const val CYCLE1_START_TIME = 1694444444000L
        const val CYCLE1_END_TIME = 1695555555000L
        const val CYCLE2_END_TIME = 1695566666000L
        const val UID = 10000

        const val BACKGROUND_USAGE = 8L
        const val FOREGROUND_USAGE = 2L
    }
}
