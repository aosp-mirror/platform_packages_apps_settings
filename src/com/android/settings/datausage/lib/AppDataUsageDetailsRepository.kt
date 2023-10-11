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
import androidx.annotation.VisibleForTesting
import com.android.settings.datausage.lib.AppDataUsageRepository.Companion.withSdkSandboxUids
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

interface IAppDataUsageDetailsRepository {
    suspend fun queryDetailsForCycles(): List<NetworkUsageDetailsData>
}

class AppDataUsageDetailsRepository @JvmOverloads constructor(
    context: Context,
    private val template: NetworkTemplate,
    private val cycles: List<Long>?,
    uids: List<Int>,
    private val networkCycleDataRepository: INetworkCycleDataRepository =
        NetworkCycleDataRepository(context, template),
) : IAppDataUsageDetailsRepository {
    private val withSdkSandboxUids = withSdkSandboxUids(uids)
    private val networkStatsRepository = NetworkStatsRepository(context, template)

    override suspend fun queryDetailsForCycles(): List<NetworkUsageDetailsData> = coroutineScope {
        getCycles().map {
            async {
                queryDetails(it)
            }
        }.awaitAll().filter { it.totalUsage > 0 }
    }

    private fun getCycles(): List<Range<Long>> =
        cycles?.zipWithNext { endTime, startTime -> Range(startTime, endTime) }
            ?: networkCycleDataRepository.getCycles()

    private fun queryDetails(range: Range<Long>): NetworkUsageDetailsData {
        var totalUsage = 0L
        var foregroundUsage = 0L
        for (uid in withSdkSandboxUids) {
            val usage = getUsage(range, uid, NetworkStats.Bucket.STATE_ALL)
            if (usage > 0L) {
                totalUsage += usage
                foregroundUsage += getUsage(range, uid, NetworkStats.Bucket.STATE_FOREGROUND)
            }
        }
        return NetworkUsageDetailsData(
            range = range,
            totalUsage = totalUsage,
            foregroundUsage = foregroundUsage,
            backgroundUsage = totalUsage - foregroundUsage,
        )
    }

    @VisibleForTesting
    fun getUsage(range: Range<Long>, uid: Int, state: Int): Long =
        networkStatsRepository.queryAggregateForUid(range, uid, state)?.usage ?: 0
}
