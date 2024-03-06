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
import android.net.NetworkTemplate
import androidx.annotation.OpenForTesting
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceScreen
import com.android.settings.core.BasePreferenceController
import com.android.settings.datausage.lib.INetworkCycleDataRepository
import com.android.settings.datausage.lib.NetworkCycleChartData
import com.android.settings.datausage.lib.NetworkCycleDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OpenForTesting
open class ChartDataUsagePreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    private lateinit var repository: INetworkCycleDataRepository
    private lateinit var preference: ChartDataUsagePreference
    private lateinit var lifecycleScope: LifecycleCoroutineScope
    private var lastStartTime: Long? = null
    private var lastEndTime: Long? = null

    open fun init(template: NetworkTemplate) {
        this.repository = NetworkCycleDataRepository(mContext, template)
    }

    @VisibleForTesting
    fun init(repository: INetworkCycleDataRepository) {
        this.repository = repository
    }

    override fun getAvailabilityStatus() = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        lifecycleScope = viewLifecycleOwner.lifecycleScope
    }

    /**
     * Sets whether billing cycle modifiable.
     *
     * Don't bind warning / limit sweeps if not modifiable.
     */
    open fun setBillingCycleModifiable(isModifiable: Boolean) {
        preference.setNetworkPolicy(
            if (isModifiable) repository.getPolicy() else null
        )
    }

    fun update(startTime: Long, endTime: Long) {
        if (lastStartTime == startTime && lastEndTime == endTime) return
        lastStartTime = startTime
        lastEndTime = endTime

        preference.setTime(startTime, endTime)
        preference.setNetworkCycleData(NetworkCycleChartData.AllZero)
        lifecycleScope.launch {
            val chartData = withContext(Dispatchers.Default) {
                repository.queryChartData(startTime, endTime)
            }
            preference.setNetworkCycleData(chartData)
        }
    }
}
