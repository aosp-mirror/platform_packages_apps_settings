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
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.network.SimOnboardingService
import com.android.settingslib.spa.testutils.waitUntilExists
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SimOnboardingSelectSimTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val mockSubscriptionManager = mock<SubscriptionManager> {
        on { addOnSubscriptionsChangedListener(any(), any()) } doAnswer {
            val listener = it.arguments[1] as SubscriptionManager.OnSubscriptionsChangedListener
            listener.onSubscriptionsChanged()
        }
        on { getPhoneNumber(SUB_ID_1) } doReturn NUMBER_1
        on { getPhoneNumber(SUB_ID_2) } doReturn NUMBER_2
        on { getPhoneNumber(SUB_ID_3) } doReturn NUMBER_3
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(SubscriptionManager::class.java) } doReturn mockSubscriptionManager
    }

    private var mockSimOnboardingService = mock<SimOnboardingService> {
        on { targetSubId }.doReturn(-1)
        on { targetSubInfo }.doReturn(null)
        on { availableSubInfoList }.doReturn(listOf())
        on { activeSubInfoList }.doReturn(listOf())
        on { uiccCardInfoList }.doReturn(listOf())

        on { targetPrimarySimCalls }.doReturn(PRIMARY_SIM_ASK_EVERY_TIME)
        on { targetPrimarySimTexts }.doReturn(PRIMARY_SIM_ASK_EVERY_TIME)
        on { targetPrimarySimMobileData }.doReturn(PRIMARY_SIM_ASK_EVERY_TIME)
    }

    private val nextAction: () -> Unit = mock()
    private val cancelAction: () -> Unit = mock()

    @Test
    fun simOnboardingSelectSimImpl_showTitle() {
        composeTestRule.setContent {
            SimOnboardingSelectSimImpl(nextAction, cancelAction, mockSimOnboardingService)
        }

        composeTestRule.onNodeWithText(context.getString(R.string.sim_onboarding_select_sim_title))
            .assertIsDisplayed()
    }

    @Test
    fun simOnboardingSelectSimImpl_showSubTitle() {
        composeTestRule.setContent {
            SimOnboardingSelectSimImpl(nextAction, cancelAction, mockSimOnboardingService)
        }

        composeTestRule.onNodeWithText(context.getString(R.string.sim_onboarding_select_sim_msg))
            .assertIsDisplayed()
    }

    @Test
    fun simOnboardingSelectSimImpl_clickNextAction_verifyNextAction() {
        mockSimOnboardingService.stub {
            on { targetSubId }.doReturn(SUB_ID_1)
            on { targetSubInfo }.doReturn(SUB_INFO_1)
            on { availableSubInfoList }.doReturn(listOf(SUB_INFO_1, SUB_INFO_2, SUB_INFO_3))
            on { activeSubInfoList }.doReturn(listOf(SUB_INFO_2, SUB_INFO_3))
            on { getSelectableSubscriptionInfoList() }.doReturn(
                listOf(
                    SUB_INFO_1,
                    SUB_INFO_2,
                    SUB_INFO_3
                )
            )
            on { getSubscriptionInfoDisplayName(SUB_INFO_1) }.doReturn(DISPLAY_NAME_1)
            on { getSubscriptionInfoDisplayName(SUB_INFO_2) }.doReturn(DISPLAY_NAME_2)
            on { getSubscriptionInfoDisplayName(SUB_INFO_3) }.doReturn(DISPLAY_NAME_3)
            on {isSimSelectionFinished}.doReturn(true)
        }

        composeTestRule.setContent {
            SimOnboardingSelectSimImpl(nextAction, cancelAction, mockSimOnboardingService)
        }

        composeTestRule.onNodeWithText(context.getString(R.string.sim_onboarding_next))
            .performClick()

        verify(nextAction)()
    }

    @Test
    fun simOnboardingSelectSimImpl_clickCancelAction_verifyCancelAction() {
        composeTestRule.setContent {
            SimOnboardingSelectSimImpl(nextAction, cancelAction, mockSimOnboardingService)
        }

        composeTestRule.onNodeWithText(context.getString(R.string.cancel))
            .performClick()

        verify(cancelAction)()
    }

    @Test
    fun simOnboardingSelectSimImpl_showItem_show3Items() {
        mockSimOnboardingService.stub {
            on { targetSubId }.doReturn(SUB_ID_1)
            on { targetSubInfo }.doReturn(SUB_INFO_1)
            on { availableSubInfoList }.doReturn(listOf(SUB_INFO_1, SUB_INFO_2, SUB_INFO_3))
            on { activeSubInfoList }.doReturn(listOf(SUB_INFO_2, SUB_INFO_3))
            on { getSelectableSubscriptionInfoList() }.doReturn(
                listOf(
                    SUB_INFO_1,
                    SUB_INFO_2,
                    SUB_INFO_3
                )
            )
            on { getSubscriptionInfoDisplayName(SUB_INFO_1) }.doReturn(DISPLAY_NAME_1)
            on { getSubscriptionInfoDisplayName(SUB_INFO_2) }.doReturn(DISPLAY_NAME_2)
            on { getSubscriptionInfoDisplayName(SUB_INFO_3) }.doReturn(DISPLAY_NAME_3)
        }

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalLifecycleOwner provides TestLifecycleOwner(),
            ) {
                SimOnboardingSelectSimImpl(nextAction, cancelAction, mockSimOnboardingService)
            }
        }
//        composeTestRule.setContent {
//            SimOnboardingSelectSimImpl(nextAction, cancelAction, mockSimOnboardingService)
//        }

        composeTestRule.onNodeWithText(DISPLAY_NAME_1).assertIsDisplayed()
        composeTestRule.waitUntilExists(hasText(NUMBER_1))
        composeTestRule.onNodeWithText(DISPLAY_NAME_2).assertIsDisplayed()
        composeTestRule.waitUntilExists(hasText(NUMBER_2))
        composeTestRule.onNodeWithText(DISPLAY_NAME_3).assertIsDisplayed()
        composeTestRule.waitUntilExists(hasText(NUMBER_3))
    }

    private companion object {
        const val SUB_ID_1 = 1
        const val SUB_ID_2 = 2
        const val SUB_ID_3 = 3
        const val DISPLAY_NAME_1 = "Sub 1"
        const val DISPLAY_NAME_2 = "Sub 2"
        const val DISPLAY_NAME_3 = "Sub 3"
        const val NUMBER_1 = "000000001"
        const val NUMBER_2 = "000000002"
        const val NUMBER_3 = "000000003"
        const val MCC = "310"
        const val PRIMARY_SIM_ASK_EVERY_TIME = -1

        val SUB_INFO_1: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(SUB_ID_1)
            setDisplayName(DISPLAY_NAME_1)
            setNumber(NUMBER_1)
            setMcc(MCC)
        }.build()

        val SUB_INFO_2: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(SUB_ID_2)
            setDisplayName(DISPLAY_NAME_2)
            setNumber(NUMBER_2)
            setMcc(MCC)
        }.build()

        val SUB_INFO_3: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(SUB_ID_3)
            setDisplayName(DISPLAY_NAME_3)
            setNumber(NUMBER_3)
            setMcc(MCC)
        }.build()
    }
}
