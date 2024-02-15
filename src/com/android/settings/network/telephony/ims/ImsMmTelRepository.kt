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

package com.android.settings.network.telephony.ims

import android.content.Context
import android.telephony.CarrierConfigManager
import android.telephony.CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL
import android.telephony.TelephonyManager
import android.telephony.ims.ImsManager
import android.telephony.ims.ImsMmTelManager
import android.telephony.ims.ImsMmTelManager.WiFiCallingMode
import android.util.Log

interface ImsMmTelRepository {
    @WiFiCallingMode
    fun getWiFiCallingMode(): Int
}

class ImsMmTelRepositoryImpl(
    context: Context,
    private val subId: Int,
    private val imsMmTelManager: ImsMmTelManager = ImsManager(context).getImsMmTelManager(subId),
) : ImsMmTelRepository {

    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)!!
        .createForSubscriptionId(subId)

    private val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)!!

    @WiFiCallingMode
    override fun getWiFiCallingMode(): Int = try {
        when {
            !imsMmTelManager.isVoWiFiSettingEnabled -> ImsMmTelManager.WIFI_MODE_UNKNOWN

            telephonyManager.isNetworkRoaming && !useWfcHomeModeForRoaming() ->
                imsMmTelManager.getVoWiFiRoamingModeSetting()

            else -> imsMmTelManager.getVoWiFiModeSetting()
        }
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "getWiFiCallingMode failed subId=$subId", e)
        ImsMmTelManager.WIFI_MODE_UNKNOWN
    }

    private fun useWfcHomeModeForRoaming(): Boolean =
        carrierConfigManager
            .getConfigForSubId(subId, KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL)
            .getBoolean(KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL)

    private companion object {
        private const val TAG = "ImsMmTelRepository"
    }
}
