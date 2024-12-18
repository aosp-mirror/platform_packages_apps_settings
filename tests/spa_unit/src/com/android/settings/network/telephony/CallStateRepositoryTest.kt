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
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.android.settingslib.spa.testutils.toListWithTimeout
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
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

@RunWith(AndroidJUnit4::class)
class CallStateRepositoryTest {
    private var callStateListener: TelephonyCallback.CallStateListener? = null

    private val mockTelephonyManager = mock<TelephonyManager> {
        on { createForSubscriptionId(SUB_ID) } doReturn mock
        on { registerTelephonyCallback(any(), any()) } doAnswer {
            callStateListener = it.arguments[1] as TelephonyCallback.CallStateListener
            callStateListener?.onCallStateChanged(TelephonyManager.CALL_STATE_IDLE)
        }
    }

    private val mockSubscriptionRepository = mock<SubscriptionRepository> {
        on { activeSubscriptionIdListFlow() } doReturn flowOf(listOf(SUB_ID))
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
    }

    private val repository = CallStateRepository(context, mockSubscriptionRepository)

    @Test
    fun callStateFlow_initial_sendInitialState() = runBlocking {
        val flow = repository.callStateFlow(SUB_ID)

        val state = flow.firstWithTimeoutOrNull()

        assertThat(state).isEqualTo(TelephonyManager.CALL_STATE_IDLE)
    }

    @Test
    fun callStateFlow_changed_sendChangedState() = runBlocking {
        val listDeferred = async {
            repository.callStateFlow(SUB_ID).toListWithTimeout()
        }
        delay(100)

        callStateListener?.onCallStateChanged(TelephonyManager.CALL_STATE_RINGING)

        assertThat(listDeferred.await())
            .containsExactly(TelephonyManager.CALL_STATE_IDLE, TelephonyManager.CALL_STATE_RINGING)
            .inOrder()
    }

    @Test
    fun isInCallFlow_noActiveSubscription() = runBlocking {
        mockSubscriptionRepository.stub {
            on { activeSubscriptionIdListFlow() } doReturn flowOf(emptyList())
        }

        val isInCall = repository.isInCallFlow().firstWithTimeoutOrNull()

        assertThat(isInCall).isFalse()
    }

    @Test
    fun isInCallFlow_initial() = runBlocking {
        val isInCall = repository.isInCallFlow().firstWithTimeoutOrNull()

        assertThat(isInCall).isFalse()
    }

    @Test
    fun isInCallFlow_changed_sendChangedState() = runBlocking {
        val listDeferred = async {
            repository.isInCallFlow().toListWithTimeout()
        }
        delay(100)

        callStateListener?.onCallStateChanged(TelephonyManager.CALL_STATE_RINGING)

        assertThat(listDeferred.await())
            .containsExactly(false, true)
            .inOrder()
    }

    private companion object {
        const val SUB_ID = 1
    }
}
