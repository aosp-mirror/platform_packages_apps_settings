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
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.android.settingslib.spa.testutils.toListWithTimeout
import com.android.settingslib.spaprivileged.settingsprovider.settingsGlobalBoolean
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class MobileDataRepositoryTest {
    private val mockTelephonyManager =
        mock<TelephonyManager> { on { createForSubscriptionId(SUB_ID) } doReturn mock }

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
        }

    private val repository = MobileDataRepository(context, flowOf(Unit))

    @Test
    fun isMobileDataPolicyEnabledFlow_invalidSub_returnFalse() = runBlocking {
        val flow =
            repository.isMobileDataPolicyEnabledFlow(
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

        val flow =
            repository.isMobileDataPolicyEnabledFlow(
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
            enabled = true)

        verify(mockTelephonyManager)
            .setMobileDataPolicyEnabled(TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH, true)
    }

    @Test
    fun mobileDataEnabledChangedFlow_notified(): Unit = runBlocking {
        val flow =
            repository.mobileDataEnabledChangedFlow(SubscriptionManager.INVALID_SUBSCRIPTION_ID)

        assertThat(flow.firstWithTimeoutOrNull()).isNotNull()
    }

    @Test
    fun mobileDataEnabledChangedFlow_changed_notified(): Unit = runBlocking {
        var mobileDataEnabled by context.settingsGlobalBoolean(Settings.Global.MOBILE_DATA)
        mobileDataEnabled = false

        val flow =
            repository.mobileDataEnabledChangedFlow(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        mobileDataEnabled = true

        assertThat(flow.firstWithTimeoutOrNull()).isNotNull()
    }

    @Test
    fun mobileDataEnabledChangedFlow_forSubIdNotChanged(): Unit = runBlocking {
        var mobileDataEnabled by context.settingsGlobalBoolean(Settings.Global.MOBILE_DATA)
        mobileDataEnabled = false
        var mobileDataEnabledForSubId by
            context.settingsGlobalBoolean(Settings.Global.MOBILE_DATA + SUB_ID)
        mobileDataEnabledForSubId = false

        val listDeferred = async {
            repository.mobileDataEnabledChangedFlow(SUB_ID).toListWithTimeout()
        }

        assertThat(listDeferred.await()).hasSize(1)
    }

    @Test
    fun mobileDataEnabledChangedFlow_forSubIdChanged(): Unit = runBlocking {
        var mobileDataEnabled by context.settingsGlobalBoolean(Settings.Global.MOBILE_DATA)
        mobileDataEnabled = false
        var mobileDataEnabledForSubId by
            context.settingsGlobalBoolean(Settings.Global.MOBILE_DATA + SUB_ID)
        mobileDataEnabledForSubId = false

        val listDeferred = async {
            repository.mobileDataEnabledChangedFlow(SUB_ID).toListWithTimeout()
        }
        delay(100)
        mobileDataEnabledForSubId = true

        assertThat(listDeferred.await().size).isAtLeast(2)
    }

    @Test
    fun isMobileDataEnabledFlow_invalidSub_returnFalse() = runBlocking {
        val state =
            repository.isMobileDataEnabledFlow(
                subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
            )

        assertThat(state.firstWithTimeoutOrNull()).isFalse()
    }

    @Test
    fun isMobileDataEnabledFlow_validSub_returnPolicyState() = runBlocking {
        mockTelephonyManager.stub {
            on { isDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER) } doReturn true
        }

        val state = repository.isMobileDataEnabledFlow(subId = SUB_ID)

        assertThat(state.firstWithTimeoutOrNull()).isTrue()
    }

    @Test
    fun isDataRoamingEnabledFlow_invalidSub_returnFalse() = runBlocking {
        val isDataRoamingEnabled =
            repository
                .isDataRoamingEnabledFlow(subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                .firstWithTimeoutOrNull()

        assertThat(isDataRoamingEnabled).isFalse()
    }

    @Test
    fun isDataRoamingEnabledFlow_validSub_returnCurrentValue() = runBlocking {
        mockTelephonyManager.stub { on { isDataRoamingEnabled } doReturn true }

        val isDataRoamingEnabled =
            repository.isDataRoamingEnabledFlow(subId = SUB_ID).firstWithTimeoutOrNull()

        assertThat(isDataRoamingEnabled).isTrue()
    }

    private companion object {
        const val SUB_ID = 123
    }
}
