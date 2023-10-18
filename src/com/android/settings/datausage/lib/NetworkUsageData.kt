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
import android.text.format.DateUtils
import android.util.Range
import com.android.settings.R
import com.android.settings.datausage.DataUsageUtils

/**
 * Base data structure representing usage data in a period.
 */
data class NetworkUsageData(
    val startTime: Long,
    val endTime: Long,
    val usage: Long,
) {
    val timeRange = Range(startTime, endTime)

    fun formatStartDate(context: Context): String =
        DateUtils.formatDateTime(context, startTime, DATE_FORMAT)

    fun formatDateRange(context: Context): String =
        DateUtils.formatDateRange(context, startTime, endTime, DATE_FORMAT)

    fun formatUsage(context: Context): CharSequence = DataUsageUtils.formatDataUsage(context, usage)

    fun getDataUsedString(context: Context): String =
        context.getString(R.string.data_used_template, formatUsage(context))

    companion object {
        val AllZero = NetworkUsageData(
            startTime = 0L,
            endTime = 0L,
            usage = 0L,
        )

        private const val DATE_FORMAT = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
    }
}

fun List<NetworkUsageData>.aggregate(): NetworkUsageData? = when {
    isEmpty() -> null
    else -> NetworkUsageData(
        startTime = minOf { it.startTime },
        endTime = maxOf { it.endTime },
        usage = sumOf { it.usage },
    )
}
