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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settings.applications.appinfo.AppInfoDashboardFragment
import com.android.settings.notification.app.AppNotificationSettings
import com.android.settings.spa.notification.IAppNotificationRepository
import com.android.settingslib.spa.testutils.delay
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
class AppNotificationPreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockSession: MockitoSession

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val repository = object : IAppNotificationRepository {
        override fun getNotificationSummary(app: ApplicationInfo) = SUMMARY
    }

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(AppInfoDashboardFragment::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun title_displayed() {
        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.notifications_label))
            .assertIsDisplayed()
    }

    @Test
    fun summary_displayed() {
        setContent()

        composeTestRule.onNodeWithText(SUMMARY).assertIsDisplayed()
    }

    @Test
    fun onClick_startActivity() {
        setContent()

        composeTestRule.onRoot().performClick()
        composeTestRule.delay()

        ExtendedMockito.verify {
            AppInfoDashboardFragment.startAppInfoFragment(
                AppNotificationSettings::class.java,
                APP,
                context,
                AppInfoSettingsProvider.METRICS_CATEGORY,
            )
        }
    }

    private fun setContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppNotificationPreference(app = APP, repository = repository)
            }
        }
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
        const val UID = 123
        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
        }
        const val SUMMARY = "Summary"
    }
}