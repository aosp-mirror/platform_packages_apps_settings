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

package com.android.settings.network.apn

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class ApnEditCarrierEnabledTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {}

    private val resources = spy(context.resources) {}

    @Before
    fun setUp() {
        context.stub { on { resources } doReturn resources }
    }

    @Test
    fun carrierEnabled_displayed() {
        composeTestRule.setContent { ApnEditCarrierEnabled(ApnData()) {} }

        composeTestRule.onCarrierEnabled().assertIsDisplayed()
    }

    @Test
    fun carrierEnabled_isChecked() {
        val apnData = ApnData(carrierEnabled = true)

        composeTestRule.setContent { ApnEditCarrierEnabled(apnData) {} }

        composeTestRule.onCarrierEnabled().assertIsOn()
    }

    @Test
    fun carrierEnabled_allowEdit_checkChanged() {
        resources.stub { on { getBoolean(R.bool.config_allow_edit_carrier_enabled) } doReturn true }
        var apnData by mutableStateOf(ApnData(carrierEnabled = true))
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                ApnEditCarrierEnabled(apnData) { apnData = apnData.copy(carrierEnabled = it) }
            }
        }

        composeTestRule.onCarrierEnabled().performClick()

        composeTestRule.onCarrierEnabled().assertIsEnabled().assertIsOff()
    }

    @Test
    fun carrierEnabled_notAllowEdit_checkNotChanged() {
        resources.stub {
            on { getBoolean(R.bool.config_allow_edit_carrier_enabled) } doReturn false
        }
        var apnData by mutableStateOf(ApnData(carrierEnabled = true))
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                ApnEditCarrierEnabled(apnData) { apnData = apnData.copy(carrierEnabled = it) }
            }
        }

        composeTestRule.onCarrierEnabled().performClick()

        composeTestRule.onCarrierEnabled().assertIsNotEnabled().assertIsOn()
    }

    private fun ComposeTestRule.onCarrierEnabled() =
        onNodeWithText(context.getString(R.string.carrier_enabled))
}
