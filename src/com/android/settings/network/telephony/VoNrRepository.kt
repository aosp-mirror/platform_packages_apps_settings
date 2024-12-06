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

class VoNrRepository(
    private val context: Context,
    private val nrRepository: NrRepository = NrRepository(context),
) {
    private val carrierConfigRepository = CarrierConfigRepository(context)

    fun isVoNrAvailable(subId: Int): Boolean {
        if (!nrRepository.isNrAvailable(subId)) return false
        data class Config(val isVoNrEnabled: Boolean, val isVoNrSettingVisibility: Boolean)

        val carrierConfig =
            carrierConfigRepository.transformConfig(subId) {
                Config(
                    isVoNrEnabled = getBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL),
                    isVoNrSettingVisibility =
                    getBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL),
                )
            }
        return carrierConfig.isVoNrEnabled && carrierConfig.isVoNrSettingVisibility
    }

    fun isVoNrEnabledFlow(subId: Int): Flow<Boolean> {
        val telephonyManager = context.telephonyManager(subId)
        return context
            .subscriptionsChangedFlow()
            .map {
                try {
                    telephonyManager.isVoNrEnabled
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "IllegalStateException - isVoNrEnabled : $e")
                    false
                }
            }
            .conflate()
            .onEach { Log.d(TAG, "[$subId] isVoNrEnabled: $it") }
            .flowOn(Dispatchers.Default)
    }

    suspend fun setVoNrEnabled(subId: Int, enabled: Boolean) =
        withContext(Dispatchers.Default) {
            if (!SubscriptionManager.isValidSubscriptionId(subId)) return@withContext
            var result = TelephonyManager.ENABLE_VONR_RADIO_INVALID_STATE
            try {
                result = context.telephonyManager(subId).setVoNrEnabled(enabled)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "IllegalStateException - setVoNrEnabled : $e")
            } finally {
                Log.d(TAG, "[$subId] setVoNrEnabled: $enabled, result: $result")
            }
        }

    private companion object {
        private const val TAG = "VoNrRepository"
    }
}