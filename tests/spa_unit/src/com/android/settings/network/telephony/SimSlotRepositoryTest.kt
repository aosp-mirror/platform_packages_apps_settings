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
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class SimSlotRepositoryTest {

    private val mockSubscriptionManager = mock<SubscriptionManager> {
        on { addOnSubscriptionsChangedListener(any(), any()) } doAnswer {
            val listener = it.arguments[1] as SubscriptionManager.OnSubscriptionsChangedListener
            listener.onSubscriptionsChanged()
        }
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(SubscriptionManager::class.java) } doReturn mockSubscriptionManager
    }

    private val repository = SimSlotRepository(context)

    @Test
    fun subIdInSimSlotFlow_valid() = runBlocking {
        mockSubscriptionManager.stub {
            on { getActiveSubscriptionInfoForSimSlotIndex(SIM_SLOT_INDEX) } doReturn
                SubscriptionInfo.Builder().setId(SUB_ID).build()
        }

        val subId = repository.subIdInSimSlotFlow(SIM_SLOT_INDEX).firstWithTimeoutOrNull()

        assertThat(subId).isEqualTo(SUB_ID)
    }

    @Test
    fun subIdInSimSlotFlow_invalid() = runBlocking {
        mockSubscriptionManager.stub {
            on { getActiveSubscriptionInfoForSimSlotIndex(SIM_SLOT_INDEX) } doReturn null
        }

        val subId = repository.subIdInSimSlotFlow(SIM_SLOT_INDEX).firstWithTimeoutOrNull()

        assertThat(subId).isEqualTo(SubscriptionManager.INVALID_SIM_SLOT_INDEX)
    }

    private companion object {
        const val SIM_SLOT_INDEX = 0
        const val SUB_ID = 1
    }
}
