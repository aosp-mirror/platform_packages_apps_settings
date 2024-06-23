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

import android.os.Flags
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.android.settings.ui.testutils.SettingsTestUtils.assertHasTexts
import com.android.settings.ui.testutils.SettingsTestUtils.clickObject
import com.android.settings.ui.testutils.SettingsTestUtils.startMainActivityFromHomeScreen
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MemorySettingsTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUp() {
        device.startMainActivityFromHomeScreen(Settings.ACTION_DEVICE_INFO_SETTINGS)
        device.assertHasTexts(listOf(BUILD_NUMBER))
        repeat(7) {  // Enable development mode
            device.clickObject(By.text(BUILD_NUMBER))
        }
        device.startMainActivityFromHomeScreen(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        device.clickObject(By.text(MEMORY_PAGE))
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_REMOVE_APP_PROFILER_PSS_COLLECTION)
    fun memoryPageIfPssFlagDisabled() {
        device.assertHasTexts(ON_SCREEN_TEXTS_DEFAULT)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_REMOVE_APP_PROFILER_PSS_COLLECTION)
    fun memoryPageIfPssFlagEnabled() {
        device.assertHasTexts(ON_SCREEN_TEXTS_PSS_PROFILING_DISABLED)
    }

    private companion object {
        private const val BUILD_NUMBER = "Build number"
        private const val MEMORY_PAGE = "Memory"
        val ON_SCREEN_TEXTS_DEFAULT = listOf(
            "Performance",
            "Total memory",
            "Average used (%)",
            "Free",
        )
        val ON_SCREEN_TEXTS_PSS_PROFILING_DISABLED = listOf(
            "Enable memory usage profiling",
        )
    }
}
