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

package com.android.settings.network.telephony.wificalling

import android.app.Application
import android.app.settings.SettingsEnums
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.settings.R
import com.android.settings.network.telephony.CarrierConfigRepository
import com.android.settings.network.telephony.DataSubscriptionRepository
import com.android.settings.network.telephony.MobileDataRepository
import com.android.settings.network.telephony.SubscriptionRepository
import com.android.settings.network.telephony.ims.ImsMmTelRepositoryImpl
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus

@OptIn(ExperimentalCoroutinesApi::class)
class CrossSimCallingViewModel(
    private val application: Application,
) : AndroidViewModel(application) {

    private val subscriptionRepository = SubscriptionRepository(application)
    private val dataSubscriptionRepository = DataSubscriptionRepository(application)
    private val mobileDataRepository = MobileDataRepository(application)
    private val carrierConfigRepository = CarrierConfigRepository(application)
    private val scope = viewModelScope + Dispatchers.Default
    private val metricsFeatureProvider = featureFactory.metricsFeatureProvider

    init {
        val resources = application.resources
        if (resources.getBoolean(R.bool.config_auto_data_switch_enables_cross_sim_calling)) {
            combine(
                    subscriptionRepository.activeSubscriptionIdListFlow(),
                    dataSubscriptionRepository.defaultDataSubscriptionIdFlow(),
                ) { activeSubIds, defaultDataSubId ->
                    activeSubIds to crossSimCallNewEnabled(activeSubIds, defaultDataSubId)
                }
                .flatMapLatest { (activeSubIds, newEnabledFlow) ->
                    newEnabledFlow.map { newEnabled -> activeSubIds to newEnabled }
                }
                .distinctUntilChanged()
                .onEach { (activeSubIds, newEnabled) ->
                    updateCrossSimCalling(activeSubIds, newEnabled)
                }
                .launchIn(scope)
        }
    }

    private suspend fun updateCrossSimCalling(activeSubIds: List<Int>, newEnabled: Boolean) {
        metricsFeatureProvider.action(
            application,
            SettingsEnums.ACTION_UPDATE_CROSS_SIM_CALLING_ON_AUTO_DATA_SWITCH_EVENT,
            newEnabled,
        )
        activeSubIds
            .filter { subId -> crossSimAvailable(subId) }
            .forEach { subId ->
                ImsMmTelRepositoryImpl(application, subId).setCrossSimCallingEnabled(newEnabled)
            }
    }

    private suspend fun crossSimAvailable(subId: Int): Boolean =
        WifiCallingRepository(application, subId).isWifiCallingSupported() &&
            carrierConfigRepository.getBoolean(
                subId, CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL)

    private fun crossSimCallNewEnabled(
        activeSubscriptionIdList: List<Int>,
        defaultDataSubId: Int,
    ): Flow<Boolean> {
        if (!SubscriptionManager.isValidSubscriptionId(defaultDataSubId)) return flowOf(false)
        val isMobileDataPolicyEnabledFlows =
            activeSubscriptionIdList
                .filter { subId -> subId != defaultDataSubId }
                .map { subId ->
                    mobileDataRepository.isMobileDataPolicyEnabledFlow(
                        subId, TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH)
                }
        return combine(isMobileDataPolicyEnabledFlows) { true in it }
    }
}
