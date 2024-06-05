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
import android.telephony.AccessNetworkConstants
import android.telephony.NetworkRegistrationInfo
import android.telephony.TelephonyManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetworkSelectRepository(context: Context, subId: Int) {
    private val telephonyManager =
        context.getSystemService(TelephonyManager::class.java)!!.createForSubscriptionId(subId)

    data class NetworkRegistrationAndForbiddenInfo(
        val networkList: List<NetworkRegistrationInfo>,
        val forbiddenPlmns: List<String>,
    )

    /** TODO: Move this to UI layer, when UI layer migrated to Kotlin. */
    fun launchUpdateNetworkRegistrationInfo(
        lifecycleOwner: LifecycleOwner,
        action: (NetworkRegistrationAndForbiddenInfo) -> Unit,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                withContext(Dispatchers.Default) {
                    getNetworkRegistrationInfo()
                }?.let(action)
            }
        }
    }

    fun getNetworkRegistrationInfo(): NetworkRegistrationAndForbiddenInfo? {
        if (telephonyManager.dataState != TelephonyManager.DATA_CONNECTED) return null
        // Try to get the network registration states
        val serviceState = telephonyManager.serviceState ?: return null
        val networkList = serviceState.getNetworkRegistrationInfoListForTransportType(
            AccessNetworkConstants.TRANSPORT_TYPE_WWAN
        )
        if (networkList.isEmpty()) return null
        // Due to the aggregation of cell between carriers, it's possible to get CellIdentity
        // containing forbidden PLMN.
        // Getting current network from ServiceState is no longer a good idea.
        // Add an additional rule to avoid from showing forbidden PLMN to the user.
        return NetworkRegistrationAndForbiddenInfo(networkList, getForbiddenPlmns())
    }

    /**
     * Update forbidden PLMNs from the USIM App
     */
    private fun getForbiddenPlmns(): List<String> {
        return telephonyManager.forbiddenPlmns?.toList() ?: emptyList()
    }
}
