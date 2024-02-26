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
import android.telephony.CarrierConfigManager
import android.telephony.CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL
import android.telephony.TelephonyManager
import android.telephony.ims.ImsMmTelManager.WiFiCallingMode
import com.android.settings.network.telephony.ims.ImsMmTelRepository
import com.android.settings.network.telephony.ims.ImsMmTelRepositoryImpl

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
}
