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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothPan
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.net.TetheringInterface
import android.net.TetheringManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class TetheredRepositoryTest {

    private var tetheringInterfaces: Set<TetheringInterface> = emptySet()

    private var tetheringEventCallback: TetheringManager.TetheringEventCallback? = null

    private val mockTetheringManager = mock<TetheringManager> {
        on { registerTetheringEventCallback(any(), any()) } doAnswer {
            tetheringEventCallback = it.arguments[1] as TetheringManager.TetheringEventCallback
            tetheringEventCallback?.onTetheredInterfacesChanged(tetheringInterfaces)
        }
    }

    private val mockBluetoothPan = mock<BluetoothPan> {
        on { isTetheringOn } doReturn false
    }

    private val mockBluetoothAdapter = mock<BluetoothAdapter> {
        on { getProfileProxy(any(), any(), eq(BluetoothProfile.PAN)) } doAnswer {
            val listener = it.arguments[1] as BluetoothProfile.ServiceListener
            listener.onServiceConnected(BluetoothProfile.PAN, mockBluetoothPan)
            true
        }
    }

    private val mockBluetoothManager = mock<BluetoothManager> {
        on { adapter } doReturn mockBluetoothAdapter
    }

    private val context = mock<Context> {
        on { getSystemService(TetheringManager::class.java) } doReturn mockTetheringManager
        on { getSystemService(BluetoothManager::class.java) } doReturn mockBluetoothManager
    }

    private val repository = TetheredRepository(context)

    @Test
    fun tetheredTypesFlow_allOff() = runBlocking {
        val tetheredTypes = repository.tetheredTypesFlow().firstWithTimeoutOrNull()

        assertThat(tetheredTypes).isEmpty()
    }

    @Test
    fun tetheredTypesFlow_wifiHotspotOn(): Unit = runBlocking {
        tetheringInterfaces = setOf(TetheringInterface(TetheringManager.TETHERING_WIFI, ""))

        val tetheredTypes = repository.tetheredTypesFlow().firstWithTimeoutOrNull()

        assertThat(tetheredTypes).containsExactly(TetheringManager.TETHERING_WIFI)
    }

    @Test
    fun tetheredTypesFlow_usbTetheringTurnOnLater(): Unit = runBlocking {
        val tetheredTypeDeferred = async {
            repository.tetheredTypesFlow().mapNotNull {
                it.singleOrNull()
            }.firstWithTimeoutOrNull()
        }
        delay(100)

        tetheringEventCallback?.onTetheredInterfacesChanged(
            setOf(TetheringInterface(TetheringManager.TETHERING_USB, ""))
        )

        assertThat(tetheredTypeDeferred.await()).isEqualTo(TetheringManager.TETHERING_USB)
    }

    @Test
    fun tetheredTypesFlow_bluetoothOff(): Unit = runBlocking {
        mockBluetoothAdapter.stub {
            on { state } doReturn BluetoothAdapter.STATE_OFF
        }

        val tetheredTypes = repository.tetheredTypesFlow().firstWithTimeoutOrNull()

        assertThat(tetheredTypes).isEmpty()
    }

    @Test
    fun tetheredTypesFlow_bluetoothOnTetheringOff(): Unit = runBlocking {
        mockBluetoothAdapter.stub {
            on { state } doReturn BluetoothAdapter.STATE_ON
        }

        val tetheredTypes = repository.tetheredTypesFlow().firstWithTimeoutOrNull()

        assertThat(tetheredTypes).isEmpty()
    }

    @Test
    fun tetheredTypesFlow_bluetoothTetheringOn(): Unit = runBlocking {
        mockBluetoothAdapter.stub {
            on { state } doReturn BluetoothAdapter.STATE_ON
        }
        mockBluetoothPan.stub {
            on { isTetheringOn } doReturn true
        }

        val tetheredTypes = repository.tetheredTypesFlow().firstWithTimeoutOrNull()

        assertThat(tetheredTypes).containsExactly(TetheringManager.TETHERING_BLUETOOTH)
    }
}
