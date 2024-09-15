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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.network.telephony.DataSubscriptionRepository.Companion.SUBSCRIPTION_KEY
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.android.settingslib.spa.testutils.toListWithTimeout
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class DataSubscriptionRepositoryTest {

    private var activeDataSubIdListener: TelephonyCallback.ActiveDataSubscriptionIdListener? = null

    private val mockTelephonyManager =
        mock<TelephonyManager> {
            on { registerTelephonyCallback(any(), any()) } doAnswer
                {
                    activeDataSubIdListener =
                        it.arguments[1] as TelephonyCallback.ActiveDataSubscriptionIdListener
                }
        }

    private val mockSubscriptionManager =
        mock<SubscriptionManager> {
            on { getActiveSubscriptionInfo(SUB_ID_10) } doReturn
                SubscriptionInfo.Builder().setId(SUB_ID_10).build()
            on { getActiveSubscriptionInfo(SUB_ID_20) } doReturn
                SubscriptionInfo.Builder().setId(SUB_ID_20).build()
        }

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
            on { getSystemService(SubscriptionManager::class.java) } doReturn
                mockSubscriptionManager

            doAnswer {
                    val broadcastReceiver = it.arguments[0] as BroadcastReceiver
                    val intent = Intent().apply { putExtra(SUBSCRIPTION_KEY, SUB_ID_10) }
                    broadcastReceiver.onReceive(mock, intent)
                    null
                }
                .whenever(mock)
                .registerReceiver(
                    any(),
                    argThat {
                        hasAction(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)
                    },
                    any(),
                )
        }

    private val repository = DataSubscriptionRepository(context) { subId -> "Name$subId" }

    @Test
    fun defaultDataSubscriptionIdFlow() = runBlocking {
        val defaultSubIdDeferred = async {
            repository.defaultDataSubscriptionIdFlow().toListWithTimeout()
        }
        delay(100)

        assertThat(defaultSubIdDeferred.await()).contains(SUB_ID_10)
    }

    @Test
    fun activeDataSubscriptionIdFlow() = runBlocking {
        val activeSubIdDeferred = async {
            repository.activeDataSubscriptionIdFlow().toListWithTimeout()
        }
        delay(100)

        activeDataSubIdListener?.onActiveDataSubscriptionIdChanged(SUB_ID_20)

        assertThat(activeSubIdDeferred.await()).contains(SUB_ID_20)
    }

    @Test
    fun dataSummaryFlow_defaultIsActive() = runBlocking {
        val summaryDeferred = async { repository.dataSummaryFlow().firstWithTimeoutOrNull() }
        delay(100)

        activeDataSubIdListener?.onActiveDataSubscriptionIdChanged(SUB_ID_10)

        assertThat(summaryDeferred.await()).isEqualTo("Name10")
    }

    @Test
    fun dataSummaryFlow_defaultIsNotActive() = runBlocking {
        val summaryDeferred = async { repository.dataSummaryFlow().firstWithTimeoutOrNull() }
        delay(100)

        activeDataSubIdListener?.onActiveDataSubscriptionIdChanged(SUB_ID_20)

        assertThat(summaryDeferred.await()).isEqualTo("Temporarily using Name20")
    }

    private companion object {
        const val SUB_ID_10 = 10
        const val SUB_ID_20 = 20
    }
}
