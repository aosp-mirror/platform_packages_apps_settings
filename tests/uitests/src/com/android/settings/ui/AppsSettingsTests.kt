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
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.android.settings.ui.testutils.SettingsTestUtils.assertHasTexts
import com.android.settings.ui.testutils.SettingsTestUtils.clickObject
import com.android.settings.ui.testutils.SettingsTestUtils.startMainActivityFromHomeScreen
import com.android.settings.ui.testutils.SettingsTestUtils.waitObject
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Test

/** Verifies basic functionality of the About Phone screen  */
class AppsSettingsTests {
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = startMainActivityFromHomeScreen(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
        val title = device.waitObject(By.text("All apps"))
        assertWithMessage("Could not find Settings > Apps screen").that(title).isNotNull()
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
        val disableButton = device.waitObject(By.text("Disable"))
        assertWithMessage("App not enabled successfully").that(disableButton).isNotNull()
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
