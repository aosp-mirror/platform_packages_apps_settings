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

package com.android.settings.network.telephony.gsm

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.telephony.CarrierConfigManager
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.core.os.persistableBundleOf
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.Settings.NetworkSelectActivity
import com.android.settings.spa.preference.ComposePreference
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
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
class AutoSelectPreferenceControllerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockTelephonyManager = mock<TelephonyManager> {
        on { createForSubscriptionId(SUB_ID) } doReturn mock
        on { simOperatorName } doReturn OPERATOR_NAME
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
        doNothing().whenever(mock).startActivity(any())
    }

    private val preference = ComposePreference(context).apply { key = TEST_KEY }
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    private val serviceState = ServiceState()

    private val carrierConfig = persistableBundleOf()

    private val controller = AutoSelectPreferenceController(
        context = context,
        key = TEST_KEY,
        allowedNetworkTypesFlowFactory = { emptyFlow() },
        serviceStateFlowFactory = { flowOf(serviceState) },
        getConfigForSubId = { carrierConfig },
    ).init(subId = SUB_ID)

    @Before
    fun setUp() {
        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)
    }

    @Test
    fun isChecked_isAutoSelection_on() {
        serviceState.isManualSelection = false

        composeTestRule.setContent {
            controller.Content()
        }

        composeTestRule.onNodeWithText(context.getString(R.string.select_automatically))
            .assertIsOn()
    }

    @Test
    fun isChecked_isManualSelection_off() {
        serviceState.isManualSelection = true

        composeTestRule.setContent {
            controller.Content()
        }

        composeTestRule.onNodeWithText(context.getString(R.string.select_automatically))
            .assertIsOff()
    }


    @Test
    fun isEnabled_isRoaming_enabled() {
        serviceState.roaming = true

        composeTestRule.setContent {
            controller.Content()
        }

        composeTestRule.onNodeWithText(context.getString(R.string.select_automatically))
            .assertIsEnabled()
    }

    @Test
    fun isEnabled_notOnlyAutoSelectInHome_enabled() {
        serviceState.roaming = false
        carrierConfig.putBoolean(
            CarrierConfigManager.KEY_ONLY_AUTO_SELECT_IN_HOME_NETWORK_BOOL, false
        )

        composeTestRule.setContent {
            controller.Content()
        }

        composeTestRule.onNodeWithText(context.getString(R.string.select_automatically))
            .assertIsEnabled()
    }

    @Test
    fun isEnabled_onlyAutoSelectInHome_notEnabled() {
        serviceState.roaming = false
        carrierConfig.putBoolean(
            CarrierConfigManager.KEY_ONLY_AUTO_SELECT_IN_HOME_NETWORK_BOOL, true
        )

        composeTestRule.setContent {
            controller.Content()
        }

        composeTestRule.onNodeWithText("Unavailable when connected to T-mobile")
            .assertIsNotEnabled()
    }

    @Test
    fun onClick_turnOff_startNetworkSelectActivity() {
        serviceState.isManualSelection = false

        composeTestRule.setContent {
            controller.Content()
        }
        composeTestRule.onRoot().performClick()

        val intent = argumentCaptor<Intent> {
            verify(context).startActivity(capture())
        }.firstValue
        assertThat(intent.component!!.className).isEqualTo(NetworkSelectActivity::class.java.name)
        assertThat(intent.getIntExtra(Settings.EXTRA_SUB_ID, 0)).isEqualTo(SUB_ID)
    }

    @Test
    fun onClick_turnOn_setNetworkSelectionModeAutomatic() = runBlocking {
        serviceState.isManualSelection = true
        controller.progressDialog = mock()

        composeTestRule.setContent {
            controller.Content()
        }
        composeTestRule.onRoot().performClick()
        delay(100)

        verify(controller.progressDialog!!).show()
        verify(mockTelephonyManager).setNetworkSelectionModeAutomatic()
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val SUB_ID = 2
        const val OPERATOR_NAME = "T-mobile"
    }
}
