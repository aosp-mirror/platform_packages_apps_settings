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

package com.android.settings.spa.app.appcompat

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.provider.DeviceConfig.NAMESPACE_WINDOW_MANAGER
import android.view.WindowManager.PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_OVERRIDE
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settings.applications.appcompat.UserAspectRatioDetails
import com.android.settings.applications.appinfo.AppInfoDashboardFragment
import com.android.settings.spa.app.appinfo.AppInfoSettingsProvider
import com.android.settings.testutils.TestDeviceConfig
import com.android.settingslib.spa.testutils.delay
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.quality.Strictness
import org.mockito.Mockito.`when` as whenever

/**
 * To run this test: atest SettingsSpaUnitTests:UserAspectRatioAppPreferenceTest
 */
@RunWith(AndroidJUnit4::class)
class UserAspectRatioAppPreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockSession: MockitoSession

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Spy
    private val resources = context.resources

    private val aspectRatioEnabledConfig =
        TestDeviceConfig(NAMESPACE_WINDOW_MANAGER, "enable_app_compat_aspect_ratio_user_settings")

    @Mock
    private lateinit var packageManager: PackageManager

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(UserAspectRatioDetails::class.java)
            .mockStatic(AppInfoDashboardFragment::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        whenever(context.resources).thenReturn(resources)
        whenever(context.packageManager).thenReturn(packageManager)
        // True is ignored but need this here or getBoolean will complain null object
        mockProperty(PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_OVERRIDE, true)
    }

    @After
    fun tearDown() {
        aspectRatioEnabledConfig.reset()
        mockSession.finishMocking()
    }

    @Test
    fun whenConfigIsFalse_notDisplayed() {
        setConfig(false)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun whenCannotDisplayAspectRatioUi_notDisplayed() {
        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun whenCanDisplayAspectRatioUiAndConfigFalse_notDisplayed() {
        setConfig(false)
        whenever(packageManager.queryIntentActivities(any(), anyInt()))
            .thenReturn(listOf(RESOLVE_INFO))

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun whenCannotDisplayAspectRatioUiAndConfigTrue_notDisplayed() {
        setConfig(true)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun whenCanDisplayAspectRatioUiAndConfigTrue_Displayed() {
        setConfig(true)
        whenever(packageManager.queryIntentActivities(any(), anyInt()))
            .thenReturn(listOf(RESOLVE_INFO))

        setContent()

        composeTestRule.onNode(
            hasTextExactly(
                context.getString(R.string.aspect_ratio_experimental_title),
                context.getString(R.string.user_aspect_ratio_app_default)
            ),
        ).assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun onClick_startActivity() {
        setConfig(true)
        whenever(packageManager.queryIntentActivities(any(), anyInt()))
            .thenReturn(listOf(RESOLVE_INFO))

        setContent()
        composeTestRule.onRoot().performClick()

        ExtendedMockito.verify {
            AppInfoDashboardFragment.startAppInfoFragment(
                UserAspectRatioDetails::class.java,
                APP,
                context,
                AppInfoSettingsProvider.METRICS_CATEGORY,
            )
        }
    }

    private fun setConfig(enabled: Boolean) {
        whenever(resources.getBoolean(
            com.android.internal.R.bool.config_appCompatUserAppAspectRatioSettingsIsEnabled
        )).thenReturn(enabled)
        aspectRatioEnabledConfig.override(enabled)
    }

    private fun setContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                UserAspectRatioAppPreference(APP)
            }
        }
        composeTestRule.delay()
    }

    private fun mockProperty(propertyName: String, value: Boolean) {
        val prop = PackageManager.Property(
            propertyName, value, PACKAGE_NAME, "" /* className */)
        whenever(packageManager.getProperty(propertyName, PACKAGE_NAME)).thenReturn(prop)
    }

    private companion object {
        const val PACKAGE_NAME = "com.test.mypackage"
        const val UID = 123
        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
        }
        private val RESOLVE_INFO = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = PACKAGE_NAME
            }
        }
    }
}