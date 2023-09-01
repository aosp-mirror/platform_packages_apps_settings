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

package com.android.settings.datausage.lib

import android.content.Context
import android.net.NetworkStats
import android.net.NetworkTemplate
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class DataUsageLibTest {

    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var telephonyManager: TelephonyManager

    @Mock
    private lateinit var subscriptionManager: SubscriptionManager

    @Before
    fun setUp() {
        whenever(context.getSystemService(TelephonyManager::class.java))
            .thenReturn(telephonyManager)
        whenever(context.getSystemService(SubscriptionManager::class.java))
            .thenReturn(subscriptionManager)

        whenever(telephonyManager.subscriptionId).thenReturn(DEFAULT_SUB_ID)
        whenever(telephonyManager.getSubscriberId(SUB_ID)).thenReturn(SUBSCRIBER_ID)
        whenever(telephonyManager.getSubscriberId(DEFAULT_SUB_ID)).thenReturn(DEFAULT_SUBSCRIBER_ID)
        whenever(telephonyManager.createForSubscriptionId(SUB_ID)).thenReturn(telephonyManager)
    }

    @Test
    fun getMobileTemplate_availableSubscriptionInfoListIsNull_returnDefaultSub() {
        whenever(subscriptionManager.availableSubscriptionInfoList).thenReturn(null)

        val mobileTemplate = DataUsageLib.getMobileTemplate(context, SUB_ID)

        assertThat(mobileTemplate.matchRule).isEqualTo(NetworkTemplate.MATCH_CARRIER)
        assertThat(mobileTemplate.subscriberIds).containsExactly(DEFAULT_SUBSCRIBER_ID)
        assertThat(mobileTemplate.meteredness).isEqualTo(NetworkStats.METERED_YES)
    }

    @Test
    fun getMobileTemplate_subscriptionNotActive_returnDefaultSub() {
        whenever(subscriptionManager.availableSubscriptionInfoList).thenReturn(listOf(null))

        val mobileTemplate = DataUsageLib.getMobileTemplate(context, SUB_ID)

        assertThat(mobileTemplate.matchRule).isEqualTo(NetworkTemplate.MATCH_CARRIER)
        assertThat(mobileTemplate.subscriberIds).containsExactly(DEFAULT_SUBSCRIBER_ID)
        assertThat(mobileTemplate.meteredness).isEqualTo(NetworkStats.METERED_YES)
    }

    @Test
    fun getMobileTemplate_mergedImsisFromGroupEmpty_returnRequestedSub() {
        whenever(subscriptionManager.availableSubscriptionInfoList)
            .thenReturn(listOf(SUBSCRIBER_INFO))
        whenever(telephonyManager.mergedImsisFromGroup).thenReturn(emptyArray())

        val mobileTemplate = DataUsageLib.getMobileTemplate(context, SUB_ID)

        assertThat(mobileTemplate.matchRule).isEqualTo(NetworkTemplate.MATCH_CARRIER)
        assertThat(mobileTemplate.subscriberIds).containsExactly(SUBSCRIBER_ID)
        assertThat(mobileTemplate.meteredness).isEqualTo(NetworkStats.METERED_YES)
    }

    @Test
    fun getMobileTemplate_mergedImsisFromGroupNotContainSub_returnRequestedSub() {
        whenever(subscriptionManager.availableSubscriptionInfoList)
            .thenReturn(listOf(SUBSCRIBER_INFO))
        whenever(telephonyManager.mergedImsisFromGroup).thenReturn(arrayOf(DEFAULT_SUBSCRIBER_ID))

        val mobileTemplate = DataUsageLib.getMobileTemplate(context, SUB_ID)

        assertThat(mobileTemplate.matchRule).isEqualTo(NetworkTemplate.MATCH_CARRIER)
        assertThat(mobileTemplate.subscriberIds).containsExactly(SUBSCRIBER_ID)
        assertThat(mobileTemplate.meteredness).isEqualTo(NetworkStats.METERED_YES)
    }

    @Test
    fun getMobileTemplate_mergedImsisFromGroupContainSub_returnRequestedSub() {
        whenever(subscriptionManager.availableSubscriptionInfoList)
            .thenReturn(listOf(SUBSCRIBER_INFO))
        whenever(telephonyManager.mergedImsisFromGroup)
            .thenReturn(arrayOf(DEFAULT_SUBSCRIBER_ID, SUBSCRIBER_ID))

        val mobileTemplate = DataUsageLib.getMobileTemplate(context, SUB_ID)

        assertThat(mobileTemplate.matchRule).isEqualTo(NetworkTemplate.MATCH_CARRIER)
        assertThat(mobileTemplate.subscriberIds)
            .containsExactly(SUBSCRIBER_ID, DEFAULT_SUBSCRIBER_ID)
        assertThat(mobileTemplate.meteredness).isEqualTo(NetworkStats.METERED_YES)
    }

    @Test
    fun getMobileTemplateForSubId_subscriberIdNotNull() {
        whenever(telephonyManager.getSubscriberId(SUB_ID)).thenReturn(SUBSCRIBER_ID)

        val mobileTemplate = DataUsageLib.getMobileTemplateForSubId(telephonyManager, SUB_ID)

        assertThat(mobileTemplate.matchRule).isEqualTo(NetworkTemplate.MATCH_CARRIER)
        assertThat(mobileTemplate.subscriberIds).containsExactly(SUBSCRIBER_ID)
        assertThat(mobileTemplate.meteredness).isEqualTo(NetworkStats.METERED_YES)
    }

    @Test
    fun getMobileTemplateForSubId_subscriberIdIsNull() {
        whenever(telephonyManager.getSubscriberId(SUB_ID)).thenReturn(null)

        val mobileTemplate = DataUsageLib.getMobileTemplateForSubId(telephonyManager, SUB_ID)

        assertThat(mobileTemplate.matchRule).isEqualTo(NetworkTemplate.MATCH_MOBILE)
        assertThat(mobileTemplate.subscriberIds).isEmpty()
        assertThat(mobileTemplate.meteredness).isEqualTo(NetworkStats.METERED_YES)
    }

    private companion object {
        const val DEFAULT_SUB_ID = 0
        const val SUB_ID = 1
        const val DEFAULT_SUBSCRIBER_ID = "Default Test Subscriber"
        const val SUBSCRIBER_ID = "Test Subscriber"
        val SUBSCRIBER_INFO: SubscriptionInfo = SubscriptionInfo.Builder().setId(SUB_ID).build()
    }
}
