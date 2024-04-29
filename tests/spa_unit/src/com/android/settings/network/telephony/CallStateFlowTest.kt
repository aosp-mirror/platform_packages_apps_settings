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
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.android.settingslib.spa.testutils.toListWithTimeout
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy

@RunWith(AndroidJUnit4::class)
class CallStateFlowTest {
    private var callStateListener: TelephonyCallback.CallStateListener? = null

    private val mockTelephonyManager = mock<TelephonyManager> {
        on { createForSubscriptionId(SUB_ID) } doReturn mock
        on { registerTelephonyCallback(any(), any()) } doAnswer {
            callStateListener = it.arguments[1] as TelephonyCallback.CallStateListener
            callStateListener?.onCallStateChanged(TelephonyManager.CALL_STATE_IDLE)
        }
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
    }

    @Test
    fun callStateFlow_initial_sendInitialState() = runBlocking {
        val flow = context.callStateFlow(SUB_ID)

        val state = flow.firstWithTimeoutOrNull()

        assertThat(state).isEqualTo(TelephonyManager.CALL_STATE_IDLE)
    }

    @Test
    fun callStateFlow_changed_sendChangedState() = runBlocking {
        val listDeferred = async {
            context.callStateFlow(SUB_ID).toListWithTimeout()
        }
        delay(100)

        callStateListener?.onCallStateChanged(TelephonyManager.CALL_STATE_RINGING)

        assertThat(listDeferred.await())
            .containsExactly(TelephonyManager.CALL_STATE_IDLE, TelephonyManager.CALL_STATE_RINGING)
            .inOrder()
    }

    private companion object {
        const val SUB_ID = 1
    }
}
