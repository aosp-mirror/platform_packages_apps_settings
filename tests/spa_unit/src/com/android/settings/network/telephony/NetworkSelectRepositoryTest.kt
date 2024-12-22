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
import android.os.PersistableBundle
import android.telephony.*
import android.telephony.satellite.SatelliteManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.network.telephony.scan.NetworkScanRepositoryTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class NetworkSelectRepositoryTest {

    private val mockServiceState = mock<ServiceState> {
        on {
            getNetworkRegistrationInfoListForTransportType(
                AccessNetworkConstants.TRANSPORT_TYPE_WWAN
            )
        } doReturn NetworkRegistrationInfos
    }

    private val mockTelephonyManager = mock<TelephonyManager> {
        on { createForSubscriptionId(SUB_ID) } doReturn mock
        on { dataState } doReturn TelephonyManager.DATA_CONNECTED
        on { serviceState } doReturn mockServiceState
    }

    private val mockSatelliteManager = mock<SatelliteManager> {
        on { getSatellitePlmnsForCarrier(anyInt()) } doReturn SatellitePlmns
    }

    private var mockCarrierConfigManager = mock<CarrierConfigManager>()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
        on { getSystemService(SatelliteManager::class.java) } doReturn mockSatelliteManager
        on { getSystemService(CarrierConfigManager::class.java) } doReturn mockCarrierConfigManager
    }

    private val repository = NetworkSelectRepository(context, SUB_ID)

    @Test
    fun getNetworkRegistrationInfo_notConnected_returnNull() {
        mockTelephonyManager.stub {
            on { dataState } doReturn TelephonyManager.DATA_DISCONNECTED
        }

        val info = repository.getNetworkRegistrationInfo()

        assertThat(info).isNull()
    }

    @Test
    fun getNetworkRegistrationInfo_nullServiceState_returnNull() {
        mockTelephonyManager.stub {
            on { serviceState } doReturn null
        }

        val info = repository.getNetworkRegistrationInfo()

        assertThat(info).isNull()
    }

    @Test
    fun getNetworkRegistrationInfo_emptyNetworkList_returnNull() {
        mockServiceState.stub {
            on {
                getNetworkRegistrationInfoListForTransportType(
                    AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                )
            } doReturn emptyList()
        }

        val info = repository.getNetworkRegistrationInfo()

        assertThat(info).isNull()
    }

    @Test
    fun getNetworkRegistrationInfo_hasNetworkList_returnInfo() {
        mockServiceState.stub {
            on {
                getNetworkRegistrationInfoListForTransportType(
                    AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                )
            } doReturn NetworkRegistrationInfos
        }
        mockTelephonyManager.stub {
            on { forbiddenPlmns } doReturn arrayOf(FORBIDDEN_PLMN)
        }

        val config = PersistableBundle()
        config.putBoolean(
            CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL,
            false)
        whenever(mockCarrierConfigManager.getConfigForSubId(
            SUB_ID, CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL))
            .thenReturn(config)

        val info = repository.getNetworkRegistrationInfo()

        assertThat(info).isEqualTo(
            NetworkSelectRepository.NetworkRegistrationAndForbiddenInfo(
                networkList = NetworkRegistrationInfos,
                forbiddenPlmns = listOf(FORBIDDEN_PLMN),
            )
        )
    }

    @Test
    fun getNetworkRegistrationInfo_filterSatellitePlmn() {

        val info1 = createTestNetworkRegistrationInfo("310", "260")
        val info2 = createTestNetworkRegistrationInfo("310", "261")
        val satelliteInfo = createTestNetworkRegistrationInfo(satelliteMcc, satelliteMnc)
        val registrationInfos = listOf(info1, info2, satelliteInfo)
        val filteredRegistrationInfos = listOf(info1, info2)

        mockServiceState.stub {
            on {
                getNetworkRegistrationInfoListForTransportType(
                    AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                )
            } doReturn registrationInfos
        }
        mockTelephonyManager.stub {
            on { forbiddenPlmns } doReturn arrayOf(FORBIDDEN_PLMN)
        }

        // disable satellite plmn filter
        var config = PersistableBundle()
        config.putBoolean(
            CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL,
            false)
        whenever(mockCarrierConfigManager.getConfigForSubId(
            SUB_ID, CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL))
            .thenReturn(config)

        var infoList = repository.getNetworkRegistrationInfo()

        assertThat(infoList).isEqualTo(
            NetworkSelectRepository.NetworkRegistrationAndForbiddenInfo(
                networkList = registrationInfos,
                forbiddenPlmns = listOf(FORBIDDEN_PLMN),
            )
        )

        // enable satellite plmn filter
        config = PersistableBundle()
        config.putBoolean(
            CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL,
            true)
        whenever(mockCarrierConfigManager.getConfigForSubId(
            SUB_ID, CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL))
            .thenReturn(config)

        infoList = repository.getNetworkRegistrationInfo()

        assertThat(infoList).isEqualTo(
            NetworkSelectRepository.NetworkRegistrationAndForbiddenInfo(
                networkList = filteredRegistrationInfos,
                forbiddenPlmns = listOf(FORBIDDEN_PLMN),
            )
        )
    }

    private companion object {
        const val SUB_ID = 1
        val NetworkRegistrationInfos = listOf(NetworkRegistrationInfo.Builder().build())
        const val FORBIDDEN_PLMN = "Forbidden PLMN"
        const val satelliteMcc = "310"
        const val satelliteMnc = "810"
        val SatellitePlmns = listOf(satelliteMcc + satelliteMnc)

        fun createTestNetworkRegistrationInfo(mcc: String, mnc: String): NetworkRegistrationInfo {
            val cellInfo = CellIdentityLte(0, 0, 0, 0, IntArray(2) { 0 },
                0, mcc, mnc, "", "", emptyList(), null)

            return NetworkRegistrationInfo.Builder().setCellIdentity(cellInfo).build()
        }
    }
}
