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
import android.content.pm.CrossProfileApps
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settings.applications.appinfo.AppInfoDashboardFragment
import com.android.settings.applications.specialaccess.interactacrossprofiles.InteractAcrossProfilesDetails
import com.android.settingslib.spa.testutils.delay
import com.android.settingslib.spa.testutils.waitUntilExists
import com.android.settingslib.spaprivileged.framework.common.crossProfileApps
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.quality.Strictness
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class InteractAcrossProfilesDetailsPreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockSession: MockitoSession

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var crossProfileApps: CrossProfileApps

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(InteractAcrossProfilesDetails::class.java)
            .mockStatic(AppInfoDashboardFragment::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        whenever(context.crossProfileApps).thenReturn(crossProfileApps)
        whenever(InteractAcrossProfilesDetails.getPreferenceSummary(context, PACKAGE_NAME))
            .thenReturn("")
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    private fun mockCanConfig(canConfig: Boolean) {
        whenever(crossProfileApps.canUserAttemptToConfigureInteractAcrossProfiles(PACKAGE_NAME))
            .thenReturn(canConfig)
    }

    @Test
    fun cannotConfig_notDisplayed() {
        mockCanConfig(false)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun canConfig_displayed() {
        mockCanConfig(true)

        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.interact_across_profiles_title))
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun hasSummary() {
        mockCanConfig(true)
        whenever(InteractAcrossProfilesDetails.getPreferenceSummary(context, PACKAGE_NAME))
            .thenReturn(SUMMARY)

        setContent()

        composeTestRule.waitUntilExists(hasText(SUMMARY))
    }

    @Ignore
    @Test
    fun whenClick_startActivity() {
        mockCanConfig(true)

        setContent()
        composeTestRule.onRoot().performClick()
        composeTestRule.delay()

        ExtendedMockito.verify {
            AppInfoDashboardFragment.startAppInfoFragment(
                InteractAcrossProfilesDetails::class.java,
                APP,
                context,
                AppInfoSettingsProvider.METRICS_CATEGORY,
            )
        }
    }

    private fun setContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                InteractAcrossProfilesDetailsPreference(APP)
            }
        }
    }

    private companion object {
        const val PACKAGE_NAME = "packageName"
        const val UID = 123
        const val SUMMARY = "summary"

        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
        }
    }
}
