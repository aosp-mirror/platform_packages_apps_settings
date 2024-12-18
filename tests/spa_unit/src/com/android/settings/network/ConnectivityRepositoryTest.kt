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
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
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
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class ConnectivityRepositoryTest {

    private var networkCallback: NetworkCallback? = null

    private val mockConnectivityManager = mock<ConnectivityManager> {
        on { registerDefaultNetworkCallback(any()) } doAnswer {
            networkCallback = it.arguments[0] as NetworkCallback
        }
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(ConnectivityManager::class.java) } doReturn mockConnectivityManager
    }

    private val connectivityRepository = ConnectivityRepository(context)

    @Test
    fun networkCapabilitiesFlow_activeNetworkIsNull_noCrash() = runBlocking {
        mockConnectivityManager.stub {
            on { activeNetwork } doReturn null
            on { getNetworkCapabilities(null) } doReturn null
        }

        val networkCapabilities =
            connectivityRepository.networkCapabilitiesFlow().firstWithTimeoutOrNull()!!

        assertThat(networkCapabilities.transportTypes).isEmpty()
    }

    @Test
    fun networkCapabilitiesFlow_getInitialValue() = runBlocking {
        val expectedNetworkCapabilities = NetworkCapabilities.Builder().apply {
            addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        }.build()
        mockConnectivityManager.stub {
            on { getNetworkCapabilities(null) } doReturn expectedNetworkCapabilities
        }

        val actualNetworkCapabilities =
            connectivityRepository.networkCapabilitiesFlow().firstWithTimeoutOrNull()!!

        assertThat(actualNetworkCapabilities).isSameInstanceAs(expectedNetworkCapabilities)
    }

    @Test
    fun networkCapabilitiesFlow_getUpdatedValue() = runBlocking {
        val expectedNetworkCapabilities = NetworkCapabilities.Builder().apply {
            addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        }.build()

        val deferredList = async {
            connectivityRepository.networkCapabilitiesFlow().toListWithTimeout()
        }
        delay(100)
        networkCallback?.onCapabilitiesChanged(mock<Network>(), expectedNetworkCapabilities)

        assertThat(deferredList.await().last()).isSameInstanceAs(expectedNetworkCapabilities)
    }
}
