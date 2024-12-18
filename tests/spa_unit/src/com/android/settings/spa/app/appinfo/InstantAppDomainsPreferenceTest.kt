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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settings.Utils
import com.android.settingslib.spa.testutils.delay
import com.android.settingslib.spa.testutils.onDialogText
import com.android.settingslib.spa.testutils.waitUntilExists
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoSession
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
class InstantAppDomainsPreferenceTest {
    @get:Rule val composeTestRule = createComposeRule()

    private lateinit var mockSession: MockitoSession

    private val packageManager = mock<PackageManager>()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { packageManager } doReturn packageManager
        doReturn(mock).whenever(mock).createContextAsUser(any(), any())
    }

    @Before
    fun setUp() {
        mockSession =
            ExtendedMockito.mockitoSession()
                .mockStatic(Utils::class.java)
                .strictness(Strictness.LENIENT)
                .startMocking()

        mockDomains(emptySet())
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    private fun mockDomains(domains: Set<String>) {
        whenever(Utils.getHandledDomains(packageManager, PACKAGE_NAME)).thenReturn(domains)
    }

    @Test
    fun notInstantApp_notDisplayed() {
        val app = ApplicationInfo()

        setContent(app)

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun title_displayed() {
        setContent()

        composeTestRule.waitUntilExists(
            hasText(context.getString(R.string.app_launch_supported_domain_urls_title)) and
                isEnabled())
    }

    @Test
    fun noDomain() {
        mockDomains(emptySet())

        setContent()

        composeTestRule.waitUntilExists(
            hasText(context.getString(R.string.domain_urls_summary_none)))
    }

    @Test
    fun oneDomain() {
        mockDomains(setOf("abc"))

        setContent()

        composeTestRule.onNodeWithText("Open abc").assertIsDisplayed()
    }

    @Test
    fun twoDomains() {
        mockDomains(setOf("abc", "def"))

        setContent()

        composeTestRule.onNodeWithText("Open abc and other URLs").assertIsDisplayed()
    }

    @Test
    fun whenClicked() {
        mockDomains(setOf("abc", "def"))

        setContent()
        composeTestRule.onRoot().performClick()
        composeTestRule.delay()

        composeTestRule
            .onDialogText(context.getString(R.string.app_launch_supported_domain_urls_title))
            .assertIsDisplayed()
        composeTestRule.onDialogText("abc").assertIsDisplayed()
        composeTestRule.onDialogText("def").assertIsDisplayed()
    }

    private fun setContent(app: ApplicationInfo = INSTANT_APP) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                InstantAppDomainsPreference(app)
            }
        }
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
        const val UID = 123

        val INSTANT_APP =
            ApplicationInfo().apply {
                packageName = PACKAGE_NAME
                uid = UID
                privateFlags = ApplicationInfo.PRIVATE_FLAG_INSTANT
            }
    }
}
