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
import android.platform.test.annotations.RequiresFlagsEnabled
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.wifitrackerlib.WifiEntry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ServerNamePreferenceControllerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        doNothing().whenever(mock).startActivity(any())
    }
    private val controller = ServerNamePreferenceController(context, TEST_KEY)

    private val mockCertificateInfo = mock<WifiEntry.CertificateInfo> {
        it.domain = DOMAIN
    }

    private val mockWifiEntry =
        mock<WifiEntry> { on { certificateInfo } doReturn mockCertificateInfo }

    @Before
    fun setUp() {
        controller.setWifiEntry(mockWifiEntry)
    }

    @Test
    @RequiresFlagsEnabled(com.android.wifi.flags.Flags.FLAG_ANDROID_V_WIFI_API)
    fun title_isDisplayed() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                controller.Content()
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.server_name_title))
            .assertIsDisplayed()
    }

    @Test
    @RequiresFlagsEnabled(com.android.wifi.flags.Flags.FLAG_ANDROID_V_WIFI_API)
    fun summary_isDisplayed() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                controller.Content()
            }
        }

        composeTestRule.onNodeWithText(DOMAIN).assertIsDisplayed()
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val DOMAIN = "domain"
    }
}