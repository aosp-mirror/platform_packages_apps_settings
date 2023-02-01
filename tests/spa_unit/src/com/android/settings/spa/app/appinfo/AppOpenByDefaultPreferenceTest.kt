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
import android.content.pm.ResolveInfo
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
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
import com.android.settings.testutils.mockAsUser
import com.android.settingslib.spaprivileged.framework.common.domainVerificationManager
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.doReturn
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class AppOpenByDefaultPreferenceTest {
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var packageManager: PackageManager

    @Mock
    private lateinit var domainVerificationManager: DomainVerificationManager

    @Mock
    private lateinit var allowedUserState: DomainVerificationUserState

    @Mock
    private lateinit var notAllowedUserState: DomainVerificationUserState

    @Before
    fun setUp() {
        whenever(context.packageManager).thenReturn(packageManager)
        context.mockAsUser()
        whenever(context.domainVerificationManager).thenReturn(domainVerificationManager)
        whenever(allowedUserState.isLinkHandlingAllowed).thenReturn(true)
        whenever(notAllowedUserState.isLinkHandlingAllowed).thenReturn(false)
    }

    @Test
    fun instantApp_notDisplay() {
        val instantApp = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            privateFlags = ApplicationInfo.PRIVATE_FLAG_INSTANT
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppOpenByDefaultPreference(instantApp)
            }
        }

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun browserApp_notDisplay() {
        val browserApp = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            privateFlags = ApplicationInfo.PRIVATE_FLAG_INSTANT
        }
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo()
            handleAllWebDataURI = true
        }
        whenever(packageManager.queryIntentActivitiesAsUser(any(), anyInt(), anyInt()))
            .thenReturn(listOf(resolveInfo))

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppOpenByDefaultPreference(browserApp)
            }
        }

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun allowedUserState_alwaysOpen() {
        whenever(domainVerificationManager.getDomainVerificationUserState(PACKAGE_NAME))
            .thenReturn(allowedUserState)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppOpenByDefaultPreference(INSTALLED_ENABLED_APP)
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.launch_by_default))
            .assertIsDisplayed()
            .assertIsEnabled()
        composeTestRule.onNodeWithText(context.getString(R.string.app_link_open_always))
            .assertIsDisplayed()
    }

    @Test
    fun notAllowedUserState_neverOpen() {
        whenever(domainVerificationManager.getDomainVerificationUserState(PACKAGE_NAME))
            .thenReturn(notAllowedUserState)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppOpenByDefaultPreference(INSTALLED_ENABLED_APP)
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.launch_by_default))
            .assertIsDisplayed()
            .assertIsEnabled()
        composeTestRule.onNodeWithText(context.getString(R.string.app_link_open_never))
            .assertIsDisplayed()
    }

    @Test
    fun notInstalledApp_disabled() {
        val notInstalledApp = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppOpenByDefaultPreference(notInstalledApp)
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.launch_by_default))
            .assertIsNotEnabled()
    }

    @Test
    fun notEnabledApp_disabled() {
        val notEnabledApp = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            flags = ApplicationInfo.FLAG_INSTALLED
            enabled = false
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppOpenByDefaultPreference(notEnabledApp)
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.launch_by_default))
            .assertIsNotEnabled()
    }

    private companion object {
        const val PACKAGE_NAME = "package name"

        val INSTALLED_ENABLED_APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            flags = ApplicationInfo.FLAG_INSTALLED
            enabled = true
        }
    }
}
