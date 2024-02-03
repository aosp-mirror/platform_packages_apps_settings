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

package com.android.settings.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardPreferenceTest {

    @get:Rule val composeTestRule = createComposeRule()
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun enableDismiss_whenEnable_shouldBeDisplayed() {
        composeTestRule.setContent { buildCardPreference(enableDismiss = true) }

        composeTestRule.onNodeWithContentDescription("Dismiss").assertIsDisplayed()
    }

    @Test
    fun enableDismiss_whenDisable_shouldBeDisplayed() {
        composeTestRule.setContent { buildCardPreference(enableDismiss = false) }

        composeTestRule.onNodeWithContentDescription("Dismiss").assertIsNotDisplayed()
    }

    @Test
    fun primaryButton_whenVisible_shouldBeDisplayed() {
        val expectedPrimaryButtonText = "You can see me"
        composeTestRule.setContent {
            buildCardPreference(
                primaryButtonText = expectedPrimaryButtonText,
                primaryButtonVisibility = true,
            )
        }

        composeTestRule.onNodeWithText(expectedPrimaryButtonText).assertIsDisplayed()
    }

    @Test
    fun primaryButton_whenInvisible_shouldBeDisplayed() {
        val expectedButtonText = "You cannot see me"
        composeTestRule.setContent {
            buildCardPreference(
                primaryButtonText = expectedButtonText,
                primaryButtonVisibility = false,
            )
        }

        composeTestRule.onNodeWithText(expectedButtonText).assertIsNotDisplayed()
    }

    @Test
    fun primaryButtonAction_whenClick_performAction() {
        val buttonText = "click me"
        var clicked = false
        composeTestRule.setContent {
            buildCardPreference(
                primaryButtonText = buttonText,
                primaryButtonVisibility = true,
                primaryButtonAction = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText(buttonText).performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun primaryButtonContentDescription_whenSet_shouldBeExists() {
        val expectedText = "this is a content description"
        val buttonText = "primary-button"
        composeTestRule.setContent {
            buildCardPreference(
                primaryButtonText = buttonText,
                primaryButtonContentDescription = expectedText,
                primaryButtonVisibility = true,
            )
        }

        composeTestRule.onNodeWithText(buttonText).assertContentDescriptionEquals(expectedText)
    }

    @Test
    fun secondaryButton_whenVisible_shouldBeDisplayed() {
        val expectedSecondaryButtonText = "You can see me"
        composeTestRule.setContent {
            buildCardPreference(
                secondaryButtonText = expectedSecondaryButtonText,
                secondaryButtonVisibility = true,
            )
        }

        composeTestRule.onNodeWithText(expectedSecondaryButtonText).assertIsDisplayed()
    }

    @Test
    fun secondaryButton_whenInvisible_shouldBeDisplayed() {
        val expectedButtonText = "You cannot see me"
        composeTestRule.setContent {
            buildCardPreference(
                secondaryButtonText = expectedButtonText,
                secondaryButtonVisibility = false,
            )
        }

        composeTestRule.onNodeWithText(expectedButtonText).assertIsNotDisplayed()
    }

    @Test
    fun secondaryButtonAction_whenClick_performAction() {
        val buttonText = "click me2"
        var clicked = false
        composeTestRule.setContent {
            buildCardPreference(
                secondaryButtonText = buttonText,
                secondaryButtonVisibility = true,
                secondaryButtonAction = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText(buttonText).performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun secondaryButtonContentDescription_whenSet_shouldBeExists() {
        val expectedText = "found bug yay"
        val buttonText = "secondary-button"
        composeTestRule.setContent {
            buildCardPreference(
                secondaryButtonText = buttonText,
                secondaryButtonContentDescription = expectedText,
                secondaryButtonVisibility = true,
            )
        }

        composeTestRule.onNodeWithText(buttonText).assertContentDescriptionEquals(expectedText)
    }

    @Test
    fun resetLayoutState_shouldRemoveThePrimaryButton() {
        val buttonText = "9527"
        val cardPreference =
            CardPreference(context)
                .apply {
                    primaryButtonText = buttonText
                    primaryButtonVisibility = true
                }
                .also { it.buildContent() }

        cardPreference.resetLayoutState()
        composeTestRule.setContent { cardPreference.Content() }

        composeTestRule.onNodeWithText(buttonText).assertDoesNotExist()
    }

    @Test
    fun resetLayoutState_shouldRemoveTheSecondaryButton() {
        val buttonText = "4567"
        val cardPreference =
            CardPreference(context)
                .apply {
                    secondaryButtonText = buttonText
                    secondaryButtonVisibility = true
                }
                .also { it.buildContent() }

        cardPreference.resetLayoutState()
        composeTestRule.setContent { cardPreference.Content() }

        composeTestRule.onNodeWithText(buttonText).assertDoesNotExist()
    }

    @Composable
    private fun buildCardPreference(
        iconResId: Int? = R.drawable.ic_battery_status_protected_24dp,
        primaryButtonText: String = "primary text",
        primaryButtonContentDescription: String? = "primary description",
        primaryButtonAction: () -> Unit = {},
        primaryButtonVisibility: Boolean = false,
        secondaryButtonText: String = "secondary button",
        secondaryButtonContentDescription: String? = null,
        secondaryButtonAction: () -> Unit = {},
        secondaryButtonVisibility: Boolean = false,
        enableDismiss: Boolean = true,
    ) =
        CardPreference(context)
            .apply {
                this.iconResId = iconResId
                this.primaryButtonText = primaryButtonText
                this.primaryButtonContentDescription = primaryButtonContentDescription
                this.primaryButtonAction = primaryButtonAction
                this.primaryButtonVisibility = primaryButtonVisibility
                this.secondaryButtonText = secondaryButtonText
                this.secondaryButtonContentDescription = secondaryButtonContentDescription
                this.secondaryButtonAction = secondaryButtonAction
                this.secondaryButtonVisibility = secondaryButtonVisibility
                this.enableDismiss(enableDismiss)
            }
            .also { it.buildContent() }
            .Content()
}
