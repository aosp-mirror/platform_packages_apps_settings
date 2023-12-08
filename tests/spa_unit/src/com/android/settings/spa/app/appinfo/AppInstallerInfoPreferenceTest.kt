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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.applications.AppStoreUtil
import com.android.settingslib.applications.AppUtils
import com.android.settingslib.spa.testutils.delay
import com.android.settingslib.spa.testutils.waitUntilExists
import com.android.settingslib.spaprivileged.model.app.userHandle
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoSession
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
class AppInstallerInfoPreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockSession: MockitoSession

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        doNothing().whenever(mock).startActivityAsUser(any(), any())
    }

    @Before
    fun setUp() {
        mockSession = mockitoSession()
            .initMocks(this)
            .mockStatic(AppStoreUtil::class.java)
            .mockStatic(Utils::class.java)
            .mockStatic(AppUtils::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        whenever(AppStoreUtil.getInstallerPackageName(any(), eq(PACKAGE_NAME)))
            .thenReturn(INSTALLER_PACKAGE_NAME)
        whenever(AppStoreUtil.getAppStoreLink(context, INSTALLER_PACKAGE_NAME, PACKAGE_NAME))
            .thenReturn(STORE_LINK)
        whenever(Utils.getApplicationLabel(context, INSTALLER_PACKAGE_NAME))
            .thenReturn(INSTALLER_PACKAGE_LABEL)
        whenever(AppUtils.isMainlineModule(any(), eq(PACKAGE_NAME))).thenReturn(false)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun whenNoInstaller_notDisplayed() {
        whenever(AppStoreUtil.getInstallerPackageName(any(), eq(PACKAGE_NAME))).thenReturn(null)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun whenInstallerLabelIsNull_notDisplayed() {
        whenever(Utils.getApplicationLabel(context, INSTALLER_PACKAGE_NAME)).thenReturn(null)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun whenIsMainlineModule_notDisplayed() {
        whenever(AppUtils.isMainlineModule(any(), eq(PACKAGE_NAME))).thenReturn(true)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun whenStoreLinkIsNull_disabled() {
        whenever(AppStoreUtil.getAppStoreLink(context, INSTALLER_PACKAGE_NAME, PACKAGE_NAME))
            .thenReturn(null)

        setContent()

        composeTestRule.waitUntilExists(preferenceNode.and(isNotEnabled()))
    }

    @Test
    fun whenIsInstantApp_hasSummaryForInstant() {
        val instantApp = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
            privateFlags = ApplicationInfo.PRIVATE_FLAG_INSTANT
        }

        setContent(instantApp)

        composeTestRule.waitUntilExists(hasText("More info on installer label").and(isEnabled()))
    }

    @Test
    fun whenNotInstantApp() {
        setContent()

        composeTestRule.waitUntilExists(hasText("App installed from installer label"))
        composeTestRule.waitUntilExists(preferenceNode.and(isEnabled()))
    }

    @Test
    fun whenClick_startActivity() {
        setContent()
        composeTestRule.delay()

        composeTestRule.onRoot().performClick()
        composeTestRule.delay()

        verify(context).startActivityAsUser(STORE_LINK, APP.userHandle)
    }

    private fun setContent(app: ApplicationInfo = APP) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppInstallerInfoPreference(app)
            }
        }
    }

    private val preferenceNode = hasText(context.getString(R.string.app_install_details_title))

    private companion object {
        const val PACKAGE_NAME = "package.name"
        const val INSTALLER_PACKAGE_NAME = "installer"
        const val INSTALLER_PACKAGE_LABEL = "installer label"
        val STORE_LINK = Intent("store/link")
        const val UID = 123
        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
        }
    }
}
