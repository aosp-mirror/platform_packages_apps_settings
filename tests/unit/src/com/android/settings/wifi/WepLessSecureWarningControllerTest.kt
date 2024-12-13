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

package com.android.settings.wifi

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.wifitrackerlib.WifiEntry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.`when` as whenever

class WepLessSecureWarningControllerTest {
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()
    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Mock
    private lateinit var mockWifiEntry: WifiEntry

    private val controller = WepLessSecureWarningController(context, TEST_KEY)

    @Test
    fun getAvailabilityStatus_default_conditionallyUnavailable() {
        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_noWepSecurityType_conditionallyUnavailable() {
        whenever(mockWifiEntry.securityTypes).thenReturn(listOf(WifiEntry.SECURITY_PSK))

        controller.setWifiEntry(mockWifiEntry)

        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_containsWepSecurityType_available() {
        whenever(mockWifiEntry.securityTypes).thenReturn(listOf(WifiEntry.SECURITY_WEP))

        controller.setWifiEntry(mockWifiEntry)

        assertThat(controller.availabilityStatus).isEqualTo(AVAILABLE)
    }

    private companion object {
        const val TEST_KEY = "test_key"
    }
}
