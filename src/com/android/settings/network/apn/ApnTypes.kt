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

package com.android.settings.network.apn

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

object ApnTypes {
    /**
     * APN types for data connections.  These are usage categories for an APN
     * entry.  One APN entry may support multiple APN types, eg, a single APN
     * may service regular internet traffic ("default") as well as MMS-specific
     * connections.<br></br>
     * APN_TYPE_ALL is a special type to indicate that this APN entry can
     * service all data connections.
     */
    const val APN_TYPE_ALL = "*"

    /** APN type for default data traffic  */
    const val APN_TYPE_DEFAULT = "default"

    /** APN type for MMS traffic  */
    const val APN_TYPE_MMS = "mms"

    /** APN type for SUPL assisted GPS  */
    const val APN_TYPE_SUPL = "supl"

    /** APN type for DUN traffic  */
    const val APN_TYPE_DUN = "dun"

    /** APN type for HiPri traffic  */
    const val APN_TYPE_HIPRI = "hipri"

    /** APN type for FOTA  */
    const val APN_TYPE_FOTA = "fota"

    /** APN type for IMS  */
    const val APN_TYPE_IMS = "ims"

    /** APN type for CBS  */
    const val APN_TYPE_CBS = "cbs"

    /** APN type for IA Initial Attach APN  */
    const val APN_TYPE_IA = "ia"

    /** APN type for Emergency PDN. This is not an IA apn, but is used
     * for access to carrier services in an emergency call situation.  */
    const val APN_TYPE_EMERGENCY = "emergency"

    /** APN type for Mission Critical Services  */
    const val APN_TYPE_MCX = "mcx"

    /** APN type for XCAP  */
    const val APN_TYPE_XCAP = "xcap"

    /** APN type for VSIM  */
    const val APN_TYPE_VSIM = "vsim"

    /** APN type for BIP  */
    const val APN_TYPE_BIP = "bip"

    /** APN type for ENTERPRISE  */
    const val APN_TYPE_ENTERPRISE = "enterprise"

    val APN_TYPES = arrayOf(
        APN_TYPE_DEFAULT,
        APN_TYPE_MMS,
        APN_TYPE_SUPL,
        APN_TYPE_DUN,
        APN_TYPE_HIPRI,
        APN_TYPE_FOTA,
        APN_TYPE_IMS,
        APN_TYPE_CBS,
        APN_TYPE_IA,
        APN_TYPE_EMERGENCY,
        APN_TYPE_MCX,
        APN_TYPE_XCAP,
        APN_TYPE_VSIM,
        APN_TYPE_BIP,
        APN_TYPE_ENTERPRISE
    )

    val APN_TYPES_OPTIONS = listOf(APN_TYPE_ALL) + APN_TYPES

    fun getApnTypeSelectedOptionsState(apnType: String): SnapshotStateList<Int> {
        val apnTypeSelectedOptionsState = mutableStateListOf<Int>()
        if (apnType.contains(APN_TYPE_ALL))
            APN_TYPES_OPTIONS.forEachIndexed { index, _ ->
                apnTypeSelectedOptionsState.add(index)
            }
        else {
            APN_TYPES_OPTIONS.forEachIndexed { index, type ->
                if (apnType.contains(type)) {
                    apnTypeSelectedOptionsState.add(index)
                }
            }
            if (apnTypeSelectedOptionsState.size == APN_TYPES.size)
                apnTypeSelectedOptionsState.add(APN_TYPES_OPTIONS.indexOf(APN_TYPE_ALL))
        }
        return apnTypeSelectedOptionsState
    }

    fun updateApnType(
        apnTypeSelectedOptionsState: SnapshotStateList<Int>,
        defaultApnTypes: List<String>,
        readOnlyApnTypes: List<String>
    ): String {
        val apnType = apnTypeSelectedOptionsState.joinToString { APN_TYPES_OPTIONS[it] }
        if (apnType.contains(APN_TYPE_ALL)) return APN_TYPE_ALL
        return if (apnType == "" && defaultApnTypes.isNotEmpty())
            getEditableApnType(defaultApnTypes, readOnlyApnTypes)
        else
            apnType
    }

    private fun getEditableApnType(
        defaultApnTypes: List<String>,
        readOnlyApnTypes: List<String>
    ): String {
        return defaultApnTypes.filterNot { apnType ->
            readOnlyApnTypes.contains(apnType) || apnType in listOf(
                APN_TYPE_IA,
                APN_TYPE_EMERGENCY,
                APN_TYPE_MCX,
                APN_TYPE_IMS,
            )
        }.joinToString()
    }
}