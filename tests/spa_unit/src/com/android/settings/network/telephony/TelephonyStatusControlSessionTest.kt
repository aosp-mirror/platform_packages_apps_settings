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

package com.android.settings.network.telephony

import android.content.Context
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.spa.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TelephonyStatusControlSessionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun init() = runTest {
        val controller = TestController(context)

        val session = TelephonyStatusControlSession(
            controllers = listOf(controller),
            lifecycle = TestLifecycleOwner().lifecycle,
        )

        waitUntil { controller.availabilityStatus == STATUS }
        session.close()
    }

    @Test
    fun close() = runTest {
        val controller = TestController(context)

        val session = TelephonyStatusControlSession(
            controllers = listOf(controller),
            lifecycle = TestLifecycleOwner().lifecycle,
        )
        session.close()

        assertThat(controller.availabilityStatus).isNull()
    }

    private companion object {
        const val KEY = "key"
        const val STATUS = BasePreferenceController.AVAILABLE
    }

    private class TestController(context: Context) : BasePreferenceController(context, KEY),
        TelephonyAvailabilityHandler {

        var availabilityStatus: Int? = null
        override fun getAvailabilityStatus(): Int = STATUS

        override fun setAvailabilityStatus(status: Int) {
            availabilityStatus = status
        }

        override fun unsetAvailabilityStatus() {
            availabilityStatus = null
        }
    }
}
