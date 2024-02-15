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
import android.net.NetworkPolicyManager
import android.net.NetworkTemplate
import android.text.format.DateUtils
import android.util.Range
import com.android.settingslib.NetworkPolicyEditor

interface INetworkCycleDataRepository {
    fun getCycles(): List<Range<Long>>
    fun getPolicy(): NetworkPolicy?
    fun queryUsage(range: Range<Long>): NetworkUsageData
}

class NetworkCycleDataRepository(
    context: Context,
    private val networkTemplate: NetworkTemplate,
    private val networkStatsRepository: NetworkStatsRepository =
        NetworkStatsRepository(context, networkTemplate),
) : INetworkCycleDataRepository {

    private val policyManager = context.getSystemService(NetworkPolicyManager::class.java)!!

    fun loadFirstCycle(): NetworkUsageData? = getCycles().firstOrNull()?.let { queryUsage(it) }

    override fun getCycles(): List<Range<Long>> =
        getPolicy()?.getCycles() ?: queryCyclesAsFourWeeks()

    private fun queryCyclesAsFourWeeks(): List<Range<Long>> {
        val timeRange = networkStatsRepository.getTimeRange() ?: return emptyList()
        return reverseBucketRange(
            startTime = timeRange.lower,
            endTime = timeRange.upper,
            step = DateUtils.WEEK_IN_MILLIS * 4,
        )
    }

    override fun getPolicy(): NetworkPolicy? =
        with(NetworkPolicyEditor(policyManager)) {
            read()
            getPolicy(networkTemplate)
        }


    override fun queryUsage(range: Range<Long>) = NetworkUsageData(
        startTime = range.lower,
        endTime = range.upper,
        usage = networkStatsRepository.querySummaryForDevice(range.lower, range.upper),
    )

    companion object {
        fun NetworkPolicy.getCycles() = cycleIterator().asSequence().map {
            Range(it.lower.toInstant().toEpochMilli(), it.upper.toInstant().toEpochMilli())
        }.toList()

        fun bucketRange(startTime: Long, endTime: Long, step: Long): List<Range<Long>> =
            (startTime..endTime step step).zipWithNext(::Range)

        fun reverseBucketRange(startTime: Long, endTime: Long, step: Long): List<Range<Long>> =
            (endTime downTo (startTime - step + 1) step step)
                .zipWithNext { bucketEnd, bucketStart -> Range(bucketStart, bucketEnd) }
    }
}
