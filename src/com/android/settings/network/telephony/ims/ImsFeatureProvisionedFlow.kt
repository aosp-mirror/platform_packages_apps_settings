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

import android.telephony.ims.ProvisioningManager
import android.telephony.ims.ProvisioningManager.FeatureProvisioningCallback
import android.telephony.ims.feature.MmTelFeature.MmTelCapabilities.MmTelCapability
import android.telephony.ims.stub.ImsRegistrationImplBase.ImsRegistrationTech
import android.util.Log
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach

private const val TAG = "ImsFeatureProvisioned"

fun imsFeatureProvisionedFlow(
    subId: Int,
    @MmTelCapability capability: Int,
    @ImsRegistrationTech tech: Int,
): Flow<Boolean> = imsFeatureProvisionedFlow(
    subId = subId,
    capability = capability,
    tech = tech,
    provisioningManager = ProvisioningManager.createForSubscriptionId(subId),
)

@VisibleForTesting
fun imsFeatureProvisionedFlow(
    subId: Int,
    @MmTelCapability capability: Int,
    @ImsRegistrationTech tech: Int,
    provisioningManager : ProvisioningManager,
): Flow<Boolean> = callbackFlow {
    val callback = object : FeatureProvisioningCallback() {
        override fun onFeatureProvisioningChanged(
            receivedCapability: Int,
            receivedTech: Int,
            isProvisioned: Boolean,
        ) {
            if (capability == receivedCapability && tech == receivedTech) trySend(isProvisioned)
        }

        override fun onRcsFeatureProvisioningChanged(
            capability: Int,
            tech: Int,
            isProvisioned: Boolean,
        ) {
        }
    }

    provisioningManager.registerFeatureProvisioningChangedCallback(
        Dispatchers.Default.asExecutor(),
        callback,
    )
    trySend(provisioningManager.getProvisioningStatusForCapability(capability, tech))

    awaitClose { provisioningManager.unregisterFeatureProvisioningChangedCallback(callback) }
}.catch { e ->
    Log.w(TAG, "[$subId] error while imsFeatureProvisionedFlow", e)
}.conflate().onEach {
    Log.d(TAG, "[$subId] changed: capability=$capability tech=$tech isProvisioned=$it")
}.flowOn(Dispatchers.Default)
