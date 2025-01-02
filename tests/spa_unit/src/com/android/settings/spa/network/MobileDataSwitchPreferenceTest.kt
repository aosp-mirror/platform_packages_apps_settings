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

package com.android.settings.spa.network

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.network.telephony.MobileDataRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class MobileDataSwitchPreferenceTest {
    @get:Rule val composeTestRule = createComposeRule()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {}

    private val mockMobileDataRepository =
        mock<MobileDataRepository> { on { isMobileDataEnabledFlow(any()) } doReturn emptyFlow() }

    @Test
    fun title_displayed() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                MobileDataSwitchPreference(SUB_ID, mockMobileDataRepository) {}
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.mobile_data_settings_title))
            .assertIsDisplayed()
    }

    @Test
    fun summary_displayed() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                MobileDataSwitchPreference(SUB_ID, mockMobileDataRepository) {}
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.mobile_data_settings_summary))
            .assertIsDisplayed()
    }

    @Test
    fun onClick_whenOff_turnedOn() {
        mockMobileDataRepository.stub {
            on { isMobileDataEnabledFlow(SUB_ID) } doReturn flowOf(false)
        }
        var newCheckedCalled: Boolean? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                MobileDataSwitchPreference(SUB_ID, mockMobileDataRepository) {
                    newCheckedCalled = it
                }
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.mobile_data_settings_title))
            .performClick()

        assertThat(newCheckedCalled).isTrue()
    }

    private companion object {
        const val SUB_ID = 12
    }
}
