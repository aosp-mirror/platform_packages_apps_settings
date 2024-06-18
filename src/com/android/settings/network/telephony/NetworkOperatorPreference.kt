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
import android.telephony.AccessNetworkConstants.AccessNetworkType
import android.telephony.CellIdentity
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoTdscdma
import android.telephony.CellInfoWcdma
import android.telephony.SignalStrength
import android.util.Log
import androidx.annotation.OpenForTesting
import androidx.preference.Preference
import com.android.internal.telephony.OperatorInfo
import com.android.settings.R
import com.android.settings.network.telephony.CellInfoUtil.getNetworkTitle
import com.android.settings.network.telephony.CellInfoUtil.getOperatorNumeric
import java.util.Objects

/**
 * A Preference represents a network operator in the NetworkSelectSetting fragment.
 */
@OpenForTesting
open class NetworkOperatorPreference(
    context: Context,
    private val forbiddenPlmns: List<String>,
    private val show4GForLTE: Boolean,
) : Preference(context) {
    private var cellInfo: CellInfo? = null
    private var cellId: CellIdentity? = null
    private val useNewApi = context.resources.getBoolean(
        com.android.internal.R.bool.config_enableNewAutoSelectNetworkUI
    )

    /**
     * Change cell information
     */
    @JvmOverloads
    fun updateCell(cellInfo: CellInfo?, cellId: CellIdentity? = cellInfo?.cellIdentity) {
        this.cellInfo = cellInfo
        this.cellId = cellId
        refresh()
    }

    /**
     * Compare cell within preference
     */
    fun isSameCell(cellInfo: CellInfo): Boolean = cellInfo.cellIdentity == cellId

    /**
     * Return true when this preference is for forbidden network
     */
    fun isForbiddenNetwork(): Boolean = cellId?.getOperatorNumeric() in forbiddenPlmns

    /**
     * Refresh the NetworkOperatorPreference by updating the title and the icon.
     */
    fun refresh() {
        var networkTitle = cellId?.getNetworkTitle() ?: return
        if (isForbiddenNetwork()) {
            if (DBG) Log.d(TAG, "refresh forbidden network: $networkTitle")
            networkTitle += " ${context.getString(R.string.forbidden_network)}"
        } else {
            if (DBG) Log.d(TAG, "refresh the network: $networkTitle")
        }
        title = networkTitle
        val level = (cellInfo ?: return).cellSignalStrength.level
        if (DBG) Log.d(TAG, "refresh level: $level")
        setIcon(level)
    }

    /**
     * Update the icon according to the input signal strength level.
     */
    override fun setIcon(level: Int) {
        if (!useNewApi || level < 0 || level >= SignalStrength.NUM_SIGNAL_STRENGTH_BINS) {
            return
        }
        icon = MobileNetworkUtils.getSignalStrengthIcon(
            context,
            level,
            SignalStrength.NUM_SIGNAL_STRENGTH_BINS,
            getIconIdForCell(),
            false,
            false,
        )
    }

    /**
     * Operator name of this cell
     */
    fun getOperatorName(): String? = cellId?.getNetworkTitle()

    /**
     * Operator info of this cell
     */
    fun getOperatorInfo() = OperatorInfo(
        Objects.toString(cellId?.operatorAlphaLong, ""),
        Objects.toString(cellId?.operatorAlphaShort, ""),
        cellId?.getOperatorNumeric(),
        getAccessNetworkTypeFromCellInfo(),
    )

    private fun getIconIdForCell(): Int = when (cellInfo) {
        is CellInfoGsm -> R.drawable.signal_strength_g
        is CellInfoCdma -> R.drawable.signal_strength_1x
        is CellInfoWcdma, is CellInfoTdscdma -> R.drawable.signal_strength_3g

        is CellInfoLte -> {
            if (show4GForLTE) R.drawable.ic_signal_strength_4g
            else R.drawable.signal_strength_lte
        }

        is CellInfoNr -> R.drawable.signal_strength_5g
        else -> MobileNetworkUtils.NO_CELL_DATA_TYPE_ICON
    }

    private fun getAccessNetworkTypeFromCellInfo(): Int = when (cellInfo) {
        is CellInfoGsm -> AccessNetworkType.GERAN
        is CellInfoCdma -> AccessNetworkType.CDMA2000
        is CellInfoWcdma, is CellInfoTdscdma -> AccessNetworkType.UTRAN
        is CellInfoLte -> AccessNetworkType.EUTRAN
        is CellInfoNr -> AccessNetworkType.NGRAN
        else -> AccessNetworkType.UNKNOWN
    }

    companion object {
        private const val TAG = "NetworkOperatorPref"
        private const val DBG = false
    }
}
