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

import android.telephony.CellIdentity
import android.telephony.CellInfo
import android.text.BidiFormatter
import android.text.TextDirectionHeuristics

/**
 * Add static Utility functions to get information from the CellInfo object.
 * TODO: Modify [CellInfo] for simplify those functions
 */
object CellInfoUtil {

    /**
     * Returns the title of the network obtained in the manual search.
     *
     * By the following order,
     * 1. Long Name if not null/empty
     * 2. Short Name if not null/empty
     * 3. OperatorNumeric (MCCMNC) string
     */
    @JvmStatic
    fun CellIdentity.getNetworkTitle(): String? {
        operatorAlphaLong?.takeIf { it.isNotBlank() }?.let { return it.toString() }
        operatorAlphaShort?.takeIf { it.isNotBlank() }?.let { return it.toString() }
        val operatorNumeric = getOperatorNumeric() ?: return null
        val bidiFormatter = BidiFormatter.getInstance()
        return bidiFormatter.unicodeWrap(operatorNumeric, TextDirectionHeuristics.LTR)
    }

    /**
     * Convert a list of cellInfos to readable string without sensitive info.
     */
    @JvmStatic
    fun cellInfoListToString(cellInfos: List<CellInfo>): String =
        cellInfos.joinToString(System.lineSeparator()) { cellInfo -> cellInfo.readableString() }

    /**
     * Convert [CellInfo] to a readable string without sensitive info.
     */
    private fun CellInfo.readableString(): String = buildString {
        append("{CellType = ${this@readableString::class.simpleName}, ")
        append("isRegistered = $isRegistered, ")
        append(cellIdentity.readableString())
        append("}")
    }

    private fun CellIdentity.readableString(): String = buildString {
        append("mcc = $mccString, ")
        append("mnc = $mncString, ")
        append("alphaL = $operatorAlphaLong, ")
        append("alphaS = $operatorAlphaShort")
    }

    /**
     * Returns the MccMnc.
     */
    @JvmStatic
    fun CellIdentity.getOperatorNumeric(): String? {
        val mcc = mccString
        val mnc = mncString
        return if (mcc == null || mnc == null) null else mcc + mnc
    }
}
