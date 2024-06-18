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
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.FakeFeatureFlagsImpl
import android.content.pm.Flags
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.platform.test.flag.junit.SetFlagsRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settings.flags.Flags as SettingsFlags
import com.android.settingslib.applications.AppUtils
import com.android.settingslib.spa.testutils.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.kotlin.any
import org.mockito.quality.Strictness
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class AppButtonsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule val setFlagsRule: SetFlagsRule = SetFlagsRule()

    private lateinit var mockSession: MockitoSession

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var packageInfoPresenter: PackageInfoPresenter

    @Mock
    private lateinit var packageManager: PackageManager

    @Mock
    private lateinit var packageInstaller: PackageInstaller

    private val featureFlags = FakeFeatureFlagsImpl()
    private val isHibernationSwitchEnabledStateFlow = MutableStateFlow(true)

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(AppUtils::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        whenever(packageInfoPresenter.context).thenReturn(context)
        whenever(packageInfoPresenter.packageName).thenReturn(PACKAGE_NAME)
        whenever(packageInfoPresenter.userPackageManager).thenReturn(packageManager)
        whenever(packageManager.getApplicationLabel(any())).thenReturn(APP_LABEL)
        whenever(packageManager.packageInstaller).thenReturn(packageInstaller)
        whenever(packageManager.getPackageInfo(PACKAGE_NAME, 0)).thenReturn(PACKAGE_INFO)
        whenever(AppUtils.isMainlineModule(packageManager, PACKAGE_NAME)).thenReturn(false)
        featureFlags.setFlag(Flags.FLAG_ARCHIVING, true)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun isMainlineModule_notDisplayed() {
        whenever(AppUtils.isMainlineModule(packageManager, PACKAGE_NAME)).thenReturn(true)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun isNormalApp_displayed() {
        setContent()

        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun launchButton_displayed_archivingDisabled() {
        whenever(packageManager.getLaunchIntentForPackage(PACKAGE_NAME)).thenReturn(Intent())
        featureFlags.setFlag(Flags.FLAG_ARCHIVING, false)
        setFlagsRule.disableFlags(SettingsFlags.FLAG_APP_ARCHIVING)
        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.launch_instant_app))
            .assertIsDisplayed()
    }

    @Test
    fun launchButton_notDisplayed_archivingEnabled() {
        whenever(packageManager.getLaunchIntentForPackage(PACKAGE_NAME)).thenReturn(Intent())
        featureFlags.setFlag(Flags.FLAG_ARCHIVING, true)
        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.launch_instant_app))
            .assertIsNotDisplayed()
    }

    @Test
    fun uninstallButton_enabled_whenAppIsArchived() {
        whenever(packageManager.getLaunchIntentForPackage(PACKAGE_NAME)).thenReturn(Intent())
        featureFlags.setFlag(Flags.FLAG_ARCHIVING, true)
        val packageInfo = PackageInfo().apply {
            applicationInfo = ApplicationInfo().apply {
                packageName = PACKAGE_NAME
                isArchived = true
            }
            packageName = PACKAGE_NAME
        }
        setContent(packageInfo)

        composeTestRule.onNodeWithText(context.getString(R.string.uninstall_text)).assertIsEnabled()
    }

    @Test
    fun archiveButton_displayed_whenAppIsNotArchived() {
        featureFlags.setFlag(Flags.FLAG_ARCHIVING, true)
        val packageInfo = PackageInfo().apply {
            applicationInfo = ApplicationInfo().apply {
                packageName = PACKAGE_NAME
                isArchived = false
            }
            packageName = PACKAGE_NAME
        }
        setContent(packageInfo)

        composeTestRule.onNodeWithText(context.getString(R.string.archive)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.restore)).assertIsNotDisplayed()
    }

    @Test
    fun restoreButton_displayed_whenAppIsArchived() {
        featureFlags.setFlag(Flags.FLAG_ARCHIVING, true)
        val packageInfo = PackageInfo().apply {
            applicationInfo = ApplicationInfo().apply {
                packageName = PACKAGE_NAME
                isArchived = true
            }
            packageName = PACKAGE_NAME
        }
        setContent(packageInfo)

        composeTestRule.onNodeWithText(context.getString(R.string.restore)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.archive)).assertIsNotDisplayed()
    }

    private fun setContent(packageInfo: PackageInfo = PACKAGE_INFO) {
        whenever(packageInfoPresenter.flow).thenReturn(MutableStateFlow(packageInfo))
        composeTestRule.setContent {
            AppButtons(packageInfoPresenter, isHibernationSwitchEnabledStateFlow, featureFlags)
        }

        composeTestRule.delay()
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
        const val APP_LABEL = "App label"
        val PACKAGE_INFO = PackageInfo().apply {
            applicationInfo = ApplicationInfo().apply {
                packageName = PACKAGE_NAME
            }
            packageName = PACKAGE_NAME
        }
    }
}
