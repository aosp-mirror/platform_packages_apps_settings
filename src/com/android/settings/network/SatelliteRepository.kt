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
import android.os.OutcomeReceiver
import android.telephony.satellite.SatelliteManager
import android.telephony.satellite.SatelliteModemStateCallback
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.Futures.immediateFuture
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.Executor

/**
 * A repository class for interacting with the SatelliteManager API.
 */
class SatelliteRepository(
    private val context: Context,
) {

    /**
     * Checks if the satellite modem is enabled.
     *
     * @param executor The executor to run the asynchronous operation on
     * @return A ListenableFuture that will resolve to `true` if the satellite modem enabled,
     *         `false` otherwise.
     */
    fun requestIsEnabled(executor: Executor): ListenableFuture<Boolean> {
        val satelliteManager: SatelliteManager? =
            context.getSystemService(SatelliteManager::class.java)
        if (satelliteManager == null) {
            Log.w(TAG, "SatelliteManager is null")
            return immediateFuture(false)
        }

        return CallbackToFutureAdapter.getFuture { completer ->
            satelliteManager.requestIsEnabled(executor,
                object : OutcomeReceiver<Boolean, SatelliteManager.SatelliteException> {
                    override fun onResult(result: Boolean) {
                        Log.i(TAG, "Satellite modem enabled status: $result")
                        completer.set(result)
                    }

                    override fun onError(error: SatelliteManager.SatelliteException) {
                        super.onError(error)
                        Log.w(TAG, "Can't get satellite modem enabled status", error)
                        completer.set(false)
                    }
                })
            "requestIsEnabled"
        }
    }

    /**
     * Checks if a satellite session has started.
     *
     * @param executor The executor to run the asynchronous operation on
     * @return A ListenableFuture that will resolve to `true` if a satellite session has started,
     *         `false` otherwise.
     */
    fun requestIsSessionStarted(executor: Executor): ListenableFuture<Boolean> {
        val satelliteManager: SatelliteManager? =
            context.getSystemService(SatelliteManager::class.java)
        if (satelliteManager == null) {
            Log.w(TAG, "SatelliteManager is null")
            return immediateFuture(false)
        }

        return CallbackToFutureAdapter.getFuture { completer ->
            val callback = object : SatelliteModemStateCallback {
                override fun onSatelliteModemStateChanged(state: Int) {
                    val isSessionStarted = isSatelliteSessionStarted(state)
                    Log.i(TAG, "Satellite modem state changed: state=$state"
                            + ", isSessionStarted=$isSessionStarted")
                    completer.set(isSessionStarted)
                    satelliteManager.unregisterForModemStateChanged(this)
                }
            }

            val registerResult = satelliteManager.registerForModemStateChanged(executor, callback)
            if (registerResult != SatelliteManager.SATELLITE_RESULT_SUCCESS) {
                Log.w(TAG, "Failed to register for satellite modem state change: $registerResult")
                completer.set(false)
            }
            "requestIsSessionStarted"
        }
    }

    /**
     * Provides a Flow that emits the enabled state of the satellite modem. Updates are triggered
     * when the modem state changes.
     *
     * @param defaultDispatcher The CoroutineDispatcher to use (Defaults to `Dispatchers.Default`).
     * @return A Flow emitting `true` when the modem is enabled and `false` otherwise.
     */
    fun getIsModemEnabledFlow(
        defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): Flow<Boolean> {
        val satelliteManager: SatelliteManager? =
            context.getSystemService(SatelliteManager::class.java)
        if (satelliteManager == null) {
            Log.w(TAG, "SatelliteManager is null")
            return flowOf(false)
        }

        return callbackFlow {
            val callback = SatelliteModemStateCallback { state ->
                val isEnabled = convertSatelliteModemStateToEnabledState(state)
                Log.i(TAG, "Satellite modem state changed: state=$state, isEnabled=$isEnabled")
                trySend(isEnabled)
            }

            val result = satelliteManager.registerForModemStateChanged(
                defaultDispatcher.asExecutor(),
                callback
            )
            Log.i(TAG, "Call registerForModemStateChanged: result=$result")

            awaitClose { satelliteManager.unregisterForModemStateChanged(callback) }
        }
    }

    /**
     * Converts a [SatelliteManager.SatelliteModemState] to a boolean representing whether the modem
     * is enabled.
     *
     * @param state The SatelliteModemState provided by the SatelliteManager.
     * @return `true` if the modem is enabled, `false` otherwise.
     */
    @VisibleForTesting
    fun convertSatelliteModemStateToEnabledState(
        @SatelliteManager.SatelliteModemState state: Int,
    ): Boolean {
        // Mapping table based on logic from b/315928920#comment24
        return when (state) {
            SatelliteManager.SATELLITE_MODEM_STATE_IDLE,
            SatelliteManager.SATELLITE_MODEM_STATE_LISTENING,
            SatelliteManager.SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING,
            SatelliteManager.SATELLITE_MODEM_STATE_DATAGRAM_RETRYING,
            SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED,
            SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED -> true
            else -> false
        }
    }

    companion object {
        private const val TAG: String = "SatelliteRepository"
    }

    /**
     * Check if the modem is in a satellite session.
     *
     * @param state The SatelliteModemState provided by the SatelliteManager.
     * @return `true` if the modem is in a satellite session, `false` otherwise.
     */
    fun isSatelliteSessionStarted(@SatelliteManager.SatelliteModemState state: Int): Boolean {
        return when (state) {
            SatelliteManager.SATELLITE_MODEM_STATE_OFF,
            SatelliteManager.SATELLITE_MODEM_STATE_UNAVAILABLE,
            SatelliteManager.SATELLITE_MODEM_STATE_UNKNOWN -> false
            else -> true
        }
    }
}
