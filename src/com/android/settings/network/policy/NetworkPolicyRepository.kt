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

package com.android.settings.network.policy

import android.content.Context
import android.net.NetworkPolicy
import android.net.NetworkPolicyManager
import android.net.NetworkTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class NetworkPolicyRepository(context: Context) {
    private val networkPolicyManager = context.getSystemService(NetworkPolicyManager::class.java)!!

    fun getNetworkPolicy(networkTemplate: NetworkTemplate): NetworkPolicy? =
        networkPolicyManager.networkPolicies.find { policy -> policy.template == networkTemplate }

    fun networkPolicyFlow(networkTemplate: NetworkTemplate): Flow<NetworkPolicy?> = flow {
        emit(getNetworkPolicy(networkTemplate))
    }.flowOn(Dispatchers.Default)
}
