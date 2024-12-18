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
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class SimSlotRepository(private val context: Context) {
    private val subscriptionManager = context.requireSubscriptionManager()

    fun subIdInSimSlotFlow(simSlotIndex: Int) =
        context.subscriptionsChangedFlow()
            .map {
                subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(simSlotIndex)
                    ?.subscriptionId
                    ?: SubscriptionManager.INVALID_SUBSCRIPTION_ID
            }
            .conflate()
            .onEach { Log.d(TAG, "sub id in sim slot $simSlotIndex: $it") }
            .flowOn(Dispatchers.Default)

    private companion object {
        private const val TAG = "SimSlotRepository"
    }
}
