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
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.datausage.lib.IAppDataUsageDetailsRepository
import com.android.settings.datausage.lib.NetworkUsageDetailsData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class AppDataUsageCycleControllerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val preference = spy(SpinnerPreference(context, null).apply { key = KEY })

    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    private val controller = AppDataUsageCycleController(context, KEY)

    @Before
    fun setUp() {
        preferenceScreen.addPreference(preference)
    }

    @Test
    fun onViewCreated_noUsage_hidePreference(): Unit = runBlocking {
        val repository = object : IAppDataUsageDetailsRepository {
            override suspend fun queryDetailsForCycles() = emptyList<NetworkUsageDetailsData>()
        }
        controller.displayPreference(preferenceScreen)
        controller.init(repository) {}

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.isVisible).isFalse()
    }

    @Test
    fun onViewCreated_hasUsage_showPreference(): Unit = runBlocking {
        val detailsData = NetworkUsageDetailsData(
            range = Range(1, 2),
            totalUsage = 11,
            foregroundUsage = 1,
            backgroundUsage = 10,
        )
        val repository = object : IAppDataUsageDetailsRepository {
            override suspend fun queryDetailsForCycles() = listOf(detailsData)
        }
        controller.displayPreference(preferenceScreen)
        controller.init(repository) {}

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.isVisible).isTrue()
    }

    @Test
    fun setInitialCycles() {
        val repository = object : IAppDataUsageDetailsRepository {
            override suspend fun queryDetailsForCycles() = emptyList<NetworkUsageDetailsData>()
        }
        controller.displayPreference(preferenceScreen)
        controller.init(repository) {}

        controller.setInitialCycles(
            initialCycles = listOf(CYCLE2_END_TIME, CYCLE1_END_TIME, CYCLE1_START_TIME),
            initialSelectedEndTime = CYCLE1_END_TIME,
        )

        verify(preference).setSelection(1)
    }

    private companion object {
        const val KEY = "test_key"
        const val CYCLE1_START_TIME = 1694444444000L
        const val CYCLE1_END_TIME = 1695555555000L
        const val CYCLE2_END_TIME = 1695566666000L
    }
}
