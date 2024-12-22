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
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.android.settingslib.spa.testutils.toListWithTimeout
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class SubscriptionRepositoryTest {
    private var subInfoListener: SubscriptionManager.OnSubscriptionsChangedListener? = null

    private val mockSubscriptionManager = mock<SubscriptionManager> {
        on { addOnSubscriptionsChangedListener(any(), any()) } doAnswer {
            subInfoListener = it.arguments[1] as SubscriptionManager.OnSubscriptionsChangedListener
            subInfoListener?.onSubscriptionsChanged()
        }
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { subscriptionManager } doReturn mockSubscriptionManager
    }

    private val repository = SubscriptionRepository(context)

    @Test
    fun isSubscriptionEnabledFlow_invalidSubId() = runBlocking {
        val isEnabled = repository
            .isSubscriptionEnabledFlow(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
            .firstWithTimeoutOrNull()

        assertThat(isEnabled).isFalse()
    }

    @Test
    fun isSubscriptionEnabledFlow_enabled() = runBlocking {
        mockSubscriptionManager.stub {
            on { isSubscriptionEnabled(SUB_ID_IN_SLOT_0) } doReturn true
        }

        val isEnabled =
            repository.isSubscriptionEnabledFlow(SUB_ID_IN_SLOT_0).firstWithTimeoutOrNull()

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun subscriptionsChangedFlow_hasInitialValue() = runBlocking {
        val initialValue = repository.subscriptionsChangedFlow().firstWithTimeoutOrNull()

        assertThat(initialValue).isSameInstanceAs(Unit)
    }

    @Test
    fun subscriptionsChangedFlow_changed() = runBlocking {
        val listDeferred = async {
            repository.subscriptionsChangedFlow().toListWithTimeout()
        }
        delay(100)

        subInfoListener?.onSubscriptionsChanged()

        assertThat(listDeferred.await().size).isAtLeast(2)
    }

    @Test
    fun subscriptionsChangedFlow_managerNotCallOnSubscriptionsChangedInitially() = runBlocking {
        mockSubscriptionManager.stub {
            on { addOnSubscriptionsChangedListener(any(), any()) } doAnswer
                {
                    subInfoListener =
                        it.arguments[1] as SubscriptionManager.OnSubscriptionsChangedListener
                    // not call onSubscriptionsChanged here
                }
        }

        val initialValue = repository.subscriptionsChangedFlow().firstWithTimeoutOrNull()

        assertThat(initialValue).isSameInstanceAs(Unit)
    }

    @Test
    fun activeSubscriptionIdListFlow(): Unit = runBlocking {
        mockSubscriptionManager.stub {
            on { activeSubscriptionIdList } doReturn intArrayOf(SUB_ID_IN_SLOT_0)
        }

        val activeSubIds = repository.activeSubscriptionIdListFlow().firstWithTimeoutOrNull()

        assertThat(activeSubIds).containsExactly(SUB_ID_IN_SLOT_0)
    }

    @Test
    fun getSelectableSubscriptionInfoList_sortedBySimSlotIndex() {
        mockSubscriptionManager.stub {
            on { getAvailableSubscriptionInfoList() } doReturn listOf(
                SubscriptionInfo.Builder().apply {
                    setSimSlotIndex(SIM_SLOT_INDEX_0)
                    setId(SUB_ID_IN_SLOT_0)
                }.build(),
                SubscriptionInfo.Builder().apply {
                    setSimSlotIndex(SIM_SLOT_INDEX_1)
                    setId(SUB_ID_IN_SLOT_1)
                }.build(),
            )
        }

        val subInfos = repository.getSelectableSubscriptionInfoList()

        assertThat(subInfos.map { it.simSlotIndex })
            .containsExactly(SIM_SLOT_INDEX_0, SIM_SLOT_INDEX_1).inOrder()
    }

    @Test
    fun getSelectableSubscriptionInfoList_oneNotInSlot_inSlotSortedFirst() {
        mockSubscriptionManager.stub {
            on { getAvailableSubscriptionInfoList() } doReturn listOf(
                SubscriptionInfo.Builder().apply {
                    setSimSlotIndex(SubscriptionManager.INVALID_SIM_SLOT_INDEX)
                    setId(SUB_ID_3_NOT_IN_SLOT)
                }.build(),
                SubscriptionInfo.Builder().apply {
                    setSimSlotIndex(SIM_SLOT_INDEX_1)
                    setId(SUB_ID_IN_SLOT_1)
                }.build(),
            )
        }

        val subInfos = repository.getSelectableSubscriptionInfoList()

        assertThat(subInfos.map { it.simSlotIndex })
            .containsExactly(SIM_SLOT_INDEX_1, SubscriptionManager.INVALID_SIM_SLOT_INDEX).inOrder()
    }

    @Test
    fun getSelectableSubscriptionInfoList_sameGroupAndOneHasSlot_returnTheOneWithSimSlotIndex() {
        mockSubscriptionManager.stub {
            on { getAvailableSubscriptionInfoList() } doReturn listOf(
                SubscriptionInfo.Builder().apply {
                    setSimSlotIndex(SubscriptionManager.INVALID_SIM_SLOT_INDEX)
                    setId(SUB_ID_3_NOT_IN_SLOT)
                    setGroupUuid(GROUP_UUID)
                }.build(),
                SubscriptionInfo.Builder().apply {
                    setSimSlotIndex(SIM_SLOT_INDEX_0)
                    setId(SUB_ID_IN_SLOT_0)
                    setGroupUuid(GROUP_UUID)
                }.build(),
            )
        }

        val subInfos = repository.getSelectableSubscriptionInfoList()

        assertThat(subInfos.map { it.subscriptionId }).containsExactly(SUB_ID_IN_SLOT_0)
    }

    @Test
    fun getSelectableSubscriptionInfoList_sameGroupAndNonHasSlot_returnTheOneWithMinimumSubId() {
        mockSubscriptionManager.stub {
            on { getAvailableSubscriptionInfoList() } doReturn listOf(
                SubscriptionInfo.Builder().apply {
                    setId(SUB_ID_4_NOT_IN_SLOT)
                    setGroupUuid(GROUP_UUID)
                }.build(),
                SubscriptionInfo.Builder().apply {
                    setId(SUB_ID_3_NOT_IN_SLOT)
                    setGroupUuid(GROUP_UUID)
                }.build(),
            )
        }

        val subInfos = repository.getSelectableSubscriptionInfoList()

        assertThat(subInfos.map { it.subscriptionId }).containsExactly(SUB_ID_3_NOT_IN_SLOT)
    }

    @Test
    fun isSubscriptionVisibleFlow_available_returnTrue() = runBlocking {
        mockSubscriptionManager.stub {
            on { getAvailableSubscriptionInfoList() } doReturn
                listOf(SubscriptionInfo.Builder().apply { setId(SUB_ID_IN_SLOT_0) }.build())
        }

        val isVisible =
            repository.isSubscriptionVisibleFlow(SUB_ID_IN_SLOT_0).firstWithTimeoutOrNull()

        assertThat(isVisible).isTrue()
    }

    @Test
    fun isSubscriptionVisibleFlow_unavailable_returnFalse() = runBlocking {
        mockSubscriptionManager.stub {
            on { getAvailableSubscriptionInfoList() } doReturn
                listOf(SubscriptionInfo.Builder().apply { setId(SUB_ID_IN_SLOT_0) }.build())
        }

        val isVisible =
            repository.isSubscriptionVisibleFlow(SUB_ID_IN_SLOT_1).firstWithTimeoutOrNull()

        assertThat(isVisible).isFalse()
    }

    @Test
    fun phoneNumberFlow() = runBlocking {
        mockSubscriptionManager.stub {
            on { getPhoneNumber(SUB_ID_IN_SLOT_1) } doReturn NUMBER_1
        }
        val subInfo = SubscriptionInfo.Builder().apply {
            setId(SUB_ID_IN_SLOT_1)
            setMcc(MCC)
        }.build()

        val phoneNumber = context.phoneNumberFlow(subInfo).firstWithTimeoutOrNull()

        assertThat(phoneNumber).isEqualTo(NUMBER_1)
    }

    @Test
    fun phoneNumberFlow_withSubId() = runBlocking {
        val subInfo = SubscriptionInfo.Builder().apply {
            setId(SUB_ID_IN_SLOT_1)
            setMcc(MCC)
        }.build()
        mockSubscriptionManager.stub {
            on { getActiveSubscriptionInfo(SUB_ID_IN_SLOT_1) } doReturn subInfo
            on { getPhoneNumber(SUB_ID_IN_SLOT_1) } doReturn NUMBER_1
        }

        val phoneNumber = repository.phoneNumberFlow(SUB_ID_IN_SLOT_1).firstWithTimeoutOrNull()

        assertThat(phoneNumber).isEqualTo(NUMBER_1)
    }

    private companion object {
        const val SIM_SLOT_INDEX_0 = 0
        const val SUB_ID_IN_SLOT_0 = 2
        const val SIM_SLOT_INDEX_1 = 1
        const val SUB_ID_IN_SLOT_1 = 1
        const val SUB_ID_3_NOT_IN_SLOT = 3
        const val SUB_ID_4_NOT_IN_SLOT = 4
        val GROUP_UUID = UUID.randomUUID().toString()
        const val NUMBER_1 = "000000001"
        const val MCC = "310"
    }
}
