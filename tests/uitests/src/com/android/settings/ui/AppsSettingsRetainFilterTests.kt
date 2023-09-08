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

package com.android.settings.ui

import android.os.RemoteException
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.android.settings.ui.testutils.SettingsTestUtils.clickObject
import com.android.settings.ui.testutils.SettingsTestUtils.startMainActivityFromHomeScreen
import com.android.settings.ui.testutils.SettingsTestUtils.waitObject
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppsSettingsRetainFilterTests {
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = startMainActivityFromHomeScreen(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
    }

    @Test
    fun testDisablingSystemAppAndRotateDevice() {
        device.clickObject(By.text("Calculator"))
        device.clickObject(By.text("Disable"))

        // Click on "Disable App" on dialog.
        device.clickObject(By.text("Disable app"))
        assertThat(device.waitObject(By.text("Enable"))).isNotNull()
        device.pressBack()
        device.clickObject(By.text("All apps"))
        device.clickObject(By.text("Disabled apps"))
        try {
            device.setOrientationLeft()
        } catch (e: RemoteException) {
            throw RuntimeException("Failed to freeze device orientation", e)
        }
        try {
            device.unfreezeRotation()
        } catch (e: RemoteException) {
            throw RuntimeException("Failed to un-freeze device orientation", e)
        }
        assertThat(device.waitObject(By.text("Disabled apps"))).isNotNull()
        device.clickObject(By.text("Calculator"))
        device.clickObject(By.text("Enable"))
    }
}
