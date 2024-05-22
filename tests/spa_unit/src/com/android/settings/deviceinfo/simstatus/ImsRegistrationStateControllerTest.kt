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

package com.android.settings.deviceinfo.simstatus

import android.content.Context
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.network.telephony.SimSlotRepository
import com.android.settings.network.telephony.ims.ImsMmTelRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class ImsRegistrationStateControllerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockSimSlotRepository = mock<SimSlotRepository> {
        on { subIdInSimSlotFlow(SIM_SLOT_INDEX) } doReturn flowOf(SUB_ID)
    }

    private val mockImsMmTelRepository = mock<ImsMmTelRepository> {
        on { imsRegisteredFlow() } doReturn flowOf(true)
    }

    private val controller = ImsRegistrationStateController(
        context = context,
        simSlotRepository = mockSimSlotRepository,
        imsMmTelRepositoryFactory = { subId ->
            assertThat(subId).isEqualTo(SUB_ID)
            mockImsMmTelRepository
        },
    )

    @Test
    fun collectImsRegistered() = runBlocking {
        var imsRegistered = false

        controller.collectImsRegistered(TestLifecycleOwner(), SIM_SLOT_INDEX) {
            imsRegistered = it
        }
        delay(100)

        assertThat(imsRegistered).isTrue()
    }

    private companion object {
        const val SIM_SLOT_INDEX = 0
        const val SUB_ID = 1
    }
}
