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
package com.android.settings.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothAdapter

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowAlertDialogCompat::class, ShadowBluetoothAdapter::class])
class RequestPermissionActivityTest {
    private lateinit var activityController: ActivityController<RequestPermissionActivity>
    private lateinit var bluetoothAdapter: ShadowBluetoothAdapter

    @Before
    fun setUp() {
        bluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter())
    }

    @After
    fun tearDown() {
        activityController.pause().stop().destroy()
        ShadowAlertDialogCompat.reset()
    }

    @Test
    fun requestEnable_whenBluetoothIsOff_showConfirmDialog() {
        bluetoothAdapter.setState(BluetoothAdapter.STATE_OFF)

        createActivity(action = BluetoothAdapter.ACTION_REQUEST_ENABLE)

        val dialog = ShadowAlertDialogCompat.getLatestAlertDialog()
        val shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog)
        assertThat(shadowDialog.message.toString())
            .isEqualTo("An app wants to turn on Bluetooth")
    }

    @Test
    fun requestEnable_whenBluetoothIsOn_doNothing() {
        bluetoothAdapter.setState(BluetoothAdapter.STATE_ON)

        createActivity(action = BluetoothAdapter.ACTION_REQUEST_ENABLE)

        val dialog = ShadowAlertDialogCompat.getLatestAlertDialog()
        assertThat(dialog).isNull()
    }

    @Test
    fun requestDisable_whenBluetoothIsOff_doNothing() {
        bluetoothAdapter.setState(BluetoothAdapter.STATE_OFF)

        createActivity(action = BluetoothAdapter.ACTION_REQUEST_DISABLE)

        val dialog = ShadowAlertDialogCompat.getLatestAlertDialog()
        assertThat(dialog).isNull()
    }

    @Test
    fun requestDisable_whenBluetoothIsOn_showConfirmDialog() {
        bluetoothAdapter.setState(BluetoothAdapter.STATE_ON)

        createActivity(action = BluetoothAdapter.ACTION_REQUEST_DISABLE)

        val dialog = ShadowAlertDialogCompat.getLatestAlertDialog()
        val shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog)
        assertThat(shadowDialog.message.toString())
            .isEqualTo("An app wants to turn off Bluetooth")
    }

    private fun createActivity(action: String) {
        activityController =
            ActivityController.of(RequestPermissionActivity(), Intent(action)).apply {
                create()
                start()
                postCreate(null)
                resume()
            }
    }
}