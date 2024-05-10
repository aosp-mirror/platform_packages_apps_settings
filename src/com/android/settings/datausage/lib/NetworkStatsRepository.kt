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
        ).aggregate()
    } catch (e: Exception) {
        Log.e(TAG, "Exception queryDetailsForUidTagState", e)
        null
    }

    fun getTimeRange(): Range<Long>? = try {
        networkStatsManager.queryDetailsForDevice(template, Long.MIN_VALUE, Long.MAX_VALUE)
            .aggregate()?.timeRange
    } catch (e: Exception) {
        Log.e(TAG, "Exception queryDetailsForDevice", e)
        null
    }

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
        )

        private fun NetworkStats.convertToBuckets(): List<Bucket> = use {
            val buckets = mutableListOf<Bucket>()
            val bucket = NetworkStats.Bucket()
            while (getNextBucket(bucket)) {
                buckets += Bucket(uid = bucket.uid, bytes = bucket.bytes, state = bucket.state)
            }
            buckets
        }

        private fun NetworkStats.aggregate(): NetworkUsageData? = use {
            var startTime = Long.MAX_VALUE
            var endTime = Long.MIN_VALUE
            var usage = 0L
            val bucket = NetworkStats.Bucket()
            while (getNextBucket(bucket)) {
                startTime = startTime.coerceAtMost(bucket.startTimeStamp)
                endTime = endTime.coerceAtLeast(bucket.endTimeStamp)
                usage += bucket.bytes
            }
            when {
                startTime > endTime -> null
                else -> NetworkUsageData(startTime, endTime, usage)
            }
        }

        private val NetworkStats.Bucket.bytes: Long
            get() = rxBytes + txBytes
    }
}
