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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class AppDataUsageSummaryRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val template = mock<NetworkTemplate>()

    private val networkStatsRepository = mock<NetworkStatsRepository> {
        on {
            queryAggregateForUid(range = NetworkStatsRepository.AllTimeRange, uid = APP_UID)
        } doReturn NetworkUsageData(APP_START_TIME, APP_END_TIME, APP_USAGE)

        on {
            queryAggregateForUid(range = NetworkStatsRepository.AllTimeRange, uid = SDK_SANDBOX_UID)
        } doReturn NetworkUsageData(SDK_SANDBOX_START_TIME, SDK_SANDBOX_END_TIME, SDK_SANDBOX_USAGE)
    }

    private val repository =
        AppDataUsageSummaryRepository(context, template, networkStatsRepository)

    @Test
    fun querySummary(): Unit = runBlocking {
        val networkUsageData = repository.querySummary(APP_UID)

        assertThat(networkUsageData).isEqualTo(
            NetworkUsageData(
                startTime = APP_START_TIME,
                endTime = SDK_SANDBOX_END_TIME,
                usage = APP_USAGE + SDK_SANDBOX_USAGE,
            )
        )
    }

    private companion object {
        const val APP_UID = 10000
        const val APP_START_TIME = 10L
        const val APP_END_TIME = 30L
        const val APP_USAGE = 3L

        const val SDK_SANDBOX_UID = 20000
        const val SDK_SANDBOX_START_TIME = 20L
        const val SDK_SANDBOX_END_TIME = 40L
        const val SDK_SANDBOX_USAGE = 5L
    }
}
