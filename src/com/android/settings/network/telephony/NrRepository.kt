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

class NrRepository(private val context: Context) {
    private val carrierConfigRepository = CarrierConfigRepository(context)

    fun isNrAvailable(subId: Int): Boolean {
        if (!SubscriptionManager.isValidSubscriptionId(subId) || !has5gCapability(subId)) {
            return false
        }
        val carrierNrAvailabilities =
            carrierConfigRepository.getIntArray(
                subId = subId,
                key = CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
            )
        return carrierNrAvailabilities?.isNotEmpty() ?: false
    }

    private fun has5gCapability(subId: Int): Boolean {
        val telephonyManager = context.telephonyManager(subId)
        return (telephonyManager.supportedRadioAccessFamily and
                TelephonyManager.NETWORK_TYPE_BITMASK_NR > 0)
            .also { Log.d(TAG, "[$subId] has5gCapability: $it") }
    }

    private companion object {
        private const val TAG = "NrRepository"
    }
}
