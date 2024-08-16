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

package com.android.settings.wifi.repository

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkScoreManager
import android.net.wifi.WifiManager
import com.android.settingslib.spaprivileged.framework.common.broadcastReceiverFlow
import com.android.settingslib.wifi.WifiStatusTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Repository that listeners to wifi callback and provide wifi status flow to client. */
class WifiStatusRepository(
    private val context: Context,
    private val wifiStatusTrackerFactory: (callback: Runnable) -> WifiStatusTracker = { callback ->
        WifiStatusTracker(
            context,
            context.getSystemService(WifiManager::class.java),
            context.getSystemService(NetworkScoreManager::class.java),
            context.getSystemService(ConnectivityManager::class.java),
            callback,
        )
    },
) {
    fun wifiStatusTrackerFlow(): Flow<WifiStatusTracker> =
        callbackFlow {
                var wifiStatusTracker: WifiStatusTracker? = null
                wifiStatusTracker = wifiStatusTrackerFactory { wifiStatusTracker?.let(::trySend) }

                // Fetches initial state first, before set listening to true, otherwise could cause
                // race condition.
                wifiStatusTracker.fetchInitialState()
                trySend(wifiStatusTracker)

                context
                    .broadcastReceiverFlow(INTENT_FILTER)
                    .onEach { intent ->
                        wifiStatusTracker.handleBroadcast(intent)
                        trySend(wifiStatusTracker)
                    }
                    .launchIn(this)

                wifiStatusTracker.setListening(true)

                awaitClose { wifiStatusTracker.setListening(false) }
            }
            .conflate()
            .flowOn(Dispatchers.Default)

    private companion object {
        val INTENT_FILTER =
            IntentFilter().apply {
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                addAction(WifiManager.RSSI_CHANGED_ACTION)
            }
    }
}
