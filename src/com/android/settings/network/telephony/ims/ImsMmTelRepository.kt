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
import android.telephony.ims.ImsReasonInfo
import android.telephony.ims.ImsRegistrationAttributes
import android.telephony.ims.ImsStateCallback
import android.telephony.ims.RegistrationManager
import android.telephony.ims.feature.MmTelFeature
import android.util.Log
import androidx.annotation.VisibleForTesting
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

interface ImsMmTelRepository {
    @WiFiCallingMode
    fun getWiFiCallingMode(useRoamingMode: Boolean): Int

    fun imsRegisteredFlow(): Flow<Boolean>

    fun imsReadyFlow(): Flow<Boolean>

    fun isSupportedFlow(
        @MmTelFeature.MmTelCapabilities.MmTelCapability capability: Int,
        @AccessNetworkConstants.TransportType transportType: Int,
    ): Flow<Boolean>

    suspend fun setCrossSimCallingEnabled(enabled: Boolean)
}

/**
 * A repository for the IMS MMTel.
 *
 * @throws IllegalArgumentException if the [subId] is invalid.
 */
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

    override fun imsRegisteredFlow(): Flow<Boolean> = callbackFlow {
        val callback = object : RegistrationManager.RegistrationCallback() {
            override fun onRegistered(attributes: ImsRegistrationAttributes) {
                Log.d(TAG, "[$subId] IMS onRegistered")
                trySend(true)
            }

            override fun onRegistering(imsTransportType: Int) {
                Log.d(TAG, "[$subId] IMS onRegistering")
                trySend(false)
            }

            override fun onTechnologyChangeFailed(imsTransportType: Int, info: ImsReasonInfo) {
                Log.d(TAG, "[$subId] IMS onTechnologyChangeFailed")
                trySend(false)
            }

            override fun onUnregistered(info: ImsReasonInfo) {
                Log.d(TAG, "[$subId] IMS onUnregistered")
                trySend(false)
            }
        }

        imsMmTelManager.registerImsRegistrationCallback(Dispatchers.Default.asExecutor(), callback)

        awaitClose { imsMmTelManager.unregisterImsRegistrationCallback(callback) }
    }.catch { e ->
        Log.w(TAG, "[$subId] error while imsRegisteredFlow", e)
    }.conflate().flowOn(Dispatchers.Default)

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
        emit(false)
    }.conflate().flowOn(Dispatchers.Default)

    override fun isSupportedFlow(capability: Int, transportType: Int): Flow<Boolean> =
        imsReadyFlow().map { imsReady -> imsReady && isSupported(capability, transportType) }

    @VisibleForTesting
    suspend fun isSupported(
        @MmTelFeature.MmTelCapabilities.MmTelCapability capability: Int,
        @AccessNetworkConstants.TransportType transportType: Int,
    ): Boolean = withContext(Dispatchers.Default) {
        val logName = "isSupported(capability=$capability,transportType=$transportType)"
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
                Log.w(TAG, "[$subId] $logName failed", e)
            }
        }.also { Log.d(TAG, "[$subId] $logName = $it") }
    }

    override suspend fun setCrossSimCallingEnabled(enabled: Boolean) {
        try {
            imsMmTelManager.setCrossSimCallingEnabled(enabled)
            Log.d(TAG, "[$subId] setCrossSimCallingEnabled: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "[$subId] failed setCrossSimCallingEnabled to $enabled", e)
        }
    }

    private companion object {
        private const val TAG = "ImsMmTelRepository"
    }
}
