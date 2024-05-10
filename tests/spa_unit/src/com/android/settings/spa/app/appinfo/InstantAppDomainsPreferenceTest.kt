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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.quality.Strictness
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class InstantAppDomainsPreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockSession: MockitoSession

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var packageManager: PackageManager

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(Utils::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        whenever(context.packageManager).thenReturn(packageManager)
        Mockito.doReturn(context).`when`(context).createContextAsUser(any(), anyInt())
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

        composeTestRule
            .onNodeWithText(context.getString(R.string.app_launch_supported_domain_urls_title))
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun noDomain() {
        mockDomains(emptySet())

        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.domain_urls_summary_none))
            .assertIsDisplayed()
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

        composeTestRule.onDialogText(
            context.getString(R.string.app_launch_supported_domain_urls_title)
        ).assertIsDisplayed()
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

        val INSTANT_APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
            privateFlags = ApplicationInfo.PRIVATE_FLAG_INSTANT
        }
    }
}
