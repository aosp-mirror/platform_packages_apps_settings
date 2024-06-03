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
import com.android.settings.network.mobileDataEnabledFlow
import com.android.settings.network.telephony.ims.ImsMmTelRepositoryImpl
import com.android.settings.network.telephony.requireSubscriptionManager
import com.android.settings.network.telephony.safeGetConfig
import com.android.settings.network.telephony.subscriptionsChangedFlow
import com.android.settings.network.telephony.telephonyManager
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.plus

@OptIn(ExperimentalCoroutinesApi::class)
class CrossSimCallingViewModel(
    private val application: Application,
) : AndroidViewModel(application) {

    private val subscriptionManager = application.requireSubscriptionManager()
    private val carrierConfigManager =
        application.getSystemService(CarrierConfigManager::class.java)!!
    private val scope = viewModelScope + Dispatchers.Default
    private val metricsFeatureProvider = featureFactory.metricsFeatureProvider
    private val updateChannel = Channel<Unit>()

    init {
        val resources = application.resources
        if (resources.getBoolean(R.bool.config_auto_data_switch_enables_cross_sim_calling)) {
            application.subscriptionsChangedFlow()
                .flatMapLatest {
                    val activeSubIds = subscriptionManager.activeSubscriptionIdList.toList()
                    merge(
                        activeSubIds.anyMobileDataEnableChangedFlow(),
                        updateChannel.receiveAsFlow(),
                    ).map {
                        activeSubIds to crossSimCallNewEnabled(activeSubIds)
                    }
                }
                .distinctUntilChanged()
                .onEach { (activeSubIds, newEnabled) ->
                    updateCrossSimCalling(activeSubIds, newEnabled)
                }
                .launchIn(scope)
        }
    }

    fun updateCrossSimCalling() {
        updateChannel.trySend(Unit)
    }

    private fun List<Int>.anyMobileDataEnableChangedFlow() = map { subId ->
        application.mobileDataEnabledFlow(subId = subId, sendInitialValue = false)
    }.merge()

    private suspend fun updateCrossSimCalling(activeSubIds: List<Int>, newEnabled: Boolean) {
        metricsFeatureProvider.action(
            application,
            SettingsEnums.ACTION_UPDATE_CROSS_SIM_CALLING_ON_AUTO_DATA_SWITCH_EVENT,
            newEnabled,
        )
        activeSubIds.filter { crossSimAvailable(it) }.forEach { subId ->
            ImsMmTelRepositoryImpl(application, subId)
                .setCrossSimCallingEnabled(newEnabled)
        }
    }

    private suspend fun crossSimAvailable(subId: Int): Boolean =
        WifiCallingRepository(application, subId).isWifiCallingSupported() &&
            crossSimImsAvailable(subId)

    private fun crossSimImsAvailable(subId: Int): Boolean =
        carrierConfigManager.safeGetConfig(
            keys = listOf(CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL),
            subId = subId,
        ).getBoolean(CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL, false)

    private fun crossSimCallNewEnabled(activeSubscriptionIdList: List<Int>): Boolean {
        val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        return SubscriptionManager.isValidSubscriptionId(defaultDataSubId) &&
            activeSubscriptionIdList.any { subId ->
                subId != defaultDataSubId &&
                    application.telephonyManager(subId).isMobileDataPolicyEnabled(
                        TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH
                    )
            }
    }
}
