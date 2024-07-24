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

package com.android.settings.wifi.details2

import android.content.Context
import android.net.wifi.WifiConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelectable
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.wifitrackerlib.WifiEntry
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class WifiPrivacyPageProviderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var mockWifiConfiguration = mock<WifiConfiguration>() {
        on { isSendDhcpHostnameEnabled } doReturn true
    }
    private var mockWifiEntry = mock<WifiEntry>() {
        on { canSetPrivacy() } doReturn true
        on { privacy } doReturn 0
        on { wifiConfiguration } doReturn mockWifiConfiguration
    }

    @Test
    fun apnEditPageProvider_name() {
        Truth.assertThat(WifiPrivacyPageProvider.name).isEqualTo("WifiPrivacy")
    }

    @Test
    fun title_displayed() {
        composeTestRule.setContent {
            WifiPrivacyPage(mockWifiEntry)
        }
        composeTestRule.onNodeWithText(
            context.getString(R.string.wifi_privacy_settings)
        ).assertIsDisplayed()
    }

    @Test
    fun category_mac_title_displayed() {
        composeTestRule.setContent {
            WifiPrivacyPage(mockWifiEntry)
        }
        composeTestRule.onNodeWithText(
            context.getString(R.string.wifi_privacy_mac_settings)
        ).assertIsDisplayed()
    }

    @Test
    fun category_mac_list_displayed() {
        composeTestRule.setContent {
            WifiPrivacyPage(mockWifiEntry)
        }
        val wifiPrivacyEntries = context.resources.getStringArray(R.array.wifi_privacy_entries)
        for (entry in wifiPrivacyEntries) {
            composeTestRule.onNodeWithText(
                entry
            ).assertIsDisplayed()
        }
    }

    @Test
    fun category_mac_list_selectable() {
        composeTestRule.setContent {
            WifiPrivacyPage(mockWifiEntry)
        }
        val wifiPrivacyEntries = context.resources.getStringArray(R.array.wifi_privacy_entries)
        for (entry in wifiPrivacyEntries) {
            composeTestRule.onNodeWithText(
                entry
            ).assertIsSelectable()
        }
    }

    @Test
    fun category_mac_list_default_selected() {
        composeTestRule.setContent {
            WifiPrivacyPage(mockWifiEntry)
        }
        val wifiPrivacyEntries = context.resources.getStringArray(R.array.wifi_privacy_entries)
        val wifiPrivacyValues = context.resources.getStringArray(R.array.wifi_privacy_values)
        composeTestRule.onNodeWithText(
            wifiPrivacyEntries[wifiPrivacyValues.indexOf("0")]
        ).assertIsSelected()
    }

    @Test
    fun category_mac_list_not_enabled() {
        mockWifiEntry.stub {
            on { canSetPrivacy() } doReturn false
        }
        composeTestRule.setContent {
            WifiPrivacyPage(mockWifiEntry)
        }
        val wifiPrivacyEntries = context.resources.getStringArray(R.array.wifi_privacy_entries)
        for (entry in wifiPrivacyEntries) {
            composeTestRule.onNodeWithText(entry).assertIsNotEnabled()
        }
    }

    @Test
    fun category_send_device_name_title_displayed() {
        composeTestRule.setContent {
            WifiPrivacyPage(mockWifiEntry)
        }
        composeTestRule.onNodeWithText(
            context.getString(R.string.wifi_privacy_device_name_settings)
        ).assertIsDisplayed()
    }

    @Test
    fun toggle_send_device_name_title_displayed() {
        composeTestRule.setContent {
            WifiPrivacyPage(mockWifiEntry)
        }
        composeTestRule.onNodeWithText(
            context.getString(R.string.wifi_privacy_send_device_name_toggle_title)
        ).assertIsDisplayed()
    }

    @Test
    fun send_device_name_turnOn() {
        composeTestRule.setContent {
            WifiPrivacyPage(mockWifiEntry)
        }
        composeTestRule.onNodeWithText(
            context.getString(R.string.wifi_privacy_send_device_name_toggle_title)
        ).assertIsOn()
    }

    @Test
    fun onClick_turnOff() {
        composeTestRule.setContent {
            WifiPrivacyPage(mockWifiEntry)
        }

        composeTestRule.onNodeWithText(
            context.getString(R.string.wifi_privacy_send_device_name_toggle_title)
        ).performClick()

        composeTestRule.onNodeWithText(
            context.getString(R.string.wifi_privacy_send_device_name_toggle_title)
        ).assertIsOff()
    }
}