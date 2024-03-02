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
import android.telephony.SubscriptionManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.android.settingslib.spa.testutils.toListWithTimeout
import com.google.common.truth.Truth.assertThat
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
        on { getSystemService(SubscriptionManager::class.java) } doReturn mockSubscriptionManager
    }

    @Test
    fun isSubscriptionEnabledFlow() = runBlocking {
        mockSubscriptionManager.stub {
            on { isSubscriptionEnabled(SUB_ID) } doReturn true
        }

        val isEnabled = context.isSubscriptionEnabledFlow(SUB_ID).firstWithTimeoutOrNull()

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun subscriptionsChangedFlow_hasInitialValue() = runBlocking {
        val initialValue = context.subscriptionsChangedFlow().firstWithTimeoutOrNull()

        assertThat(initialValue).isSameInstanceAs(Unit)
    }

    @Test
    fun subscriptionsChangedFlow_changed() = runBlocking {
        val listDeferred = async {
            context.subscriptionsChangedFlow().toListWithTimeout()
        }
        delay(100)

        subInfoListener?.onSubscriptionsChanged()

        assertThat(listDeferred.await()).hasSize(2)
    }

    private companion object {
        const val SUB_ID = 1
    }
}
