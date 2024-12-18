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

package com.android.settings.datausage

import android.content.Context
import android.util.Range
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.datausage.lib.NetworkUsageDetailsData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDataUsageSummaryControllerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val controller = AppDataUsageSummaryController(context, TEST_KEY)

    @Test
    fun summary() {
        val appUsage = NetworkUsageDetailsData(
            range = Range(1L, 2L),
            totalUsage = BACKGROUND_BYTES + FOREGROUND_BYTES,
            foregroundUsage = FOREGROUND_BYTES,
            backgroundUsage = BACKGROUND_BYTES,
        )

        controller.update(appUsage)
        composeTestRule.setContent {
            controller.Content()
        }

        composeTestRule.onNode(hasTextExactly("Total", "6.75 kB")).assertIsDisplayed()
        composeTestRule.onNode(hasTextExactly("Foreground", "5.54 kB")).assertIsDisplayed()
        composeTestRule.onNode(hasTextExactly("Background", "1.21 kB")).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("6.75 kB").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("5.54 kB").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("1.21 kB").assertIsDisplayed()
    }

    @Test
    fun summary_zero() {
        val appUsage = NetworkUsageDetailsData(
            range = Range(1L, 2L),
            totalUsage = 3,
            foregroundUsage = 1,
            backgroundUsage = 2,
        )

        controller.update(appUsage)
        composeTestRule.setContent {
            controller.Content()
        }

        composeTestRule.onNode(hasTextExactly("Total", "3 B")).assertIsDisplayed()
        composeTestRule.onNode(hasTextExactly("Foreground", "1 B")).assertIsDisplayed()
        composeTestRule.onNode(hasTextExactly("Background", "2 B")).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("3 byte").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("1 byte").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("2 byte").assertIsDisplayed()
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val BACKGROUND_BYTES = 1234L
        const val FOREGROUND_BYTES = 5678L
    }
}
