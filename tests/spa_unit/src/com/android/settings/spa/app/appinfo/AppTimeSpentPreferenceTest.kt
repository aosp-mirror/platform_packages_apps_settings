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

package com.android.settings.spa.app.appinfo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settingslib.spa.testutils.waitUntilExists
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AppTimeSpentPreferenceTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val mockPackageManager =
        mock<PackageManager> { on { wellbeingPackageName } doReturn WELLBEING_PACKAGE_NAME }

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getString(com.android.internal.R.string.config_systemWellbeing) } doReturn
                WELLBEING_PACKAGE_NAME
            on { packageManager } doReturn mockPackageManager
        }

    private val fakeFeatureFactory = FakeFeatureFactory()
    private val appFeatureProvider = fakeFeatureFactory.mockApplicationFeatureProvider

    @Before
    fun setUp() {
        whenever(appFeatureProvider.getTimeSpentInApp(PACKAGE_NAME)).thenReturn(TIME_SPENT)
    }

    private fun mockActivityQueryResult(resolveInfo: ResolveInfo?) {
        mockPackageManager.stub {
            on { resolveActivityAsUser(any(), any<Int>(), any()) } doReturn resolveInfo
        }
    }

    @Test
    fun noIntentHandler_notDisplay() {
        mockActivityQueryResult(null)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppTimeSpentPreference(INSTALLED_APP)
            }
        }

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun resolveActivityAsUser_calledWithWellbeingPackageName() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppTimeSpentPreference(INSTALLED_APP)
            }
        }

        verify(mockPackageManager)
            .resolveActivityAsUser(
                argThat { `package` == WELLBEING_PACKAGE_NAME },
                any<Int>(),
                any(),
            )
    }

    @Test
    fun installedApp_enabled() {
        mockActivityQueryResult(ResolveInfo())

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppTimeSpentPreference(INSTALLED_APP)
            }
        }

        composeTestRule.waitUntilExists(
            hasText(context.getString(R.string.time_spent_in_app_pref_title)) and isEnabled()
        )
        composeTestRule.onNodeWithText(TIME_SPENT).assertIsDisplayed()
    }

    @Test
    fun notInstalledApp_disabled() {
        mockActivityQueryResult(ResolveInfo())
        val notInstalledApp = ApplicationInfo().apply { packageName = PACKAGE_NAME }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppTimeSpentPreference(notInstalledApp)
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.time_spent_in_app_pref_title))
            .assertIsNotEnabled()
    }

    @Test
    fun onClick_startActivityWithWellbeingPackageName() {
        mockActivityQueryResult(ResolveInfo())

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppTimeSpentPreference(INSTALLED_APP)
            }
        }
        composeTestRule.waitUntilExists(
            hasText(context.getString(R.string.time_spent_in_app_pref_title)) and isEnabled()
        )
        composeTestRule.onRoot().performClick()

        verify(context).startActivityAsUser(argThat { `package` == WELLBEING_PACKAGE_NAME }, any())
    }

    companion object {
        private const val PACKAGE_NAME = "package.name"
        private const val TIME_SPENT = "15 minutes"
        private const val WELLBEING_PACKAGE_NAME = "wellbeing"

        private val INSTALLED_APP =
            ApplicationInfo().apply {
                packageName = PACKAGE_NAME
                flags = ApplicationInfo.FLAG_INSTALLED
            }
    }
}
