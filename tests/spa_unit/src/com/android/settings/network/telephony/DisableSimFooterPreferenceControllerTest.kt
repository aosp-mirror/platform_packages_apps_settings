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

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class DisableSimFooterPreferenceControllerTest {

    private val subscriptionInfo = mock<SubscriptionInfo> {
        on { subscriptionId } doReturn SUB_ID
    }

    private var context: Context = ApplicationProvider.getApplicationContext()

    private val mockSubscriptionRepository = mock<SubscriptionRepository> {
        on { getSelectableSubscriptionInfoList() } doReturn listOf(subscriptionInfo)
    }

    private var controller = DisableSimFooterPreferenceController(
        context = context,
        preferenceKey = PREFERENCE_KEY,
        subscriptionRepository = mockSubscriptionRepository,
    ).apply { init(SUB_ID) }

    @Test
    fun getAvailabilityStatus_invalidId_notAvailable() {
        val availabilityStatus = controller.getAvailabilityStatus(INVALID_SUBSCRIPTION_ID)

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_eSim_notAvailable() {
        subscriptionInfo.stub {
            on { isEmbedded } doReturn true
        }

        val availabilityStatus = controller.getAvailabilityStatus(SUB_ID)

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_pSimAndCannotDisable_available() {
        mockSubscriptionRepository.stub {
            on { canDisablePhysicalSubscription() } doReturn false
        }
        subscriptionInfo.stub {
            on { isEmbedded } doReturn false
        }

        val availabilityStatus = controller.getAvailabilityStatus(SUB_ID)

        assertThat(availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_pSimAndCanDisable_notAvailable() {
        mockSubscriptionRepository.stub {
            on { canDisablePhysicalSubscription() } doReturn true
        }
        subscriptionInfo.stub {
            on { isEmbedded } doReturn false
        }

        val availabilityStatus = controller.getAvailabilityStatus(SUB_ID)

        assertThat(availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    private companion object {
        const val PREFERENCE_KEY = "preference_key"
        const val SUB_ID = 111
    }
}
