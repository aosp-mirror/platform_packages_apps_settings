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

import android.content.Context
import android.telephony.AccessNetworkConstants
import android.telephony.CarrierConfigManager
import android.telephony.CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.ims.ImsMmTelManager.WiFiCallingMode
import android.telephony.ims.feature.MmTelFeature
import android.telephony.ims.stub.ImsRegistrationImplBase
import com.android.settings.network.telephony.ims.ImsMmTelRepository
import com.android.settings.network.telephony.ims.ImsMmTelRepositoryImpl
import com.android.settings.network.telephony.ims.imsFeatureProvisionedFlow
import com.android.settings.network.telephony.subscriptionsChangedFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class WifiCallingRepository(
    private val context: Context,
    private val subId: Int,
    private val imsMmTelRepository : ImsMmTelRepository = ImsMmTelRepositoryImpl(context, subId)
) {
    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)!!
        .createForSubscriptionId(subId)

    private val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)!!

    @WiFiCallingMode
    fun getWiFiCallingMode(): Int {
        val useRoamingMode = telephonyManager.isNetworkRoaming && !useWfcHomeModeForRoaming()
        return imsMmTelRepository.getWiFiCallingMode(useRoamingMode)
    }

    private fun useWfcHomeModeForRoaming(): Boolean =
        carrierConfigManager
            .getConfigForSubId(subId, KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL)
            .getBoolean(KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun wifiCallingReadyFlow(): Flow<Boolean> {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return flowOf(false)
        return context.subscriptionsChangedFlow().flatMapLatest {
            combine(
                imsFeatureProvisionedFlow(
                    subId = subId,
                    capability = MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                    tech = ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN,
                ),
                isWifiCallingSupportedFlow(),
            ) { imsFeatureProvisioned, isWifiCallingSupported ->
                imsFeatureProvisioned && isWifiCallingSupported
            }
        }
    }

    private fun isWifiCallingSupportedFlow(): Flow<Boolean> {
        return imsMmTelRepository.imsReadyFlow().map { imsReady ->
            imsReady && imsMmTelRepository.isSupported(
                capability = MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                transportType = AccessNetworkConstants.TRANSPORT_TYPE_WLAN,
            )
        }
    }
}
