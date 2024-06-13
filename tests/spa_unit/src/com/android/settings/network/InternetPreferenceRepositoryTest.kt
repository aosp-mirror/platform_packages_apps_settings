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

package com.android.settings.network

import android.content.Context
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.network.telephony.DataSubscriptionRepository
import com.android.settings.wifi.WifiSummaryRepository
import com.android.settings.wifi.repository.WifiRepository
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class InternetPreferenceRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockConnectivityRepository = mock<ConnectivityRepository>()
    private val mockWifiSummaryRepository = mock<WifiSummaryRepository>()
    private val mockDataSubscriptionRepository = mock<DataSubscriptionRepository>()
    private val mockWifiRepository = mock<WifiRepository>()
    private val airplaneModeOnFlow = MutableStateFlow(false)

    private val repository =
        InternetPreferenceRepository(
            context = context,
            connectivityRepository = mockConnectivityRepository,
            wifiSummaryRepository = mockWifiSummaryRepository,
            dataSubscriptionRepository = mockDataSubscriptionRepository,
            wifiRepository = mockWifiRepository,
            airplaneModeOnFlow = airplaneModeOnFlow,
        )

    @Test
    fun displayInfoFlow_wifi() = runBlocking {
        val wifiNetworkCapabilities =
            NetworkCapabilities.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()
        mockConnectivityRepository.stub {
            on { networkCapabilitiesFlow() } doReturn flowOf(wifiNetworkCapabilities)
        }
        mockWifiSummaryRepository.stub { on { summaryFlow() } doReturn flowOf(SUMMARY) }

        val displayInfo = repository.displayInfoFlow().firstWithTimeoutOrNull()

        assertThat(displayInfo)
            .isEqualTo(
                InternetPreferenceRepository.DisplayInfo(
                    summary = SUMMARY,
                    iconResId = R.drawable.ic_wifi_signal_4,
                )
            )
    }

    @Test
    fun displayInfoFlow_carrierMergedWifi_asCellular() = runBlocking {
        val wifiInfo =
            mock<WifiInfo> {
                on { isCarrierMerged } doReturn true
                on { makeCopy(any()) } doReturn mock
            }
        val wifiNetworkCapabilities =
            NetworkCapabilities.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .setTransportInfo(wifiInfo)
                .build()
        mockConnectivityRepository.stub {
            on { networkCapabilitiesFlow() } doReturn flowOf(wifiNetworkCapabilities)
        }
        mockDataSubscriptionRepository.stub { on { dataSummaryFlow() } doReturn flowOf(SUMMARY) }

        val displayInfo = repository.displayInfoFlow().firstWithTimeoutOrNull()

        assertThat(displayInfo)
            .isEqualTo(
                InternetPreferenceRepository.DisplayInfo(
                    summary = SUMMARY,
                    iconResId = R.drawable.ic_network_cell,
                )
            )
    }

    @Test
    fun displayInfoFlow_cellular() = runBlocking {
        val wifiNetworkCapabilities =
            NetworkCapabilities.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()
        mockConnectivityRepository.stub {
            on { networkCapabilitiesFlow() } doReturn flowOf(wifiNetworkCapabilities)
        }
        mockDataSubscriptionRepository.stub { on { dataSummaryFlow() } doReturn flowOf(SUMMARY) }

        val displayInfo = repository.displayInfoFlow().firstWithTimeoutOrNull()

        assertThat(displayInfo)
            .isEqualTo(
                InternetPreferenceRepository.DisplayInfo(
                    summary = SUMMARY,
                    iconResId = R.drawable.ic_network_cell,
                )
            )
    }

    @Test
    fun displayInfoFlow_ethernet() = runBlocking {
        val wifiNetworkCapabilities =
            NetworkCapabilities.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()
        mockConnectivityRepository.stub {
            on { networkCapabilitiesFlow() } doReturn flowOf(wifiNetworkCapabilities)
        }

        val displayInfo = repository.displayInfoFlow().firstWithTimeoutOrNull()

        assertThat(displayInfo)
            .isEqualTo(
                InternetPreferenceRepository.DisplayInfo(
                    summary = context.getString(R.string.to_switch_networks_disconnect_ethernet),
                    iconResId = R.drawable.ic_settings_ethernet,
                )
            )
    }

    @Test
    fun displayInfoFlow_airplaneModeOnAndWifiOn() = runBlocking {
        mockConnectivityRepository.stub {
            on { networkCapabilitiesFlow() } doReturn flowOf(NetworkCapabilities())
        }
        airplaneModeOnFlow.value = true
        mockWifiRepository.stub {
            on { wifiStateFlow() } doReturn flowOf(WifiManager.WIFI_STATE_ENABLED)
        }

        val displayInfo = repository.displayInfoFlow().firstWithTimeoutOrNull()

        assertThat(displayInfo)
            .isEqualTo(
                InternetPreferenceRepository.DisplayInfo(
                    summary = context.getString(R.string.networks_available),
                    iconResId = R.drawable.ic_no_internet_available,
                )
            )
    }

    @Test
    fun displayInfoFlow_airplaneModeOnAndWifiOff() = runBlocking {
        mockConnectivityRepository.stub {
            on { networkCapabilitiesFlow() } doReturn flowOf(NetworkCapabilities())
        }
        airplaneModeOnFlow.value = true
        mockWifiRepository.stub {
            on { wifiStateFlow() } doReturn flowOf(WifiManager.WIFI_STATE_DISABLED)
        }

        val displayInfo = repository.displayInfoFlow().firstWithTimeoutOrNull()

        assertThat(displayInfo)
            .isEqualTo(
                InternetPreferenceRepository.DisplayInfo(
                    summary = context.getString(R.string.condition_airplane_title),
                    iconResId = R.drawable.ic_no_internet_unavailable,
                )
            )
    }

    @Test
    fun displayInfoFlow_airplaneModeOff() = runBlocking {
        mockConnectivityRepository.stub {
            on { networkCapabilitiesFlow() } doReturn flowOf(NetworkCapabilities())
        }
        airplaneModeOnFlow.value = false
        mockWifiRepository.stub {
            on { wifiStateFlow() } doReturn flowOf(WifiManager.WIFI_STATE_DISABLED)
        }

        val displayInfo = repository.displayInfoFlow().firstWithTimeoutOrNull()

        assertThat(displayInfo)
            .isEqualTo(
                InternetPreferenceRepository.DisplayInfo(
                    summary = context.getString(R.string.networks_available),
                    iconResId = R.drawable.ic_no_internet_available,
                )
            )
    }

    private companion object {
        const val SUMMARY = "Summary"
    }
}
