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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.network.telephony.SubscriptionRepository
import com.android.settings.network.telephony.euicc.EuiccRepository
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class MobileNetworkSummaryRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockSubscriptionRepository = mock<SubscriptionRepository>()
    private val mockEuiccRepository = mock<EuiccRepository>()

    private val repository =
        MobileNetworkSummaryRepository(
            context = context,
            subscriptionRepository = mockSubscriptionRepository,
            euiccRepository = mockEuiccRepository,
            getDisplayName = { it.displayName.toString() },
        )

    @Test
    fun subscriptionsStateFlow_noSubscriptionsAndShowEuicc_returnsAddNetwork() = runBlocking {
        mockSubscriptionRepository.stub {
            on { selectableSubscriptionInfoListFlow() } doReturn flowOf(emptyList())
        }
        mockEuiccRepository.stub { on { showEuiccSettings() } doReturn true }

        val state = repository.subscriptionsStateFlow().firstWithTimeoutOrNull()

        assertThat(state).isEqualTo(MobileNetworkSummaryRepository.AddNetwork)
    }

    @Test
    fun subscriptionsStateFlow_noSubscriptionsAndHideEuicc_returnsNoSubscriptions() = runBlocking {
        mockSubscriptionRepository.stub {
            on { selectableSubscriptionInfoListFlow() } doReturn flowOf(emptyList())
        }
        mockEuiccRepository.stub { on { showEuiccSettings() } doReturn false }

        val state = repository.subscriptionsStateFlow().firstWithTimeoutOrNull()

        assertThat(state).isEqualTo(MobileNetworkSummaryRepository.NoSubscriptions)
    }

    @Test
    fun subscriptionsStateFlow_hasSubscriptions_returnsHasSubscriptions() = runBlocking {
        mockSubscriptionRepository.stub {
            on { selectableSubscriptionInfoListFlow() } doReturn
                flowOf(
                    listOf(
                        SubscriptionInfo.Builder().setDisplayName(DISPLAY_NAME_1).build(),
                        SubscriptionInfo.Builder().setDisplayName(DISPLAY_NAME_2).build(),
                    )
                )
        }

        val state = repository.subscriptionsStateFlow().firstWithTimeoutOrNull()

        assertThat(state)
            .isEqualTo(
                MobileNetworkSummaryRepository.HasSubscriptions(
                    listOf(DISPLAY_NAME_1, DISPLAY_NAME_2)
                )
            )
    }

    private companion object {
        const val DISPLAY_NAME_1 = "Sub 1"
        const val DISPLAY_NAME_2 = "Sub 2"
    }
}
