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
import android.telephony.CarrierConfigManager
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.deviceinfo.simstatus.SimStatusDialogRepository.SimStatusDialogInfo
import com.android.settings.network.telephony.CarrierConfigRepository
import com.android.settings.network.telephony.SimSlotRepository
import com.android.settings.network.telephony.ims.ImsMmTelRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class SimStatusDialogRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockSimSlotRepository =
        mock<SimSlotRepository> {
            on { subIdInSimSlotFlow(SIM_SLOT_INDEX) } doReturn flowOf(SUB_ID)
        }

    private val mockSignalStrengthRepository =
        mock<SignalStrengthRepository> {
            on { signalStrengthDisplayFlow(SUB_ID) } doReturn flowOf(SIGNAL_STRENGTH)
        }

    private val mockImsMmTelRepository =
        mock<ImsMmTelRepository> { on { imsRegisteredFlow() } doReturn flowOf(true) }

    private val controller =
        SimStatusDialogRepository(
            context = context,
            simSlotRepository = mockSimSlotRepository,
            signalStrengthRepository = mockSignalStrengthRepository,
            imsMmTelRepositoryFactory = { subId ->
                assertThat(subId).isEqualTo(SUB_ID)
                mockImsMmTelRepository
            },
        )

    @Before
    fun setUp() {
        CarrierConfigRepository.resetForTest()
    }

    @Test
    fun collectSimStatusDialogInfo() = runBlocking {
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL,
            value = true,
        )
        var simStatusDialogInfo = SimStatusDialogInfo()

        controller.collectSimStatusDialogInfo(TestLifecycleOwner(), SIM_SLOT_INDEX) {
            simStatusDialogInfo = it
        }
        delay(100)

        assertThat(simStatusDialogInfo)
            .isEqualTo(
                SimStatusDialogInfo(
                    signalStrength = SIGNAL_STRENGTH,
                    imsRegistered = true,
                ))
    }

    @Test
    fun collectSimStatusDialogInfo_doNotShowSignalStrength() = runBlocking {
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_SHOW_SIGNAL_STRENGTH_IN_SIM_STATUS_BOOL,
            value = false,
        )
        var simStatusDialogInfo = SimStatusDialogInfo()

        controller.collectSimStatusDialogInfo(TestLifecycleOwner(), SIM_SLOT_INDEX) {
            simStatusDialogInfo = it
        }
        delay(100)

        assertThat(simStatusDialogInfo.signalStrength).isNull()
    }

    @Test
    fun collectSimStatusDialogInfo_doNotShowImsRegistration() = runBlocking {
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL,
            value = false,
        )
        var simStatusDialogInfo = SimStatusDialogInfo()

        controller.collectSimStatusDialogInfo(TestLifecycleOwner(), SIM_SLOT_INDEX) {
            simStatusDialogInfo = it
        }
        delay(100)

        assertThat(simStatusDialogInfo.imsRegistered).isNull()
    }

    private companion object {
        const val SIM_SLOT_INDEX = 0
        const val SUB_ID = 1

        const val SIGNAL_STRENGTH = "-82 dBm 58 asu"
    }
}
