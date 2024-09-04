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
import android.content.pm.PackageManager
import android.telephony.ims.ProvisioningManager
import android.telephony.ims.ProvisioningManager.FeatureProvisioningCallback
import android.telephony.ims.feature.MmTelFeature
import android.telephony.ims.stub.ImsRegistrationImplBase
import androidx.test.core.app.ApplicationProvider
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class ProvisioningRepositoryTest {

    private var callback: FeatureProvisioningCallback? = null

    private val mockProvisioningManager =
        mock<ProvisioningManager> {
            on { registerFeatureProvisioningChangedCallback(any(), any()) } doAnswer
                {
                    callback = it.arguments[1] as FeatureProvisioningCallback
                    callback?.onFeatureProvisioningChanged(CAPABILITY, TECH, true)
                }
        }

    private val mockPackageManager =
        mock<PackageManager> {
            on { hasSystemFeature(PackageManager.FEATURE_TELEPHONY_IMS) } doReturn true
        }

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { packageManager } doReturn mockPackageManager
        }

    private val repository = ProvisioningRepository(context) { mockProvisioningManager }

    @Test
    fun imsFeatureProvisionedFlow_hasNotIms_returnFalse() = runBlocking {
        mockPackageManager.stub {
            on { hasSystemFeature(PackageManager.FEATURE_TELEPHONY_IMS) } doReturn false
        }
        val flow = repository.imsFeatureProvisionedFlow(SUB_ID, CAPABILITY, TECH)

        val state = flow.first()

        assertThat(state).isFalse()
    }

    @Test
    fun imsFeatureProvisionedFlow_sendInitialValue() = runBlocking {
        val flow = repository.imsFeatureProvisionedFlow(SUB_ID, CAPABILITY, TECH)

        val state = flow.first()

        assertThat(state).isTrue()
    }

    @Test
    fun imsFeatureProvisionedFlow_changed(): Unit = runBlocking {
        val listDeferred = async {
            repository.imsFeatureProvisionedFlow(SUB_ID, CAPABILITY, TECH).toListWithTimeout()
        }
        delay(100)

        callback?.onFeatureProvisioningChanged(CAPABILITY, TECH, false)

        assertThat(listDeferred.await().last()).isFalse()
    }

    private companion object {
        const val SUB_ID = 10
        const val CAPABILITY = MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE
        const val TECH = ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN
    }
}
