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

package com.android.settings.network

import android.app.Application
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus

class SubscriptionInfoListViewModel(application: Application) : AndroidViewModel(application) {
    private val scope = viewModelScope + Dispatchers.Default
    val subscriptionInfoListFlow = callbackFlow<List<SubscriptionInfo>> {
        val subscriptionManager = application.getSystemService(SubscriptionManager::class.java)!!

        val listener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
            override fun onSubscriptionsChanged() {
                trySend(SubscriptionUtil.getActiveSubscriptions(subscriptionManager))
            }
        }

        subscriptionManager.addOnSubscriptionsChangedListener(
            Dispatchers.Default.asExecutor(),
            listener,
        )

        awaitClose { subscriptionManager.removeOnSubscriptionsChangedListener(listener) }
    }.conflate().stateIn(scope, SharingStarted.Eagerly, initialValue = emptyList())
}
