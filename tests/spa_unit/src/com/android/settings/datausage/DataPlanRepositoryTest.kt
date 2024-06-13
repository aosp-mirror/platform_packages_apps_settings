/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.net.NetworkPolicy
import android.telephony.SubscriptionPlan
import android.util.Range
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.datausage.lib.INetworkCycleDataRepository
import com.android.settings.datausage.lib.NetworkUsageData
import com.android.settings.testutils.zonedDateTime
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class DataPlanRepositoryTest {

    private object FakeNetworkCycleDataRepository : INetworkCycleDataRepository {
        override fun getCycles(): List<Range<Long>> = emptyList()
        override fun getPolicy() = null

        override fun queryUsage(range: Range<Long>) = NetworkUsageData(
            startTime = CYCLE_CYCLE_START_TIME,
            endTime = CYCLE_CYCLE_END_TIME,
            usage = CYCLE_BYTES,
        )
    }

    private val repository = DataPlanRepositoryImpl(FakeNetworkCycleDataRepository)

    private val policy = mock<NetworkPolicy> {
        on { cycleIterator() } doReturn listOf(
            Range(zonedDateTime(CYCLE_CYCLE_START_TIME), zonedDateTime(CYCLE_CYCLE_END_TIME)),
        ).iterator()
    }

    @Test
    fun getDataPlanInfo_hasSubscriptionPlan() {
        val dataPlanInfo = repository.getDataPlanInfo(policy, listOf(SUBSCRIPTION_PLAN))

        assertThat(dataPlanInfo).isEqualTo(
            DataPlanInfo(
                dataPlanCount = 1,
                dataPlanSize = DATA_LIMIT_BYTES,
                dataBarSize = DATA_LIMIT_BYTES,
                dataPlanUse = DATA_USAGE_BYTES,
                cycleEnd = PLAN_CYCLE_END_TIME,
                snapshotTime = DATA_USAGE_TIME,
            )
        )
    }

    @Test
    fun getDataPlanInfo_noSubscriptionPlan() {
        val dataPlanInfo = repository.getDataPlanInfo(policy, emptyList())

        assertThat(dataPlanInfo).isEqualTo(
            DataPlanInfo(
                dataPlanCount = 0,
                dataPlanSize = SubscriptionPlan.BYTES_UNKNOWN,
                dataBarSize = CYCLE_BYTES,
                dataPlanUse = CYCLE_BYTES,
                cycleEnd = CYCLE_CYCLE_END_TIME,
                snapshotTime = SubscriptionPlan.TIME_UNKNOWN,
            )
        )
    }

    private companion object {
        const val CYCLE_CYCLE_START_TIME = 1L
        const val CYCLE_CYCLE_END_TIME = 2L
        const val CYCLE_BYTES = 11L

        const val PLAN_CYCLE_START_TIME = 100L
        const val PLAN_CYCLE_END_TIME = 200L
        const val DATA_LIMIT_BYTES = 300L
        const val DATA_USAGE_BYTES = 400L
        const val DATA_USAGE_TIME = 500L

        val SUBSCRIPTION_PLAN: SubscriptionPlan = SubscriptionPlan.Builder.createNonrecurring(
            zonedDateTime(PLAN_CYCLE_START_TIME),
            zonedDateTime(PLAN_CYCLE_END_TIME),
        ).apply {
            setDataLimit(DATA_LIMIT_BYTES, SubscriptionPlan.LIMIT_BEHAVIOR_DISABLED)
            setDataUsage(DATA_USAGE_BYTES, DATA_USAGE_TIME)
        }.build()
    }
}
