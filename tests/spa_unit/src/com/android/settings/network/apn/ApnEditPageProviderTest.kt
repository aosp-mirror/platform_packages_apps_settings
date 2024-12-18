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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class ApnEditPageProviderTest {
    @get:Rule val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val apnName = "apn_name"
    private val proxy = "proxy"
    private val port = "port"
    private val apnType = context.resources.getString(R.string.apn_type)
    private val apnRoaming = "IPv4"
    private val apnProtocolOptions =
        context.resources.getStringArray(R.array.apn_protocol_entries).toList()
    private val passwordTitle = context.resources.getString(R.string.apn_password)
    private val apnInit =
        ApnData(
            name = apnName,
            proxy = proxy,
            port = port,
            apnType = apnType,
            apnRoaming = apnProtocolOptions.indexOf(apnRoaming),
        )
    private val apnData = mutableStateOf(apnInit)
    private val uri = mock<Uri> {}

    @Test
    fun apnEditPageProvider_name() {
        assertThat(ApnEditPageProvider.name).isEqualTo("ApnEdit")
    }

    @Test
    fun title_displayed() {
        composeTestRule.setContent { ApnPage(apnInit, apnData, uri) }

        composeTestRule.onNodeWithText(context.getString(R.string.apn_edit)).assertIsDisplayed()
    }

    @Test
    fun name_displayed() {
        composeTestRule.setContent { ApnPage(apnInit, apnData, uri) }

        composeTestRule.onNodeWithText(apnName, true).assertIsDisplayed()
    }

    @Test
    fun proxy_displayed() {
        composeTestRule.setContent { ApnPage(apnInit, apnData, uri) }

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText(proxy, true))
        composeTestRule.onNodeWithText(proxy, true).assertIsDisplayed()
    }

    @Test
    fun port_displayed() {
        composeTestRule.setContent { ApnPage(apnInit, apnData, uri) }

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText(port, true))
        composeTestRule.onNodeWithText(port, true).assertIsDisplayed()
    }

    @Test
    fun apnType_displayed() {
        composeTestRule.setContent { ApnPage(apnInit, apnData, uri) }

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText(apnType, true))
        composeTestRule.onNodeWithText(apnType, true).assertIsDisplayed()
    }

    @Test
    fun apnRoaming_displayed() {
        composeTestRule.setContent { ApnPage(apnInit, apnData, uri) }

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText(apnRoaming, true))
        composeTestRule.onNodeWithText(apnRoaming, true).assertIsDisplayed()
    }

    @Test
    fun password_displayed() {
        composeTestRule.setContent { ApnPage(apnInit, apnData, uri) }

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText(passwordTitle, true))
        composeTestRule.onNodeWithText(passwordTitle, true).assertIsDisplayed()
    }
}
