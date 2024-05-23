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

package com.android.settings.deviceinfo.simstatus

import android.content.Context
import android.telephony.SubscriptionManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.settings.network.telephony.SimSlotRepository
import com.android.settings.network.telephony.ims.ImsMmTelRepository
import com.android.settings.network.telephony.ims.ImsMmTelRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ImsRegistrationStateController @JvmOverloads constructor(
    private val context: Context,
    private val simSlotRepository: SimSlotRepository = SimSlotRepository(context),
    private val imsMmTelRepositoryFactory: (subId: Int) -> ImsMmTelRepository = { subId ->
        ImsMmTelRepositoryImpl(context, subId)
    },
) {
    fun collectImsRegistered(
        lifecycleOwner: LifecycleOwner,
        simSlotIndex: Int,
        action: (imsRegistered: Boolean) -> Unit,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                imsRegisteredFlow(simSlotIndex).collect(action)
            }
        }
    }

    private fun imsRegisteredFlow(simSlotIndex: Int): Flow<Boolean> =
        simSlotRepository.subIdInSimSlotFlow(simSlotIndex)
            .flatMapLatest { subId ->
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    imsMmTelRepositoryFactory(subId).imsRegisteredFlow()
                } else {
                    flowOf(false)
                }
            }
            .conflate()
            .flowOn(Dispatchers.Default)
}
