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
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.android.settings.ui.testutils.SettingsTestUtils.assertHasTexts
import com.android.settings.ui.testutils.SettingsTestUtils.startMainActivityFromHomeScreen
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies basic functionality of the About Phone screen  */
@RunWith(AndroidJUnit4::class)
@SmallTest
class AboutPhoneSettingsTests {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUp() {
        device.startMainActivityFromHomeScreen(Settings.ACTION_DEVICE_INFO_SETTINGS)
    }

    @Test
    fun testAllMenuEntriesExist() {
        device.assertHasTexts(ON_SCREEN_TEXTS)
    }

    private companion object {
        val ON_SCREEN_TEXTS = listOf(
            "Device name",
            "Legal information",
            "Regulatory labels"
        )
    }
}
