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

package com.android.settings.ui.privatespace


import android.os.Flags
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.Settings
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.android.settings.ui.testutils.SettingsTestUtils.assertHasTexts
import com.android.settings.ui.testutils.SettingsTestUtils.clickObject
import com.android.settings.ui.testutils.SettingsTestUtils.startMainActivityFromHomeScreen
import com.android.settings.ui.testutils.SettingsTestUtils.waitObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@RequiresFlagsEnabled(Flags.FLAG_ALLOW_PRIVATE_PROFILE)
class PrivateSpaceAuthenticationActivityTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    public val mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Before
    fun setUp() {
        device.startMainActivityFromHomeScreen(Settings.ACTION_SECURITY_SETTINGS)
        device.assertHasTexts(listOf(PRIVATE_SPACE_SETTING))
    }

    @Test
    fun showAuthenticationScreen() {
        Thread.sleep(1000)
        device.clickObject(By.text(PRIVATE_SPACE_SETTING))
        device.waitObject(By.text(DIALOG_TITLE))
        Thread.sleep(1000)
        device.assertHasTexts(listOf("Set a screen lock","Cancel"))
    }

    @Test
    fun onCancelLockExitSetup() {
        Thread.sleep(1000)
        device.clickObject(By.text(PRIVATE_SPACE_SETTING))
        device.waitObject(By.text(DIALOG_TITLE))
        Thread.sleep(1000)
        device.assertHasTexts(listOf(SET_LOCK_BUTTON, CANCEL_TEXT))
        device.clickObject(By.text(CANCEL_TEXT))
        device.assertHasTexts(listOf(PRIVATE_SPACE_SETTING))
    }

    @Test
    fun onSetupSetLock() {
        Thread.sleep(1000)
        device.clickObject(By.text(PRIVATE_SPACE_SETTING))
        device.waitObject(By.text(DIALOG_TITLE))
        Thread.sleep(1000)
        device.assertHasTexts(listOf(SET_LOCK_BUTTON,CANCEL_TEXT))
        device.clickObject(By.text(SET_LOCK_BUTTON))
        Thread.sleep(1000)
        device.assertHasTexts(listOf(PATTERN_TEXT, PIN_TEXT, PASSWORD_TEXT))
    }

    private companion object {
        // Items we really want to always show
        val PRIVATE_SPACE_SETTING = "Private space"
        const val SET_LOCK_BUTTON = "Set screen lock"
        val CANCEL_TEXT = "Cancel"
        val DIALOG_TITLE = "Set a screen lock"
        val PATTERN_TEXT = "Pattern"
        val PIN_TEXT = "PIN"
        val PASSWORD_TEXT = "Password"
    }
}
