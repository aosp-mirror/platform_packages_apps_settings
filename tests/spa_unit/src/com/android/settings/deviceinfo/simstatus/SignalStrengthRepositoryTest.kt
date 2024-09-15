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

package com.android.settings.deviceinfo.simstatus

import android.content.Context
import android.telephony.CellSignalStrengthCdma
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.CellSignalStrengthTdscdma
import android.telephony.CellSignalStrengthWcdma
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy

@RunWith(AndroidJUnit4::class)
class SignalStrengthRepositoryTest {

    private var signalStrength = SignalStrength()

    private val mockTelephonyManager = mock<TelephonyManager> {
        on { createForSubscriptionId(SUB_ID) } doReturn mock
        on { registerTelephonyCallback(any(), any()) } doAnswer {
            val listener = it.getArgument<TelephonyCallback.SignalStrengthsListener>(1)
            listener.onSignalStrengthsChanged(signalStrength)
        }
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(TelephonyManager::class.java) } doAnswer { mockTelephonyManager }
    }

    private val serviceState = ServiceState()

    private val repository = SignalStrengthRepository(context) { flowOf(serviceState) }

    @Test
    fun signalStrengthDisplayFlow_serviceStatePowerOff() = runBlocking {
        serviceState.state = ServiceState.STATE_POWER_OFF

        val signalStrength = repository.signalStrengthDisplayFlow(SUB_ID).firstWithTimeoutOrNull()

        assertThat(signalStrength).isEqualTo("0")
    }

    @Test
    fun signalStrengthDisplayFlow_lteWcdma() = runBlocking {
        serviceState.state = ServiceState.STATE_IN_SERVICE
        signalStrength = SignalStrength(
            CellSignalStrengthCdma(),
            CellSignalStrengthGsm(),
            mock<CellSignalStrengthWcdma> {
                on { isValid } doReturn true
                on { dbm } doReturn 40
                on { asuLevel } doReturn 41
            },
            CellSignalStrengthTdscdma(),
            mock<CellSignalStrengthLte> {
                on { isValid } doReturn true
                on { dbm } doReturn 50
                on { asuLevel } doReturn 51
            },
            CellSignalStrengthNr(),
        )

        val signalStrength = repository.signalStrengthDisplayFlow(SUB_ID).firstWithTimeoutOrNull()

        assertThat(signalStrength).isEqualTo("50 dBm 51 asu")
    }

    @Test
    fun signalStrengthDisplayFlow_lteCdma() = runBlocking {
        serviceState.state = ServiceState.STATE_IN_SERVICE
        signalStrength = SignalStrength(
            mock<CellSignalStrengthCdma> {
                on { isValid } doReturn true
                on { dbm } doReturn 30
                on { asuLevel } doReturn 31
            },
            CellSignalStrengthGsm(),
            CellSignalStrengthWcdma(),
            CellSignalStrengthTdscdma(),
            mock<CellSignalStrengthLte> {
                on { isValid } doReturn true
                on { dbm } doReturn 50
                on { asuLevel } doReturn 51
            },
            CellSignalStrengthNr(),
        )

        val signalStrength = repository.signalStrengthDisplayFlow(SUB_ID).firstWithTimeoutOrNull()

        assertThat(signalStrength).isEqualTo("50 dBm 51 asu")
    }

    @Test
    fun signalStrengthDisplayFlow_lteOnly() = runBlocking {
        serviceState.state = ServiceState.STATE_IN_SERVICE
        signalStrength = SignalStrength(
            CellSignalStrengthCdma(),
            CellSignalStrengthGsm(),
            CellSignalStrengthWcdma(),
            CellSignalStrengthTdscdma(),
            mock<CellSignalStrengthLte> {
                on { isValid } doReturn true
                on { dbm } doReturn 50
                on { asuLevel } doReturn 51
            },
            CellSignalStrengthNr(),
        )

        val signalStrength = repository.signalStrengthDisplayFlow(SUB_ID).firstWithTimeoutOrNull()

        assertThat(signalStrength).isEqualTo("50 dBm 51 asu")
    }

    private companion object {
        const val SUB_ID = 1
    }
}
