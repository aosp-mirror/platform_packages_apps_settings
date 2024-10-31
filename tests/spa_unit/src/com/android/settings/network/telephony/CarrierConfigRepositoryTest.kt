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
import androidx.core.os.persistableBundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class CarrierConfigRepositoryTest {

    private val mockCarrierConfigManager = mock<CarrierConfigManager>()

    private val context =
        mock<Context> {
            on { applicationContext } doReturn mock
            on { getSystemService(CarrierConfigManager::class.java) } doReturn
                mockCarrierConfigManager
        }

    private val repository = CarrierConfigRepository(context)

    @Before
    fun setUp() {
        CarrierConfigRepository.resetForTest()
    }

    @Test
    fun getBoolean_returnValue() {
        val key = CarrierConfigManager.KEY_CARRIER_CONFIG_APPLIED_BOOL
        mockCarrierConfigManager.stub {
            on { getConfigForSubId(any(), eq(key)) } doReturn persistableBundleOf(key to true)
        }

        val value = repository.getBoolean(SUB_ID, key)

        assertThat(value).isTrue()
    }

    @Test
    fun getInt_returnValue() {
        val key = CarrierConfigManager.KEY_GBA_MODE_INT
        mockCarrierConfigManager.stub {
            on { getConfigForSubId(any(), eq(key)) } doReturn persistableBundleOf(key to 99)
        }

        val value = repository.getInt(SUB_ID, key)

        assertThat(value).isEqualTo(99)
    }

    @Test
    fun getIntArray_returnValue() {
        val key = CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY
        mockCarrierConfigManager.stub {
            on { getConfigForSubId(any(), eq(key)) } doReturn
                persistableBundleOf(key to intArrayOf(99))
        }

        val value = repository.getIntArray(SUB_ID, key)!!.toList()

        assertThat(value).containsExactly(99)
    }

    @Test
    fun getString_returnValue() {
        val key = CarrierConfigManager.KEY_CARRIER_NAME_STRING
        mockCarrierConfigManager.stub {
            on { getConfigForSubId(any(), eq(key)) } doReturn
                persistableBundleOf(key to STRING_VALUE)
        }

        val value = repository.getString(SUB_ID, key)

        assertThat(value).isEqualTo(STRING_VALUE)
    }

    @Test
    fun transformConfig_managerThrowIllegalStateException_returnDefaultValue() {
        mockCarrierConfigManager.stub {
            on { getConfigForSubId(any(), anyVararg()) } doThrow IllegalStateException()
        }

        val carrierName =
            repository.transformConfig(SUB_ID) {
                getInt(CarrierConfigManager.KEY_CARRIER_DEFAULT_WFC_IMS_MODE_INT)
            }

        assertThat(carrierName)
            .isEqualTo(
                CarrierConfigManager.getDefaultConfig()
                    .getInt(CarrierConfigManager.KEY_CARRIER_DEFAULT_WFC_IMS_MODE_INT)
            )
    }

    @Test
    fun transformConfig_getValueTwice_cached() {
        val key = CarrierConfigManager.KEY_CARRIER_NAME_STRING
        mockCarrierConfigManager.stub {
            on { getConfigForSubId(any(), eq(key)) } doReturn
                persistableBundleOf(key to STRING_VALUE)
        }

        repository.transformConfig(SUB_ID) { getString(key) }
        repository.transformConfig(SUB_ID) { getString(key) }

        verify(mockCarrierConfigManager, times(1)).getConfigForSubId(any(), anyVararg())
    }

    @Test
    fun transformConfig_registerCarrierConfigChangeListener() {
        val key = CarrierConfigManager.KEY_CARRIER_NAME_STRING

        repository.transformConfig(SUB_ID) { getString(key) }
        repository.transformConfig(SUB_ID) { getString(key) }

        verify(mockCarrierConfigManager, times(1)).registerCarrierConfigChangeListener(any(), any())
    }

    private companion object {
        const val SUB_ID = 123
        const val STRING_VALUE = "value"
    }
}
