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
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.util.Log
import com.android.settings.R
import com.android.settings.network.telephony.serviceStateFlow
import com.android.settings.network.telephony.telephonyCallbackFlow
import com.android.settingslib.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class SignalStrengthRepository(
    private val context: Context,
    private val serviceStateFlowFactory: (subId: Int) -> Flow<ServiceState> = {
        context.serviceStateFlow(it)
    },
) {
    fun signalStrengthDisplayFlow(subId: Int): Flow<String> =
        serviceStateFlowFactory(subId).flatMapLatest { serviceState ->
            if (Utils.isInService(serviceState)) {
                signalStrengthFlow(subId).map { it.displayString() }
            } else {
                flowOf("0")
            }
        }.conflate().flowOn(Dispatchers.Default)

    /** Creates an instance of a cold Flow for [SignalStrength] of given [subId]. */
    private fun signalStrengthFlow(subId: Int): Flow<SignalStrength> =
        context.telephonyCallbackFlow(subId) {
            object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    trySend(signalStrength)
                    val cellSignalStrengths = signalStrength.cellSignalStrengths
                    Log.d(TAG, "[$subId] onSignalStrengthsChanged: $cellSignalStrengths")
                }
            }
        }

    private fun SignalStrength.displayString() =
        context.getString(R.string.sim_signal_strength, signalDbm(), signalAsu())

    private companion object {
        private const val TAG = "SignalStrengthRepo"


        private fun SignalStrength.signalDbm(): Int =
            cellSignalStrengths.firstOrNull { it.dbm != -1 }?.dbm ?: 0

        private fun SignalStrength.signalAsu(): Int =
            cellSignalStrengths.firstOrNull { it.asuLevel != -1 }?.asuLevel ?: 0
    }
}
