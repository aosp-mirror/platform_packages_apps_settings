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

import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
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
class TelephonyRepositoryTest {
    private var telephonyCallback: TelephonyCallback? = null

    private val mockTelephonyManager = mock<TelephonyManager> {
        on { createForSubscriptionId(SUB_ID) } doReturn mock
        on { registerTelephonyCallback(any(), any()) } doAnswer {
            telephonyCallback = it.arguments[1] as TelephonyCallback
        }
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
    }

    private val repository = TelephonyRepository(context, flowOf(Unit))

    @Test
    fun isMobileDataPolicyEnabledFlow_invalidSub_returnFalse() = runBlocking {
        val flow = repository.isMobileDataPolicyEnabledFlow(
            subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
            policy = TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH,
        )

        assertThat(flow.firstWithTimeoutOrNull()).isFalse()
    }

    @Test
    fun isMobileDataPolicyEnabledFlow_validSub_returnPolicyState() = runBlocking {
        mockTelephonyManager.stub {
            on {
                isMobileDataPolicyEnabled(TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH)
            } doReturn true
        }

        val flow = repository.isMobileDataPolicyEnabledFlow(
            subId = SUB_ID,
            policy = TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH,
        )

        assertThat(flow.firstWithTimeoutOrNull()).isTrue()
    }

    @Test
    fun setMobileDataPolicyEnabled() = runBlocking {
        repository.setMobileDataPolicyEnabled(
            subId = SUB_ID,
            policy = TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH,
            enabled = true
        )

        verify(mockTelephonyManager)
            .setMobileDataPolicyEnabled(TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH, true)
    }

    @Test
    fun isDataEnabled_invalidSub_returnFalse() = runBlocking {
        val state = repository.isDataEnabled(
            subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
        )

        assertThat(state.firstWithTimeoutOrNull()).isFalse()
    }

    @Test
    fun isDataEnabled_validSub_returnPolicyState() = runBlocking {
        mockTelephonyManager.stub {
            on {
                isDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER)
            } doReturn true
        }

        val state = repository.isDataEnabled(
            subId = SUB_ID,
        )

        assertThat(state.firstWithTimeoutOrNull()).isTrue()
    }

    @Test
    fun telephonyCallbackFlow_callbackRegistered() = runBlocking {
        val flow = context.telephonyCallbackFlow<Unit>(SUB_ID) {
            object : TelephonyCallback() {}
        }

        flow.firstWithTimeoutOrNull()

        assertThat(telephonyCallback).isNotNull()
    }

    @Test
    fun telephonyCallbackFlow_callbackUnregistered() = runBlocking {
        val flow = context.telephonyCallbackFlow<Unit>(SUB_ID) {
            object : TelephonyCallback() {}
        }

        flow.firstWithTimeoutOrNull()

        verify(mockTelephonyManager).unregisterTelephonyCallback(telephonyCallback!!)
    }

    private companion object {
        const val SUB_ID = 1
    }
}
