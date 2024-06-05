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
import android.content.Intent
import android.net.wifi.WifiManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spa.framework.util.KEY_DESTINATION
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class WifiPrivacyPreferenceControllerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockWifiManager = mock<WifiManager> {
        on { isConnectedMacRandomizationSupported } doReturn true
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(WifiManager::class.java) } doReturn mockWifiManager
        doNothing().whenever(mock).startActivity(any())
    }

    private val controller = WifiPrivacyPreferenceController(context, TEST_KEY)

    @Test
    fun title_isDisplayed() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                controller.Content()
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.wifi_privacy_settings))
            .assertIsDisplayed()
    }

    @Test
    fun onClick_startWifiPrivacyPage() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                controller.setWifiEntryKey("")
                controller.Content()
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.wifi_privacy_settings))
            .performClick()

        val intent = argumentCaptor<Intent> {
            verify(context).startActivity(capture())
        }.firstValue
        Truth.assertThat(intent.getStringExtra(KEY_DESTINATION))
            .isEqualTo(WifiPrivacyPageProvider.getRoute(""))
    }

    private companion object {
        const val TEST_KEY = "test_key"
    }
}