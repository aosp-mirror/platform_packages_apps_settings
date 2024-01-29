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
import com.android.settings.datausage.lib.AppDataUsageRepository.Companion.withSdkSandboxUids
import com.android.settingslib.spa.framework.util.asyncMap

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
    private val networkStatsRepository: NetworkStatsRepository =
        NetworkStatsRepository(context, template),
) : IAppDataUsageDetailsRepository {
    private val withSdkSandboxUids = withSdkSandboxUids(uids)

    override suspend fun queryDetailsForCycles(): List<NetworkUsageDetailsData> =
        getCycles().asyncMap { queryDetails(it) }.filter { it.totalUsage > 0 }

    private fun getCycles(): List<Range<Long>> =
        cycles?.zipWithNext { endTime, startTime -> Range(startTime, endTime) }
            ?: networkCycleDataRepository.getCycles()

    private fun queryDetails(range: Range<Long>): NetworkUsageDetailsData {
        val buckets = networkStatsRepository.queryBuckets(range.lower, range.upper)
            .filter { it.uid in withSdkSandboxUids }
        val totalUsage = buckets.sumOf { it.bytes }
        val foregroundUsage =
            buckets.filter { it.state == NetworkStats.Bucket.STATE_FOREGROUND }.sumOf { it.bytes }
        return NetworkUsageDetailsData(
            range = range,
            totalUsage = totalUsage,
            foregroundUsage = foregroundUsage,
            backgroundUsage = totalUsage - foregroundUsage,
        )
    }
}
