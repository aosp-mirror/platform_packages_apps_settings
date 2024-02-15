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

package com.android.settings.network.apn

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class ApnEditPageProviderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val apnName = "apn_name"
    private val proxy = "proxy"
    private val port = "port"
    private val apnType = context.resources.getString(R.string.apn_type)
    private val apnRoaming = "IPv4"
    private val apnEnable = context.resources.getString(R.string.carrier_enabled)
    private val apnProtocolOptions =
        context.resources.getStringArray(R.array.apn_protocol_entries).toList()
    private val networkType = context.resources.getString(R.string.network_type)
    private val passwordTitle = context.resources.getString(R.string.apn_password)
    private val apnInit = ApnData(
        name = apnName,
        proxy = proxy,
        port = port,
        apnType = apnType,
        apnRoaming = apnProtocolOptions.indexOf(apnRoaming),
        apnEnable = true
    )
    private val apnData = mutableStateOf(
        apnInit
    )
    private val uri = mock<Uri> {}

    @Test
    fun apnEditPageProvider_name() {
        Truth.assertThat(ApnEditPageProvider.name).isEqualTo("ApnEdit")
    }

    @Test
    fun title_displayed() {
        composeTestRule.setContent {
            ApnPage(apnInit, remember { apnData }, uri)
        }
        composeTestRule.onNodeWithText(context.getString(R.string.apn_edit)).assertIsDisplayed()
    }

    @Test
    fun name_displayed() {
        composeTestRule.setContent {
            ApnPage(apnInit, remember { apnData }, uri)
        }
        composeTestRule.onNodeWithText(apnName, true).assertIsDisplayed()
    }

    @Test
    fun proxy_displayed() {
        composeTestRule.setContent {
            ApnPage(apnInit, remember { apnData }, uri)
        }
        composeTestRule.onRoot().onChild().onChildAt(0)
            .performScrollToNode(hasText(proxy, true))
        composeTestRule.onNodeWithText(proxy, true).assertIsDisplayed()
    }

    @Test
    fun port_displayed() {
        composeTestRule.setContent {
            ApnPage(apnInit, remember { apnData }, uri)
        }
        composeTestRule.onRoot().onChild().onChildAt(0)
            .performScrollToNode(hasText(port, true))
        composeTestRule.onNodeWithText(port, true).assertIsDisplayed()
    }

    @Test
    fun apn_type_displayed() {
        composeTestRule.setContent {
            ApnPage(apnInit, remember { apnData }, uri)
        }
        composeTestRule.onRoot().onChild().onChildAt(0)
            .performScrollToNode(hasText(apnType, true))
        composeTestRule.onNodeWithText(apnType, true).assertIsDisplayed()
    }

    @Test
    fun apn_roaming_displayed() {
        composeTestRule.setContent {
            ApnPage(apnInit, remember { apnData }, uri)
        }
        composeTestRule.onRoot().onChild().onChildAt(0)
            .performScrollToNode(hasText(apnRoaming, true))
        composeTestRule.onNodeWithText(apnRoaming, true).assertIsDisplayed()
    }

    @Test
    fun carrier_enabled_displayed() {
        composeTestRule.setContent {
            ApnPage(apnInit, remember { apnData }, uri)
        }
        composeTestRule.onRoot().onChild().onChildAt(0)
            .performScrollToNode(hasText(apnEnable, true))
        composeTestRule.onNodeWithText(apnEnable, true).assertIsDisplayed()
    }

    @Test
    fun carrier_enabled_isChecked() {
        composeTestRule.setContent {
            ApnPage(apnInit, remember { apnData }, uri)
        }
        composeTestRule.onRoot().onChild().onChildAt(0)
            .performScrollToNode(hasText(apnEnable, true))
        composeTestRule.onNodeWithText(apnEnable, true).assertIsOn()
    }

    @Test
    fun carrier_enabled_checkChanged() {
        composeTestRule.setContent {
            ApnPage(apnInit, remember { apnData }, uri)
        }
        composeTestRule.onRoot().onChild().onChildAt(0)
            .performScrollToNode(hasText(apnEnable, true))
        composeTestRule.onNodeWithText(apnEnable, true).performClick()
        composeTestRule.onNodeWithText(apnEnable, true).assertIsOff()
    }

    @Test
    fun network_type_displayed() {
        composeTestRule.setContent {
            ApnPage(apnInit, remember { apnData }, uri)
        }
        composeTestRule.onRoot().onChild().onChildAt(0)
            .performScrollToNode(hasText(networkType, true))
        composeTestRule.onNodeWithText(networkType, true).assertIsDisplayed()
    }

    @Test
    fun network_type_changed() {
        composeTestRule.setContent {
            ApnPage(apnInit, remember { apnData }, uri)
        }
        composeTestRule.onRoot().onChild().onChildAt(0)
            .performScrollToNode(hasText(networkType, true))
        composeTestRule.onNodeWithText(networkType, true).performClick()
        composeTestRule.onNodeWithText(NETWORK_TYPE_LTE, true).performClick()
        composeTestRule.onNode(hasText(NETWORK_TYPE_UNSPECIFIED) and isFocused(), true)
            .assertDoesNotExist()
        composeTestRule.onNode(hasText(NETWORK_TYPE_LTE) and isFocused(), true).assertIsDisplayed()
    }

    @Test
    fun network_type_changed_back2Default() {
        composeTestRule.setContent {
            ApnPage(apnInit, remember { apnData }, uri)
        }
        composeTestRule.onRoot().onChild().onChildAt(0)
            .performScrollToNode(hasText(networkType, true))
        composeTestRule.onNodeWithText(networkType, true).performClick()
        composeTestRule.onNodeWithText(NETWORK_TYPE_LTE, true).performClick()
        composeTestRule.onNode(hasText(NETWORK_TYPE_UNSPECIFIED) and isFocused(), true)
            .assertDoesNotExist()
        composeTestRule.onNode(hasText(NETWORK_TYPE_LTE) and isFocused(), true).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(NETWORK_TYPE_LTE, true).onLast().performClick()
        composeTestRule.onNode(hasText(NETWORK_TYPE_UNSPECIFIED) and isFocused(), true)
            .assertIsDisplayed()
        composeTestRule.onNode(hasText(NETWORK_TYPE_LTE) and isFocused(), true).assertDoesNotExist()
    }

    @Test
    fun password_displayed() {
        composeTestRule.setContent {
            ApnPage(apnInit, remember { apnData }, uri)
        }
        composeTestRule.onRoot().onChild().onChildAt(0)
            .performScrollToNode(hasText(passwordTitle, true))
        composeTestRule.onNodeWithText(passwordTitle, true).assertIsDisplayed()
    }

    private companion object {
        const val NETWORK_TYPE_UNSPECIFIED = "Unspecified"
        const val NETWORK_TYPE_LTE = "LTE"
    }
}