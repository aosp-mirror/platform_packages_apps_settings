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

package com.android.settings.network.telephony.ims

import android.content.Context
import android.telephony.AccessNetworkConstants
import android.telephony.ims.feature.MmTelFeature
import android.telephony.ims.stub.ImsRegistrationImplBase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class ImsFeatureRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockProvisioningRepository = mock<ProvisioningRepository>()
    private val mockImsMmTelRepository = mock<ImsMmTelRepository>()

    @Test
    fun isReadyFlow_notProvisioned_returnFalse() = runBlocking {
        mockProvisioningRepository.stub {
            onBlocking { imsFeatureProvisionedFlow(SUB_ID, CAPABILITY, TECH) } doReturn
                flowOf(false)
        }

        val repository =
            ImsFeatureRepository(
                context = context,
                subId = SUB_ID,
                provisioningRepository = mockProvisioningRepository,
            )

        val isReady = repository.isReadyFlow(CAPABILITY, TECH, TRANSPORT_TYPE).first()

        assertThat(isReady).isFalse()
    }

    @Test
    fun isReadyFlow_notSupported_returnFalse() = runBlocking {
        mockImsMmTelRepository.stub {
            onBlocking { isSupportedFlow(CAPABILITY, TRANSPORT_TYPE) } doReturn flowOf(false)
        }

        val repository =
            ImsFeatureRepository(
                context = context,
                subId = SUB_ID,
                imsMmTelRepository = mockImsMmTelRepository,
            )

        val isReady = repository.isReadyFlow(CAPABILITY, TECH, TRANSPORT_TYPE).first()

        assertThat(isReady).isFalse()
    }

    @Test
    fun isReadyFlow_provisionedAndSupported_returnFalse() = runBlocking {
        mockProvisioningRepository.stub {
            onBlocking { imsFeatureProvisionedFlow(SUB_ID, CAPABILITY, TECH) } doReturn flowOf(true)
        }
        mockImsMmTelRepository.stub {
            onBlocking { isSupportedFlow(CAPABILITY, TRANSPORT_TYPE) } doReturn flowOf(true)
        }

        val repository =
            ImsFeatureRepository(
                context = context,
                subId = SUB_ID,
                provisioningRepository = mockProvisioningRepository,
                imsMmTelRepository = mockImsMmTelRepository,
            )

        val isReady = repository.isReadyFlow(CAPABILITY, TECH, TRANSPORT_TYPE).first()

        assertThat(isReady).isTrue()
    }

    private companion object {
        const val SUB_ID = 10
        const val CAPABILITY = MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE
        const val TECH = ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN
        const val TRANSPORT_TYPE = AccessNetworkConstants.TRANSPORT_TYPE_WLAN
    }
}
