/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.network.ethernet

import android.content.Context
import android.content.ContextWrapper
import android.net.EthernetManager
import android.net.EthernetManager.STATE_ABSENT
import android.net.EthernetManager.STATE_LINK_DOWN
import android.net.EthernetManager.STATE_LINK_UP
import android.net.IpConfiguration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class EthernetInterfaceTest {

    private val mockEthernetManager = mock<EthernetManager>()

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    Context.ETHERNET_SERVICE -> mockEthernetManager
                    else -> super.getSystemService(name)
                }
        }

    private val ethernetInterface = EthernetInterface(context, "eth0")

    @Test
    fun getInterfaceState_shouldReturnDefaultState() {
        assertEquals(ethernetInterface.getInterfaceState(), STATE_ABSENT)
    }

    @Test
    fun getConfiguration_shouldReturnDefaultIpConfig() {
        val ipConfiguration: IpConfiguration = ethernetInterface.getConfiguration()

        assertEquals(ipConfiguration.getIpAssignment(), IpConfiguration.IpAssignment.UNASSIGNED)
    }

    @Test
    fun interfaceStateChanged_shouldUpdateState() {
        val testConfig = IpConfiguration()
        testConfig.setIpAssignment(IpConfiguration.IpAssignment.STATIC)

        ethernetInterface.onInterfaceStateChanged("eth0", STATE_LINK_UP, 0, testConfig)

        assertEquals(ethernetInterface.getInterfaceState(), STATE_LINK_UP)
        assertEquals(
            ethernetInterface.getConfiguration().getIpAssignment(),
            IpConfiguration.IpAssignment.STATIC,
        )
    }

    @Test
    fun interfaceStateChanged_iddoesnotmatch_shouldNotUpdateState() {
        val testConfig = IpConfiguration()
        testConfig.setIpAssignment(IpConfiguration.IpAssignment.STATIC)

        ethernetInterface.onInterfaceStateChanged("eth1", STATE_LINK_DOWN, 0, testConfig)

        assertEquals(ethernetInterface.getInterfaceState(), STATE_ABSENT)
        assertEquals(
            ethernetInterface.getConfiguration().getIpAssignment(),
            IpConfiguration.IpAssignment.UNASSIGNED,
        )
    }
}
