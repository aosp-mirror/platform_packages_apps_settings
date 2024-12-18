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

package com.android.settings.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn

class ConnectivityRepository(context: Context) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)!!

    fun networkCapabilitiesFlow(): Flow<NetworkCapabilities> = callbackFlow {
        val callback = object : NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(networkCapabilities)
                Log.d(TAG, "onCapabilitiesChanged: $networkCapabilities")
            }

            override fun onLost(network: Network) {
                trySend(NetworkCapabilities())
                Log.d(TAG, "onLost")
            }
        }
        trySend(getNetworkCapabilities())
        connectivityManager.registerDefaultNetworkCallback(callback)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.conflate().flowOn(Dispatchers.Default)

    private fun getNetworkCapabilities(): NetworkCapabilities =
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?: NetworkCapabilities()

    private companion object {
        private const val TAG = "ConnectivityRepository"
    }
}
