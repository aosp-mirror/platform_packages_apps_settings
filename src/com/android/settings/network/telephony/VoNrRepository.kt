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

package com.android.settings.network.telephony

import android.content.Context
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class VoNrRepository(private val context: Context, private val subId: Int) {
    private val telephonyManager = context.telephonyManager(subId)
    private val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)!!

    fun isVoNrAvailable(): Boolean {
        if (!SubscriptionManager.isValidSubscriptionId(subId) || !has5gCapability()) return false
        val carrierConfig = carrierConfigManager.safeGetConfig(
            keys = listOf(
                CarrierConfigManager.KEY_VONR_ENABLED_BOOL,
                CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL,
                CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
            ),
            subId = subId,
        )
        return carrierConfig.getBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL) &&
            carrierConfig.getBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL) &&
            (carrierConfig.getIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY)
                ?.isNotEmpty() ?: false)
    }

    private fun has5gCapability() =
        ((telephonyManager.supportedRadioAccessFamily and
            TelephonyManager.NETWORK_TYPE_BITMASK_NR) > 0)
            .also { Log.d(TAG, "[$subId] has5gCapability: $it") }

    fun isVoNrEnabledFlow(): Flow<Boolean> = context.subscriptionsChangedFlow()
        .map { telephonyManager.isVoNrEnabled }
        .conflate()
        .onEach { Log.d(TAG, "[$subId] isVoNrEnabled: $it") }
        .flowOn(Dispatchers.Default)

    suspend fun setVoNrEnabled(enabled: Boolean) = withContext(Dispatchers.Default) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return@withContext
        val result = telephonyManager.setVoNrEnabled(enabled)
        Log.d(TAG, "[$subId] setVoNrEnabled: $enabled, result: $result")
    }

    private companion object {
        private const val TAG = "VoNrRepository"
    }
}
