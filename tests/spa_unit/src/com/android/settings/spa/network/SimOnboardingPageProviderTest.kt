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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.network.SimOnboardingService
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class SimOnboardingPageProviderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var mockSimOnboardingService = mock<SimOnboardingService> {
        on { targetSubId }.doReturn(SUB_ID)
        on { targetSubInfo }.doReturn(null)
        on { availableSubInfoList }.doReturn(listOf())
        on { activeSubInfoList }.doReturn(listOf())
        on { slotInfoList }.doReturn(listOf())
        on { uiccCardInfoList }.doReturn(listOf())

        on { targetPrimarySimCalls }.doReturn(PRIMARY_SIM_ASK_EVERY_TIME)
        on { targetPrimarySimTexts }.doReturn(PRIMARY_SIM_ASK_EVERY_TIME)
        on { targetPrimarySimMobileData }.doReturn(PRIMARY_SIM_ASK_EVERY_TIME)
    }

    @Test
    fun simOnboardingPageProvider_name() {
        assertThat(SimOnboardingPageProvider.name).isEqualTo("SimOnboardingPageProvider")
    }

    @Test
    fun simOnboardingPage_labelSim() {
        composeTestRule.setContent {
            val navHostController = rememberNavController()
            PageImpl(mockSimOnboardingService, navHostController)
        }

        composeTestRule.onNodeWithText(context.getString(R.string.sim_onboarding_label_sim_title))
            .assertIsDisplayed()
    }

    @Test
    fun simOnboardingPage_nextAction_fromLabelSimToPrimarySim() {
        mockSimOnboardingService.stub {
            on { isMultipleEnabledProfilesSupported }.thenReturn(false)
        }
        composeTestRule.setContent {
            val navHostController = rememberNavController()
            PageImpl(mockSimOnboardingService, navHostController)
        }

        composeTestRule.onNodeWithText(context.getString(R.string.sim_onboarding_next))
            .performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.sim_onboarding_primary_sim_title))
            .assertIsDisplayed()
    }

    @Test
    fun simOnboardingPage_nextAction_fromLabelSimToSelectSim() {
        mockSimOnboardingService.stub {
            on { isMultipleEnabledProfilesSupported }.thenReturn(true)
            on { isAllOfSlotAssigned }.thenReturn(true)
        }

        composeTestRule.setContent {
            val navHostController = rememberNavController()
            PageImpl(mockSimOnboardingService, navHostController)
        }

        composeTestRule.onNodeWithText(context.getString(R.string.sim_onboarding_next))
            .performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.sim_onboarding_select_sim_title))
            .assertIsDisplayed()
    }

    @Test
    fun simOnboardingPage_nextAction_fromSelectSimToPrimarySim() {
        composeTestRule.setContent {
            val navHostController = rememberNavController()
            PageImpl(mockSimOnboardingService, navHostController)
        }

        composeTestRule.onNodeWithText(context.getString(R.string.sim_onboarding_next))
            .performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.sim_onboarding_primary_sim_title))
            .assertIsDisplayed()
    }

    private companion object {
        const val SUB_ID = 1
        const val PRIMARY_SIM_ASK_EVERY_TIME = -1
    }
}
