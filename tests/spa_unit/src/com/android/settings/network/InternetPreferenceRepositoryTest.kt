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
import android.net.wifi.WifiManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.wifi.WifiSummaryRepository
import com.android.settings.wifi.repository.WifiRepository
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class InternetPreferenceRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockConnectivityRepository = mock<ConnectivityRepository>()
    private val mockWifiSummaryRepository = mock<WifiSummaryRepository>()
    private val mockWifiRepository = mock<WifiRepository>()
    private val airplaneModeOnFlow = MutableStateFlow(false)

    private val repository = InternetPreferenceRepository(
        context = context,
        connectivityRepository = mockConnectivityRepository,
        wifiSummaryRepository = mockWifiSummaryRepository,
        wifiRepository = mockWifiRepository,
        airplaneModeOnFlow = airplaneModeOnFlow,
    )

    @Test
    fun summaryFlow_wifi() = runBlocking {
        val wifiNetworkCapabilities = NetworkCapabilities.Builder().apply {
            addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }.build()
        mockConnectivityRepository.stub {
            on { networkCapabilitiesFlow() } doReturn flowOf(wifiNetworkCapabilities)
        }
        mockWifiSummaryRepository.stub {
            on { summaryFlow() } doReturn flowOf(SUMMARY)
        }

        val summary = repository.summaryFlow().firstWithTimeoutOrNull()

        assertThat(summary).isEqualTo(SUMMARY)
    }

    @Test
    fun summaryFlow_airplaneModeOnAndWifiOn() = runBlocking {
        mockConnectivityRepository.stub {
            on { networkCapabilitiesFlow() } doReturn flowOf(NetworkCapabilities())
        }
        airplaneModeOnFlow.value = true
        mockWifiRepository.stub {
            on { wifiStateFlow() } doReturn flowOf(WifiManager.WIFI_STATE_ENABLED)
        }

        val summary = repository.summaryFlow().firstWithTimeoutOrNull()

        assertThat(summary).isEqualTo(context.getString(R.string.networks_available))
    }

    @Test
    fun summaryFlow_airplaneModeOnAndWifiOff() = runBlocking {
        mockConnectivityRepository.stub {
            on { networkCapabilitiesFlow() } doReturn flowOf(NetworkCapabilities())
        }
        airplaneModeOnFlow.value = true
        mockWifiRepository.stub {
            on { wifiStateFlow() } doReturn flowOf(WifiManager.WIFI_STATE_DISABLED)
        }

        val summary = repository.summaryFlow().firstWithTimeoutOrNull()

        assertThat(summary).isEqualTo(context.getString(R.string.condition_airplane_title))
    }

    @Test
    fun summaryFlow_airplaneModeOff() = runBlocking {
        mockConnectivityRepository.stub {
            on { networkCapabilitiesFlow() } doReturn flowOf(NetworkCapabilities())
        }
        airplaneModeOnFlow.value = false
        mockWifiRepository.stub {
            on { wifiStateFlow() } doReturn flowOf(WifiManager.WIFI_STATE_DISABLED)
        }

        val summary = repository.summaryFlow().firstWithTimeoutOrNull()

        assertThat(summary).isEqualTo(context.getString(R.string.networks_available))
    }

    private companion object {
        const val SUMMARY = "Summary"
    }
}
