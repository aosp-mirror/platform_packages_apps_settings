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
import android.telephony.AccessNetworkConstants.TransportType
import android.telephony.ims.feature.MmTelFeature.MmTelCapabilities.MmTelCapability
import android.telephony.ims.stub.ImsRegistrationImplBase.ImsRegistrationTech
import com.android.settings.network.telephony.subscriptionsChangedFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

/**
 * A repository for the IMS feature.
 *
 * @throws IllegalArgumentException if the [subId] is invalid.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ImsFeatureRepository(
    private val context: Context,
    private val subId: Int,
    private val provisioningRepository: ProvisioningRepository = ProvisioningRepository(context),
    private val imsMmTelRepository: ImsMmTelRepository = ImsMmTelRepositoryImpl(context, subId)
) {
    /**
     * A cold flow that determines the provisioning status for the specified IMS MmTel capability,
     * and whether or not the requested MmTel capability is supported by the carrier on the
     * specified network transport.
     *
     * @return true if the feature is provisioned and supported, false otherwise.
     */
    fun isReadyFlow(
        @MmTelCapability capability: Int,
        @ImsRegistrationTech tech: Int,
        @TransportType transportType: Int,
    ): Flow<Boolean> =
        context.subscriptionsChangedFlow().flatMapLatest {
            combine(
                provisioningRepository.imsFeatureProvisionedFlow(subId, capability, tech),
                imsMmTelRepository.isSupportedFlow(capability, transportType),
            ) { imsFeatureProvisioned, isSupported ->
                imsFeatureProvisioned && isSupported
            }
        }
}
