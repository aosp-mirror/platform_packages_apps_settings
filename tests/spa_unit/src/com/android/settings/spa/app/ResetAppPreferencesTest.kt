/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.spa.app

import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spa.widget.scaffold.MoreOptionsScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResetAppPreferencesTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun resetAppPreferences_titleIsDisplayed() {
        setResetAppPreferences()

        composeTestRule.onNodeWithText(context.getString(R.string.reset_app_preferences))
            .assertIsDisplayed()
    }

    private fun setResetAppPreferences() {
        val fakeMoreOptionsScope = object : MoreOptionsScope() {
            override fun dismiss() {}
        }
        composeTestRule.setContent {
            fakeMoreOptionsScope.ResetAppPreferences {}
        }
    }

    @Test
    fun resetAppDialogPresenter_confirmButtonDisplayed() {
        setAndOpenDialog()

        composeTestRule.onNodeWithText(context.getString(R.string.reset_app_preferences_button))
            .assertIsDisplayed()
    }

    @Test
    fun resetAppDialogPresenter_titleDisplayed() {
        setAndOpenDialog()

        composeTestRule.onNodeWithText(context.getString(R.string.reset_app_preferences_title))
            .assertIsDisplayed()
    }

    @Test
    fun resetAppDialogPresenter_textDisplayed() {
        setAndOpenDialog()

        composeTestRule.onNodeWithText(context.getString(R.string.reset_app_preferences_desc))
            .assertIsDisplayed()
    }

    private fun setAndOpenDialog() {
        composeTestRule.setContent {
            val dialogPresenter = rememberResetAppDialogPresenter()
            LaunchedEffect(Unit) {
                dialogPresenter.open()
            }
        }
    }
}
