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
import android.telephony.SubscriptionInfo
import com.android.settings.network.telephony.SubscriptionRepository
import com.android.settings.network.telephony.euicc.EuiccRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class MobileNetworkSummaryRepository(
    private val context: Context,
    private val subscriptionRepository: SubscriptionRepository = SubscriptionRepository(context),
    private val euiccRepository: EuiccRepository = EuiccRepository(context),
    private val getDisplayName: (SubscriptionInfo) -> String = { subInfo ->
        SubscriptionUtil.getUniqueSubscriptionDisplayName(subInfo, context).toString()
    },
) {
    sealed interface SubscriptionsState

    data object AddNetwork : SubscriptionsState

    data object NoSubscriptions : SubscriptionsState

    data class HasSubscriptions(val displayNames: List<String>) : SubscriptionsState

    fun subscriptionsStateFlow(): Flow<SubscriptionsState> =
        subDisplayNamesFlow()
            .map { displayNames ->
                if (displayNames.isEmpty()) {
                    if (euiccRepository.showEuiccSettings()) AddNetwork else NoSubscriptions
                } else {
                    HasSubscriptions(displayNames)
                }
            }
            .distinctUntilChanged()
            .conflate()
            .flowOn(Dispatchers.Default)

    private fun subDisplayNamesFlow(): Flow<List<String>> =
        subscriptionRepository
            .selectableSubscriptionInfoListFlow()
            .map { subInfos -> subInfos.map(getDisplayName) }
            .distinctUntilChanged()
            .conflate()
            .flowOn(Dispatchers.Default)
}
