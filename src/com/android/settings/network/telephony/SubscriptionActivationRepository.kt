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
import android.content.Intent
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS
import android.util.Log
import com.android.settings.Utils
import com.android.settings.flags.Flags
import com.android.settings.network.SatelliteRepository
import com.android.settings.network.SimOnboardingActivity.Companion.startSimOnboardingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class SubscriptionActivationRepository(
    private val context: Context,
    private val callStateRepository: CallStateRepository = CallStateRepository(context),
    private val satelliteRepository: SatelliteRepository = SatelliteRepository(context),
) {
    fun isActivationChangeableFlow(): Flow<Boolean> = combine(
        callStateRepository.isInCallFlow(),
        satelliteRepository.getIsSessionStartedFlow()
    ) { isInCall, isSatelliteModemEnabled ->
        !isInCall && !isSatelliteModemEnabled
    }

    /**
     * Starts a dialog activity to handle SIM enabling / disabling.
     * @param subId The id of subscription need to be enabled or disabled.
     * @param active Whether the subscription with [subId] should be enabled or disabled.
     */
    suspend fun setActive(subId: Int, active: Boolean) {
        if (!SubscriptionManager.isUsableSubscriptionId(subId)) {
            Log.i(TAG, "Unable to toggle subscription due to unusable subscription ID.")
            return
        }
        if (!active && isEmergencyCallbackMode(subId)) {
            val intent = Intent(ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS).apply {
                setPackage(Utils.PHONE_PACKAGE_NAME)
            }
            context.startActivity(intent)
            return
        }
        if (active && Flags.isDualSimOnboardingEnabled()) {
            startSimOnboardingActivity(context, subId)
            return
        }
        context.startActivity(ToggleSubscriptionDialogActivity.getIntent(context, subId, active))
    }

    private suspend fun isEmergencyCallbackMode(subId: Int) = withContext(Dispatchers.Default) {
        context.telephonyManager(subId).emergencyCallbackMode
    }

    private companion object {
        private const val TAG = "SubscriptionActivationR"
    }
}
