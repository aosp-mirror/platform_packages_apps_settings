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

import android.telephony.TelephonyManager
import android.telephony.UiccSlotInfo
import android.util.Log

class UiccSlotRepository(private val telephonyManager: TelephonyManager?) {

    /** Returns whether any removable physical sim is enabled. */
    fun anyRemovablePhysicalSimEnabled(): Boolean {
        val result =
            telephonyManager?.uiccSlotsInfo?.any { uiccSlotInfo: UiccSlotInfo? ->
                uiccSlotInfo.isRemovablePhysicalSimEnabled()
            } ?: false
        Log.i(TAG, "anyRemovablePhysicalSimEnabled: $result")
        return result
    }

    private fun UiccSlotInfo?.isRemovablePhysicalSimEnabled(): Boolean {
        return this != null &&
            isRemovable &&
            !isEuicc &&
            ports.any { port -> port.isActive } &&
            cardStateInfo == UiccSlotInfo.CARD_STATE_INFO_PRESENT
    }

    companion object {
        private const val TAG = "UiccRepository"
    }
}
