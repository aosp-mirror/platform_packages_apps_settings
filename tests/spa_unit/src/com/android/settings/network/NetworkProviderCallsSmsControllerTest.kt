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

package com.android.settings.network

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetworkProviderCallsSmsControllerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private var isInService: (Int) -> Boolean = { true }

    private val controller = NetworkProviderCallsSmsController(
        context = context,
        preferenceKey = TEST_KEY,
        getDisplayName = { subInfo -> subInfo.displayName },
        isInService = { isInService(it) },
    )

    @Test
    fun getSummary_noSim_returnNoSim() {
        val summary = controller.getSummary(
            activeSubscriptionInfoList = emptyList(),
            defaultVoiceSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
            defaultSmsSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
        )

        assertThat(summary).isEqualTo(context.getString(R.string.calls_sms_no_sim))
    }

    @Test
    fun getSummary_invalidSubId_returnUnavailable() {
        isInService = { false }

        val summary = controller.getSummary(
            activeSubscriptionInfoList = listOf(SUB_INFO_1),
            defaultVoiceSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
            defaultSmsSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
        )

        assertThat(summary).isEqualTo("Sub 1 (Temporarily unavailable)")
    }

    @Test
    fun getSummary_oneIsInvalidSubIdTwoIsValidSubId_returnOneIsUnavailable() {
        isInService = { it == SUB_INFO_2.subscriptionId }

        val summary = controller.getSummary(
            activeSubscriptionInfoList = listOf(SUB_INFO_1, SUB_INFO_2),
            defaultVoiceSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
            defaultSmsSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
        )

        assertThat(summary).isEqualTo("Sub 1 (unavailable), Sub 2")
    }

    @Test
    fun getSummary_oneSubscription_returnDisplayName() {
        val summary = controller.getSummary(
            activeSubscriptionInfoList = listOf(SUB_INFO_1),
            defaultVoiceSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
            defaultSmsSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
        )

        assertThat(summary).isEqualTo(DISPLAY_NAME_1)
    }

    @Test
    fun getSummary_allSubscriptionsHaveNoPreferredStatus_returnDisplayName() {
        val summary = controller.getSummary(
            activeSubscriptionInfoList = listOf(SUB_INFO_1, SUB_INFO_2),
            defaultVoiceSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
            defaultSmsSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
        )

        assertThat(summary).isEqualTo("Sub 1, Sub 2")
    }

    @Test
    fun getSummary_oneSubscriptionsIsCallPreferredTwoIsSmsPreferred_returnStatus() {
        val summary = controller.getSummary(
            activeSubscriptionInfoList = listOf(SUB_INFO_1, SUB_INFO_2),
            defaultVoiceSubscriptionId = SUB_INFO_1.subscriptionId,
            defaultSmsSubscriptionId = SUB_INFO_2.subscriptionId,
        )

        assertThat(summary).isEqualTo("Sub 1 (preferred for calls), Sub 2 (preferred for SMS)")
    }

    @Test
    fun getSummary_oneSubscriptionsIsSmsPreferredTwoIsCallPreferred_returnStatus() {
        val summary = controller.getSummary(
            activeSubscriptionInfoList = listOf(SUB_INFO_1, SUB_INFO_2),
            defaultVoiceSubscriptionId = SUB_INFO_2.subscriptionId,
            defaultSmsSubscriptionId = SUB_INFO_1.subscriptionId,
        )

        assertThat(summary).isEqualTo("Sub 1 (preferred for SMS), Sub 2 (preferred for calls)")
    }

    @Test
    fun getSummary_oneSubscriptionsIsSmsPreferredAndIsCallPreferred_returnStatus() {
        val summary = controller.getSummary(
            activeSubscriptionInfoList = listOf(SUB_INFO_1, SUB_INFO_2),
            defaultVoiceSubscriptionId = SUB_INFO_1.subscriptionId,
            defaultSmsSubscriptionId = SUB_INFO_1.subscriptionId,
        )

        assertThat(summary).isEqualTo("Sub 1 (preferred), Sub 2")
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val DISPLAY_NAME_1 = "Sub 1"
        const val DISPLAY_NAME_2 = "Sub 2"

        val SUB_INFO_1: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(1)
            setDisplayName(DISPLAY_NAME_1)
        }.build()

        val SUB_INFO_2: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(2)
            setDisplayName(DISPLAY_NAME_2)
        }.build()
    }
}
