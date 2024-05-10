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

package com.android.settings.network

import android.content.Context
import android.provider.Settings
import android.telephony.SubscriptionManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.android.settingslib.spa.testutils.toListWithTimeout
import com.android.settingslib.spaprivileged.settingsprovider.settingsGlobalBoolean
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MobileDataEnabledFlowTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun mobileDataEnabledFlow_notified(): Unit = runBlocking {
        val flow = context.mobileDataEnabledFlow(SubscriptionManager.INVALID_SUBSCRIPTION_ID)

        assertThat(flow.firstWithTimeoutOrNull()).isNotNull()
    }

    @Test
    fun mobileDataEnabledFlow_changed_notified(): Unit = runBlocking {
        var mobileDataEnabled by context.settingsGlobalBoolean(Settings.Global.MOBILE_DATA)
        mobileDataEnabled = false

        val flow = context.mobileDataEnabledFlow(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        mobileDataEnabled = true

        assertThat(flow.firstWithTimeoutOrNull()).isNotNull()
    }

    @Test
    fun mobileDataEnabledFlow_forSubIdNotChanged(): Unit = runBlocking {
        var mobileDataEnabled by context.settingsGlobalBoolean(Settings.Global.MOBILE_DATA)
        mobileDataEnabled = false
        var mobileDataEnabledForSubId
            by context.settingsGlobalBoolean(Settings.Global.MOBILE_DATA + SUB_ID)
        mobileDataEnabledForSubId = false

        val listDeferred = async {
            context.mobileDataEnabledFlow(SUB_ID).toListWithTimeout()
        }

        assertThat(listDeferred.await()).hasSize(1)
    }

    @Test
    fun mobileDataEnabledFlow_forSubIdChanged(): Unit = runBlocking {
        var mobileDataEnabled by context.settingsGlobalBoolean(Settings.Global.MOBILE_DATA)
        mobileDataEnabled = false
        var mobileDataEnabledForSubId
            by context.settingsGlobalBoolean(Settings.Global.MOBILE_DATA + SUB_ID)
        mobileDataEnabledForSubId = false

        val listDeferred = async {
            context.mobileDataEnabledFlow(SUB_ID).toListWithTimeout()
        }
        delay(100)
        mobileDataEnabledForSubId = true

        assertThat(listDeferred.await()).hasSize(2)
    }

    private companion object {
        const val SUB_ID = 123
    }
}
