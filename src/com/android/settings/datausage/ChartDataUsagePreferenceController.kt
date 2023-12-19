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
import androidx.preference.PreferenceScreen
import com.android.settings.core.BasePreferenceController
import com.android.settings.datausage.lib.INetworkCycleDataRepository
import com.android.settings.datausage.lib.NetworkCycleChartData
import com.android.settings.datausage.lib.NetworkCycleDataRepository

class ChartDataUsagePreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    private lateinit var repository: INetworkCycleDataRepository
    private lateinit var preference: ChartDataUsagePreference

    fun init(template: NetworkTemplate) {
        this.repository = NetworkCycleDataRepository(mContext, template)
    }

    override fun getAvailabilityStatus() = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    /**
     * Sets whether billing cycle modifiable.
     *
     * Don't bind warning / limit sweeps if not modifiable.
     */
    fun setBillingCycleModifiable(isModifiable: Boolean) {
        preference.setNetworkPolicy(
            if (isModifiable) repository.getPolicy() else null
        )
    }

    /** Updates chart to show selected cycle. */
    fun update(chartData: NetworkCycleChartData) {
        preference.setTime(chartData.total.startTime, chartData.total.endTime)
        preference.setNetworkCycleData(chartData)
    }
}
