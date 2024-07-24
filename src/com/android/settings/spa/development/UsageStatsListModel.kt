/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.spa.development

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import com.android.settings.R
import com.android.settings.spa.development.UsageStatsListModel.SpinnerItem.Companion.toSpinnerItem
import com.android.settingslib.spa.widget.ui.SpinnerOption
import com.android.settingslib.spaprivileged.model.app.AppEntry
import com.android.settingslib.spaprivileged.model.app.AppListModel
import com.android.settingslib.spaprivileged.model.app.AppRecord
import java.text.DateFormat
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class UsageStatsAppRecord(
    override val app: ApplicationInfo,
    val usageStats: UsageStats?,
) : AppRecord

class UsageStatsListModel(private val context: Context) : AppListModel<UsageStatsAppRecord> {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val now = System.currentTimeMillis()

    override fun transform(
        userIdFlow: Flow<Int>,
        appListFlow: Flow<List<ApplicationInfo>>,
    ) = userIdFlow.map { getUsageStats() }
        .combine(appListFlow) { usageStatsMap, appList ->
            appList.map { app -> UsageStatsAppRecord(app, usageStatsMap[app.packageName]) }
        }

    override fun getSpinnerOptions(recordList: List<UsageStatsAppRecord>): List<SpinnerOption> =
        SpinnerItem.entries.map {
            SpinnerOption(
                id = it.ordinal,
                text = context.getString(it.stringResId),
            )
        }

    override fun filter(
        userIdFlow: Flow<Int>,
        option: Int,
        recordListFlow: Flow<List<UsageStatsAppRecord>>,
    ) = recordListFlow.map { recordList ->
        recordList.filter { it.usageStats != null }
    }

    override fun getComparator(option: Int) = when (option.toSpinnerItem()) {
        SpinnerItem.UsageTime -> compareByDescending { it.record.usageStats?.totalTimeInForeground }
        SpinnerItem.LastTimeUsed -> compareByDescending { it.record.usageStats?.lastTimeUsed }
        else -> compareBy<AppEntry<UsageStatsAppRecord>> { 0 }
    }.then(super.getComparator(option))

    @Composable
    override fun getSummary(option: Int, record: UsageStatsAppRecord): (() -> String)? {
        val usageStats = record.usageStats ?: return null
        val lastTimeUsedLine =
            "${context.getString(R.string.last_time_used_label)}: ${usageStats.getLastUsedString()}"
        val usageTime = DateUtils.formatElapsedTime(usageStats.totalTimeInForeground / 1000)
        val usageTimeLine = "${context.getString(R.string.usage_time_label)}: $usageTime"
        return { "$lastTimeUsedLine\n$usageTimeLine" }
    }

    private fun UsageStats.getLastUsedString() = when {
        lastTimeUsed < Duration.ofDays(1)
            .toMillis() -> context.getString(R.string.last_time_used_never)

        else -> DateUtils.formatSameDayTime(
            lastTimeUsed,
            now,
            DateFormat.MEDIUM,
            DateFormat.MEDIUM
        )
    }

    private fun getUsageStats(): Map<String, UsageStats> {
        val startTime = now - TimeUnit.DAYS.toMillis(5)

        return usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, now)
            .groupingBy { it.packageName }.reduce { _, a, b -> a.add(b); a }
    }

    private enum class SpinnerItem(val stringResId: Int) {
        UsageTime(R.string.usage_stats_sort_by_usage_time),
        LastTimeUsed(R.string.usage_stats_sort_by_last_time_used),
        AppName(R.string.usage_stats_sort_by_app_name);

        companion object {
            fun Int.toSpinnerItem(): SpinnerItem = entries[this]
        }
    }
}
