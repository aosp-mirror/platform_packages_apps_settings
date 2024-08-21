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
import android.telephony.ims.ImsMmTelManager.WiFiCallingMode
import android.telephony.ims.feature.MmTelFeature
import android.telephony.ims.stub.ImsRegistrationImplBase
import androidx.lifecycle.LifecycleOwner
import com.android.settings.network.telephony.ims.ImsFeatureRepository
import com.android.settings.network.telephony.ims.ImsMmTelRepository
import com.android.settings.network.telephony.ims.ImsMmTelRepositoryImpl
import com.android.settings.network.telephony.telephonyManager
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface IWifiCallingRepository {
    /** TODO: Move this to UI layer, when UI layer migrated to Kotlin. */
    fun collectIsWifiCallingReadyFlow(lifecycleOwner: LifecycleOwner, action: (Boolean) -> Unit)
}

class WifiCallingRepository
@JvmOverloads
constructor(
    private val context: Context,
    private val subId: Int,
    private val imsFeatureRepository: ImsFeatureRepository = ImsFeatureRepository(context, subId),
    private val imsMmTelRepository: ImsMmTelRepository = ImsMmTelRepositoryImpl(context, subId),
) : IWifiCallingRepository {
    private val telephonyManager = context.telephonyManager(subId)

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

    /** TODO: Move this to UI layer, when UI layer migrated to Kotlin. */
    override fun collectIsWifiCallingReadyFlow(
        lifecycleOwner: LifecycleOwner,
        action: (Boolean) -> Unit,
    ) {
        wifiCallingReadyFlow().collectLatestWithLifecycle(lifecycleOwner, action = action)
    }

    fun wifiCallingReadyFlow(): Flow<Boolean> =
        imsFeatureRepository.isReadyFlow(
            capability = MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
            tech = ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN,
            transportType = AccessNetworkConstants.TRANSPORT_TYPE_WLAN,
        )

    suspend fun isWifiCallingSupported(): Boolean = withContext(Dispatchers.Default) {
        imsMmTelRepository.isSupported(
            capability = MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
            transportType = AccessNetworkConstants.TRANSPORT_TYPE_WLAN,
        )
    }
}
