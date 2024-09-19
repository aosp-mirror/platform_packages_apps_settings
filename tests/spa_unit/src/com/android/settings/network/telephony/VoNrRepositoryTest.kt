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
import android.telephony.CarrierConfigManager
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VoNrRepositoryTest {

    private val mockTelephonyManager =
        mock<TelephonyManager> { on { createForSubscriptionId(SUB_ID) } doReturn mock }

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
        }

    private val mockNrRepository = mock<NrRepository> { on { isNrAvailable(SUB_ID) } doReturn true }

    private val repository = VoNrRepository(context, mockNrRepository)

    @Before
    fun setUp() {
        CarrierConfigRepository.resetForTest()
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_VONR_ENABLED_BOOL,
            value = true,
        )
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL,
            value = true,
        )
    }

    @Test
    fun isVoNrAvailable_visibleDisable_returnFalse() {
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL,
            value = false,
        )

        val available = repository.isVoNrAvailable(SUB_ID)

        assertThat(available).isFalse()
    }

    @Test
    fun isVoNrAvailable_voNrDisabled_returnFalse() {
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_VONR_ENABLED_BOOL,
            value = false,
        )

        val available = repository.isVoNrAvailable(SUB_ID)

        assertThat(available).isFalse()
    }

    @Test
    fun isVoNrAvailable_allEnabled_returnTrue() {
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_VONR_ENABLED_BOOL,
            value = true,
        )
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL,
            value = true,
        )

        val available = repository.isVoNrAvailable(SUB_ID)

        assertThat(available).isTrue()
    }

    @Test
    fun isVoNrAvailable_noNr_returnFalse() {
        mockNrRepository.stub { on { isNrAvailable(SUB_ID) } doReturn false }

        val available = repository.isVoNrAvailable(SUB_ID)

        assertThat(available).isFalse()
    }

    @Test
    fun isVoNrEnabledFlow_voNrDisabled() = runBlocking {
        mockTelephonyManager.stub { on { isVoNrEnabled } doReturn false }

        val isVoNrEnabled = repository.isVoNrEnabledFlow(SUB_ID).firstWithTimeoutOrNull()

        assertThat(isVoNrEnabled).isFalse()
    }

    @Test
    fun isVoNrEnabledFlow_voNrEnabled() = runBlocking {
        mockTelephonyManager.stub { on { isVoNrEnabled } doReturn true }

        val isVoNrEnabled = repository.isVoNrEnabledFlow(SUB_ID).firstWithTimeoutOrNull()

        assertThat(isVoNrEnabled).isTrue()
    }

    @Test
    fun isVoNrEnabledFlow_noPhoneProcess_noCrash() = runBlocking {
        mockTelephonyManager.stub { on { isVoNrEnabled } doThrow IllegalStateException("no Phone") }

        val isVoNrEnabled = repository.isVoNrEnabledFlow(SUB_ID).firstWithTimeoutOrNull()

        assertThat(isVoNrEnabled).isFalse()
    }

    @Test
    fun setVoNrEnabled(): Unit = runBlocking {
        repository.setVoNrEnabled(SUB_ID, true)

        verify(mockTelephonyManager).setVoNrEnabled(true)
    }

    @Test
    fun setVoNrEnabled_noPhoneProcess_noCrash(): Unit = runBlocking {
        mockTelephonyManager.stub {
            on {
                setVoNrEnabled(any())
            } doThrow IllegalStateException("no Phone")
        }

        repository.setVoNrEnabled(SUB_ID, true)

        verify(mockTelephonyManager).setVoNrEnabled(true)
    }

    private companion object {
        const val SUB_ID = 1
    }
}