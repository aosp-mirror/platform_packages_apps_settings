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
import androidx.core.os.persistableBundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VoNrRepositoryTest {

    private val mockTelephonyManager = mock<TelephonyManager> {
        on { createForSubscriptionId(SUB_ID) } doReturn mock
        on { supportedRadioAccessFamily } doReturn TelephonyManager.NETWORK_TYPE_BITMASK_NR
    }

    private val carrierConfig = persistableBundleOf(
        CarrierConfigManager.KEY_VONR_ENABLED_BOOL to true,
        CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL to true,
        CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY to intArrayOf(1, 2),
    )

    private val mockCarrierConfigManager = mock<CarrierConfigManager> {
        on { getConfigForSubId(eq(SUB_ID), anyVararg()) } doReturn carrierConfig
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
        on { getSystemService(CarrierConfigManager::class.java) } doReturn mockCarrierConfigManager
    }

    private val repository = VoNrRepository(context, SUB_ID)

    @Test
    fun isVoNrAvailable_visibleDisable_returnFalse() {
        carrierConfig.apply {
            putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, false)
        }

        val available = repository.isVoNrAvailable()

        assertThat(available).isFalse()
    }

    @Test
    fun isVoNrAvailable_voNrDisabled_returnFalse() {
        carrierConfig.apply {
            putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, false)
        }

        val available = repository.isVoNrAvailable()

        assertThat(available).isFalse()
    }

    @Test
    fun isVoNrAvailable_allEnabled_returnTrue() {
        mockTelephonyManager.stub {
            on { supportedRadioAccessFamily } doReturn TelephonyManager.NETWORK_TYPE_BITMASK_NR
        }
        carrierConfig.apply {
            putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, true)
            putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, true)
            putIntArray(
                CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
                intArrayOf(1, 2),
            )
        }

        val available = repository.isVoNrAvailable()

        assertThat(available).isTrue()
    }

    @Test
    fun isVoNrAvailable_deviceNoNr_returnFalse() {
        mockTelephonyManager.stub {
            on { supportedRadioAccessFamily } doReturn TelephonyManager.NETWORK_TYPE_BITMASK_LTE
        }

        val available = repository.isVoNrAvailable()

        assertThat(available).isFalse()
    }

    @Test
    fun isVoNrAvailable_carrierNoNr_returnFalse() {
        carrierConfig.apply {
            putIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY, intArrayOf())
        }

        val available = repository.isVoNrAvailable()

        assertThat(available).isFalse()
    }

    @Test
    fun isVoNrAvailable_carrierConfigNrIsNull_returnFalse() {
        carrierConfig.apply {
            putIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY, null)
        }

        val available = repository.isVoNrAvailable()

        assertThat(available).isFalse()
    }

    @Test
    fun isVoNrEnabledFlow_voNrDisabled() = runBlocking {
        mockTelephonyManager.stub {
            on { isVoNrEnabled } doReturn false
        }

        val isVoNrEnabled = repository.isVoNrEnabledFlow().firstWithTimeoutOrNull()

        assertThat(isVoNrEnabled).isFalse()
    }

    @Test
    fun isVoNrEnabledFlow_voNrEnabled() = runBlocking {
        mockTelephonyManager.stub {
            on { isVoNrEnabled } doReturn true
        }

        val isVoNrEnabled = repository.isVoNrEnabledFlow().firstWithTimeoutOrNull()

        assertThat(isVoNrEnabled).isTrue()
    }

    @Test
    fun setVoNrEnabled(): Unit = runBlocking {
        repository.setVoNrEnabled(true)

        verify(mockTelephonyManager).setVoNrEnabled(true)
    }

    private companion object {
        const val SUB_ID = 1
    }
}
