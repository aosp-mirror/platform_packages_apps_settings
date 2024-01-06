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
import android.net.NetworkTemplate
import android.text.format.DateUtils
import android.util.Range
import com.android.settings.datausage.lib.NetworkCycleDataRepository.Companion.bucketRange
import com.android.settings.datausage.lib.NetworkCycleDataRepository.Companion.getCycles
import com.android.settings.datausage.lib.NetworkCycleDataRepository.Companion.reverseBucketRange
import com.android.settings.datausage.lib.NetworkStatsRepository.Companion.Bucket
import com.android.settings.datausage.lib.NetworkStatsRepository.Companion.aggregate
import com.android.settings.datausage.lib.NetworkStatsRepository.Companion.filterTime

class NetworkCycleBucketRepository(
    context: Context,
    networkTemplate: NetworkTemplate,
    private val buckets: List<Bucket>,
    private val networkCycleDataRepository: NetworkCycleDataRepository =
        NetworkCycleDataRepository(context, networkTemplate)
) {

    fun loadCycles(): List<NetworkUsageData> =
        getCycles().map { aggregateUsage(it) }.filter { it.usage > 0 }

    private fun getCycles(): List<Range<Long>> =
        networkCycleDataRepository.getPolicy()?.getCycles() ?: queryCyclesAsFourWeeks()

    private fun queryCyclesAsFourWeeks(): List<Range<Long>> {
        val timeRange = buckets.aggregate()?.timeRange ?: return emptyList()
        return reverseBucketRange(
            startTime = timeRange.lower,
            endTime = timeRange.upper,
            step = DateUtils.WEEK_IN_MILLIS * 4,
        )
    }

    fun queryChartData(usageData: NetworkUsageData) = NetworkCycleChartData(
        total = usageData,
        dailyUsage = bucketRange(
            startTime = usageData.startTime,
            endTime = usageData.endTime,
            step = NetworkCycleChartData.BUCKET_DURATION.inWholeMilliseconds,
        ).map { aggregateUsage(it) },
    )

    private fun aggregateUsage(range: Range<Long>) = NetworkUsageData(
        startTime = range.lower,
        endTime = range.upper,
        usage = buckets.filterTime(range.lower, range.upper).aggregate()?.usage ?: 0,
    )
}
