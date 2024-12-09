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

package com.android.settings.connecteddevice

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class BluetoothMainSwitchPreferenceTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothMainSwitchPreference: BluetoothMainSwitchPreference

    @Before
    fun setUp() {
        bluetoothAdapter = spy(BluetoothAdapter.getDefaultAdapter())
        whenever(bluetoothAdapter.state).thenReturn(BluetoothAdapter.STATE_ON)
        bluetoothMainSwitchPreference = BluetoothMainSwitchPreference(bluetoothAdapter)
    }

    @Test
    fun isEnabled_bluetoothOn_returnTrue() {
        assertThat(bluetoothMainSwitchPreference.isEnabled(context)).isTrue()
    }

    @Test
    fun isEnabled_bluetoothTurningOn_returnFalse() {
        whenever(bluetoothAdapter.state).thenReturn(BluetoothAdapter.STATE_TURNING_ON)

        assertThat(bluetoothMainSwitchPreference.isEnabled(context)).isFalse()
    }

    @Test
    fun storageSetOff_turnOff() {
        bluetoothMainSwitchPreference
            .storage(context)
            .setBoolean(bluetoothMainSwitchPreference.key, false)

        verify(bluetoothAdapter).disable()
    }

    @Test
    fun storageSetOn_turnOn() {
        bluetoothMainSwitchPreference
            .storage(context)
            .setBoolean(bluetoothMainSwitchPreference.key, true)

        verify(bluetoothAdapter).enable()
    }
}
