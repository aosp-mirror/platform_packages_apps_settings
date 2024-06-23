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

import android.telephony.CellIdentityCdma
import android.telephony.CellIdentityGsm
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.telephony.OperatorInfo
import com.android.settings.network.telephony.CellInfoUtil.getNetworkTitle
import com.android.settings.network.telephony.CellInfoUtil.getOperatorNumeric
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CellInfoUtilTest {

    @Test
    fun getNetworkTitle_alphaLong() {
        val networkTitle = CELL_IDENTITY_GSM.getNetworkTitle()

        assertThat(networkTitle).isEqualTo(LONG)
    }

    @Test
    fun getNetworkTitle_alphaShort() {
        val cellIdentity = CellIdentityGsm(
            /* lac = */ 1,
            /* cid = */ 2,
            /* arfcn = */ 3,
            /* bsic = */ 4,
            /* mccStr = */ "123",
            /* mncStr = */ "01",
            /* alphal = */ "",
            /* alphas = */ SHORT,
            /* additionalPlmns = */ emptyList(),
        )

        val networkTitle = cellIdentity.getNetworkTitle()

        assertThat(networkTitle).isEqualTo(SHORT)
    }

    @Test
    fun getNetworkTitle_operatorNumeric() {
        val cellIdentity = CellIdentityGsm(
            /* lac = */ 1,
            /* cid = */ 2,
            /* arfcn = */ 3,
            /* bsic = */ 4,
            /* mccStr = */ "123",
            /* mncStr = */ "01",
            /* alphal = */ "",
            /* alphas = */ "",
            /* additionalPlmns = */ emptyList(),
        )

        val networkTitle = cellIdentity.getNetworkTitle()

        assertThat(networkTitle).isEqualTo("12301")
    }

    @Test
    fun getNetworkTitle_null() {
        val cellIdentity = CellIdentityGsm(
            /* lac = */ 1,
            /* cid = */ 2,
            /* arfcn = */ 3,
            /* bsic = */ 4,
            /* mccStr = */ null,
            /* mncStr = */ null,
            /* alphal = */ null,
            /* alphas = */ null,
            /* additionalPlmns = */ emptyList(),
        )

        val networkTitle = cellIdentity.getNetworkTitle()

        assertThat(networkTitle).isNull()
    }

    @Test
    fun convertOperatorInfoToCellInfo() {
        val operatorInfo = OperatorInfo(LONG, SHORT, "12301")

        val cellInfo = CellInfoUtil.convertOperatorInfoToCellInfo(operatorInfo)

        assertThat(cellInfo.cellIdentity.mccString).isEqualTo("123")
        assertThat(cellInfo.cellIdentity.mncString).isEqualTo("01")
        assertThat(cellInfo.cellIdentity.operatorAlphaLong).isEqualTo(LONG)
        assertThat(cellInfo.cellIdentity.operatorAlphaShort).isEqualTo(SHORT)
    }

    @Test
    fun cellInfoListToString() {
        val cellInfoList =
            listOf(
                CellInfoCdma().apply {
                    cellIdentity = CELL_IDENTITY_CDMA
                },
                CellInfoGsm().apply {
                    isRegistered = true
                    cellIdentity = CELL_IDENTITY_GSM
                },
            )

        val string = CellInfoUtil.cellInfoListToString(cellInfoList)

        assertThat(string).isEqualTo(
            "{CellType = CellInfoCdma, isRegistered = false, " +
                "mcc = null, mnc = null, alphaL = Long, alphaS = Short}, " +
                "{CellType = CellInfoGsm, isRegistered = true, " +
                "mcc = 123, mnc = 01, alphaL = Long, alphaS = Short}"
        )
    }

    @Test
    fun getOperatorNumeric_cdma() {
        val operatorNumeric = CELL_IDENTITY_CDMA.getOperatorNumeric()

        assertThat(operatorNumeric).isNull()
    }

    @Test
    fun getOperatorNumeric_gsm() {
        val operatorNumeric = CELL_IDENTITY_GSM.getOperatorNumeric()

        assertThat(operatorNumeric).isEqualTo("12301")
    }

    private companion object {
        const val LONG = "Long"
        const val SHORT = "Short"

        val CELL_IDENTITY_GSM = CellIdentityGsm(
            /* lac = */ 1,
            /* cid = */ 2,
            /* arfcn = */ 3,
            /* bsic = */ 4,
            /* mccStr = */ "123",
            /* mncStr = */ "01",
            /* alphal = */ LONG,
            /* alphas = */ SHORT,
            /* additionalPlmns = */ emptyList(),
        )

        val CELL_IDENTITY_CDMA = CellIdentityCdma(
            /* nid = */ 1,
            /* sid = */ 2,
            /* bid = */ 3,
            /* lon = */ 4,
            /* lat = */ 5,
            /* alphal = */ LONG,
            /* alphas = */ SHORT,
        )
    }
}
