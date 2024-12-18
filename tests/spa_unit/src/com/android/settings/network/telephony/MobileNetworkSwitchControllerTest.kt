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
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spa.testutils.waitUntilExists
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class MobileNetworkSwitchControllerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockSubscriptionManager = mock<SubscriptionManager> {
        on { isSubscriptionEnabled(SUB_ID) } doReturn true
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { subscriptionManager } doReturn mockSubscriptionManager
        doNothing().whenever(mock).startActivity(any())
    }

    private val mockSubscriptionRepository = mock<SubscriptionRepository> {
        on { getSelectableSubscriptionInfoList() } doReturn listOf(SubInfo)
        on { isSubscriptionEnabledFlow(SUB_ID) } doReturn flowOf(false)
    }

    private val mockSubscriptionActivationRepository = mock<SubscriptionActivationRepository> {
        on { isActivationChangeableFlow() } doReturn flowOf(true)
    }

    private val controller = MobileNetworkSwitchController(
        context = context,
        preferenceKey = TEST_KEY,
        subscriptionRepository = mockSubscriptionRepository,
        subscriptionActivationRepository = mockSubscriptionActivationRepository,
    ).apply { init(SUB_ID) }

    @Test
    fun isVisible_pSimAndCanDisablePhysicalSubscription_returnTrue() {
        val pSimSubInfo = SubscriptionInfo.Builder().apply {
            setId(SUB_ID)
            setEmbedded(false)
        }.build()
        mockSubscriptionManager.stub {
            on { canDisablePhysicalSubscription() } doReturn true
        }
        mockSubscriptionRepository.stub {
            on { getSelectableSubscriptionInfoList() } doReturn listOf(pSimSubInfo)
        }

        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.mobile_network_use_sim_on))
            .assertIsDisplayed()
    }

    @Test
    fun isVisible_pSimAndCannotDisablePhysicalSubscription_returnFalse() {
        val pSimSubInfo = SubscriptionInfo.Builder().apply {
            setId(SUB_ID)
            setEmbedded(false)
        }.build()
        mockSubscriptionManager.stub {
            on { canDisablePhysicalSubscription() } doReturn false
        }
        mockSubscriptionRepository.stub {
            on { getSelectableSubscriptionInfoList() } doReturn listOf(pSimSubInfo)
        }

        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.mobile_network_use_sim_on))
            .assertDoesNotExist()
    }

    @Test
    fun isVisible_eSim_returnTrue() {
        val eSimSubInfo = SubscriptionInfo.Builder().apply {
            setId(SUB_ID)
            setEmbedded(true)
        }.build()
        mockSubscriptionRepository.stub {
            on { getSelectableSubscriptionInfoList() } doReturn listOf(eSimSubInfo)
        }

        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.mobile_network_use_sim_on))
            .assertIsDisplayed()
    }

    @Test
    fun isChecked_subscriptionEnabled_switchIsOn() {
        mockSubscriptionRepository.stub {
            on { isSubscriptionEnabledFlow(SUB_ID) } doReturn flowOf(true)
        }

        setContent()

        composeTestRule.waitUntilExists(
            hasText(context.getString(R.string.mobile_network_use_sim_on)) and isOn()
        )
    }

    @Test
    fun isChecked_subscriptionNotEnabled_switchIsOff() {
        mockSubscriptionRepository.stub {
            on { isSubscriptionEnabledFlow(SUB_ID) } doReturn flowOf(false)
        }

        setContent()

        composeTestRule.waitUntilExists(
            hasText(context.getString(R.string.mobile_network_use_sim_on)) and isOff()
        )
    }

    private fun setContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                controller.Content()
            }
        }
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val SUB_ID = 123

        val SubInfo: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(SUB_ID)
            setEmbedded(true)
        }.build()
    }
}
