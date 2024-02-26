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

package com.android.settings.network.telephony.ims

import android.content.Context
import android.telephony.ims.ImsManager
import android.telephony.ims.ImsMmTelManager
import android.telephony.ims.ImsMmTelManager.WiFiCallingMode
import android.util.Log

interface ImsMmTelRepository {
    @WiFiCallingMode
    fun getWiFiCallingMode(useRoamingMode: Boolean): Int
}

class ImsMmTelRepositoryImpl(
    context: Context,
    private val subId: Int,
    private val imsMmTelManager: ImsMmTelManager = ImsManager(context).getImsMmTelManager(subId),
) : ImsMmTelRepository {

    @WiFiCallingMode
    override fun getWiFiCallingMode(useRoamingMode: Boolean): Int = try {
        when {
            !imsMmTelManager.isVoWiFiSettingEnabled -> ImsMmTelManager.WIFI_MODE_UNKNOWN
            useRoamingMode -> imsMmTelManager.getVoWiFiRoamingModeSetting()
            else -> imsMmTelManager.getVoWiFiModeSetting()
        }
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "[$subId] getWiFiCallingMode failed useRoamingMode=$useRoamingMode", e)
        ImsMmTelManager.WIFI_MODE_UNKNOWN
    }

    private companion object {
        private const val TAG = "ImsMmTelRepository"
    }
}
