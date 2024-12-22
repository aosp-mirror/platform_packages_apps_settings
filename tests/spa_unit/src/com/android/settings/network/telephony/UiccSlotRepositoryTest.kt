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

import android.telephony.TelephonyManager
import android.telephony.UiccPortInfo
import android.telephony.UiccSlotInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class UiccSlotRepositoryTest {

    private val mockTelephonyManager = mock<TelephonyManager>()

    private val repository = UiccSlotRepository(mockTelephonyManager)

    @Test
    fun anyRemovablePhysicalSimEnabled_oneSimSlotDeviceActiveEsim_returnsFalse() {
        mockTelephonyManager.stub {
            on { uiccSlotsInfo } doReturn
                arrayOf(
                    createUiccSlotInfo(
                        isEuicc = true, isRemovable = false, logicalSlotIdx = 1, isActive = true),
                )
        }

        val result = repository.anyRemovablePhysicalSimEnabled()

        assertThat(result).isFalse()
    }

    @Test
    fun anyRemovablePhysicalSimEnabled_activeRemovableEsimAndInactivePsim_returnsFalse() {
        mockTelephonyManager.stub {
            on { uiccSlotsInfo } doReturn
                arrayOf(
                    createUiccSlotInfo(
                        isEuicc = true, isRemovable = true, logicalSlotIdx = 0, isActive = true),
                    createUiccSlotInfo(
                        isEuicc = false, isRemovable = true, logicalSlotIdx = -1, isActive = false),
                )
        }

        val result = repository.anyRemovablePhysicalSimEnabled()

        assertThat(result).isFalse()
    }

    @Test
    fun anyRemovablePhysicalSimEnabled_activeRemovableEsimAndActivePsim_returnsTrue() {
        mockTelephonyManager.stub {
            on { uiccSlotsInfo } doReturn
                arrayOf(
                    createUiccSlotInfo(
                        isEuicc = false, isRemovable = true, logicalSlotIdx = 0, isActive = true),
                    createUiccSlotInfo(
                        isEuicc = true, isRemovable = true, logicalSlotIdx = 1, isActive = true),
                )
        }

        val result = repository.anyRemovablePhysicalSimEnabled()

        assertThat(result).isTrue()
    }

    @Test
    fun anyRemovablePhysicalSimEnabled_inactiveRemovableEsimAndActivePsim_returnsTrue() {
        mockTelephonyManager.stub {
            on { uiccSlotsInfo } doReturn
                arrayOf(
                    createUiccSlotInfo(
                        isEuicc = true, isRemovable = true, logicalSlotIdx = -1, isActive = false),
                    createUiccSlotInfo(
                        isEuicc = false, isRemovable = true, logicalSlotIdx = 0, isActive = true),
                )
        }

        val result = repository.anyRemovablePhysicalSimEnabled()

        assertThat(result).isTrue()
    }

    @Test
    fun anyRemovablePhysicalSimEnabled_twoActiveRemovableEsimsAndInactivePsim_returnsFalse() {
        mockTelephonyManager.stub {
            on { uiccSlotsInfo } doReturn
                arrayOf(
                    createUiccSlotInfoForRemovableEsimMep(
                        logicalSlotIdx1 = 0,
                        isActiveEsim1 = true,
                        logicalSlotIdx2 = 1,
                        isActiveEsim2 = true,
                    ),
                    createUiccSlotInfo(
                        isEuicc = false, isRemovable = true, logicalSlotIdx = -1, isActive = false),
                )
        }

        val result = repository.anyRemovablePhysicalSimEnabled()

        assertThat(result).isFalse()
    }

    @Test
    fun anyRemovablePhysicalSimEnabled_oneActiveOneInactiveRemovableEsimActivePsim_returnsTrue() {
        mockTelephonyManager.stub {
            on { uiccSlotsInfo } doReturn
                arrayOf(
                    createUiccSlotInfoForRemovableEsimMep(
                        logicalSlotIdx1 = 1,
                        isActiveEsim1 = true,
                        logicalSlotIdx2 = -1,
                        isActiveEsim2 = false,
                    ),
                    createUiccSlotInfo(
                        isEuicc = false, isRemovable = true, logicalSlotIdx = 0, isActive = true),
                )
        }

        val result = repository.anyRemovablePhysicalSimEnabled()

        assertThat(result).isTrue()
    }

    @Test
    fun anyRemovablePhysicalSimEnabled_activePsim_returnsTrue() {
        mockTelephonyManager.stub {
            on { uiccSlotsInfo } doReturn
                arrayOf(
                    createUiccSlotInfo(
                        isEuicc = false, isRemovable = true, logicalSlotIdx = 0, isActive = true),
                )
        }

        val result = repository.anyRemovablePhysicalSimEnabled()

        assertThat(result).isTrue()
    }

    @Test
    fun anyRemovablePhysicalSimEnabled_inactivePsim_returnsFalse() {
        mockTelephonyManager.stub {
            on { uiccSlotsInfo } doReturn
                arrayOf(
                    createUiccSlotInfo(
                        isEuicc = false, isRemovable = true, logicalSlotIdx = -1, isActive = false),
                )
        }

        val result = repository.anyRemovablePhysicalSimEnabled()

        assertThat(result).isFalse()
    }

    @Test
    fun anyRemovablePhysicalSimEnabled_activeEsimAndActivePsim_returnsTrue() {
        mockTelephonyManager.stub {
            on { uiccSlotsInfo } doReturn
                arrayOf(
                    createUiccSlotInfo(
                        isEuicc = false, isRemovable = true, logicalSlotIdx = 0, isActive = true),
                    createUiccSlotInfo(
                        isEuicc = true, isRemovable = false, logicalSlotIdx = 1, isActive = true),
                )
        }

        val result = repository.anyRemovablePhysicalSimEnabled()

        assertThat(result).isTrue()
    }

    @Test
    fun anyRemovablePhysicalSimEnabled_activeEsimAndInactivePsim_returnsFalse() {
        mockTelephonyManager.stub {
            on { uiccSlotsInfo } doReturn
                arrayOf(
                    createUiccSlotInfo(
                        isEuicc = false, isRemovable = true, logicalSlotIdx = 0, isActive = false),
                    createUiccSlotInfo(
                        isEuicc = true, isRemovable = false, logicalSlotIdx = 1, isActive = true),
                )
        }

        val result = repository.anyRemovablePhysicalSimEnabled()

        assertThat(result).isFalse()
    }

    @Test
    fun anyRemovablePhysicalSimEnabled_uiccSlotInfoIsNull_returnsFalse() {
        mockTelephonyManager.stub { on { uiccSlotsInfo } doReturn arrayOf(null) }

        val result = repository.anyRemovablePhysicalSimEnabled()

        assertThat(result).isFalse()
    }

    private companion object {
        fun createUiccSlotInfo(
            isEuicc: Boolean,
            isRemovable: Boolean,
            logicalSlotIdx: Int,
            isActive: Boolean
        ) =
            UiccSlotInfo(
                isEuicc,
                /* cardId = */ "123",
                /* cardStateInfo = */ UiccSlotInfo.CARD_STATE_INFO_PRESENT,
                /* isExtendedApduSupported = */ true,
                isRemovable,
                /* portList = */ listOf(
                    UiccPortInfo(/* iccId= */ "", /* portIndex= */ 0, logicalSlotIdx, isActive),
                ),
            )

        fun createUiccSlotInfoForRemovableEsimMep(
            logicalSlotIdx1: Int,
            isActiveEsim1: Boolean,
            logicalSlotIdx2: Int,
            isActiveEsim2: Boolean,
        ) =
            UiccSlotInfo(
                /* isEuicc = */ true,
                /* cardId = */ "123",
                /* cardStateInfo = */ UiccSlotInfo.CARD_STATE_INFO_PRESENT,
                /* isExtendedApduSupported = */ true,
                /* isRemovable = */ true,
                /* portList = */ listOf(
                    UiccPortInfo(
                        /* iccId = */ "",
                        /* portIndex = */ 0,
                        /* logicalSlotIndex = */ logicalSlotIdx1,
                        /* isActive = */ isActiveEsim1),
                    UiccPortInfo(
                        /* iccId = */ "",
                        /* portIndex = */ 1,
                        /* logicalSlotIndex = */ logicalSlotIdx2,
                        /* isActive = */ isActiveEsim2),
                ),
            )
    }
}
