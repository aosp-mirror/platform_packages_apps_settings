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

package com.android.settings.spa.preference

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposePreferenceControllerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val controller = object : ComposePreferenceController(
        context = context,
        preferenceKey = TEST_KEY,
    ) {
        override fun getAvailabilityStatus() = AVAILABLE

        @Composable
        override fun Content() {
            Text(TEXT)
        }
    }

    private val preference = ComposePreference(context).apply {
        key = TEST_KEY
    }

    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)
        .apply { addPreference(preference) }

    @Test
    fun displayPreference() {
        controller.displayPreference(preferenceScreen)

        composeTestRule.setContent {
            preference.Content()
        }
        composeTestRule.onNodeWithText(TEXT).assertIsDisplayed()
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val TEXT = "Text"
    }
}
