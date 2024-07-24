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
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.NetworkTemplate
import android.util.Log
import android.util.Range

class NetworkStatsRepository(context: Context, private val template: NetworkTemplate) {
    private val networkStatsManager = context.getSystemService(NetworkStatsManager::class.java)!!

    fun queryAggregateForUid(
        range: Range<Long>,
        uid: Int,
        state: Int = NetworkStats.Bucket.STATE_ALL,
    ): NetworkUsageData? = try {
        networkStatsManager.queryDetailsForUidTagState(
            template, range.lower, range.upper, uid, NetworkStats.Bucket.TAG_NONE, state,
        ).convertToBuckets().aggregate()
    } catch (e: Exception) {
        Log.e(TAG, "Exception queryDetailsForUidTagState", e)
        null
    }

    fun queryDetailsForDevice(): List<Bucket> = try {
        networkStatsManager.queryDetailsForDevice(template, Long.MIN_VALUE, Long.MAX_VALUE)
            .convertToBuckets()
    } catch (e: Exception) {
        Log.e(TAG, "Exception queryDetailsForDevice", e)
        emptyList()
    }

    fun getTimeRange(): Range<Long>? = queryDetailsForDevice().aggregate()?.timeRange

    fun querySummaryForDevice(startTime: Long, endTime: Long): Long = try {
        networkStatsManager.querySummaryForDevice(template, startTime, endTime).bytes
    } catch (e: Exception) {
        Log.e(TAG, "Exception querySummaryForDevice", e)
        0
    }

    fun queryBuckets(startTime: Long, endTime: Long): List<Bucket> = try {
        networkStatsManager.querySummary(template, startTime, endTime).convertToBuckets()
    } catch (e: Exception) {
        Log.e(TAG, "Exception querySummary", e)
        emptyList()
    }

    companion object {
        private const val TAG = "NetworkStatsRepository"

        val AllTimeRange = Range(Long.MIN_VALUE, Long.MAX_VALUE)

        data class Bucket(
            val uid: Int,
            val bytes: Long,
            val state: Int = NetworkStats.Bucket.STATE_ALL,
            val startTimeStamp: Long,
            val endTimeStamp: Long,
        )

        fun List<Bucket>.aggregate(): NetworkUsageData? = when {
            isEmpty() -> null
            else -> NetworkUsageData(
                startTime = minOf { it.startTimeStamp },
                endTime = maxOf { it.endTimeStamp },
                usage = sumOf { it.bytes },
            )
        }

        fun List<Bucket>.filterTime(startTime: Long, endTime: Long): List<Bucket> = filter {
            it.startTimeStamp >= startTime && it.endTimeStamp <= endTime
        }

        private fun NetworkStats.convertToBuckets(): List<Bucket> = use {
            val buckets = mutableListOf<Bucket>()
            val bucket = NetworkStats.Bucket()
            while (getNextBucket(bucket)) {
                buckets += Bucket(
                    uid = bucket.uid,
                    bytes = bucket.bytes,
                    state = bucket.state,
                    startTimeStamp = bucket.startTimeStamp,
                    endTimeStamp = bucket.endTimeStamp,
                )
            }
            buckets
        }

        private val NetworkStats.Bucket.bytes: Long
            get() = rxBytes + txBytes
    }
}
