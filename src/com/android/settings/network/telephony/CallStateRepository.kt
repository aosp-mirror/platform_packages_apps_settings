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
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalCoroutinesApi::class)
class CallStateRepository(
    private val context: Context,
    private val subscriptionRepository: SubscriptionRepository = SubscriptionRepository(context),
) {

    /** Flow for call state of given [subId]. */
    fun callStateFlow(subId: Int): Flow<Int> = context.telephonyCallbackFlow(subId) {
        object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                trySend(state)
            }
        }
    }

    /**
     * Flow for in call state.
     *
     * @return true if any active subscription's call state is not idle.
     */
    fun isInCallFlow(): Flow<Boolean> = subscriptionRepository.activeSubscriptionIdListFlow()
        .flatMapLatest { subIds ->
            if (subIds.isEmpty()) {
                flowOf(false)
            } else {
                combine(subIds.map(::callStateFlow)) { states ->
                    states.any { it != TelephonyManager.CALL_STATE_IDLE }
                }
            }
        }
        .distinctUntilChanged()
        .conflate()
        .onEach { Log.d(TAG, "isInCallFlow: $it") }
        .flowOn(Dispatchers.Default)

    private companion object {
        private const val TAG = "CallStateRepository"
    }
}
