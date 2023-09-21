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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.android.settings.ui.testutils.SettingsTestUtils.assertHasTexts
import com.android.settings.ui.testutils.SettingsTestUtils.assertObject
import com.android.settings.ui.testutils.SettingsTestUtils.clickObject
import com.android.settings.ui.testutils.SettingsTestUtils.startMainActivityFromHomeScreen
import com.android.settings.ui.testutils.SettingsTestUtils.waitObject
import org.junit.Before
import org.junit.Test

/** Verifies basic functionality of the About Phone screen  */
class AppsSettingsTests {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUp() {
        device.startMainActivityFromHomeScreen(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
        device.assertObject(By.text("All apps"))
    }

    @Test
    fun testAppSettingsListForCalculator() {
        device.clickObject(By.text("Calculator"))
        device.waitObject(By.text("Open"))
        device.assertHasTexts(ON_SCREEN_TEXTS)
    }

    @Test
    fun testDisablingAndEnablingSystemApp() {
        device.clickObject(By.text("Calculator"))
        device.clickObject(By.text("Disable"))
        device.clickObject(By.text("Disable app"))  // Click on "Disable app" on dialog.
        device.clickObject(By.text("Enable"))
        device.assertObject(By.text("Disable"))
    }

    private companion object {
        val ON_SCREEN_TEXTS = listOf(
            "Notifications",
            "Permissions",
            "Storage & cache",
            "Mobile data & Wi‑Fi",
            "Screen time",
            "App battery usage",
            "Language",
            "Unused app settings",
        )
    }
}
