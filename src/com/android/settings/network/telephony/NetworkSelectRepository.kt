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
import android.telephony.CarrierConfigManager
import android.telephony.NetworkRegistrationInfo
import android.telephony.TelephonyManager
import android.telephony.satellite.SatelliteManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetworkSelectRepository(context: Context, private val subId: Int) {
    private val telephonyManager =
        context.getSystemService(TelephonyManager::class.java)!!.createForSubscriptionId(subId)
    private val satelliteManager = context.getSystemService(SatelliteManager::class.java)
    private val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)

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
        var networkList = serviceState.getNetworkRegistrationInfoListForTransportType(
            AccessNetworkConstants.TRANSPORT_TYPE_WWAN
        )
        if (networkList.isEmpty()) return null

        val satellitePlmn = getSatellitePlmns()
        // If connected network is Satellite, filter out
        if (satellitePlmn.isNotEmpty()) {
            val filteredNetworkList = networkList.filter {
                val cellIdentity = it.cellIdentity
                val plmn = cellIdentity?.plmn
                plmn != null && !satellitePlmn.contains(plmn)
            }
            networkList = filteredNetworkList
        }
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

    /**
     * Update satellite PLMNs from the satellite framework.
     */
    private fun getSatellitePlmns(): List<String> {
        val config = carrierConfigManager.getConfigForSubId(
            subId,
            CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL
        )

        val shouldFilter = config.getBoolean(
            CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL,
            true)

        return if (shouldFilter) {
            satelliteManager.getSatellitePlmnsForCarrier(subId)
        } else {
            emptyList();
        }
    }
}
