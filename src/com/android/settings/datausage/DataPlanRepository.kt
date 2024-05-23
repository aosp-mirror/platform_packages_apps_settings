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
import com.android.settings.datausage.lib.INetworkCycleDataRepository
import com.android.settings.datausage.lib.NetworkCycleDataRepository.Companion.getCycles
import com.android.settings.datausage.lib.NetworkStatsRepository

interface DataPlanRepository {
    fun getDataPlanInfo(policy: NetworkPolicy, plans: List<SubscriptionPlan>): DataPlanInfo
}

class DataPlanRepositoryImpl(
    private val networkCycleDataRepository: INetworkCycleDataRepository,
) : DataPlanRepository {
    override fun getDataPlanInfo(
        policy: NetworkPolicy,
        plans: List<SubscriptionPlan>,
    ): DataPlanInfo {
        getPrimaryPlan(plans)?.let { primaryPlan ->
            val dataPlanSize = when (primaryPlan.dataLimitBytes) {
                SubscriptionPlan.BYTES_UNLIMITED -> SubscriptionPlan.BYTES_UNKNOWN
                else -> primaryPlan.dataLimitBytes
            }
            return DataPlanInfo(
                dataPlanCount = plans.size,
                dataPlanSize = dataPlanSize,
                dataBarSize = dataPlanSize,
                dataPlanUse = primaryPlan.dataUsageBytes,
                cycleEnd = primaryPlan.cycleRule.end?.toInstant()?.toEpochMilli(),
                snapshotTime = primaryPlan.dataUsageTime,
            )
        }

        val cycle = policy.getCycles().firstOrNull()
        val dataUsage = networkCycleDataRepository.queryUsage(
            cycle ?: NetworkStatsRepository.AllTimeRange
        ).usage
        return DataPlanInfo(
            dataPlanCount = 0,
            dataPlanSize = SubscriptionPlan.BYTES_UNKNOWN,
            dataBarSize = maxOf(dataUsage, policy.limitBytes, policy.warningBytes),
            dataPlanUse = dataUsage,
            cycleEnd = cycle?.upper,
            snapshotTime = SubscriptionPlan.TIME_UNKNOWN,
        )
    }

    companion object {
        private const val PETA = 1_000_000_000_000_000L

        private fun getPrimaryPlan(plans: List<SubscriptionPlan>): SubscriptionPlan? =
            plans.firstOrNull()?.takeIf { plan ->
                plan.dataLimitBytes > 0 && validSize(plan.dataUsageBytes) && plan.cycleRule != null
            }

        private fun validSize(value: Long): Boolean = value in 0L until PETA
    }
}
