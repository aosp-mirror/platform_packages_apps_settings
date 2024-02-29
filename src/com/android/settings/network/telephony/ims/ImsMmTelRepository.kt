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
import android.telephony.AccessNetworkConstants
import android.telephony.ims.ImsManager
import android.telephony.ims.ImsMmTelManager
import android.telephony.ims.ImsMmTelManager.WiFiCallingMode
import android.telephony.ims.ImsStateCallback
import android.telephony.ims.feature.MmTelFeature
import android.util.Log
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

interface ImsMmTelRepository {
    @WiFiCallingMode
    fun getWiFiCallingMode(useRoamingMode: Boolean): Int
    fun imsReadyFlow(): Flow<Boolean>
    suspend fun isSupported(
        @MmTelFeature.MmTelCapabilities.MmTelCapability capability: Int,
        @AccessNetworkConstants.TransportType transportType: Int,
    ): Boolean
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

    override fun imsReadyFlow(): Flow<Boolean> = callbackFlow {
        val callback = object : ImsStateCallback() {
            override fun onAvailable() {
                Log.d(TAG, "[$subId] IMS onAvailable")
                trySend(true)
            }

            override fun onError() {
                Log.d(TAG, "[$subId] IMS onError")
                trySend(false)
            }

            override fun onUnavailable(reason: Int) {
                Log.d(TAG, "[$subId] IMS onUnavailable")
                trySend(false)
            }
        }

        imsMmTelManager.registerImsStateCallback(Dispatchers.Default.asExecutor(), callback)

        awaitClose { imsMmTelManager.unregisterImsStateCallback(callback) }
    }.catch { e ->
        Log.w(TAG, "[$subId] error while imsReadyFlow", e)
    }.conflate().flowOn(Dispatchers.Default)

    override suspend fun isSupported(
        @MmTelFeature.MmTelCapabilities.MmTelCapability capability: Int,
        @AccessNetworkConstants.TransportType transportType: Int,
    ): Boolean = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            try {
                imsMmTelManager.isSupported(
                    capability,
                    transportType,
                    Dispatchers.Default.asExecutor(),
                    continuation::resume,
                )
            } catch (e: Exception) {
                continuation.resume(false)
                Log.w(TAG, "[$subId] isSupported failed", e)
            }
        }.also { Log.d(TAG, "[$subId] isSupported = $it") }
    }

    private companion object {
        private const val TAG = "ImsMmTelRepository"
    }
}
