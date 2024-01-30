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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy

@RunWith(AndroidJUnit4::class)
class NetworkStatsRepositoryTest {
    private val template = mock<NetworkTemplate>()

    private val mockNetworkStatsManager = mock<NetworkStatsManager> {
        on { querySummaryForDevice(template, START_TIME, END_TIME) } doReturn BUCKET
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(NetworkStatsManager::class.java) } doReturn mockNetworkStatsManager
    }

    private val repository = NetworkStatsRepository(context, template)

    @Test
    fun querySummaryForDevice() {
        val bytes = repository.querySummaryForDevice(START_TIME, END_TIME)

        assertThat(bytes).isEqualTo(11)
    }

    private companion object {
        const val START_TIME = 1L
        const val END_TIME = 2L

        val BUCKET = mock<NetworkStats.Bucket> {
            on { rxBytes } doReturn 1
            on { txBytes } doReturn 10
        }
    }
}
