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
import android.telephony.AccessNetworkConstants
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.ims.feature.MmTelFeature
import android.telephony.ims.stub.ImsRegistrationImplBase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.network.telephony.ims.ImsFeatureRepository
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class VideoCallingRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockMobileDataRepository = mock<MobileDataRepository>()
    private val mockImsFeatureRepository = mock<ImsFeatureRepository>()

    private val repository =
        VideoCallingRepository(
            context = context,
            mobileDataRepository = mockMobileDataRepository,
            imsFeatureRepositoryFactory = { mockImsFeatureRepository },
        )

    @Before
    fun setUp() {
        CarrierConfigRepository.resetForTest()
    }

    @Test
    fun isVideoCallReadyFlow_invalidSubId() = runBlocking {
        val isVideoCallReady =
            repository
                .isVideoCallReadyFlow(subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                .firstWithTimeoutOrNull()

        assertThat(isVideoCallReady).isFalse()
    }

    @Test
    fun isVideoCallReadyFlow_ignoreDataEnabledChangedAndIsReady_returnTrue() = runBlocking {
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS,
            value = true,
        )
        mockImsFeatureRepository.stub {
            on {
                isReadyFlow(
                    capability = MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO,
                    tech = ImsRegistrationImplBase.REGISTRATION_TECH_LTE,
                    transportType = AccessNetworkConstants.TRANSPORT_TYPE_WWAN,
                )
            } doReturn flowOf(true)
        }

        val isVideoCallReady = repository.isVideoCallReadyFlow(SUB_ID).firstWithTimeoutOrNull()

        assertThat(isVideoCallReady).isTrue()
    }

    @Test
    fun isVideoCallReadyFlow_ignoreDataEnabledChangedAndNotReady_returnFalse() = runBlocking {
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS,
            value = true,
        )
        mockImsFeatureRepository.stub {
            on {
                isReadyFlow(
                    capability = MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO,
                    tech = ImsRegistrationImplBase.REGISTRATION_TECH_LTE,
                    transportType = AccessNetworkConstants.TRANSPORT_TYPE_WWAN,
                )
            } doReturn flowOf(false)
        }

        val isVideoCallReady = repository.isVideoCallReadyFlow(SUB_ID).firstWithTimeoutOrNull()

        assertThat(isVideoCallReady).isFalse()
    }

    @Test
    fun isVideoCallReadyFlow_mobileDataEnabledAndIsReady_returnTrue() = runBlocking {
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS,
            value = false,
        )
        mockMobileDataRepository.stub {
            on { isMobileDataEnabledFlow(SUB_ID) } doReturn flowOf(true)
        }
        mockImsFeatureRepository.stub {
            on {
                isReadyFlow(
                    capability = MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO,
                    tech = ImsRegistrationImplBase.REGISTRATION_TECH_LTE,
                    transportType = AccessNetworkConstants.TRANSPORT_TYPE_WWAN,
                )
            } doReturn flowOf(true)
        }

        val isVideoCallReady = repository.isVideoCallReadyFlow(SUB_ID).firstWithTimeoutOrNull()

        assertThat(isVideoCallReady).isTrue()
    }

    @Test
    fun isVideoCallReadyFlow_ignoreDataEnabledChangedAndIsReady_returnFalse() = runBlocking {
        CarrierConfigRepository.setBooleanForTest(
            subId = SUB_ID,
            key = CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS,
            value = false,
        )
        mockMobileDataRepository.stub {
            on { isMobileDataEnabledFlow(SUB_ID) } doReturn flowOf(false)
        }
        mockImsFeatureRepository.stub {
            on {
                isReadyFlow(
                    capability = MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO,
                    tech = ImsRegistrationImplBase.REGISTRATION_TECH_LTE,
                    transportType = AccessNetworkConstants.TRANSPORT_TYPE_WWAN,
                )
            } doReturn flowOf(true)
        }

        val isVideoCallReady = repository.isVideoCallReadyFlow(SUB_ID).firstWithTimeoutOrNull()

        assertThat(isVideoCallReady).isFalse()
    }

    private companion object {
        const val SUB_ID = 10
    }
}
