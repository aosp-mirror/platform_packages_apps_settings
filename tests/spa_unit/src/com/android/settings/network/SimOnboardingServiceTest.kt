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

package com.android.settings.network

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.UiccCardInfo
import android.telephony.UiccPortInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub


@RunWith(AndroidJUnit4::class)
class SimOnboardingServiceTest {
    val simOnboardingService = SimOnboardingService()

    private val mockTelephonyManager = mock<TelephonyManager> {
        on { activeModemCount } doReturn 2
        on { isMultiSimSupported } doReturn TelephonyManager.MULTISIM_ALLOWED
        on { uiccCardsInfo } doReturn mepUiccCardInfoList
    }

    private val mockSubscriptionManager = mock<SubscriptionManager> {
            on { activeSubscriptionInfoList } doReturn listOf(
                SUB_INFO_1,
                SUB_INFO_2
            )
            on { availableSubscriptionInfoList } doReturn listOf(
                SUB_INFO_1,
                SUB_INFO_2,
                SUB_INFO_3,
            )
        }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(SubscriptionManager::class.java) } doReturn mockSubscriptionManager
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
    }

    @Test
    fun addItemForRenaming_addItemWithNewName_findItem() {
        val newName = "NewName"
        simOnboardingService.addItemForRenaming(SUB_INFO_1, newName)

        assertThat(simOnboardingService.renameMutableMap)
            .containsEntry(SUB_INFO_1.subscriptionId, newName)
    }

    @Test
    fun addItemForRenaming_sameNameAndItemNotInList_removeItem() {
        simOnboardingService.addItemForRenaming(SUB_INFO_1, DISPLAY_NAME_1)

        assertThat(simOnboardingService.renameMutableMap)
            .doesNotContainKey(SUB_INFO_1.subscriptionId)
    }

    @Test
    fun addItemForRenaming_sameNameAndItemInList_removeItem() {
        simOnboardingService.renameMutableMap[SUB_INFO_1.subscriptionId] = "NewName"

        simOnboardingService.addItemForRenaming(SUB_INFO_1, DISPLAY_NAME_1)

        assertThat(simOnboardingService.renameMutableMap)
            .doesNotContainKey(SUB_INFO_1.subscriptionId)
    }

    @Test
    fun isDsdsConditionSatisfied_isMultiSimEnabled_returnFalse(){
        simOnboardingService.initData(SUB_ID_3, context, {})

        assertThat(simOnboardingService.isDsdsConditionSatisfied()).isFalse()
    }

    @Test
    fun isDsdsConditionSatisfied_isNotMultiSimSupported_returnFalse() {
        mockTelephonyManager.stub {
            on { activeModemCount } doReturn 1
            on {
                isMultiSimSupported
            } doReturn TelephonyManager.MULTISIM_NOT_SUPPORTED_BY_HARDWARE
        }
        simOnboardingService.initData(SUB_ID_3, context, {})

        assertThat(simOnboardingService.isDsdsConditionSatisfied()).isFalse()
    }

    @Test
    fun isDsdsConditionSatisfied_mepAndOneActiveSim_returnTrue() = runBlocking {
        mockTelephonyManager.stub {
            on { activeModemCount } doReturn 1
        }
        simOnboardingService.initData(SUB_ID_3, context, {})
        delay(100)

        assertThat(simOnboardingService.isDsdsConditionSatisfied()).isTrue()
    }

    @Test
    fun isDsdsConditionSatisfied_mepAndNoActiveSim_returnFalse() = runBlocking {
        mockTelephonyManager.stub {
            on { activeModemCount } doReturn 1
        }
        mockSubscriptionManager.stub {
            on { activeSubscriptionInfoList } doReturn listOf()
        }
        simOnboardingService.initData(SUB_ID_3, context, {})
        delay(100)

        assertThat(simOnboardingService.isDsdsConditionSatisfied()).isFalse()
    }

    @Test
    fun isDsdsConditionSatisfied_insertEsimAndOneActivePsimNoMep_returnTrue() = runBlocking {
        mockTelephonyManager.stub {
            on { getActiveModemCount() } doReturn 1
            on { uiccCardsInfo } doReturn noMepUiccCardInfoList
        }
        simOnboardingService.initData(SUB_ID_3, context, {})
        delay(100)

        assertThat(simOnboardingService.isDsdsConditionSatisfied()).isTrue()
    }

    @Test
    fun isDsdsConditionSatisfied_insertEsimAndNoPsimNoMep_returnFalse() = runBlocking {
        mockTelephonyManager.stub {
            on { getActiveModemCount() } doReturn 1
            on { uiccCardsInfo } doReturn noMepUiccCardInfoList
        }
        mockSubscriptionManager.stub {
            on { activeSubscriptionInfoList } doReturn listOf()
        }
        simOnboardingService.initData(SUB_ID_3, context, {})
        delay(100)

        assertThat(simOnboardingService.isDsdsConditionSatisfied()).isFalse()
    }

    @Test
    fun isDsdsConditionSatisfied_insertPsimAndOneActiveEsimNoMep_returnTrue() = runBlocking {
        mockTelephonyManager.stub {
            on { getActiveModemCount() } doReturn 1
            on { uiccCardsInfo } doReturn noMepUiccCardInfoList
        }
        mockSubscriptionManager.stub {
            on { activeSubscriptionInfoList } doReturn listOf(
                SUB_INFO_2
            )
        }
        simOnboardingService.initData(SUB_ID_1, context, {})
        delay(100)

        assertThat(simOnboardingService.isDsdsConditionSatisfied()).isTrue()
    }

    @Test
    fun isDsdsConditionSatisfied_insertPsimAndNoEsimNoMep_returnFalse() = runBlocking {
        mockTelephonyManager.stub {
            on { getActiveModemCount() } doReturn 1
            on { uiccCardsInfo } doReturn noMepUiccCardInfoList
        }
        mockSubscriptionManager.stub {
            on { activeSubscriptionInfoList } doReturn listOf()
        }
        simOnboardingService.initData(SUB_ID_1, context, {})
        delay(100)

        assertThat(simOnboardingService.isDsdsConditionSatisfied()).isFalse()
    }

    private companion object {
        const val SUB_ID_1 = 1
        const val SUB_ID_2 = 2
        const val SUB_ID_3 = 3
        const val SUB_ID_4 = 4
        const val DISPLAY_NAME_1 = "Sub 1"

        val SUB_INFO_1: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(SUB_ID_1)
            setDisplayName(DISPLAY_NAME_1)
        }.build()

        val SUB_INFO_2: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(SUB_ID_2)
            setEmbedded(true)
        }.build()

        val SUB_INFO_3: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(SUB_ID_3)
            setEmbedded(true)
        }.build()

        val SUB_INFO_4: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(SUB_ID_4)
        }.build()

        private const val REMOVABLE_CARD_ID_1: Int = 25
        private const val REMOVABLE_CARD_ID_2: Int = 26
        private const val EUICC_CARD_ID_3: Int = 27
        private const val EUICC_CARD_ID_4: Int = 28

        val noMepUiccCardInfoList: List<UiccCardInfo> = listOf(
            createUiccCardInfo(
                isEuicc = true,
                cardId = EUICC_CARD_ID_3,
                physicalSlotIndex = 0,
                isRemovable = false,
                isMultipleEnabledProfileSupported = false,
                logicalSlotIndex = -1,
                portIndex = -1
            ),
            createUiccCardInfo(
                isEuicc = false,
                cardId = REMOVABLE_CARD_ID_1,
                physicalSlotIndex = 1,
                isRemovable = true,
                isMultipleEnabledProfileSupported = false,
                logicalSlotIndex = -1,
                portIndex = -1
            )
        )
        val mepUiccCardInfoList: List<UiccCardInfo> = listOf(
            createUiccCardInfo(
                isEuicc = true,
                cardId = EUICC_CARD_ID_3,
                physicalSlotIndex = 0,
                isRemovable = false,
                logicalSlotIndex = -1,
                portIndex = -1
            ),
            createUiccCardInfo(
                isEuicc = false,
                cardId = REMOVABLE_CARD_ID_1,
                physicalSlotIndex = 1,
                isRemovable = true,
                logicalSlotIndex = -1,
                portIndex = -1
            )
        )

        private fun createUiccCardInfo(
            isEuicc: Boolean,
            cardId: Int,
            physicalSlotIndex: Int,
            isRemovable: Boolean,
            logicalSlotIndex: Int,
            portIndex: Int,
            isMultipleEnabledProfileSupported:Boolean = true,
        ): UiccCardInfo {
            return UiccCardInfo(
                isEuicc,  /* isEuicc */
                cardId,  /* cardId */
                null,  /* eid */
                physicalSlotIndex,  /* physicalSlotIndex */
                isRemovable,  /* isRemovable */
                isMultipleEnabledProfileSupported,  /* isMultipleEnabledProfileSupported */
                listOf(
                    UiccPortInfo(
                        "123451234567890",  /* iccId */
                        portIndex,  /* portIdx */
                        logicalSlotIndex,  /* logicalSlotIdx */
                        true /* isActive */
                    )
                )
            )
        }
    }
}