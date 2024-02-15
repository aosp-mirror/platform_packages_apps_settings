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
import android.telephony.ServiceState
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy

@RunWith(AndroidJUnit4::class)
class OpenNetworkSelectPagePreferenceControllerTest {

    private val subscriptionInfo = mock<SubscriptionInfo> {
        on { subscriptionId } doReturn SUB_ID
        on { carrierName } doReturn OPERATOR_NAME
    }

    private val mockSubscriptionManager = mock<SubscriptionManager> {
        on { createForAllUserProfiles() } doReturn mock
        on { getActiveSubscriptionInfo(SUB_ID) } doReturn subscriptionInfo
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(SubscriptionManager::class.java) } doReturn mockSubscriptionManager
    }

    private val preference = Preference(context).apply { key = TEST_KEY }
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    private val serviceState = ServiceState()

    private val controller = OpenNetworkSelectPagePreferenceController(
        context = context,
        key = TEST_KEY,
        allowedNetworkTypesFlowFactory = { emptyFlow() },
        serviceStateFlowFactory = { flowOf(serviceState) },
    ).init(subId = SUB_ID)

    @Before
    fun setUp() {
        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)
    }

    @Test
    fun isEnabled_modeManual_enabled() {
        controller.onNetworkSelectModeUpdated(TelephonyManager.NETWORK_SELECTION_MODE_MANUAL)

        assertThat(preference.isEnabled).isTrue()
    }

    @Test
    fun isEnabled_modeAuto_disabled() {
        controller.onNetworkSelectModeUpdated(TelephonyManager.NETWORK_SELECTION_MODE_AUTO)

        assertThat(preference.isEnabled).isFalse()
    }

    @Test
    fun summary_inService_isOperatorName() = runBlocking {
        serviceState.state = ServiceState.STATE_IN_SERVICE

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.summary).isEqualTo(OPERATOR_NAME)
    }

    @Test
    fun summary_notInService_isDisconnect() = runBlocking {
        serviceState.state = ServiceState.STATE_OUT_OF_SERVICE

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.summary).isEqualTo(context.getString(R.string.network_disconnected))
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val SUB_ID = 2
        const val OPERATOR_NAME = "T-mobile"
    }
}
