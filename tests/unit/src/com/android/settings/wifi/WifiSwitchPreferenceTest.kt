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

package com.android.settings.wifi

import android.app.settings.SettingsEnums.ACTION_WIFI_OFF
import android.app.settings.SettingsEnums.ACTION_WIFI_ON
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.net.wifi.WifiManager
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class WifiSwitchPreferenceTest {

    private val mockWifiManager = mock<WifiManager>()
    private val mockConnectivityManager = mock<ConnectivityManager>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getApplicationContext() = this

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(WifiManager::class.java) -> mockWifiManager
                    getSystemServiceName(ConnectivityManager::class.java) -> mockConnectivityManager
                    else -> super.getSystemService(name)
                }
        }

    private val wifiSwitchPreference = WifiSwitchPreference()

    @Test
    fun getValue_defaultOn_returnOn() {
        mockWifiManager.stub { on { isWifiEnabled } doReturn true }

        val getValue = wifiSwitchPreference.storage(context).getBoolean(WifiSwitchPreference.KEY)

        assertThat(getValue).isTrue()
    }

    @Test
    fun getValue_defaultOff_returnOff() {
        mockWifiManager.stub { on { isWifiEnabled } doReturn false }

        val getValue = wifiSwitchPreference.storage(context).getBoolean(WifiSwitchPreference.KEY)

        assertThat(getValue).isFalse()
    }

    @Test
    fun setValue_valueTrue_metricsActionWifiOn() {
        val metricsFeatureProvider = FakeFeatureFactory.setupForTest().metricsFeatureProvider

        wifiSwitchPreference.storage(context).setBoolean(WifiSwitchPreference.KEY, true)

        verify(metricsFeatureProvider).action(context, ACTION_WIFI_ON)
    }

    @Test
    fun setValue_valueFalseWithoutDefaultWifi_metricsActionWifiOffWithFalse() {
        val metricsFeatureProvider = FakeFeatureFactory.setupForTest().metricsFeatureProvider
        mockDefaultNetwork(TRANSPORT_CELLULAR)

        wifiSwitchPreference.storage(context).setBoolean(WifiSwitchPreference.KEY, false)

        verify(metricsFeatureProvider).action(context, ACTION_WIFI_OFF, false)
    }

    @Test
    fun setValue_valueFalseWithDefaultWifi_metricsActionWifiOffWithTrue() {
        val metricsFeatureProvider = FakeFeatureFactory.setupForTest().metricsFeatureProvider
        mockDefaultNetwork(TRANSPORT_WIFI)

        wifiSwitchPreference.storage(context).setBoolean(WifiSwitchPreference.KEY, false)

        verify(metricsFeatureProvider).action(context, ACTION_WIFI_OFF, true)
    }

    @Test
    fun performClick_defaultOn_checkedIsFalse() {
        mockWifiManager.stub { on { isWifiEnabled } doReturn true }

        val preference = getSwitchPreference().apply { performClick() }

        assertThat(preference.isChecked).isFalse()
    }

    @Test
    fun performClick_defaultOff_checkedIsTrue() {
        mockWifiManager.stub { on { isWifiEnabled } doReturn false }

        val preference = getSwitchPreference().apply { performClick() }

        assertThat(preference.isChecked).isTrue()
    }

    private fun getSwitchPreference(): SwitchPreferenceCompat =
        wifiSwitchPreference.createAndBindWidget(context)

    private fun mockDefaultNetwork(transportType: Int) {
        val mockNetwork = mock<Network>()
        val networkCapabilities =
            NetworkCapabilities.Builder.withoutDefaultCapabilities()
                .addTransportType(transportType)
                .build()
        mockConnectivityManager.stub {
            on { activeNetwork } doReturn mockNetwork
            on { getNetworkCapabilities(mockNetwork) } doReturn networkCapabilities
        }
    }
}
