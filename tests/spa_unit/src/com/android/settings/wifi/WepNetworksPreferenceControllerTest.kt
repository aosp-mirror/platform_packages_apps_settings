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

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.spa.preference.ComposePreference
import com.android.settingslib.spa.testutils.onDialogText
import com.android.wifitrackerlib.WifiEntry
import java.util.function.Consumer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy

@RunWith(AndroidJUnit4::class)
class WepNetworksPreferenceControllerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var wepAllowed = true

    private var mockWifiInfo = mock<android.net.wifi.WifiInfo> {
        on { it.currentSecurityType } doReturn WifiEntry.SECURITY_EAP
        on { it.ssid } doReturn SSID
    }

    private var mockWifiManager = mock<WifiManager> {
        on { queryWepAllowed(any(), any()) } doAnswer {
            @Suppress("UNCHECKED_CAST")
            val consumer = it.arguments[1] as Consumer<Boolean>
            consumer.accept(wepAllowed)
        }
        on { it.isWepSupported } doReturn true
        on { it.connectionInfo } doReturn mockWifiInfo
    }

    private var context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getSystemService(WifiManager::class.java) } doReturn mockWifiManager
        }

    private var controller = WepNetworksPreferenceController(context, TEST_KEY)
    private val preference = ComposePreference(context).apply { key = TEST_KEY }
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    @Before
    fun setUp() {
        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)
    }

    @Test
    fun wepAllowedTrue_turnOn() {
        wepAllowed = true
        composeTestRule.setContent {
            controller.Content()
        }

        composeTestRule.onNodeWithText(context.getString(R.string.wifi_allow_wep_networks))
            .assertIsOn()
    }

    @Test
    fun wepAllowedFalse_turnOff() {
        wepAllowed = false
        composeTestRule.setContent {
            controller.Content()
        }

        composeTestRule.onNodeWithText(context.getString(R.string.wifi_allow_wep_networks))
            .assertIsOff()
    }

    @Test
    fun onClick_turnOn() {
        wepAllowed = false
        composeTestRule.setContent {
            controller.Content()
        }

        composeTestRule.onRoot().performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.wifi_allow_wep_networks))
            .assertIsOn()
    }

    @Test
    fun onClick_turnOff() {
        wepAllowed = true
        composeTestRule.setContent {
            controller.Content()
        }

        composeTestRule.onRoot().performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.wifi_allow_wep_networks))
            .assertIsOff()
    }

    @Test
    fun whenClick_wepAllowed_openDialog() {
        wepAllowed = true
        Mockito.`when`(mockWifiInfo.currentSecurityType).thenReturn(WifiEntry.SECURITY_WEP)
        composeTestRule.setContent {
            controller.Content()
        }

        composeTestRule.onRoot().performClick()
        composeTestRule.onDialogText(context.getString(R.string.wifi_disconnect_button_text))
            .isDisplayed()
    }

    @Test
    fun whenClick_wepDisallowed_openDialog() {
        wepAllowed = false
        Mockito.`when`(mockWifiInfo.currentSecurityType).thenReturn(WifiEntry.SECURITY_WEP)
        composeTestRule.setContent {
            controller.Content()
        }

        composeTestRule.onRoot().performClick()
        composeTestRule.onDialogText(context.getString(R.string.wifi_disconnect_button_text))
            .isNotDisplayed()
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val SSID = "ssid"
    }
}