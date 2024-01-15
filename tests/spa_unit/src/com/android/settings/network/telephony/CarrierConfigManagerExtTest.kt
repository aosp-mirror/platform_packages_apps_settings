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
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class CarrierConfigManagerExtTest {

    private val mockCarrierConfigManager = mock<CarrierConfigManager>()

    private val context = mock<Context> {
        on { getSystemService(CarrierConfigManager::class.java) } doReturn mockCarrierConfigManager
    }

    @Test
    fun safeGetConfig_managerReturnKeyValue_returnNonEmptyBundle() {
        mockCarrierConfigManager.stub {
            on { getConfigForSubId(any(), eq(KEY)) } doReturn persistableBundleOf(KEY to VALUE)
        }
        val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)!!

        val bundle = carrierConfigManager.safeGetConfig(listOf(KEY))

        assertThat(bundle.getString(KEY)).isEqualTo(VALUE)
    }

    @Test
    fun safeGetConfig_managerThrowIllegalStateException_returnEmptyBundle() {
        mockCarrierConfigManager.stub {
            on { getConfigForSubId(any(), eq(KEY)) } doThrow IllegalStateException()
        }
        val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)!!

        val bundle = carrierConfigManager.safeGetConfig(listOf(KEY))

        assertThat(bundle.containsKey(KEY)).isFalse()
    }

    private companion object {
        const val KEY = "key"
        const val VALUE = "value"
    }
}
