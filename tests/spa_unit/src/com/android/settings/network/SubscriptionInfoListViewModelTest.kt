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

import android.telephony.SubscriptionManager.PROFILE_CLASS_PROVISIONING

import android.app.Application
import android.content.Context
import android.platform.test.flag.junit.SetFlagsRule
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.telephony.flags.Flags
import com.android.settings.network.telephony.CallStateFlowTest
import com.android.settingslib.spa.testutils.toListWithTimeout
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
class SubscriptionInfoListViewModelTest {
    @get:Rule
    val mSetFlagsRule = SetFlagsRule()
    private var subInfoListener: SubscriptionManager.OnSubscriptionsChangedListener? = null
    private val mockSubscriptionManager = mock<SubscriptionManager> {
        on { activeSubscriptionInfoList } doAnswer { activeSubscriptionInfoList }
        on { addOnSubscriptionsChangedListener(any(), any()) } doAnswer {
            subInfoListener =
                it.arguments[1] as SubscriptionManager.OnSubscriptionsChangedListener
            subInfoListener?.onSubscriptionsChanged()
        }
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(SubscriptionManager::class.java) } doReturn mockSubscriptionManager
    }

    private val subscriptionInfoListViewModel: SubscriptionInfoListViewModel =
        SubscriptionInfoListViewModel(context as Application);

    private var activeSubscriptionInfoList: List<SubscriptionInfo>? = null

    @Test
    fun onSubscriptionsChanged_noProvisioning_resultSameAsInput() = runBlocking {
        activeSubscriptionInfoList = listOf(SUB_INFO_1, SUB_INFO_2)

        val listDeferred = async {
            subscriptionInfoListViewModel.subscriptionInfoListFlow.toListWithTimeout()
        }
        delay(100)
        subInfoListener?.onSubscriptionsChanged()

        assertThat(listDeferred.await()).contains(activeSubscriptionInfoList)
    }

    @Test
    fun onSubscriptionsChanged_hasProvisioning_filterProvisioning() = runBlocking {
        activeSubscriptionInfoList = listOf(SUB_INFO_1, SUB_INFO_2, SUB_INFO_3)
        val expectation = listOf(SUB_INFO_1, SUB_INFO_2)

        val listDeferred = async {
            subscriptionInfoListViewModel.subscriptionInfoListFlow.toListWithTimeout()
        }
        delay(100)
        subInfoListener?.onSubscriptionsChanged()

        assertThat(listDeferred.await()).contains(expectation)
    }

    @Test
    fun onSubscriptionsChanged_flagOffHasNonTerrestrialNetwork_filterNonTerrestrialNetwork() =
        runBlocking {
            mSetFlagsRule.disableFlags(Flags.FLAG_OEM_ENABLED_SATELLITE_FLAG)

            activeSubscriptionInfoList = listOf(SUB_INFO_1, SUB_INFO_2, SUB_INFO_4)
            val expectation = listOf(SUB_INFO_1, SUB_INFO_2, SUB_INFO_4)

            val listDeferred = async {
                subscriptionInfoListViewModel.subscriptionInfoListFlow.toListWithTimeout()
            }
            delay(100)
            subInfoListener?.onSubscriptionsChanged()

            assertThat(listDeferred.await()).contains(expectation)
        }

    @Test
    fun onSubscriptionsChanged_flagOnHasNonTerrestrialNetwork_filterNonTerrestrialNetwork() =
        runBlocking {
            mSetFlagsRule.enableFlags(Flags.FLAG_OEM_ENABLED_SATELLITE_FLAG)

            activeSubscriptionInfoList = listOf(SUB_INFO_1, SUB_INFO_2, SUB_INFO_4)
            val expectation = listOf(SUB_INFO_1, SUB_INFO_2)

            val listDeferred = async {
                subscriptionInfoListViewModel.subscriptionInfoListFlow.toListWithTimeout()
            }
            delay(100)
            subInfoListener?.onSubscriptionsChanged()

            assertThat(listDeferred.await()).contains(expectation)
        }

    private companion object {
        val SUB_INFO_1: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(1)
        }.build()

        val SUB_INFO_2: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(2)
        }.build()

        val SUB_INFO_3: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(3)
            setEmbedded(true)
            setProfileClass(PROFILE_CLASS_PROVISIONING)
            setOnlyNonTerrestrialNetwork(false)
        }.build()

        val SUB_INFO_4: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(4)
            setEmbedded(true)
            setOnlyNonTerrestrialNetwork(true)
        }.build()
    }
}