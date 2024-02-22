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

package com.android.settings.network.apn

import android.telephony.TelephonyManager
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.android.settingslib.spa.widget.editor.SettingsDropdownCheckOption

object ApnNetworkTypes {
    private val Types = listOf(
        TelephonyManager.NETWORK_TYPE_LTE,
        TelephonyManager.NETWORK_TYPE_HSPAP,
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_HSUPA,
        TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_UMTS,
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_GPRS,
        TelephonyManager.NETWORK_TYPE_EHRPD,
        TelephonyManager.NETWORK_TYPE_EVDO_B,
        TelephonyManager.NETWORK_TYPE_EVDO_A,
        TelephonyManager.NETWORK_TYPE_EVDO_0,
        TelephonyManager.NETWORK_TYPE_1xRTT,
        TelephonyManager.NETWORK_TYPE_CDMA,
        TelephonyManager.NETWORK_TYPE_NR,
    )

    fun getNetworkTypeOptions(): List<SettingsDropdownCheckOption> =
        Types.map { SettingsDropdownCheckOption(TelephonyManager.getNetworkTypeName(it)) }

    /**
     * Gets the selected Network type Selected Options according to network type.
     * @param networkType Initialized network type bitmask, often multiple network type options may
     *                    be included.
     */
    fun networkTypeToSelectedStateMap(
        options: List<SettingsDropdownCheckOption>,
        networkType: Long,
    ): SnapshotStateMap<SettingsDropdownCheckOption, Boolean> {
        val stateMap = mutableStateMapOf<SettingsDropdownCheckOption, Boolean>()
        Types.forEachIndexed { index, type ->
            if (networkType and TelephonyManager.getBitMaskForNetworkType(type) != 0L) {
                stateMap[options[index]] = true
            }
        }
        return stateMap
    }

    /**
     * Gets the network type according to the selected Network type Selected Options.
     * @param stateMap the selected Network type Selected Options.
     */
    fun selectedStateMapToNetworkType(
        options: List<SettingsDropdownCheckOption>,
        stateMap: SnapshotStateMap<SettingsDropdownCheckOption, Boolean>,
    ): Long {
        var networkType = 0L
        options.forEachIndexed { index, option ->
            if (stateMap[option] == true) {
                networkType = networkType or TelephonyManager.getBitMaskForNetworkType(Types[index])
            }
        }
        return networkType
    }
}
