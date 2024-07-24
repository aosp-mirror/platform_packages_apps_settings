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

package com.android.settings.system

import android.content.Context
import android.content.Intent
import android.telephony.CarrierConfigManager
import androidx.core.os.persistableBundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ClientInitiatedActionRepositoryTest {
    private val mockCarrierConfigManager = mock<CarrierConfigManager>()

    private val context = mock<Context> {
        on { applicationContext } doReturn mock
        on { getSystemService(CarrierConfigManager::class.java) } doReturn mockCarrierConfigManager
    }

    private val repository = ClientInitiatedActionRepository(context)

    @Test
    fun onSystemUpdate_notEnabled() {
        mockCarrierConfigManager.stub {
            on { getConfigForSubId(any(), any()) } doReturn persistableBundleOf()
        }

        repository.onSystemUpdate()

        verify(context, never()).sendBroadcast(any())
    }

    @Test
    fun onSystemUpdate_enabled() {
        mockCarrierConfigManager.stub {
            on { getConfigForSubId(any(), any()) } doReturn persistableBundleOf(
                CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_BOOL to true,
                CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_INTENT_STRING to ACTION,
            )
        }

        repository.onSystemUpdate()

        val intent = argumentCaptor<Intent> {
            verify(context).sendBroadcast(capture())
        }.firstValue
        assertThat(intent.action).isEqualTo(ACTION)
    }

    private companion object {
        const val ACTION = "ACTION"
    }
}
