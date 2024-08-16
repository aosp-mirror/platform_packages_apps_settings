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

package com.android.settings.network

import android.content.Context
import android.os.OutcomeReceiver
import android.telephony.satellite.SatelliteManager
import android.telephony.satellite.SatelliteManager.SatelliteException
import android.telephony.satellite.SatelliteModemStateCallback
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Executor


@RunWith(RobolectricTestRunner::class)
class SatelliteRepositoryTest {

    @JvmField
    @Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Spy
    var spyContext: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var mockSatelliteManager: SatelliteManager

    @Mock
    private lateinit var mockExecutor: Executor

    private lateinit var repository: SatelliteRepository


    @Before
    fun setUp() {
        `when`(this.spyContext.getSystemService(SatelliteManager::class.java))
            .thenReturn(mockSatelliteManager)

        repository = SatelliteRepository(spyContext)
    }

    @Test
    fun requestIsEnabled_resultIsTrue() = runBlocking {
        `when`(
            mockSatelliteManager.requestIsEnabled(
                eq(mockExecutor), any<OutcomeReceiver<Boolean, SatelliteException>>()
            )
        )
            .thenAnswer { invocation ->
                val receiver =
                    invocation.getArgument<OutcomeReceiver<Boolean, SatelliteException>>(1)
                receiver.onResult(true)
                null
            }

        val result: ListenableFuture<Boolean> =
            repository.requestIsEnabled(mockExecutor)

        assertTrue(result.get())
    }

    @Test
    fun requestIsSessionStarted_resultIsTrue() = runBlocking {
        `when`(
            mockSatelliteManager.registerForModemStateChanged(any(), any())
        ).thenAnswer { invocation ->
            val callback = invocation.getArgument<SatelliteModemStateCallback>(1)
            callback.onSatelliteModemStateChanged(SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED)
            SatelliteManager.SATELLITE_RESULT_SUCCESS
        }

        val result: ListenableFuture<Boolean> = repository.requestIsSessionStarted(mockExecutor)
        assertTrue(result.get())
        verify(mockSatelliteManager).unregisterForModemStateChanged(any())
    }

    @Test
    fun requestIsSessionStarted_resultIsFalse() = runBlocking {
        `when`(
            mockSatelliteManager.registerForModemStateChanged(any(), any())
        ).thenAnswer { invocation ->
            val callback = invocation.getArgument<SatelliteModemStateCallback>(1)
            callback.onSatelliteModemStateChanged(SatelliteManager.SATELLITE_MODEM_STATE_OFF)
            SatelliteManager.SATELLITE_RESULT_SUCCESS
        }

        val result: ListenableFuture<Boolean> = repository.requestIsSessionStarted(mockExecutor)
        assertFalse(result.get())
        verify(mockSatelliteManager).unregisterForModemStateChanged(any())
    }

    @Test
    fun requestIsSessionStarted_registerFailed() = runBlocking {
        `when`(
            mockSatelliteManager.registerForModemStateChanged(any(), any())
        ).thenAnswer {
            SatelliteManager.SATELLITE_RESULT_ERROR
        }

        val result: ListenableFuture<Boolean> = repository.requestIsSessionStarted(mockExecutor)
        assertFalse(result.get())
        verify(mockSatelliteManager, never()).unregisterForModemStateChanged(any())
    }

    @Test
    fun requestIsSessionStarted_phoneCrash_registerFailed() = runBlocking {
        `when`(
            mockSatelliteManager.registerForModemStateChanged(any(), any())
        ).thenThrow(IllegalStateException("Telephony is null"))

        val result: ListenableFuture<Boolean> = repository.requestIsSessionStarted(mockExecutor)
        assertFalse(result.get())
        verify(mockSatelliteManager, never()).unregisterForModemStateChanged(any())
    }

    @Test
    fun requestIsSessionStarted_nullSatelliteManager() = runBlocking {
        `when`(spyContext.getSystemService(SatelliteManager::class.java)).thenReturn(null)

        val result: ListenableFuture<Boolean> = repository.requestIsSessionStarted(mockExecutor)
        assertFalse(result.get())
        verifyNoInteractions(mockSatelliteManager)
    }

    @Test
    fun requestIsEnabled_resultIsFalse() = runBlocking {
        `when`(
            mockSatelliteManager.requestIsEnabled(
                eq(mockExecutor), any<OutcomeReceiver<Boolean, SatelliteException>>()
            )
        )
            .thenAnswer { invocation ->
                val receiver =
                    invocation.getArgument<OutcomeReceiver<Boolean, SatelliteException>>(1)
                receiver.onResult(false)
                null
            }

        val result: ListenableFuture<Boolean> =
            repository.requestIsEnabled(mockExecutor)
        assertFalse(result.get())
    }

    @Test
    fun requestIsEnabled_phoneCrash_resultIsFalse() = runBlocking {
        `when`(
            mockSatelliteManager.requestIsEnabled(any(), any())
        ).thenThrow(IllegalStateException("Telephony is null"))

        val result: ListenableFuture<Boolean> =
            repository.requestIsEnabled(mockExecutor)
        assertFalse(result.get())
    }


    @Test
    fun requestIsEnabled_exceptionFailure() = runBlocking {
        `when`(
            mockSatelliteManager.requestIsEnabled(
                eq(mockExecutor), any<OutcomeReceiver<Boolean, SatelliteException>>()
            )
        )
            .thenAnswer { invocation ->
                val receiver =
                    invocation.getArgument<OutcomeReceiver<Boolean, SatelliteException>>(1)
                receiver.onError(SatelliteException(SatelliteManager.SATELLITE_RESULT_ERROR))
                null
            }

        val result = repository.requestIsEnabled(mockExecutor)

        assertFalse(result.get())
    }

    @Test
    fun requestIsEnabled_nullSatelliteManager() = runBlocking {
        `when`(spyContext.getSystemService(SatelliteManager::class.java)).thenReturn(null)

        val result: ListenableFuture<Boolean> = repository.requestIsEnabled(mockExecutor)

        assertFalse(result.get())
    }

    @Test
    fun getIsSessionStartedFlow_isSatelliteEnabledState() = runBlocking {
        `when`(
            mockSatelliteManager.registerForModemStateChanged(
                any(),
                any()
            )
        ).thenAnswer { invocation ->
            val callback = invocation.getArgument<SatelliteModemStateCallback>(1)
            callback.onSatelliteModemStateChanged(SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED)
            SatelliteManager.SATELLITE_RESULT_SUCCESS
        }

        val flow = repository.getIsSessionStartedFlow()

        assertThat(flow.first()).isTrue()
    }

    @Test
    fun getIsSessionStartedFlow_isSatelliteDisabledState() = runBlocking {
        `when`(
            mockSatelliteManager.registerForModemStateChanged(
                any(),
                any()
            )
        ).thenAnswer { invocation ->
            val callback = invocation.getArgument<SatelliteModemStateCallback>(1)
            callback.onSatelliteModemStateChanged(SatelliteManager.SATELLITE_MODEM_STATE_OFF)
            SatelliteManager.SATELLITE_RESULT_SUCCESS
        }

        val flow = repository.getIsSessionStartedFlow()

        assertThat(flow.first()).isFalse()
    }

    @Test
    fun getIsSessionStartedFlow_nullSatelliteManager() = runBlocking {
        `when`(spyContext.getSystemService(SatelliteManager::class.java)).thenReturn(null)

        val flow = repository.getIsSessionStartedFlow()
        assertThat(flow.first()).isFalse()
    }

    @Test
    fun getIsSessionStartedFlow_registerFailed() = runBlocking {
        `when`(
            mockSatelliteManager.registerForModemStateChanged(any(), any())
        ).thenAnswer {
            SatelliteManager.SATELLITE_RESULT_ERROR
        }

        val flow = repository.getIsSessionStartedFlow()

        assertThat(flow.first()).isFalse()
    }
}