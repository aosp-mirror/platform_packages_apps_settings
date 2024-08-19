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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CarrierSettingsVersionPreferenceControllerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val controller =
        CarrierSettingsVersionPreferenceController(context, TEST_KEY).apply { init(SUB_ID) }

    @Before
    fun setUp() {
        CarrierConfigRepository.resetForTest()
    }

    @Test
    fun getSummary_nullConfig_noCrash() {
        controller.getSummary()
    }

    @Test
    fun getSummary_nullVersionString_returnNull() {
        CarrierConfigRepository.setStringForTest(
            SUB_ID, CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING, null)

        val summary = controller.getSummary()

        assertThat(summary).isNull()
    }

    @Test
    fun getSummary_hasVersionString_returnCorrectSummary() {
        CarrierConfigRepository.setStringForTest(
            SUB_ID, CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING, "test_version_123")

        val summary = controller.getSummary()

        assertThat(summary).isEqualTo("test_version_123")
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val SUB_ID = 10
    }
}
