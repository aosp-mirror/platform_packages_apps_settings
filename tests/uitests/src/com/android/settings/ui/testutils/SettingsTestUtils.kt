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

package com.android.settings.ui.testutils

import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage

object SettingsTestUtils {
    const val SETTINGS_PACKAGE = "com.android.settings"
    const val TIMEOUT = 2000L

    fun UiDevice.waitObject(bySelector: BySelector): UiObject2? =
        wait(Until.findObject(bySelector), TIMEOUT)

    fun UiDevice.clickObject(bySelector: BySelector) = checkNotNull(waitObject(bySelector)).click()

    fun startMainActivityFromHomeScreen(action: String): UiDevice {
        // Initialize UiDevice instance
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressKeyCodes(intArrayOf(KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_MENU))  // unlock

        // Start from the home screen
        device.pressHome()

        // Wait for launcher
        val launcherPackage: String = device.launcherPackageName
        assertThat(launcherPackage).isNotNull()
        device.waitObject(By.pkg(launcherPackage).depth(0))

        // Launch the app
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        // Wait for the app to appear
        device.waitObject(By.pkg(SETTINGS_PACKAGE).depth(0))

        return device
    }

    fun UiDevice.assertHasTexts(texts: List<String>) {
        val scrollableObj = findObject(By.scrollable(true))
        for (text in texts) {
            val selector = By.text(text)
            assertWithMessage("Missing text: $text").that(
                findObject(selector)
                    ?: scrollableObj.scrollUntil(Direction.DOWN, Until.findObject(selector))
                    ?: waitObject(selector)
            ).isNotNull()
        }
    }
}
