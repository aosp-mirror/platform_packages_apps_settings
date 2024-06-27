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
import android.telephony.TelephonyManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spa.testutils.hasRole
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApnNetworkTypeCheckBoxTest {
    @get:Rule val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val apnData = ApnData()

    @Test
    fun networkType_displayed() {
        composeTestRule.setContent { ApnNetworkTypeCheckBox(apnData) {} }

        composeTestRule.onNodeWithText(context.getString(R.string.network_type)).assertIsDisplayed()
    }

    @Test
    fun networkType_changed() {
        composeTestRule.setContent { ApnNetworkTypeCheckBox(apnData) {} }

        composeTestRule.onNodeWithText(context.getString(R.string.network_type)).performClick()
        composeTestRule.onNode(hasText(LTE_TEXT) and isToggleable()).performClick()

        composeTestRule
            .onDropdownListWithText(context.getString(R.string.network_type_unspecified))
            .assertDoesNotExist()
        composeTestRule.onDropdownListWithText(LTE_TEXT).assertIsDisplayed()
    }

    @Test
    fun networkType_changed_back2Default() {
        composeTestRule.setContent { ApnNetworkTypeCheckBox(apnData) {} }

        composeTestRule.onNodeWithText(context.getString(R.string.network_type)).performClick()
        composeTestRule.onNode(hasText(LTE_TEXT) and isToggleable()).performClick()
        composeTestRule.onNode(hasText(LTE_TEXT) and isToggleable()).performClick()

        composeTestRule
            .onDropdownListWithText(context.getString(R.string.network_type_unspecified))
            .assertIsDisplayed()
        composeTestRule.onDropdownListWithText(LTE_TEXT).assertDoesNotExist()
    }

    private fun ComposeTestRule.onDropdownListWithText(text: String) =
        onNode(hasText(text) and hasRole(Role.DropdownList))

    private companion object {
        val LTE_TEXT = TelephonyManager.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_LTE)
    }
}
