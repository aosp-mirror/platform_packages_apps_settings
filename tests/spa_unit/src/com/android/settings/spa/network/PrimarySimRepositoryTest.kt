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
import android.content.res.Resources
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class PrimarySimRepositoryTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockSubscriptionManager = mock<SubscriptionManager> {
        on { addOnSubscriptionsChangedListener(any(), any()) } doAnswer {
            val listener = it.arguments[1] as SubscriptionManager.OnSubscriptionsChangedListener
            listener.onSubscriptionsChanged()
        }
        on { getPhoneNumber(SUB_ID_1) } doReturn NUMBER_1
        on { getPhoneNumber(SUB_ID_2) } doReturn NUMBER_2
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(SubscriptionManager::class.java) } doReturn mockSubscriptionManager
    }
    private val spyResources: Resources = spy(context.resources)

    @Test
    fun getPrimarySimInfo_oneSim_returnNull() {
        val simList = listOf(
            SUB_INFO_1,
        )

        val primarySimInfo = PrimarySimRepository(context).getPrimarySimInfo(simList)

        assertThat(primarySimInfo).isNull()
    }

    @Test
    fun getPrimarySimInfo_verifyCallsList() {
        val simList = listOf(
            SUB_INFO_1,
            SUB_INFO_2
        )
        val expectedList = listOf(
            ListPreferenceOption(
                id = SUB_INFO_1.subscriptionId,
                text = "${SUB_INFO_1.displayName}",
                summary = NUMBER_1
            ),
            ListPreferenceOption(
                id = SUB_INFO_2.subscriptionId,
                text = "${SUB_INFO_2.displayName}",
                summary = NUMBER_2
            ),
            ListPreferenceOption(
                id = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
                text = context.getString(R.string.sim_calls_ask_first_prefs_title),
            ),
        )

        val primarySimInfo = PrimarySimRepository(context).getPrimarySimInfo(simList)

        assertThat(primarySimInfo).isNotNull()
        assertThat(primarySimInfo?.callsList).isEqualTo(expectedList)
    }

    @Test
    fun getPrimarySimInfo_verifySmsList() {
        val simList = listOf(
            SUB_INFO_1,
            SUB_INFO_2
        )
        val expectedList = listOf(
            ListPreferenceOption(
                id = SUB_INFO_1.subscriptionId,
                text = "${SUB_INFO_1.displayName}",
                summary = NUMBER_1
            ),
            ListPreferenceOption(
                id = SUB_INFO_2.subscriptionId,
                text = "${SUB_INFO_2.displayName}",
                summary = NUMBER_2
            ),
            ListPreferenceOption(
                id = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
                text = context.getString(R.string.sim_calls_ask_first_prefs_title),
            ),
        )

        val primarySimInfo = PrimarySimRepository(context).getPrimarySimInfo(simList)

        assertThat(primarySimInfo).isNotNull()
        assertThat(primarySimInfo?.smsList).isEqualTo(expectedList)
    }

    @Test
    fun getPrimarySimInfo_noAskEveryTime_verifySmsList() {
        val simList = listOf(
            SUB_INFO_1,
            SUB_INFO_2
        )
        context.stub {
            on { resources } doReturn spyResources
        }
        spyResources.stub {
            on {
                getBoolean(com.android.internal.R.bool.config_sms_ask_every_time_support)
            } doReturn false
        }
        val expectedList = listOf(
            ListPreferenceOption(
                id = SUB_INFO_1.subscriptionId,
                text = "${SUB_INFO_1.displayName}",
                summary = NUMBER_1
            ),
            ListPreferenceOption(
                id = SUB_INFO_2.subscriptionId,
                text = "${SUB_INFO_2.displayName}",
                summary = NUMBER_2
            ),
        )

        val primarySimInfo = PrimarySimRepository(context).getPrimarySimInfo(simList)

        assertThat(primarySimInfo).isNotNull()
        assertThat(primarySimInfo?.smsList).isEqualTo(expectedList)
    }

    @Test
    fun getPrimarySimInfo_verifyDataList() {
        val simList = listOf(
            SUB_INFO_1,
            SUB_INFO_2
        )
        val expectedList = listOf(
            ListPreferenceOption(
                id = SUB_INFO_1.subscriptionId,
                text = "${SUB_INFO_1.displayName}",
                summary = NUMBER_1
            ),
            ListPreferenceOption(
                id = SUB_INFO_2.subscriptionId,
                text = "${SUB_INFO_2.displayName}",
                summary = NUMBER_2
            ),
        )

        val primarySimInfo = PrimarySimRepository(context).getPrimarySimInfo(simList)

        assertThat(primarySimInfo).isNotNull()
        assertThat(primarySimInfo?.dataList).isEqualTo(expectedList)
    }

    private companion object {
        const val SUB_ID_1 = 1
        const val SUB_ID_2 = 2
        const val DISPLAY_NAME_1 = "Sub 1"
        const val DISPLAY_NAME_2 = "Sub 2"
        const val NUMBER_1 = "000000001"
        const val NUMBER_2 = "000000002"
        const val MCC = "310"

        val SUB_INFO_1: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(SUB_ID_1)
            setDisplayName(DISPLAY_NAME_1)
            setMcc(MCC)
        }.build()

        val SUB_INFO_2: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(SUB_ID_2)
            setDisplayName(DISPLAY_NAME_2)
            setMcc(MCC)
        }.build()
    }
}
