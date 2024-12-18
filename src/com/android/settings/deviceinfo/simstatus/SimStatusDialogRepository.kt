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
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.settings.network.telephony.CarrierConfigRepository
import com.android.settings.network.telephony.SimSlotRepository
import com.android.settings.network.telephony.ims.ImsMmTelRepository
import com.android.settings.network.telephony.ims.ImsMmTelRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SimStatusDialogRepository
@JvmOverloads
constructor(
    private val context: Context,
    private val simSlotRepository: SimSlotRepository = SimSlotRepository(context),
    private val signalStrengthRepository: SignalStrengthRepository =
        SignalStrengthRepository(context),
    private val imsMmTelRepositoryFactory: (subId: Int) -> ImsMmTelRepository = { subId ->
        ImsMmTelRepositoryImpl(context, subId)
    },
) {
    private val carrierConfigRepository = CarrierConfigRepository(context)

    data class SimStatusDialogInfo(
        val signalStrength: String? = null,
        val imsRegistered: Boolean? = null,
    )

    private data class SimStatusDialogVisibility(
        val signalStrengthShowUp: Boolean,
        val imsRegisteredShowUp: Boolean,
    )

    fun collectSimStatusDialogInfo(
        lifecycleOwner: LifecycleOwner,
        simSlotIndex: Int,
        action: (info: SimStatusDialogInfo) -> Unit,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                simStatusDialogInfoBySlotFlow(simSlotIndex).collect(action)
            }
        }
    }

    private fun simStatusDialogInfoBySlotFlow(simSlotIndex: Int): Flow<SimStatusDialogInfo> =
        simSlotRepository
            .subIdInSimSlotFlow(simSlotIndex)
            .flatMapLatest { subId ->
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    simStatusDialogInfoFlow(subId)
                } else {
                    flowOf(SimStatusDialogInfo())
                }
            }
            .conflate()
            .flowOn(Dispatchers.Default)

    private fun simStatusDialogInfoFlow(subId: Int): Flow<SimStatusDialogInfo> =
        showUpFlow(subId).flatMapLatest { visibility ->
            combine(
                if (visibility.signalStrengthShowUp) {
                    signalStrengthRepository.signalStrengthDisplayFlow(subId)
                } else flowOf(null),
                if (visibility.imsRegisteredShowUp) {
                    imsMmTelRepositoryFactory(subId).imsRegisteredFlow()
                } else flowOf(null),
            ) { signalStrength, imsRegistered ->
                SimStatusDialogInfo(signalStrength = signalStrength, imsRegistered = imsRegistered)
            }
        }

    private fun showUpFlow(subId: Int) = flow {
        val visibility =
            carrierConfigRepository.transformConfig(subId) {
                SimStatusDialogVisibility(
                    signalStrengthShowUp =
                        getBoolean(
                            CarrierConfigManager.KEY_SHOW_SIGNAL_STRENGTH_IN_SIM_STATUS_BOOL),
                    imsRegisteredShowUp =
                        getBoolean(CarrierConfigManager.KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL),
                )
            }
        emit(visibility)
    }
}
