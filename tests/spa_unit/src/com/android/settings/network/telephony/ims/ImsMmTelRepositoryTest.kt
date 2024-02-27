/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.network.telephony.ims

import android.content.Context
import android.telephony.ims.ImsMmTelManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class ImsMmTelRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockImsMmTelManager = mock<ImsMmTelManager> {
        on { isVoWiFiSettingEnabled } doReturn true
        on { getVoWiFiRoamingModeSetting() } doReturn ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED
        on { getVoWiFiModeSetting() } doReturn ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED
    }

    private val repository = ImsMmTelRepositoryImpl(context, SUB_ID, mockImsMmTelManager)

    @Test
    fun getWiFiCallingMode_voWiFiSettingNotEnabled_returnUnknown() {
        mockImsMmTelManager.stub {
            on { isVoWiFiSettingEnabled } doReturn false
        }

        val wiFiCallingMode = repository.getWiFiCallingMode(false)

        assertThat(wiFiCallingMode).isEqualTo(ImsMmTelManager.WIFI_MODE_UNKNOWN)
    }

    @Test
    fun getWiFiCallingMode_useRoamingMode_returnRoamingSetting() {
        val wiFiCallingMode = repository.getWiFiCallingMode(true)

        assertThat(wiFiCallingMode).isEqualTo(mockImsMmTelManager.getVoWiFiRoamingModeSetting())
    }

    @Test
    fun getWiFiCallingMode_notSseRoamingMode_returnHomeSetting() {
        val wiFiCallingMode = repository.getWiFiCallingMode(false)

        assertThat(wiFiCallingMode).isEqualTo(mockImsMmTelManager.getVoWiFiModeSetting())
    }

    @Test
    fun getWiFiCallingMode_illegalArgumentException_returnUnknown() {
        mockImsMmTelManager.stub {
            on { isVoWiFiSettingEnabled } doThrow IllegalArgumentException()
        }

        val wiFiCallingMode = repository.getWiFiCallingMode(false)

        assertThat(wiFiCallingMode).isEqualTo(ImsMmTelManager.WIFI_MODE_UNKNOWN)
    }

    private companion object {
        const val SUB_ID = 1
    }
}
