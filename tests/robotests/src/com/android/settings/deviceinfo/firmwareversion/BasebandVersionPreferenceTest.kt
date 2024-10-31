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

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import android.content.ContextWrapper
import android.sysprop.TelephonyProperties
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

// LINT.IfChange
@RunWith(RobolectricTestRunner::class)
class BasebandVersionPreferenceTest {
    private lateinit var telephonyManager: TelephonyManager

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when {
                    name == getSystemServiceName(TelephonyManager::class.java) -> telephonyManager
                    else -> super.getSystemService(name)
                }
        }

    private val basebandVersionPreference = BasebandVersionPreference()

    @Test
    fun isAvailable_wifiOnly_unavailable() {
        telephonyManager = mock { on { isDataCapable } doReturn false }
        assertThat(basebandVersionPreference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_hasMobile_available() {
        TelephonyProperties.baseband_version(listOf("test"))
        telephonyManager = mock { on { isDataCapable } doReturn true }
        assertThat(basebandVersionPreference.isAvailable(context)).isTrue()
    }
}
// LINT.ThenChange(BasebandVersionPreferenceControllerTest.java)
