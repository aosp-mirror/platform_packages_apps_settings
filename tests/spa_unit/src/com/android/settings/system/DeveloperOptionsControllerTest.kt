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

package com.android.settings.system

import android.content.Context
import android.content.Intent
import android.os.UserManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.SettingsActivity
import com.android.settings.core.BasePreferenceController
import com.android.settings.development.DevelopmentSettingsDashboardFragment
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class DeveloperOptionsControllerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockUserManager = mock<UserManager>()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { userManager } doReturn mockUserManager
        doNothing().whenever(mock).startActivity(any())
    }

    private val controller = DeveloperOptionsController(context, TEST_KEY)

    @Test
    fun getAvailabilityStatus_isAdminUser_returnAvailable() {
        mockUserManager.stub {
            on { isAdminUser } doReturn true
        }

        val availabilityStatus = controller.getAvailabilityStatus()

        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_notAdminUser_returnDisabledForUser() {
        mockUserManager.stub {
            on { isAdminUser } doReturn false
        }

        val availabilityStatus = controller.getAvailabilityStatus()

        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.DISABLED_FOR_USER)
    }

    @Test
    fun title_isDisplayed() {
        composeTestRule.setContent {
            controller.DeveloperOptionsPreference()
        }

        composeTestRule.onNodeWithText(
            context.getString(com.android.settingslib.R.string.development_settings_title)
        ).assertIsDisplayed()
    }

    @Test
    fun onClick_launchDevelopmentSettingsDashboardFragment() {
        composeTestRule.setContent {
            controller.DeveloperOptionsPreference()
        }

        composeTestRule.onNodeWithText(
            context.getString(com.android.settingslib.R.string.development_settings_title)
        ).performClick()

        val intent = argumentCaptor<Intent> {
            verify(context).startActivity(capture())
        }.firstValue
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
            .isEqualTo(DevelopmentSettingsDashboardFragment::class.qualifiedName)
    }

    private companion object {
        const val TEST_KEY = "test_key"
    }
}
