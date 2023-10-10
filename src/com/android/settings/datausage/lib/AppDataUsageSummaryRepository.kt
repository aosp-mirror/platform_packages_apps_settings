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
import com.android.settings.datausage.lib.AppDataUsageRepository.Companion.withSdkSandboxUids
import com.android.settings.datausage.lib.NetworkStatsRepository.Companion.AllTimeRange
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

interface IAppDataUsageSummaryRepository {
    suspend fun querySummary(uid: Int): NetworkUsageData?
}

class AppDataUsageSummaryRepository(
    context: Context,
    template: NetworkTemplate,
    private val networkStatsRepository: NetworkStatsRepository =
        NetworkStatsRepository(context, template),
) : IAppDataUsageSummaryRepository {

    override suspend fun querySummary(uid: Int): NetworkUsageData? = coroutineScope {
        withSdkSandboxUids(listOf(uid)).map { uid ->
            async {
                networkStatsRepository.queryAggregateForUid(range = AllTimeRange, uid = uid)
            }
        }.awaitAll().filterNotNull().aggregate()
    }
}
