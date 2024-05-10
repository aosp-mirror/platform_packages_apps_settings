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
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ResolveInfoFlags
import android.content.pm.ResolveInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.FakeFeatureFactory
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class AppTimeSpentPreferenceTest {
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var packageManager: PackageManager

    private val fakeFeatureFactory = FakeFeatureFactory()
    private val appFeatureProvider = fakeFeatureFactory.mockApplicationFeatureProvider

    @Before
    fun setUp() {
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(appFeatureProvider.getTimeSpentInApp(PACKAGE_NAME)).thenReturn(TIME_SPENT)
    }

    private fun mockActivitiesQueryResult(resolveInfos: List<ResolveInfo>) {
        whenever(
            packageManager.queryIntentActivitiesAsUser(any(), any<ResolveInfoFlags>(), anyInt())
        ).thenReturn(resolveInfos)
    }

    @Test
    fun noIntentHandler_notDisplay() {
        mockActivitiesQueryResult(emptyList())

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppTimeSpentPreference(INSTALLED_APP)
            }
        }

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun hasIntentHandler_notSystemApp_notDisplay() {
        mockActivitiesQueryResult(listOf(ResolveInfo()))

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppTimeSpentPreference(INSTALLED_APP)
            }
        }

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun installedApp_enabled() {
        mockActivitiesQueryResult(listOf(MATCHED_RESOLVE_INFO))

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppTimeSpentPreference(INSTALLED_APP)
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.time_spent_in_app_pref_title))
            .assertIsDisplayed()
            .assertIsEnabled()
        composeTestRule.onNodeWithText(TIME_SPENT).assertIsDisplayed()
    }

    @Test
    fun notInstalledApp_disabled() {
        mockActivitiesQueryResult(listOf(MATCHED_RESOLVE_INFO))
        val notInstalledApp = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppTimeSpentPreference(notInstalledApp)
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.time_spent_in_app_pref_title))
            .assertIsNotEnabled()
    }

    companion object {
        private const val PACKAGE_NAME = "package name"
        private const val TIME_SPENT = "15 minutes"

        private val INSTALLED_APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            flags = ApplicationInfo.FLAG_INSTALLED
        }

        private val MATCHED_RESOLVE_INFO = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                applicationInfo = ApplicationInfo().apply {
                    flags = ApplicationInfo.FLAG_SYSTEM
                }
            }
        }
    }
}
