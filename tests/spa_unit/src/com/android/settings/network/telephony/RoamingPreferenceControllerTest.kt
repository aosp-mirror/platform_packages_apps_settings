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

package com.android.settings.network.telephony

import android.content.Context
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.fragment.app.FragmentManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class RoamingPreferenceControllerTest {
    @get:Rule val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockMobileDataRepository =
        mock<MobileDataRepository> {
            on { isDataRoamingEnabledFlow(SUB_ID) } doReturn flowOf(false)
        }

    private val controller =
        RoamingPreferenceController(context, TEST_KEY, mockMobileDataRepository)

    @Before
    fun setUp() {
        CarrierConfigRepository.resetForTest()
    }

    @Test
    fun getAvailabilityStatus_validSubId_returnAvailable() {
        controller.init(mock<FragmentManager>(), SUB_ID)

        val availabilityStatus = controller.getAvailabilityStatus()

        assertThat(availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_invalidSubId_returnConditionallyUnavailable() {
        controller.init(mock<FragmentManager>(), SubscriptionManager.INVALID_SUBSCRIPTION_ID)

        val availabilityStatus = controller.getAvailabilityStatus()

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_forceHomeNetworkIsTrue_returnConditionallyUnavailable() {
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL,
            value = true,
        )
        controller.init(mock<FragmentManager>(), SUB_ID)

        val availabilityStatus = controller.getAvailabilityStatus()

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_forceHomeNetworkIsFalse_returnAvailable() {
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL,
            value = false,
        )
        controller.init(mock<FragmentManager>(), SUB_ID)

        val availabilityStatus = controller.getAvailabilityStatus()

        assertThat(availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    fun title_displayed() {
        controller.init(mock<FragmentManager>(), SUB_ID)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) { controller.Content() }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.roaming)).assertIsDisplayed()
    }

    @Test
    fun summary_displayed() {
        controller.init(mock<FragmentManager>(), SUB_ID)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) { controller.Content() }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.roaming_enable))
            .assertIsDisplayed()
    }

    @Test
    fun isDialogNeeded_enableChargeIndication_returnTrue() {
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_DISABLE_CHARGE_INDICATION_BOOL,
            value = false,
        )
        controller.init(mock<FragmentManager>(), SUB_ID)

        val isDialogNeeded = controller.isDialogNeeded()

        assertThat(isDialogNeeded).isTrue()
    }

    @Test
    fun isDialogNeeded_disableChargeIndication_returnFalse() {
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_DISABLE_CHARGE_INDICATION_BOOL,
            value = true,
        )
        controller.init(mock<FragmentManager>(), SUB_ID)

        val isDialogNeeded = controller.isDialogNeeded()

        assertThat(isDialogNeeded).isFalse()
    }

    @Test
    fun checked_roamingEnabled_isOn() {
        mockMobileDataRepository.stub {
            on { isDataRoamingEnabledFlow(SUB_ID) } doReturn flowOf(true)
        }
        controller.init(mock<FragmentManager>(), SUB_ID)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) { controller.Content() }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.roaming)).assertIsOn()
    }

    @Test
    fun checked_roamingDisabled_isOff() {
        mockMobileDataRepository.stub {
            on { isDataRoamingEnabledFlow(SUB_ID) } doReturn flowOf(false)
        }
        controller.init(mock<FragmentManager>(), SUB_ID)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) { controller.Content() }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.roaming)).assertIsOff()
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val SUB_ID = 2
    }
}
