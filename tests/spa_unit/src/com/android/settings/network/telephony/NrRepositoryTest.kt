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
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class NrRepositoryTest {
    private val mockTelephonyManager =
        mock<TelephonyManager> {
            on { createForSubscriptionId(SUB_ID) } doReturn mock
            on { supportedRadioAccessFamily } doReturn TelephonyManager.NETWORK_TYPE_BITMASK_NR
        }

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
        }

    private val repository = NrRepository(context)

    @Before
    fun setUp() {
        CarrierConfigRepository.resetForTest()
    }

    @Test
    fun isNrAvailable_deviceNoNr_returnFalse() {
        mockTelephonyManager.stub {
            on { supportedRadioAccessFamily } doReturn TelephonyManager.NETWORK_TYPE_BITMASK_LTE
        }

        val available = repository.isNrAvailable(SUB_ID)

        assertThat(available).isFalse()
    }

    @Test
    fun isNrAvailable_carrierConfigNrIsEmpty_returnFalse() {
        CarrierConfigRepository.setIntArrayForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
            value = intArrayOf(),
        )

        val available = repository.isNrAvailable(SUB_ID)

        assertThat(available).isFalse()
    }

    @Test
    fun isNrAvailable_carrierConfigNrIsNull_returnFalse() {
        CarrierConfigRepository.setIntArrayForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
            value = null,
        )

        val available = repository.isNrAvailable(SUB_ID)

        assertThat(available).isFalse()
    }

    @Test
    fun isNrAvailable_allEnabled_returnTrue() {
        mockTelephonyManager.stub {
            on { supportedRadioAccessFamily } doReturn TelephonyManager.NETWORK_TYPE_BITMASK_NR
        }
        CarrierConfigRepository.setIntArrayForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
            value = intArrayOf(1, 2),
        )

        val available = repository.isNrAvailable(SUB_ID)

        assertThat(available).isTrue()
    }

    private companion object {
        const val SUB_ID = 10
    }
}
