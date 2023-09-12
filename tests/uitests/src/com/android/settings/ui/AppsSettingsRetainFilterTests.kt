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

import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.android.settings.ui.testutils.SettingsTestUtils.assertObject
import com.android.settings.ui.testutils.SettingsTestUtils.clickObject
import com.android.settings.ui.testutils.SettingsTestUtils.startMainActivityFromHomeScreen
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppsSettingsRetainFilterTests {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUp() {
        device.startMainActivityFromHomeScreen(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
    }

    @Test
    fun testDisablingSystemAppAndRotateDevice() {
        device.clickObject(By.text("Calculator"))
        device.clickObject(By.text("Disable"))
        device.clickObject(By.text("Disable app"))  // Click on "Disable App" on dialog.
        device.assertObject(By.text("Enable"))
        device.pressBack()
        device.clickObject(By.text("All apps"))
        device.clickObject(By.text("Disabled apps"))
        device.setOrientationLeft()
        device.assertObject(By.text("Disabled apps"))
        device.setOrientationNatural()
        device.assertObject(By.text("Disabled apps"))
        device.clickObject(By.text("Calculator"))
        device.clickObject(By.text("Enable"))
    }
}
