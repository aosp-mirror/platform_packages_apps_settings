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

package com.android.settings.network.telephony

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.telephony.ims.ImsMmTelManager
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.network.telephony.wificalling.WifiCallingRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class WifiCallingPreferenceControllerTest {
    private val mockTelecomManager = mock<TelecomManager>()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(TelecomManager::class.java) } doReturn mockTelecomManager
    }

    private val preferenceIntent = Intent()

    private val preference = Preference(context).apply {
        key = TEST_KEY
        intent = preferenceIntent
    }
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    private var callState = TelephonyManager.CALL_STATE_IDLE

    private val mockWifiCallingRepository = mock<WifiCallingRepository> {
        on { getWiFiCallingMode() } doReturn ImsMmTelManager.WIFI_MODE_UNKNOWN
        on { wifiCallingReadyFlow() } doReturn flowOf(true)
    }

    private val callingPreferenceCategoryController =
        CallingPreferenceCategoryController(context, "calling_category")

    private val controller = WifiCallingPreferenceController(
        context = context,
        key = TEST_KEY,
        callStateFlowFactory = { flowOf(callState) },
        wifiCallingRepositoryFactory = { mockWifiCallingRepository },
    ).init(subId = SUB_ID, callingPreferenceCategoryController)

    @Before
    fun setUp() {
        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)
    }

    @Test
    fun summary_noSimCallManager_setCorrectSummary() = runBlocking {
        mockTelecomManager.stub {
            on { getSimCallManagerForSubscription(SUB_ID) } doReturn null
        }
        mockWifiCallingRepository.stub {
            on { getWiFiCallingMode() } doReturn ImsMmTelManager.WIFI_MODE_WIFI_ONLY
        }

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.summary)
            .isEqualTo(context.getString(com.android.internal.R.string.wfc_mode_wifi_only_summary))
    }

    @Test
    fun summary_hasSimCallManager_summaryIsNull() = runBlocking {
        mockTelecomManager.stub {
            on { getSimCallManagerForSubscription(SUB_ID) } doReturn
                PhoneAccountHandle(ComponentName("", ""), "")
        }

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.summary).isNull()
    }

    @Test
    fun isEnabled_callIdle_enabled() = runBlocking {
        callState = TelephonyManager.CALL_STATE_IDLE

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.isEnabled).isTrue()
    }

    @Test
    fun isEnabled_notCallIdle_disabled() = runBlocking {
        callState = TelephonyManager.CALL_STATE_RINGING

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)

        assertThat(preference.isEnabled).isFalse()
    }

    @Test
    fun displayPreference_setsSubscriptionIdOnIntent() = runBlocking {
        assertThat(preference.intent!!.getIntExtra(Settings.EXTRA_SUB_ID, 0)).isEqualTo(SUB_ID)
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val SUB_ID = 2
    }
}
