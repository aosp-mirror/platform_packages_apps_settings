/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.network.ethernet

import android.content.Context
import android.net.ConnectivityManager
import android.net.EthernetManager
import android.net.EthernetManager.STATE_ABSENT
import android.net.EthernetNetworkManagementException
import android.net.EthernetNetworkUpdateRequest
import android.net.IpConfiguration
import android.os.OutcomeReceiver
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class EthernetInterface(private val context: Context, private val id: String) :
    EthernetManager.InterfaceStateListener {
    private val ethernetManager =
        context.getSystemService(EthernetManager::class.java)
    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)
    private val executor = ContextCompat.getMainExecutor(context)

    private val TAG = "EthernetInterface"

    private var interfaceState = STATE_ABSENT
    private var ipConfiguration = IpConfiguration()

    fun getInterfaceState() = interfaceState

    fun getConfiguration(): IpConfiguration {
        return ipConfiguration
    }

    fun setConfiguration(ipConfiguration: IpConfiguration) {
        val request =
            EthernetNetworkUpdateRequest.Builder().setIpConfiguration(ipConfiguration).build()
        ethernetManager.updateConfiguration(
            id,
            request,
            executor,
            object : OutcomeReceiver<String, EthernetNetworkManagementException> {
                override fun onError(e: EthernetNetworkManagementException) {
                    Log.e(TAG, "Failed to updateConfiguration: ", e)
                }

                override fun onResult(id: String) {
                    Log.d(TAG, "Successfully updated configuration: " + id)
                }
            },
        )
    }

    override fun onInterfaceStateChanged(id: String, state: Int, role: Int, cfg: IpConfiguration?) {
        if (id == this.id) {
            ipConfiguration = cfg ?: IpConfiguration()
            interfaceState = state
        }
    }
}
