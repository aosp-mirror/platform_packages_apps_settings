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

package com.android.settings.network

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkPolicyManager
import android.net.VpnManager
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class BluetoothWiFiResetPreferenceControllerTest {

    private val mockUserManager = mock<UserManager>()
    private val mockBluetoothAdapter = mock<BluetoothAdapter>()
    private val mockBluetoothManager = mock<BluetoothManager> {
        on { adapter } doReturn mockBluetoothAdapter
    }
    private val mockConnectivityManager = mock<ConnectivityManager>()
    private val mockNetworkPolicyManager = mock<NetworkPolicyManager>()
    private val mockVpnManager = mock<VpnManager>()
    private val mockResources = mock<Resources>()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(Context.USER_SERVICE) } doReturn mockUserManager
        on { getSystemService(Context.BLUETOOTH_SERVICE) } doReturn mockBluetoothManager
        on { getSystemService(Context.CONNECTIVITY_SERVICE) } doReturn mockConnectivityManager
        on { getSystemService(Context.NETWORK_POLICY_SERVICE) } doReturn mockNetworkPolicyManager
        on { getSystemService(Context.VPN_MANAGEMENT_SERVICE) } doReturn mockVpnManager
        on { resources } doReturn mockResources
    }

    private val controller = BluetoothWiFiResetPreferenceController(context, TEST_KEY)

    @Test
    fun getAvailabilityStatus_isAdminUser_returnAvailable() {
        mockUserManager.stub {
            on { isAdminUser } doReturn true
        }

        val availabilityStatus = controller.getAvailabilityStatus()

        assertThat(availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_notAdminUser_returnConditionallyUnavailable() {
        mockUserManager.stub {
            on { isAdminUser } doReturn false
        }

        val availabilityStatus = controller.getAvailabilityStatus()

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun resetOperation_resetBluetooth() {
        controller.resetOperation().run()

        verify(mockBluetoothAdapter).clearBluetooth()
    }

    @Test
    fun resetOperation_onDeviceWithSimVisible_notResetConnectivity() {
        mockResources.stub {
            on { getBoolean(R.bool.config_show_sim_info) } doReturn true
        }

        controller.resetOperation().run()

        verify(mockConnectivityManager, never()).factoryReset()
    }

    @Test
    fun resetOperation_onDeviceWithSimInvisible_resetVpn() {
        mockResources.stub {
            on { getBoolean(R.bool.config_show_sim_info) } doReturn false
        }

        controller.resetOperation().run()

        verify(mockVpnManager).factoryReset()
    }

    private companion object {
        const val TEST_KEY = "test_key"
    }
}
