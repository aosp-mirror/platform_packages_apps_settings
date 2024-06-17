/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import com.android.settings.network.SubscriptionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

private const val TAG = "SubscriptionRepository"

fun Context.isSubscriptionEnabledFlow(subId: Int) = subscriptionsChangedFlow().map {
    val subscriptionManager = getSystemService(SubscriptionManager::class.java)

    subscriptionManager?.isSubscriptionEnabled(subId) ?: false
}.flowOn(Dispatchers.Default)

fun Context.phoneNumberFlow(subscriptionInfo: SubscriptionInfo) = subscriptionsChangedFlow().map {
    SubscriptionUtil.getFormattedPhoneNumber(this, subscriptionInfo)
}.flowOn(Dispatchers.Default)

fun Context.subscriptionsChangedFlow() = callbackFlow {
    val subscriptionManager = getSystemService(SubscriptionManager::class.java)!!

    val listener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
        override fun onSubscriptionsChanged() {
            trySend(Unit)
        }
    }

    subscriptionManager.addOnSubscriptionsChangedListener(
        Dispatchers.Default.asExecutor(),
        listener,
    )

    awaitClose { subscriptionManager.removeOnSubscriptionsChangedListener(listener) }
}.conflate().onEach { Log.d(TAG, "subscriptions changed") }.flowOn(Dispatchers.Default)
