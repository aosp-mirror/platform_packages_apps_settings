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
import android.telephony.SubscriptionManager

/**
 * Shows information about disable a physical SIM.
 */
class DisableSimFooterPreferenceController @JvmOverloads constructor(
    context: Context,
    preferenceKey: String,
    private val subscriptionRepository: SubscriptionRepository = SubscriptionRepository(context),
) : TelephonyBasePreferenceController(context, preferenceKey) {

    /**
     * Re-init for SIM based on given subscription ID.
     *
     * @param subId is the given subscription ID
     */
    fun init(subId: Int) {
        mSubId = subId
    }

    override fun getAvailabilityStatus(subId: Int): Int {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID ||
            subscriptionRepository.canDisablePhysicalSubscription()
        ) {
            return CONDITIONALLY_UNAVAILABLE
        }

        val isAvailable =
            subscriptionRepository.getSelectableSubscriptionInfoList().any { subInfo ->
                subInfo.subscriptionId == subId && !subInfo.isEmbedded
            }

        return if (isAvailable) AVAILABLE else CONDITIONALLY_UNAVAILABLE
    }
}
