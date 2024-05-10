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

package com.android.settings.datausage

import android.content.Context
import android.util.Range
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.datausage.lib.INetworkCycleDataRepository
import com.android.settings.datausage.lib.NetworkCycleChartData
import com.android.settings.datausage.lib.NetworkUsageData
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ChartDataUsagePreferenceControllerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val repository = object : INetworkCycleDataRepository {
        override suspend fun loadCycles() = emptyList<NetworkUsageData>()
        override fun getCycles() = emptyList<Range<Long>>()
        override fun getPolicy() = null

        override suspend fun queryChartData(startTime: Long, endTime: Long) = when {
            startTime == START_TIME && endTime == END_TIME -> CycleChartDate
            else -> null
        }
    }

    private val preference = mock<ChartDataUsagePreference>()
    private val preferenceScreen = mock<PreferenceScreen> {
        onGeneric { findPreference(KEY) } doReturn preference
    }

    private val controller = ChartDataUsagePreferenceController(context, KEY)

    @Before
    fun setUp() {
        controller.init(repository)
        controller.displayPreference(preferenceScreen)
        controller.onViewCreated(TestLifecycleOwner())
    }

    @Test
    fun update() = runBlocking {
        controller.update(START_TIME, END_TIME)
        delay(100L)

        verify(preference).setTime(START_TIME, END_TIME)
        verify(preference).setNetworkCycleData(CycleChartDate)
    }

    private companion object {
        const val KEY = "test_key"
        const val START_TIME = 1L
        const val END_TIME = 2L

        val UsageData = NetworkUsageData(startTime = START_TIME, endTime = END_TIME, usage = 10)
        val CycleChartDate =
            NetworkCycleChartData(total = UsageData, dailyUsage = listOf(UsageData))
    }
}
