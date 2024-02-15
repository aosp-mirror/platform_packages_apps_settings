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
import android.telephony.SubscriptionManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.settings.network.telephony.subscriptionsChangedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus

class SubscriptionInfoListViewModel(application: Application) : AndroidViewModel(application) {
    private val subscriptionManager =
        application.getSystemService(SubscriptionManager::class.java)!!
    private val scope = viewModelScope + Dispatchers.Default

    /**
     * Getting the active Subscription list
     */
    //ToDo: renaming the function name
    val subscriptionInfoListFlow = application.subscriptionsChangedFlow().map {
        SubscriptionUtil.getActiveSubscriptions(subscriptionManager)
    }.stateIn(scope, SharingStarted.Eagerly, initialValue = emptyList())

    /**
     * Getting the Selectable SubscriptionInfo List from the SubscriptionManager's
     * getAvailableSubscriptionInfoList
     */
    val selectableSubscriptionInfoListFlow = application.subscriptionsChangedFlow().map {
        SubscriptionUtil.getSelectableSubscriptionInfoList(application)
    }.stateIn(scope, SharingStarted.Eagerly, initialValue = emptyList())
}
