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
import android.telephony.SubscriptionManager
import android.telephony.ims.feature.MmTelFeature
import android.telephony.ims.stub.ImsRegistrationImplBase
import com.android.settings.network.telephony.ims.ImsFeatureRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
class VideoCallingRepository(
    context: Context,
    private val mobileDataRepository: MobileDataRepository = MobileDataRepository(context),
    private val imsFeatureRepositoryFactory: (Int) -> ImsFeatureRepository = { subId ->
        ImsFeatureRepository(context, subId)
    },
) {
    private val carrierConfigRepository = CarrierConfigRepository(context)

    fun isVideoCallReadyFlow(subId: Int): Flow<Boolean> {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return flowOf(false)

        return isPreconditionMeetFlow(subId).flatMapLatest { isPreconditionMeet ->
            if (isPreconditionMeet) {
                imsFeatureRepositoryFactory(subId)
                    .isReadyFlow(
                        capability = MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO,
                        tech = ImsRegistrationImplBase.REGISTRATION_TECH_LTE,
                        transportType = AccessNetworkConstants.TRANSPORT_TYPE_WWAN,
                    )
            } else {
                flowOf(false)
            }
        }
    }

    private fun isPreconditionMeetFlow(subId: Int): Flow<Boolean> =
        if (carrierConfigRepository.getBoolean(
            subId, CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS)) {
            flowOf(true)
        } else {
            mobileDataRepository.isMobileDataEnabledFlow(subId)
        }
}
