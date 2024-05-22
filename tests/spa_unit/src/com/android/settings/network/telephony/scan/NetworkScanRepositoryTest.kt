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

package com.android.settings.network.telephony.scan

import android.content.Context
import android.telephony.AccessNetworkConstants.AccessNetworkType
import android.telephony.CellIdentityCdma
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.NetworkScan
import android.telephony.NetworkScanRequest
import android.telephony.PhoneCapability
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.NETWORK_CLASS_BITMASK_5G
import android.telephony.TelephonyScanManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.android.settingslib.spa.testutils.toListWithTimeout
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class NetworkScanRepositoryTest {

    private var callback: TelephonyScanManager.NetworkScanCallback? = null

    private val mockTelephonyManager = mock<TelephonyManager> {
        on { createForSubscriptionId(SUB_ID) } doReturn mock
        on { requestNetworkScan(any(), any(), any()) } doAnswer {
            callback = it.arguments[2] as TelephonyScanManager.NetworkScanCallback
            mock<NetworkScan>()
        }
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
    }

    private val repository = NetworkScanRepository(context, SUB_ID)

    @Test
    fun networkScanFlow_initial() = runBlocking {
        val result = repository.networkScanFlow().firstWithTimeoutOrNull()

        assertThat(result).isNull()
    }

    @Test
    fun networkScanFlow_onResults(): Unit = runBlocking {
        val cellInfos = listOf(CellInfoCdma().apply { cellIdentity = CELL_IDENTITY_CDMA })
        val listDeferred = async {
            repository.networkScanFlow().toListWithTimeout()
        }
        delay(100)

        callback?.onResults(cellInfos)

        assertThat(listDeferred.await()).containsExactly(
            NetworkScanRepository.NetworkScanResult(
                state = NetworkScanRepository.NetworkScanState.ACTIVE,
                cellInfos = cellInfos,
            )
        )
    }

    @Test
    fun networkScanFlow_onComplete(): Unit = runBlocking {
        val listDeferred = async {
            repository.networkScanFlow().toListWithTimeout()
        }
        delay(100)

        callback?.onComplete()

        assertThat(listDeferred.await()).containsExactly(
            NetworkScanRepository.NetworkScanResult(
                state = NetworkScanRepository.NetworkScanState.COMPLETE,
                cellInfos = emptyList(),
            )
        )
    }

    @Test
    fun networkScanFlow_onError(): Unit = runBlocking {
        val listDeferred = async {
            repository.networkScanFlow().toListWithTimeout()
        }
        delay(100)

        callback?.onError(1)

        assertThat(listDeferred.await()).containsExactly(
            NetworkScanRepository.NetworkScanResult(
                state = NetworkScanRepository.NetworkScanState.ERROR,
                cellInfos = emptyList(),
            )
        )
    }

    @Test
    fun networkScanFlow_hasDuplicateItems(): Unit = runBlocking {
        val cellInfos = listOf(
            createCellInfoLte("123", false),
            createCellInfoLte("123", false),
            createCellInfoLte("124", true),
            createCellInfoLte("124", true),
            createCellInfoGsm("123", false),
            createCellInfoGsm("123", false),
        )
        val listDeferred = async {
            repository.networkScanFlow().toListWithTimeout()
        }
        delay(100)

        callback?.onResults(cellInfos)

        assertThat(listDeferred.await()).containsExactly(
            NetworkScanRepository.NetworkScanResult(
                state = NetworkScanRepository.NetworkScanState.ACTIVE,
                cellInfos = listOf(
                    createCellInfoLte("123", false),
                    createCellInfoLte("124", true),
                    createCellInfoGsm("123", false),
                ),
            )
        )
    }


    @Test
    fun networkScanFlow_noDuplicateItems(): Unit = runBlocking {
        val cellInfos = listOf(
            createCellInfoLte("123", false),
            createCellInfoLte("123", true),
            createCellInfoLte("124", false),
            createCellInfoLte("124", true),
            createCellInfoGsm("456", false),
            createCellInfoGsm("456", true),
        )
        val listDeferred = async {
            repository.networkScanFlow().toListWithTimeout()
        }
        delay(100)

        callback?.onResults(cellInfos)

        assertThat(listDeferred.await()).containsExactly(
            NetworkScanRepository.NetworkScanResult(
                state = NetworkScanRepository.NetworkScanState.ACTIVE,
                cellInfos = listOf(
                    createCellInfoLte("123", false),
                    createCellInfoLte("123", true),
                    createCellInfoLte("124", false),
                    createCellInfoLte("124", true),
                    createCellInfoGsm("456", false),
                    createCellInfoGsm("456", true),
                )
            )
        )
    }

    @Test
    fun createNetworkScan_deviceHasNrSa_requestNgran(): Unit = runBlocking {
        mockTelephonyManager.stub {
            on { getAllowedNetworkTypesBitmask() } doReturn NETWORK_CLASS_BITMASK_5G
            on { getPhoneCapability() } doReturn
                createPhoneCapability(intArrayOf(PhoneCapability.DEVICE_NR_CAPABILITY_SA))
        }

        repository.networkScanFlow().firstWithTimeoutOrNull()

        verify(mockTelephonyManager).requestNetworkScan(argThat<NetworkScanRequest> {
            specifiers.any { it.radioAccessNetwork == AccessNetworkType.NGRAN }
        }, any(), any())
    }

    @Test
    fun createNetworkScan_deviceNoNrSa_noNgran(): Unit = runBlocking {
        mockTelephonyManager.stub {
            on { getAllowedNetworkTypesBitmask() } doReturn NETWORK_CLASS_BITMASK_5G
            on { getPhoneCapability() } doReturn
                createPhoneCapability(intArrayOf(PhoneCapability.DEVICE_NR_CAPABILITY_NSA))
        }

        repository.networkScanFlow().firstWithTimeoutOrNull()

        verify(mockTelephonyManager).requestNetworkScan(argThat<NetworkScanRequest> {
            specifiers.none { it.radioAccessNetwork == AccessNetworkType.NGRAN }
        }, any(), any())
    }

    private companion object {
        const val SUB_ID = 1
        const val LONG = "Long"
        const val SHORT = "Short"

        val CELL_IDENTITY_CDMA = CellIdentityCdma(
            /* nid = */ 1,
            /* sid = */ 2,
            /* bid = */ 3,
            /* lon = */ 4,
            /* lat = */ 5,
            /* alphal = */ LONG,
            /* alphas = */ SHORT,
        )

        private fun createCellInfoLte(alphaLong: String, registered: Boolean): CellInfoLte {
            val cellIdentityLte = CellIdentityLte(
                /* ci = */ 1,
                /* pci = */ 2,
                /* tac = */ 3,
                /* earfcn = */ 4,
                /* bands = */ intArrayOf(1, 2),
                /* bandwidth = */ 10000,
                /* mccStr = */ null,
                /* mncStr = */ null,
                /* alphal = */ alphaLong,
                /* alphas = */ null,
                /* additionalPlmns = */ emptyList(),
                /* csgInfo = */ null,
            )
            return CellInfoLte().apply {
                cellIdentity = cellIdentityLte
                isRegistered = registered
            }
        }

        private fun createCellInfoGsm(alphaLong: String, registered: Boolean): CellInfoGsm {
            val cellIdentityGsm = CellIdentityGsm(
                /* lac = */ 1,
                /* cid = */ 2,
                /* arfcn = */ 3,
                /* bsic = */ 4,
                /* mccStr = */ "123",
                /* mncStr = */ "01",
                /* alphal = */ alphaLong,
                /* alphas = */ null,
                /* additionalPlmns = */ emptyList(),
            )
            return CellInfoGsm().apply {
                cellIdentity = cellIdentityGsm
                isRegistered = registered
            }
        }

        private fun createPhoneCapability(deviceNrCapabilities: IntArray) =
            PhoneCapability.Builder().setDeviceNrCapabilities(deviceNrCapabilities).build()
    }
}
