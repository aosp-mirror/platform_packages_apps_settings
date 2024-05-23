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

import android.telephony.ims.ProvisioningManager
import android.telephony.ims.ProvisioningManager.FeatureProvisioningCallback
import android.telephony.ims.feature.MmTelFeature
import android.telephony.ims.stub.ImsRegistrationImplBase
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.toListWithTimeout
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class ImsFeatureProvisionedFlowTest {

    private var callback: FeatureProvisioningCallback? = null

    private val mockProvisioningManager = mock<ProvisioningManager> {
        on { registerFeatureProvisioningChangedCallback(any(), any()) } doAnswer {
            callback = it.arguments[1] as FeatureProvisioningCallback
            callback?.onFeatureProvisioningChanged(CAPABILITY, TECH, true)
        }
    }

    @Test
    fun imsFeatureProvisionedFlow_sendInitialValue() = runBlocking {
        val flow = imsFeatureProvisionedFlow(SUB_ID, CAPABILITY, TECH, mockProvisioningManager)

        val state = flow.first()

        assertThat(state).isTrue()
    }

    @Test
    fun imsFeatureProvisionedFlow_changed(): Unit = runBlocking {
        val listDeferred = async {
            imsFeatureProvisionedFlow(SUB_ID, CAPABILITY, TECH, mockProvisioningManager)
                .toListWithTimeout()
        }
        delay(100)

        callback?.onFeatureProvisioningChanged(CAPABILITY, TECH, false)

        assertThat(listDeferred.await().last()).isFalse()
    }

    private companion object {
        const val SUB_ID = 1
        const val CAPABILITY = MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE
        const val TECH = ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN
    }
}
