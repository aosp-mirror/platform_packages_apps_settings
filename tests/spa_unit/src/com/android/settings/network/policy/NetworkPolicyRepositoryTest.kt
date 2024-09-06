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

package com.android.settings.network.policy

import android.content.Context
import android.net.NetworkPolicy
import android.net.NetworkPolicyManager
import android.net.NetworkTemplate
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy

@RunWith(AndroidJUnit4::class)
class NetworkPolicyRepositoryTest {

    private val mockNetworkPolicyManager = mock<NetworkPolicyManager> {
        on { networkPolicies } doReturn arrayOf(Policy1, Policy2)
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(NetworkPolicyManager::class.java) } doReturn mockNetworkPolicyManager
    }

    private val repository = NetworkPolicyRepository(context)

    @Test
    fun getNetworkPolicy() {
        val networkPolicy = repository.getNetworkPolicy(Template1)

        assertThat(networkPolicy).isSameInstanceAs(Policy1)
    }

    @Test
    fun networkPolicyFlow() = runBlocking {
        val networkPolicy = repository.networkPolicyFlow(Template2).firstWithTimeoutOrNull()

        assertThat(networkPolicy).isSameInstanceAs(Policy2)
    }

    private companion object {
        val Template1: NetworkTemplate =
            NetworkTemplate.Builder(NetworkTemplate.MATCH_MOBILE).build()
        val Template2: NetworkTemplate = NetworkTemplate.Builder(NetworkTemplate.MATCH_WIFI).build()
        val Policy1 = mock<NetworkPolicy>().apply {
            template = Template1
        }
        val Policy2 = mock<NetworkPolicy>().apply {
            template = Template2
        }
    }
}
