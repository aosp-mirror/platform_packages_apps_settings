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

package com.android.settings.print

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.SettingsActivity
import com.android.settings.print.PrintRepository.PrintServiceDisplayInfo
import com.android.settings.print.PrintSettingsFragment.EXTRA_CHECKED
import com.android.settings.print.PrintSettingsFragment.EXTRA_SERVICE_COMPONENT_NAME
import com.android.settings.print.PrintSettingsFragment.EXTRA_TITLE
import com.android.settings.print.PrintSettingsPageProvider.AddPrintService
import com.android.settings.print.PrintSettingsPageProvider.PrintService
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class PrintSettingsPageProviderTest {
    @get:Rule val composeTestRule = createComposeRule()

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            doNothing().whenever(mock).startActivity(any())
        }

    private val displayInfo =
        PrintServiceDisplayInfo(
            title = TITLE,
            isEnabled = true,
            summary = SUMMARY,
            icon = context.getDrawable(R.drawable.ic_settings_print)!!,
            componentName = "ComponentName",
        )

    @Test
    fun printService_titleDisplayed() {
        composeTestRule.setContent { PrintService(displayInfo) }

        composeTestRule.onNodeWithText(TITLE).isDisplayed()
    }

    @Test
    fun printService_summaryDisplayed() {
        composeTestRule.setContent { PrintService(displayInfo) }

        composeTestRule.onNodeWithText(SUMMARY).isDisplayed()
    }

    @Test
    fun printService_onClick() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) { PrintService(displayInfo) }
        }

        composeTestRule.onNodeWithText(TITLE).performClick()

        verify(context)
            .startActivity(
                argThat {
                    val fragment = getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)
                    val arguments = getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)!!
                    fragment == PrintServiceSettingsFragment::class.qualifiedName &&
                        arguments.getBoolean(EXTRA_CHECKED) == displayInfo.isEnabled &&
                        arguments.getString(EXTRA_TITLE) == displayInfo.title &&
                        arguments.getString(EXTRA_SERVICE_COMPONENT_NAME) ==
                            displayInfo.componentName
                }
            )
    }

    @Test
    fun addPrintService_onClick() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AddPrintService(flowOf(SEARCH_URI))
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.print_menu_item_add_service))
            .performClick()

        verify(context).startActivity(argThat { data == Uri.parse(SEARCH_URI) })
    }

    private companion object {
        const val TITLE = "Title"
        const val SUMMARY = "Summary"
        const val SEARCH_URI = "search.uri"
    }
}
