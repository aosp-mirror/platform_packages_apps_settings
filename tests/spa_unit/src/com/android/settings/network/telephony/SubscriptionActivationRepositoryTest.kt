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
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.network.SatelliteRepository
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SubscriptionActivationRepositoryTest {

    private val mockTelephonyManager = mock<TelephonyManager> {
        on { createForSubscriptionId(SUB_ID) } doReturn mock
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        doNothing().whenever(mock).startActivity(any())
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
    }

    private val mockCallStateRepository = mock<CallStateRepository>()
    private val mockSatelliteRepository = mock<SatelliteRepository>()

    private val repository =
        SubscriptionActivationRepository(context, mockCallStateRepository, mockSatelliteRepository)

    @Test
    fun isActivationChangeableFlow_changeable() = runBlocking {
        mockCallStateRepository.stub {
            on { isInCallFlow() } doReturn flowOf(false)
        }
        mockSatelliteRepository.stub {
            on { getIsSessionStartedFlow() } doReturn flowOf(false)
        }

        val changeable = repository.isActivationChangeableFlow().firstWithTimeoutOrNull()

        assertThat(changeable).isTrue()
    }

    @Test
    fun isActivationChangeableFlow_inCall_notChangeable() = runBlocking {
        mockCallStateRepository.stub {
            on { isInCallFlow() } doReturn flowOf(true)
        }
        mockSatelliteRepository.stub {
            on { getIsSessionStartedFlow() } doReturn flowOf(false)
        }

        val changeable = repository.isActivationChangeableFlow().firstWithTimeoutOrNull()

        assertThat(changeable).isFalse()
    }

    @Test
    fun isActivationChangeableFlow_satelliteSessionStarted_notChangeable() = runBlocking {
        mockCallStateRepository.stub {
            on { isInCallFlow() } doReturn flowOf(false)
        }
        mockSatelliteRepository.stub {
            on { getIsSessionStartedFlow() } doReturn flowOf(true)
        }

        val changeable = repository.isActivationChangeableFlow().firstWithTimeoutOrNull()

        assertThat(changeable).isFalse()
    }

    @Test
    fun setActive_defaultSubId_doNothing() = runBlocking {
        repository.setActive(subId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID, active = true)

        verify(context, never()).startActivity(any())
    }

    @Test
    fun setActive_turnOffAndIsEmergencyCallbackMode() = runBlocking {
        mockTelephonyManager.stub {
            on { emergencyCallbackMode } doReturn true
        }

        repository.setActive(subId = SUB_ID, active = false)

        verify(context).startActivity(argThat { action == ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS })
    }

    @Test
    fun setActive_turnOffAndNotEmergencyCallbackMode() = runBlocking {
        mockTelephonyManager.stub {
            on { emergencyCallbackMode } doReturn false
        }

        repository.setActive(subId = SUB_ID, active = false)

        verify(context).startActivity(argThat {
            component?.className == ToggleSubscriptionDialogActivity::class.qualifiedName
        })
    }

    private companion object {
        const val SUB_ID = 1
    }
}
