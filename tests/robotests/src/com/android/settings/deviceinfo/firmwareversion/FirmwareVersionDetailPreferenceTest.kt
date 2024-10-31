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
import android.os.Build
import android.os.SystemClock
import android.os.UserManager
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.deviceinfo.firmwareversion.FirmwareVersionDetailPreference.Companion.DELAY_TIMER_MILLIS
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

// LINT.IfChange
@RunWith(RobolectricTestRunner::class)
class FirmwareVersionDetailPreferenceTest {
    private var userManager: UserManager? = null

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                if (name == Context.USER_SERVICE) userManager else super.getSystemService(name)
        }

    private val preference = Preference(context)

    private val firmwareVersionDetailPreference = FirmwareVersionDetailPreference()

    @Test
    fun getSummary() {
        assertThat(firmwareVersionDetailPreference.getSummary(context))
            .isEqualTo(Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY)
    }

    @Test
    fun onPreferenceClick_hits() {
        prepareClick()
        assertThat(firmwareVersionDetailPreference.onPreferenceClick(preference)).isFalse()
    }

    @Test
    fun onPreferenceClick_restricted() {
        prepareClick()
        userManager = mock { on { hasUserRestriction(UserManager.DISALLOW_FUN) } doReturn true }
        assertThat(firmwareVersionDetailPreference.onPreferenceClick(preference)).isTrue()
    }

    private fun prepareClick() {
        SystemClock.sleep(DELAY_TIMER_MILLIS + 1)
        assertThat(SystemClock.uptimeMillis()).isGreaterThan(DELAY_TIMER_MILLIS)
        for (i in 1..<FirmwareVersionDetailPreference.ACTIVITY_TRIGGER_COUNT) {
            assertThat(firmwareVersionDetailPreference.onPreferenceClick(preference)).isTrue()
        }
    }
}
// LINT.ThenChange(FirmwareVersionDetailPreferenceControllerTest.java)
